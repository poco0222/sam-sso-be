/**
 * @file 编码规则工具类
 * @author Codex
 * @date 2026-03-11
 */
package com.yr.system.utils;

import com.yr.common.exception.CustomException;
import com.yr.common.utils.DateUtils;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.constant.CodeRuleConstant;
import com.yr.system.domain.entity.SysCodeRule;
import com.yr.system.domain.entity.SysCodeRuleDetail;
import com.yr.system.domain.entity.SysCodeRuleLine;
import com.yr.system.domain.entity.SysCodeRuleValue;
import com.yr.system.service.ISysCodeRuleDetailService;
import com.yr.system.service.ISysCodeRuleLineService;
import com.yr.system.service.ISysCodeRuleService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-11-4 9:51
 * @description 编码规则工具类
 */

@Component
public class CodeRuleUtils {

    /** 使用 Redis 脚本在服务端直接覆盖序列值，避免 delete + increment 的竞态窗口。 */
    private static final DefaultRedisScript<Long> SET_SEQUENCE_SCRIPT = buildSetSequenceScript();

    /** 在同一重置窗口内只允许首个请求执行重置，其余请求继续递增。 */
    private static final DefaultRedisScript<Long> ADVANCE_SEQUENCE_SCRIPT = buildAdvanceSequenceScript();

    private final ISysCodeRuleService codeRuleService;
    private final ISysCodeRuleLineService codeRuleLineService;
    private final ISysCodeRuleDetailService codeRuleDetailService;
    private final RedisTemplate<String, String> redisTemplate;

