package com.yr.system.service;

import com.yr.common.mybatisplus.service.ICustomService;
import com.yr.system.domain.entity.SysCodeRuleDetail;
import com.yr.system.domain.entity.SysCodeRuleValue;

/**
 * 编码规则明细值(SysCodeRuleValue)表服务接口
 *
 * @author Youngron
 * @since 2021-10-29 19:53:15
 */
public interface ISysCodeRuleValueService extends ICustomService<SysCodeRuleValue> {

    /**
     * 更新序列信息
     *
     * @param levelValue     层级值
     * @param codeRuleDetail 规则明细
     */
    void updateSeqNumber(String levelValue, SysCodeRuleDetail codeRuleDetail);
}
