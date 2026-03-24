/**
 * @file 编码规则服务实现，负责规则缓存读写与规则维护
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.common.utils.DateUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.constant.CodeRuleConstant;
import com.yr.system.domain.entity.SysCodeRule;
import com.yr.system.domain.entity.SysCodeRuleDetail;
import com.yr.system.domain.entity.SysCodeRuleLine;
import com.yr.system.mapper.SysCodeRuleMapper;
import com.yr.system.service.ISysCodeRuleDetailService;
import com.yr.system.service.ISysCodeRuleLineService;
import com.yr.system.service.ISysCodeRuleService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 编码规则(SysCodeRule)表服务实现类
 *
 * @author Youngron
 * @since 2021-10-29 19:53:13
 */
@Service
public class SysCodeRuleServiceImpl extends CustomServiceImpl<SysCodeRuleMapper, SysCodeRule> implements ISysCodeRuleService {

    private final static Logger LOGGER = LoggerFactory.getLogger(SysCodeRuleServiceImpl.class);

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        // 日期格式化
        OBJECT_MAPPER.setDateFormat(new SimpleDateFormat(DateUtils.YYYY_MM_DD_HH_MM_SS));
        // 如果json中有新增的字段并且是实体类类中不存在的，不报错
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private final ISysCodeRuleLineService codeRuleLineService;
    private final ISysCodeRuleDetailService codeRuleDetailService;
    private final HashOperations<String, String, String> hashOpr;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 显式声明规则服务依赖，避免字段注入隐藏初始化顺序。
     *
     * @param codeRuleLineService 规则行服务
     * @param codeRuleDetailService 规则明细服务
     * @param hashOpr Redis Hash 操作器
     * @param redisTemplate Redis 模板
     */
    public SysCodeRuleServiceImpl(ISysCodeRuleLineService codeRuleLineService,
                                  ISysCodeRuleDetailService codeRuleDetailService,
                                  HashOperations<String, String, String> hashOpr,
                                  RedisTemplate<String, String> redisTemplate) {
        this.codeRuleLineService = codeRuleLineService;
        this.codeRuleDetailService = codeRuleDetailService;
        this.hashOpr = hashOpr;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 启动后批量预热编码规则缓存，并返回成功写入的规则行数量。
     *
     * @return 成功写入缓存的规则行数量
     */
    @Override
    public int warmUpCodeRuleCache() {
        int warmedLineCount = 0;
        List<SysCodeRule> codeRuleList = this.list();
        if (CollectionUtils.isNotEmpty(codeRuleList)) {
            for (SysCodeRule codeRule : codeRuleList) {
                List<SysCodeRuleLine> codeRuleLineList = listRuleLinesByRuleId(codeRule.getRuleId());
                if (CollectionUtils.isNotEmpty(codeRuleLineList)) {
                    for (SysCodeRuleLine codeRuleLine : codeRuleLineList) {
                        List<SysCodeRuleDetail> codeRuleDetailList = listRuleDetailsByRuleLineId(codeRuleLine.getRuleLineId());
                        if (CollectionUtils.isNotEmpty(codeRuleDetailList)) {
                            this.saveCache(codeRule, codeRuleLine, codeRuleDetailList);
                            warmedLineCount++;
                        }
                    }
                }
            }
        }
        return warmedLineCount;
    }

    @Override
    public IPage<SysCodeRule> pageByCondition(IPage<SysCodeRule> page, SysCodeRule codeRule) {
        LambdaQueryWrapper<SysCodeRule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(codeRule.getRuleCode()), SysCodeRule::getRuleCode, codeRule.getRuleCode());
        queryWrapper.like(StringUtils.isNotBlank(codeRule.getRuleName()), SysCodeRule::getRuleName, codeRule.getRuleName());
        if (codeRule.getParams() != null) {
            queryWrapper.ge(codeRule.getParams().containsKey("beginTime"), SysCodeRule::getCreateAt, codeRule.getParams().get("beginTime"));
            if (codeRule.getParams().containsKey("endTime")) {
                queryWrapper.lt(SysCodeRule::getCreateAt, DateUtils.addDays(DateUtils.parseDate(codeRule.getParams().get("endTime")), 1));
            }
        }
        queryWrapper.orderByDesc(SysCodeRule::getCreateAt);
        return this.page(page, queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertOrUpdateCodeRule(SysCodeRule codeRule) {
        if (codeRule.getRuleId() == null) {
            LambdaQueryWrapper<SysCodeRule> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysCodeRule::getRuleCode, codeRule.getRuleCode());
            long count = this.count(queryWrapper);
            if (count > 0) {
                throw new CustomException("编码重复");
            }
            // 新增头
            this.save(codeRule);
            // 新增头时默认新增一个全局的行
            codeRuleLineService.insertGlobalLine(codeRule.getRuleId());
            this.clearFailFastCache(codeRule.getRuleCode(), CodeRuleConstant.LevelCode.GLOBAL, CodeRuleConstant.LevelCode.GLOBAL);
        } else {
            SysCodeRule oldCodeRule = this.getById(codeRule.getRuleId());
            if (oldCodeRule == null) {
                throw new CustomException("未查询到编码规则");
            }
            SysCodeRule mergedCodeRule = mergeCodeRuleForUpdate(oldCodeRule, codeRule);
            if (!this.updateById(mergedCodeRule)) {
                throw new CustomException("更新失败，数据可能已经被修改");
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCodeRule(List<SysCodeRule> codeRuleList) {
        if (CollectionUtils.isEmpty(codeRuleList)) {
            return false;
        }
        Map<String, List<SysCodeRuleLine>> codeRuleMap = new HashMap<>(codeRuleList.size());
        codeRuleList.forEach(codeRule -> {
            List<SysCodeRuleLine> codeRuleLineList = listRuleLinesByRuleId(codeRule.getRuleId());
            if (CollectionUtils.isNotEmpty(codeRuleLineList)) {
                codeRuleLineList.forEach(codeRuleLine -> {
                    if (CodeRuleConstant.EnabledFlag.ENABLED.equals(codeRuleLine.getEnabledFlag()) || CodeRuleConstant.UsedFlag.YES.equals(codeRuleLine.getUsedFlag())) {
                        throw new CustomException("规则 " + codeRule.getRuleCode() + " -> " + codeRuleLine.getLevelCode() + " 已经启用或被使用，不能删除");
                    }
                });
                codeRuleLineList.forEach(codeRuleLine -> {
                    List<SysCodeRuleDetail> codeRuleDetailList = listRuleDetailsByRuleLineId(codeRuleLine.getRuleLineId());
                    if (CollectionUtils.isNotEmpty(codeRuleDetailList)) {
                        codeRuleDetailService.removeByIds(codeRuleDetailList.stream().map(SysCodeRuleDetail::getRuleDetailId).collect(Collectors.toList()));
                    }
                    codeRuleLineService.removeById(codeRuleLine.getRuleLineId());
                });
                codeRuleMap.put(codeRule.getRuleCode(), codeRuleLineList);
            }
            this.removeById(codeRule.getRuleId());
        });
        for (Map.Entry<String, List<SysCodeRuleLine>> entry : codeRuleMap.entrySet()) {
            List<SysCodeRuleLine> codeRuleLineList = entry.getValue();
            codeRuleLineList.forEach(codeRuleLine -> {
                String key = entry.getKey();
                this.deleteCache(key, codeRuleLine);
                this.deleteSequenceCache(key, codeRuleLine);
                this.deleteUsedCache(key, codeRuleLine);
            });
        }
        return true;
    }

    @Override
    public boolean insertOrUpdateCodeRuleLine(SysCodeRuleLine codeRuleLine) {
        SysCodeRule codeRule = this.getById(codeRuleLine.getRuleId());
        if (codeRule == null) {
            throw new CustomException("未查询到编码规则");
        }
        // 如果不是自定义层级，那么层级值默认为层级CODE
        if (!CodeRuleConstant.LevelCode.CUSTOM.equals(codeRuleLine.getLevelCode())) {
            codeRuleLine.setLevelValue(codeRuleLine.getLevelCode());
        }
        if (codeRuleLine.getRuleLineId() == null) {
            codeRuleLine = codeRuleLineService.insertCodeRuleLine(codeRuleLine);
        } else {
            SysCodeRuleLine oldCodeRuleLine = codeRuleLineService.getById(codeRuleLine.getRuleLineId());
            if (oldCodeRuleLine == null) {
                throw new CustomException("未查询到编码规则行");
            }
            if (CodeRuleConstant.UsedFlag.YES.equals(codeRuleLine.getUsedFlag())) {
                throw new CustomException("规则已经被使用，不能修改");
            }
            codeRuleLine = codeRuleLineService.updateCodeRuleLine(codeRuleLine, oldCodeRuleLine);
            // 删除原来的缓存
            this.deleteCache(codeRule.getRuleCode(), oldCodeRuleLine);
            // 插入新的缓存信息
            this.saveCache(codeRule, codeRuleLine, listRuleDetailsByRuleLineId(codeRuleLine.getRuleLineId()));
        }
        if (CodeRuleConstant.EnabledFlag.ENABLED.equals(codeRuleLine.getEnabledFlag())) {
            this.clearFailFastCache(codeRule.getRuleCode(), codeRuleLine.getLevelCode(), codeRuleLine.getLevelValue());
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCodeRuleLine(List<SysCodeRuleLine> codeRuleLineList) {
        if (CollectionUtils.isEmpty(codeRuleLineList)) {
            return true;
        }
        SysCodeRule codeRule = null;
        for (SysCodeRuleLine codeRuleLine : codeRuleLineList) {
            if (CodeRuleConstant.LevelCode.GLOBAL.equals(codeRuleLine.getLevelCode())) {
                throw new CustomException("不能删除全局层级");
            }
            if (CodeRuleConstant.EnabledFlag.ENABLED.equals(codeRuleLine.getEnabledFlag()) || CodeRuleConstant.UsedFlag.YES.equals(codeRuleLine.getUsedFlag())) {
                throw new CustomException("规则已经启用或被使用，不能删除");
            }
            if (codeRule == null) {
                codeRule = this.getById(codeRuleLine.getRuleId());
                if (codeRule == null) {
                    throw new CustomException("未查询到编码规则");
                }
            }
            List<SysCodeRuleDetail> codeRuleDetailList = listRuleDetailsByRuleLineId(codeRuleLine.getRuleLineId());
            if (CollectionUtils.isNotEmpty(codeRuleDetailList)) {
                codeRuleDetailService.removeByIds(codeRuleDetailList.stream().map(SysCodeRuleDetail::getRuleDetailId).collect(Collectors.toList()));
            }
            codeRuleLineService.removeById(codeRuleLine.getRuleLineId());
        }
        // 清除缓存
        for (SysCodeRuleLine codeRuleLine : codeRuleLineList) {
            this.deleteCache(codeRule.getRuleCode(), codeRuleLine);
            this.deleteSequenceCache(codeRule.getRuleCode(), codeRuleLine);
            this.deleteUsedCache(codeRule.getRuleCode(), codeRuleLine);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean insertOrUpdateCodeRuleDetail(SysCodeRuleDetail codeRuleDetail) {
        SysCodeRuleLine codeRuleLine = codeRuleLineService.getById(codeRuleDetail.getRuleLineId());
        if (codeRuleLine == null) {
            throw new CustomException("未查询到编码规则行");
        }
        if (CodeRuleConstant.UsedFlag.YES.equals(codeRuleLine.getUsedFlag())) {
            throw new CustomException("规则已经被使用，不能修改");
        }
        if (codeRuleDetail.getRuleDetailId() == null) {
            codeRuleDetailService.validate(codeRuleDetail);
            codeRuleDetailService.save(codeRuleDetail);
        } else {
            codeRuleDetailService.updateById(codeRuleDetail);
        }
        if (CodeRuleConstant.EnabledFlag.ENABLED.equals(codeRuleLine.getEnabledFlag())) {
            SysCodeRule codeRule = this.getById(codeRuleLine.getRuleId());
            if (codeRule == null) {
                throw new CustomException("未查询到编码规则");
            }
            this.clearFailFastCache(codeRule.getRuleCode(), codeRuleLine.getLevelCode(), codeRuleLine.getLevelValue());
            this.deleteCache(codeRule.getRuleCode(), codeRuleLine);
            this.saveCache(codeRule, codeRuleLine, listRuleDetailsByRuleLineId(codeRuleDetail.getRuleLineId()));
        }
        return true;
    }

    @Override
    public boolean deleteCodeRuleDetail(List<SysCodeRuleDetail> codeRuleDetailList) {
        if (CollectionUtils.isEmpty(codeRuleDetailList)) {
            return true;
        }
        SysCodeRuleLine codeRuleLine = codeRuleLineService.getById(codeRuleDetailList.get(0).getRuleLineId());
        if (codeRuleLine == null) {
            throw new CustomException("未查询到编码规则行");
        }
        // 已使用的数据不允许删除
        if (CodeRuleConstant.UsedFlag.YES.equals(codeRuleLine.getUsedFlag())) {
            throw new CustomException("规则已经被使用，不能删除");
        }
        SysCodeRule codeRule = this.getById(codeRuleLine.getRuleId());
        if (codeRule == null) {
            throw new CustomException("未查询到编码规则");
        }
        // 删除数据
        codeRuleDetailService.removeByIds(codeRuleDetailList.stream().map(SysCodeRuleDetail::getRuleDetailId).collect(Collectors.toList()));
        // 删除缓存
        this.deleteCache(codeRule.getRuleCode(), codeRuleLine);
        // 重新生成缓存信息
        List<SysCodeRuleDetail> restCodeRuleDetailList = listRuleDetailsByRuleLineId(codeRuleLine.getRuleLineId());
        if (CollectionUtils.isNotEmpty(restCodeRuleDetailList)) {
            this.saveCache(codeRule, codeRuleLine, restCodeRuleDetailList);
        }
        return true;
    }

    @Override
    public List<SysCodeRuleDetail> getRuleCodeDetailListFromCache(String key) {
        Map<String, String> value = hashOpr.entries(key);
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        return value.values().stream()
                .filter(StringUtils::isNotBlank)
                .map(this::deserializeCachedRuleDetail)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 反序列化缓存中的规则明细，遇到坏数据时记录告警并跳过该条目。
     *
     * @param cachedRuleDetailJson 缓存中的规则明细 JSON
     * @return 反序列化后的规则明细；坏数据返回 null
     */
    private SysCodeRuleDetail deserializeCachedRuleDetail(String cachedRuleDetailJson) {
        try {
            return OBJECT_MAPPER.readValue(cachedRuleDetailJson, SysCodeRuleDetail.class);
        } catch (IOException exception) {
            LOGGER.warn("skip corrupted code rule detail cache entry: {}", exception.getMessage());
            return null;
        }
    }

    /**
     * 缓存到redis
     *
     * @param codeRule           编码规则头
     * @param codeRuleLine       编码规则行
     * @param codeRuleDetailList 编码规则明细集合
     */
    @Override
    public void saveCache(SysCodeRule codeRule, SysCodeRuleLine codeRuleLine, List<SysCodeRuleDetail> codeRuleDetailList) {
        if (CodeRuleConstant.EnabledFlag.DISABLED.equals(codeRuleLine.getEnabledFlag()) || CollectionUtils.isEmpty(codeRuleDetailList)) {
            return;
        }
        String key = CodeRuleConstant.generateCacheKey(codeRule.getRuleCode(), codeRuleLine.getLevelCode(), codeRuleLine.getLevelValue());
        Map<String, String> map = new HashMap<>(codeRuleDetailList.size());
        // 编码规则明细排序
        codeRuleDetailList.sort(Comparator.comparingLong(SysCodeRuleDetail::getOrderSeq));
        codeRuleDetailList.forEach(item -> {
            try {
                map.put(String.valueOf(item.getOrderSeq()), OBJECT_MAPPER.writeValueAsString(item));
            } catch (JsonProcessingException e) {
                LOGGER.warn("writeValueAsString error, {}", e.getMessage());
            }
        });
        hashOpr.putAll(key, map);
    }

    @Override
    public void updateSeqNumber(String ruleCode, String levelCode, String levelValue, SysCodeRuleDetail codeRuleDetail) {
        codeRuleDetailService.updateSeqNumber(levelCode, levelValue, codeRuleDetail);
        if (CodeRuleConstant.LevelCode.GLOBAL.equals(levelCode) || CodeRuleConstant.LevelCode.CUSTOM.equals(levelCode)) {
            String redisData = "";
            try {
                redisData = OBJECT_MAPPER.writeValueAsString(codeRuleDetail);
            } catch (JsonProcessingException e) {
                LOGGER.warn("writeValueAsString error, {}", e.getMessage());
            }
            hashOpr.put(CodeRuleConstant.generateCacheKey(ruleCode, levelCode, levelValue), String.valueOf(codeRuleDetail.getOrderSeq()), redisData);
        }
    }

    /**
     * 清除redis缓存
     *
     * @param ruleCode     规则CODE
     * @param codeRuleLine 编码规则行
     */
    private void deleteCache(String ruleCode, SysCodeRuleLine codeRuleLine) {
        redisTemplate.delete(CodeRuleConstant.generateCacheKey(ruleCode, codeRuleLine.getLevelCode(), codeRuleLine.getLevelValue()));
    }

    /**
     * 清除redis序列缓存
     *
     * @param ruleCode     规则CODE
     * @param codeRuleLine 编码规则行
     */
    private void deleteSequenceCache(String ruleCode, SysCodeRuleLine codeRuleLine) {
        redisTemplate.delete(CodeRuleConstant.generateSequenceKey(ruleCode, codeRuleLine.getLevelCode(), codeRuleLine.getLevelValue()));
    }

    /**
     * 清除redis已使用缓存
     *
     * @param ruleCode     规则CODE
     * @param codeRuleLine 编码规则行
     */
    private void deleteUsedCache(String ruleCode, SysCodeRuleLine codeRuleLine) {
        redisTemplate.delete(CodeRuleConstant.generateUsedKey(ruleCode, codeRuleLine.getLevelCode(), codeRuleLine.getLevelValue()));
    }

    /**
     * 清除redis失败标记
     *
     * @param ruleCode   规则CODE
     * @param levelCode  层级CODE
     * @param levelValue 层级值
     */
    private void clearFailFastCache(String ruleCode, String levelCode, String levelValue) {
        redisTemplate.delete(CodeRuleConstant.generateFailFastKey(ruleCode, levelCode, levelValue));
    }

    /**
     * 根据规则头 ID 查询规则行，避免重复拼接字符串列名。
     *
     * @param ruleId 规则头 ID
     * @return 规则行列表
     */
    private List<SysCodeRuleLine> listRuleLinesByRuleId(Long ruleId) {
        LambdaQueryWrapper<SysCodeRuleLine> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysCodeRuleLine::getRuleId, ruleId);
        return codeRuleLineService.list(queryWrapper);
    }

    /**
     * 根据规则行 ID 查询规则明细，统一收敛查询风格。
     *
     * @param ruleLineId 规则行 ID
     * @return 规则明细列表
     */
    private List<SysCodeRuleDetail> listRuleDetailsByRuleLineId(Long ruleLineId) {
        LambdaQueryWrapper<SysCodeRuleDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysCodeRuleDetail::getRuleLineId, ruleLineId);
        return codeRuleDetailService.list(queryWrapper);
    }

    /**
     * 合并旧实体与更新命令，避免 partial payload 覆盖头表已有字段。
     *
     * @param oldCodeRule 已存在的编码规则头
     * @param updateCommand 本次更新命令
     * @return 可安全持久化的合并结果
     */
    private SysCodeRule mergeCodeRuleForUpdate(SysCodeRule oldCodeRule, SysCodeRule updateCommand) {
        SysCodeRule mergedCodeRule = new SysCodeRule();
        mergedCodeRule.setRuleId(oldCodeRule.getRuleId());
        mergedCodeRule.setRuleCode(updateCommand.getRuleCode() != null ? updateCommand.getRuleCode() : oldCodeRule.getRuleCode());
        mergedCodeRule.setRuleName(updateCommand.getRuleName() != null ? updateCommand.getRuleName() : oldCodeRule.getRuleName());
        mergedCodeRule.setDescription(updateCommand.getDescription() != null ? updateCommand.getDescription() : oldCodeRule.getDescription());
        mergedCodeRule.setOrgId(updateCommand.getOrgId() != null ? updateCommand.getOrgId() : oldCodeRule.getOrgId());
        mergedCodeRule.setCreateBy(updateCommand.getCreateBy() != null ? updateCommand.getCreateBy() : oldCodeRule.getCreateBy());
        mergedCodeRule.setCreateAt(updateCommand.getCreateAt() != null ? updateCommand.getCreateAt() : oldCodeRule.getCreateAt());
        mergedCodeRule.setUpdateBy(updateCommand.getUpdateBy() != null ? updateCommand.getUpdateBy() : oldCodeRule.getUpdateBy());
        mergedCodeRule.setUpdateAt(updateCommand.getUpdateAt() != null ? updateCommand.getUpdateAt() : oldCodeRule.getUpdateAt());
        mergedCodeRule.setObjectVersionNumber(updateCommand.getObjectVersionNumber() != null
                ? updateCommand.getObjectVersionNumber()
                : oldCodeRule.getObjectVersionNumber());
        return mergedCodeRule;
    }

}
