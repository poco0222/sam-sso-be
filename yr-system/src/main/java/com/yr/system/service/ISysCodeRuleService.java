package com.yr.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.mybatisplus.service.ICustomService;
import com.yr.system.domain.entity.SysCodeRule;
import com.yr.system.domain.entity.SysCodeRuleDetail;
import com.yr.system.domain.entity.SysCodeRuleLine;

import java.util.List;

/**
 * 编码规则(SysCodeRule)表服务接口
 *
 * @author Youngron
 * @since 2021-10-29 19:53:13
 */
public interface ISysCodeRuleService extends ICustomService<SysCodeRule> {

    /**
     * 分页查询编码规则
     *
     * @param page
     * @param codeRule
     * @return
     */
    IPage<SysCodeRule> pageByCondition(IPage<SysCodeRule> page, SysCodeRule codeRule);

    /**
     * 新增或更新编码规则
     *
     * @param codeRule
     * @return
     */
    boolean insertOrUpdateCodeRule(SysCodeRule codeRule);

    /**
     * 删除编码规则
     *
     * @param codeRuleList 编码规则头集合
     * @return
     */
    boolean deleteCodeRule(List<SysCodeRule> codeRuleList);

    /**
     * 新增或更新编码规则行
     *
     * @param codeRuleLine
     * @return
     */
    boolean insertOrUpdateCodeRuleLine(SysCodeRuleLine codeRuleLine);

    /**
     * 删除编码规则行
     *
     * @param codeRuleLineList
     * @return
     */
    boolean deleteCodeRuleLine(List<SysCodeRuleLine> codeRuleLineList);

    /**
     * 新增或更新编码规则明细
     *
     * @param codeRuleDetail
     * @return
     */
    boolean insertOrUpdateCodeRuleDetail(SysCodeRuleDetail codeRuleDetail);

    /**
     * 删除编码规则明细
     *
     * @param codeRuleDetailList
     * @return
     */
    boolean deleteCodeRuleDetail(List<SysCodeRuleDetail> codeRuleDetailList);

    /**
     * 从换缓存中获取编码规则明细
     *
     * @param key 缓存key
     * @return
     */
    List<SysCodeRuleDetail> getRuleCodeDetailListFromCache(String key);

    /**
     * 缓存到redis
     *
     * @param codeRule           编码规则头
     * @param codeRuleLine       编码规则行
     * @param codeRuleDetailList 编码规则明细集合
     */
    void saveCache(SysCodeRule codeRule, SysCodeRuleLine codeRuleLine, List<SysCodeRuleDetail> codeRuleDetailList);

    /**
     * 更新序列并重新缓存规则明细到redis
     *
     * @param ruleCode       规则编码
     * @param levelCode      层级CODE
     * @param levelValue     层级值
     * @param codeRuleDetail 规则明细
     */
    void updateSeqNumber(String ruleCode, String levelCode, String levelValue, SysCodeRuleDetail codeRuleDetail);

    /**
     * 启动后批量预热编码规则缓存，并返回成功写入的规则行数量。
     *
     * @return 预热的规则行数量
     */
    int warmUpCodeRuleCache();
}
