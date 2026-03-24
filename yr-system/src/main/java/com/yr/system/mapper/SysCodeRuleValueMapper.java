package com.yr.system.mapper;

import com.yr.common.mybatisplus.custommapper.CustomMapper;
import com.yr.system.domain.entity.SysCodeRuleValue;

/**
 * 编码规则明细值(SysCodeRuleValue)表数据库访问层
 *
 * @author Youngron
 * @since 2021-10-29 19:53:15
 */
public interface SysCodeRuleValueMapper extends CustomMapper<SysCodeRuleValue> {

    /**
     * 更新序列信息
     *
     * @param codeRuleValue
     * @return
     */
    int updateSeqNumber(SysCodeRuleValue codeRuleValue);
}
