package com.yr.system.service;

import com.yr.common.mybatisplus.service.ICustomService;
import com.yr.system.domain.entity.SysCodeRuleLine;

/**
 * 编码规则行(SysCodeRuleLine)表服务接口
 *
 * @author Youngron
 * @since 2021-10-29 19:53:14
 */
public interface ISysCodeRuleLineService extends ICustomService<SysCodeRuleLine> {

    /**
     * 插入全局层级的行信息
     *
     * @param codeRuleId 编码规则头ID
     */
    void insertGlobalLine(Long codeRuleId);

    /**
     * 新增编码规则行
     *
     * @param codeRuleLine
     * @return
     */
    SysCodeRuleLine insertCodeRuleLine(SysCodeRuleLine codeRuleLine);

    /**
     * 更新编码规则行
     *
     * @param codeRuleLine    新信息
     * @param oldCodeRuleLine 原始信息
     * @return
     */
    SysCodeRuleLine updateCodeRuleLine(SysCodeRuleLine codeRuleLine, SysCodeRuleLine oldCodeRuleLine);

    /**
     * 将编码规则行更新为已使用
     *
     * @param ruleLineId 编码规则行ID
     */
    void updateCodeRuleLineUsedFlag(Long ruleLineId);
}