    public CodeRuleUtils(ISysCodeRuleService codeRuleService,
                         ISysCodeRuleLineService codeRuleLineService,
                         ISysCodeRuleDetailService codeRuleDetailService,
                         RedisTemplate<String, String> redisTemplate) {
        this.codeRuleService = codeRuleService;
        this.codeRuleLineService = codeRuleLineService;
        this.codeRuleDetailService = codeRuleDetailService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 生成全局级编码
     *
     * @param ruleCode    规则编码
     * @param variableMap 如果编码规则中使用了变量，变量的值会从Map中获取，如果获取不到会使用空串代替
     * @return
     */
    public String generateCode(String ruleCode, Map<String, String> variableMap) {
        return this.generateCode(ruleCode, CodeRuleConstant.LevelCode.GLOBAL, CodeRuleConstant.LevelCode.GLOBAL, variableMap);
    }

    /**
     * 生成指定层级的编码
     *
     * @param ruleCode    规则编码
     * @param levelCode   层级编码 {@link CodeRuleConstant.LevelCode}
     * @param levelValue  层级值
     *                    levelCode={@link CodeRuleConstant.LevelCode#GLOBAL}时，levelValue默认为GLOBAL
     *                    levelCode={@link CodeRuleConstant.LevelCode#ORGANIZATION}时，levelValue默认为SecurityUtils.getOrgId()
     *                    levelCode={@link CodeRuleConstant.LevelCode#DEPARTMENT}时，需要手动传入levelValue的值
     *                    levelCode={@link CodeRuleConstant.LevelCode#CUSTOM}时，需要手动传入在页面维护的levelValue
     * @param variableMap 如果编码规则中使用了变量，变量的值会从Map中获取，如果获取不到会使用空串代替
     * @return 编码
     */
    public String generateCode(String ruleCode, String levelCode, String levelValue, Map<String, String> variableMap) {
        // 校验levelCode
        CodeRuleConstant.LevelCode.contains(levelCode);
        levelValue = this.getLevelValue(levelCode, levelValue);
        List<SysCodeRuleDetail> codeRuleDetailList = this.getRuleCodeDetail(ruleCode, levelCode, levelValue);
        // 根据获取到的编码规则定义，生成编码规则
        return generate(ruleCode, levelCode, levelValue, variableMap, codeRuleDetailList);
    }

    /**
     * 设置默认levelValue
     *
     * @param levelCode
     * @param levelValue
     * @return
     */
    private String getLevelValue(String levelCode, String levelValue) {
        if (CodeRuleConstant.LevelCode.GLOBAL.equals(levelCode)) {
            return CodeRuleConstant.LevelCode.GLOBAL;
        } else if (CodeRuleConstant.LevelCode.ORGANIZATION.equals(levelCode)) {
            return String.valueOf(SecurityUtils.getOrgId());
        } else if (StringUtils.isBlank(levelValue)) {
            throw new CustomException("levelValue不能为空");
        }
        return levelValue;
    }

    /**
     * 获取编码规则明细
     *
     * @param ruleCode   规则编码
     * @param levelCode  层级CODE
     * @param levelValue 层级值
     * @return
     */
    private List<SysCodeRuleDetail> getRuleCodeDetail(String ruleCode, String levelCode, String levelValue) {
        // 如果不是自定义层级，那么levelValue就是levelCode
        if (!CodeRuleConstant.LevelCode.CUSTOM.equals(levelCode)) {
            levelValue = levelCode;
        }
        // 从缓存中找
        List<SysCodeRuleDetail> codeRuleDetailList = codeRuleService.getRuleCodeDetailListFromCache(CodeRuleConstant.generateCacheKey(ruleCode, levelCode, levelValue));
        // 缓存中找不到再从数据库找
        if (CollectionUtils.isEmpty(codeRuleDetailList)) {
            if (redisTemplate.hasKey(CodeRuleConstant.generateFailFastKey(ruleCode, levelCode, levelValue))) {
                // 如果之前在数据库中找过并且没找到，刷新失败时间
                redisTemplate.expire(CodeRuleConstant.generateFailFastKey(ruleCode, levelCode, levelValue), 3600, TimeUnit.SECONDS);
            } else {
                // 查询数据库
                codeRuleDetailList = codeRuleDetailService.listRuleCodeDetail(ruleCode, levelCode, levelValue);
                if (CollectionUtils.isEmpty(codeRuleDetailList)) {
                    // 未数据库查到，标记为失败编码
                    redisTemplate.opsForValue().set(CodeRuleConstant.generateFailFastKey(ruleCode, levelCode, levelValue), "1");
                    redisTemplate.expire(CodeRuleConstant.generateFailFastKey(ruleCode, levelCode, levelValue), 3600, TimeUnit.SECONDS);
                } else {
                    // 从数据库查到了，缓存到redis
                    SysCodeRule codeRule = new SysCodeRule();
                    codeRule.setRuleCode(ruleCode);
                    SysCodeRuleLine codeRuleLine = new SysCodeRuleLine();
                    codeRuleLine.setLevelCode(levelCode);
                    codeRuleLine.setLevelValue(levelValue);
                    codeRuleService.saveCache(codeRule, codeRuleLine, codeRuleDetailList);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(codeRuleDetailList)) {
            // 判断是否已经使用，如果没有使用过，更新编码规则行的使用标记
            if (!redisTemplate.hasKey(CodeRuleConstant.generateUsedKey(ruleCode, levelCode, levelValue))) {
                codeRuleLineService.updateCodeRuleLineUsedFlag(codeRuleDetailList.get(0).getRuleLineId());
                redisTemplate.opsForValue().set(CodeRuleConstant.generateUsedKey(ruleCode, levelCode, levelValue), "1");
            }
        }
        return codeRuleDetailList;
    }

    /**
     * 生成编码
     *
     * @param ruleCode
     * @param levelCode
     * @param levelValue
     * @param variableMap
     * @param codeRuleDetailList
     * @return
     */
    private String generate(String ruleCode, String levelCode, String levelValue, Map<String, String> variableMap, List<SysCodeRuleDetail> codeRuleDetailList) {
        final Date now = new Date();
        StringBuilder ruleCodeBuilder = new StringBuilder();
        codeRuleDetailList.stream()
                .sorted(Comparator.comparingLong(SysCodeRuleDetail::getOrderSeq))
                .forEach(codeRuleDetail -> {
                    // 使用 switch expression 收敛分支赋值，同时保持原有空值与默认分支语义不变。
                    String fieldValue = switch (codeRuleDetail.getFieldType()) {
                        case CodeRuleConstant.FieldType.CONSTANT -> codeRuleDetail.getFieldValue();
                        case CodeRuleConstant.FieldType.DATE -> DateUtils.parseDateToStr(codeRuleDetail.getDateMask(), now);
                        case CodeRuleConstant.FieldType.SEQUENCE -> String.format(
                                "%0" + codeRuleDetail.getSeqLength() + "d",
                                this.getSequenceValue(ruleCode, levelCode, levelValue, codeRuleDetail, now)
                        );
                        case CodeRuleConstant.FieldType.UUID -> {
                            // -1 时为默认生成 32 位 UUID。
                            Long seqLength = Optional.ofNullable(codeRuleDetail.getSeqLength()).orElse(-1L);
                            yield UUIDUtils.getUUID(seqLength.intValue());
                        }
                        case CodeRuleConstant.FieldType.VARIABLE ->
                                variableMap == null || !variableMap.containsKey(codeRuleDetail.getFieldValue())
                                        ? ""
                                        : variableMap.get(codeRuleDetail.getFieldValue());
                        default -> null;
                    };
                    ruleCodeBuilder.append(fieldValue);
                });
        return ruleCodeBuilder.toString();
    }

    /**
     * 生成本次的序列号
     *
     * @param ruleCode
     * @param levelCode
     * @param levelValue
     * @param codeRuleDetail
     * @param now
     * @return
     */
    private synchronized long getSequenceValue(String ruleCode, String levelCode, String levelValue, SysCodeRuleDetail codeRuleDetail, Date now) {
        long sequence;
        // 序列的redis key
        String sequenceKey = CodeRuleConstant.generateSequenceKey(ruleCode, levelCode, levelValue);
        // 层级是全局或自定义，序列值是存在sys_code_rule_detail里的
        if (CodeRuleConstant.LevelCode.GLOBAL.equals(levelCode) || CodeRuleConstant.LevelCode.CUSTOM.equals(levelCode)) {
            if (redisTemplate.hasKey(sequenceKey)) {
                // 是否需要重置
                if (isResetSequence(codeRuleDetail.getResetFrequency(), codeRuleDetail.getResetDate(), now)) {
                    codeRuleDetail.setResetDate(now);
                    sequence = advanceSequenceAtomically(sequenceKey, codeRuleDetail.getStartValue(), codeRuleDetail.getResetFrequency(), now, true);
                } else {
                    sequence = redisTemplate.opsForValue().increment(sequenceKey, 1);
                    // 异常情况，Redis数据不一致，需要从数据库拿当前值
                    if (codeRuleDetail.getCurrentValue() != null && sequence <= codeRuleDetail.getCurrentValue()) {
                        SysCodeRuleDetail tempCoeRuleDetail = codeRuleDetailService.getById(codeRuleDetail.getRuleDetailId());
                        sequence = resetSequence(sequenceKey, tempCoeRuleDetail.getCurrentValue() + 1);
                    }
                }
            } else {
                codeRuleDetail.setResetDate(now);
                if (codeRuleDetail.getCurrentValue() == null) {
                    // 第一次使用序列
                    sequence = advanceSequenceAtomically(sequenceKey, codeRuleDetail.getStartValue(), codeRuleDetail.getResetFrequency(), now, false);
                } else {
                    // redis数据全部被清空，在redis里表现为第一次使用，但实际上不是第一次使用
                    sequence = advanceSequenceAtomically(sequenceKey, codeRuleDetail.getCurrentValue() + 1, codeRuleDetail.getResetFrequency(), now, false);
                }
            }
        } else {
            // 层级是租户或部门，序列是存在sys_code_rule_value里的
            SysCodeRuleValue codeRuleValue = codeRuleDetailService.getSysCodeRuleValue(levelValue, codeRuleDetail.getRuleDetailId());
            codeRuleDetail.setCurrentValue(codeRuleValue.getCurrentValue());
            codeRuleDetail.setResetDate(codeRuleValue.getResetDate());
            if (redisTemplate.hasKey(sequenceKey)) {
                // 是否需要重置
                if (isResetSequence(codeRuleDetail.getResetFrequency(), codeRuleDetail.getResetDate(), now)) {
                    codeRuleDetail.setResetDate(now);
                    sequence = advanceSequenceAtomically(sequenceKey, codeRuleDetail.getStartValue(), codeRuleDetail.getResetFrequency(), now, true);
                } else {
                    sequence = redisTemplate.opsForValue().increment(sequenceKey, 1);
                    // 异常情况，Redis数据不一致，需要从数据库拿当前值
                    if (sequence <= codeRuleDetail.getCurrentValue()) {
                        sequence = resetSequence(sequenceKey, codeRuleDetail.getCurrentValue() + 1);
                    }
                }
            } else {
                codeRuleDetail.setResetDate(now);
                if (codeRuleDetail.getCurrentValue() == null) {
                    // 第一次使用序列
                    sequence = advanceSequenceAtomically(sequenceKey, codeRuleDetail.getStartValue(), codeRuleDetail.getResetFrequency(), now, false);
                } else {
                    // redis数据全部被清空，在redis里表现为第一次使用，但实际上不是第一次使用
                    sequence = advanceSequenceAtomically(sequenceKey, codeRuleDetail.getCurrentValue() + 1, codeRuleDetail.getResetFrequency(), now, false);
                }
            }
        }
        // 更新到缓存和数据库
        codeRuleDetail.setCurrentValue(sequence);
        codeRuleService.updateSeqNumber(ruleCode, levelCode, levelValue, codeRuleDetail);
        return sequence;
    }

    /**
     * 判断序列是否需要重置
     *
     * @param resetType    重置类型
     * @param lastRestDate 最后一次重置时间
     * @param now          现在时间
     * @return boolean
     */
    private boolean isResetSequence(String resetType, Date lastRestDate, Date now) {
        if (!CodeRuleConstant.ResetFrequency.NEVER.equalsIgnoreCase(resetType) && lastRestDate == null) {
            return true;
        } else if (CodeRuleConstant.ResetFrequency.YEAR.equalsIgnoreCase(resetType)) {
            return !DateUtils.parseDateToStr(DateUtils.YYYY, lastRestDate).equals(DateUtils.parseDateToStr(DateUtils.YYYY, now));
        } else if (CodeRuleConstant.ResetFrequency.QUARTER.equalsIgnoreCase(resetType)) {
            Calendar resetDate = Calendar.getInstance();
            resetDate.setTime(lastRestDate);
            Calendar nowDate = Calendar.getInstance();
            nowDate.setTime(now);
            return resetDate.get(Calendar.YEAR) != nowDate.get(Calendar.YEAR) || (resetDate.get(Calendar.MONTH) / 3 != nowDate.get(Calendar.MONTH) / 3);
        } else if (CodeRuleConstant.ResetFrequency.MONTH.equalsIgnoreCase(resetType)) {
            return !DateUtils.parseDateToStr(DateUtils.YYYY_MM, lastRestDate).equals(DateUtils.parseDateToStr(DateUtils.YYYY_MM, now));
        } else if (CodeRuleConstant.ResetFrequency.DAY.equalsIgnoreCase(resetType)) {
            return !DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, lastRestDate).equals(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, now));
        }
        return false;
    }

    /**
     * 重置序列，并返回初始值
     *
     * @param sequenceKey 缓存key
     * @param startValue  开始值
     */
    private Long resetSequence(String sequenceKey, Long startValue) {
        Long sequence = redisTemplate.execute(SET_SEQUENCE_SCRIPT, Collections.singletonList(sequenceKey), String.valueOf(startValue));
        if (sequence == null) {
            throw new CustomException("重置编码序列失败");
        }
        return sequence;
    }

    /**
     * 在 Redis 侧原子推进序列，避免“判断需要重置”和“真正重置”之间被其他实例穿插。
     *
     * @param sequenceKey   序列缓存 key
     * @param initialValue  首次或重置时要写入的起始值
     * @param resetType     重置频率
     * @param now           当前时间
     * @param resetRequired 当前请求是否判定需要重置
     * @return 推进后的序列值
     */
    private Long advanceSequenceAtomically(String sequenceKey, Long initialValue, String resetType, Date now, boolean resetRequired) {
        Long sequence = redisTemplate.execute(
                ADVANCE_SEQUENCE_SCRIPT,
                Arrays.asList(sequenceKey, buildResetWindowKey(sequenceKey)),
                resetRequired ? "1" : "0",
                resolveResetWindowToken(resetType, now),
                String.valueOf(initialValue)
        );
        if (sequence == null) {
            throw new CustomException("生成编码序列失败");
        }
        return sequence;
    }

    /**
     * 为每个序列维护当前重置窗口标记，避免同一窗口重复执行重置。
     *
     * @param sequenceKey 序列缓存 key
     * @return 重置窗口缓存 key
     */
    private String buildResetWindowKey(String sequenceKey) {
        return sequenceKey + ":reset-window";
    }

    /**
     * 将重置频率映射为当前时间窗口标识，供 Redis 脚本判断本窗口是否已执行过重置。
     *
     * @param resetType 重置频率
     * @param now       当前时间
     * @return 时间窗口标识
     */
    private String resolveResetWindowToken(String resetType, Date now) {
        if (CodeRuleConstant.ResetFrequency.YEAR.equalsIgnoreCase(resetType)) {
            return DateUtils.parseDateToStr(DateUtils.YYYY, now);
        } else if (CodeRuleConstant.ResetFrequency.QUARTER.equalsIgnoreCase(resetType)) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(now);
            return calendar.get(Calendar.YEAR) + "-Q" + (calendar.get(Calendar.MONTH) / 3 + 1);
        } else if (CodeRuleConstant.ResetFrequency.MONTH.equalsIgnoreCase(resetType)) {
            return DateUtils.parseDateToStr(DateUtils.YYYY_MM, now);
        } else if (CodeRuleConstant.ResetFrequency.DAY.equalsIgnoreCase(resetType)) {
            return DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, now);
        }
        return "";
    }

    /**
     * 构造“直接设置序列值”的 Redis 脚本。
     *
     * @return Redis 脚本对象
     */
    private static DefaultRedisScript<Long> buildSetSequenceScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptText("""
                redis.call('SET', KEYS[1], ARGV[1])
                return tonumber(ARGV[1])
                """);
        return redisScript;
    }

    /**
     * 构造“按窗口原子推进序列”的 Redis 脚本。
     *
     * @return Redis 脚本对象
     */
    private static DefaultRedisScript<Long> buildAdvanceSequenceScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptText("""
                local shouldReset = ARGV[1] == '1'
                local resetWindow = ARGV[2]
                local initialValue = tonumber(ARGV[3])

                if shouldReset then
                    local appliedWindow = redis.call('GET', KEYS[2])
                    if appliedWindow ~= resetWindow then
                        redis.call('SET', KEYS[1], initialValue)
                        redis.call('SET', KEYS[2], resetWindow)
                        return initialValue
                    end
                end

                if redis.call('EXISTS', KEYS[1]) == 0 then
                    redis.call('SET', KEYS[1], initialValue)
                    if resetWindow ~= '' then
                        redis.call('SET', KEYS[2], resetWindow)
                    end
                    return initialValue
                end

                return redis.call('INCRBY', KEYS[1], 1)
                """);
        return redisScript;
    }

}
