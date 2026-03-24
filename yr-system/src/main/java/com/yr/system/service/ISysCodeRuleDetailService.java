package com.yr.system.service;

import com.yr.common.mybatisplus.service.ICustomService;
import com.yr.system.domain.entity.SysCodeRuleDetail;
import com.yr.system.domain.entity.SysCodeRuleValue;

import java.util.List;

/**
 * 编码规则明细(SysCodeRuleDetail)表服务接口
 *
 * @author Youngron
 * @since 2021-10-29 19:53:14
 */
public interface ISysCodeRuleDetailService extends ICustomService<SysCodeRuleDetail> {

    /**
     * 新增时校验数据合法性
     *
     * @param codeRuleDetail
     */
    void validate(SysCodeRuleDetail codeRuleDetail);

    /**
     * 根据规则编码、层级code和层级值查询规则明细
     *
     * @param ruleCode   规则编码
     * @param levelCode  层级code
     * @param levelValue 层级值
     * @return
     */
    List<SysCodeRuleDetail> listRuleCodeDetail(String ruleCode, String levelCode, String levelValue);

    /**
     * 更新序列信息
     *
     * @param levelCode      层级CODE
     * @param levelValue     层级值
     * @param codeRuleDetail 编码规则明细
     */
    void updateSeqNumber(String levelCode, String levelValue, SysCodeRuleDetail codeRuleDetail);

    /**
     * 获取SysCodeRuleValue
     *
     * @param levelValue   层级值
     * @param ruleDetailId 编码规则明细ID
     * @return
     */
    SysCodeRuleValue getSysCodeRuleValue(String levelValue, Long ruleDetailId);
}
