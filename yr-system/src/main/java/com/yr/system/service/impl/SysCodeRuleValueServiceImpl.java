/**
 * @file 编码规则明细值服务实现，负责序号值的查询与回写
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.system.domain.entity.SysCodeRuleDetail;
import com.yr.system.domain.entity.SysCodeRuleValue;
import com.yr.system.mapper.SysCodeRuleValueMapper;
import com.yr.system.service.ISysCodeRuleValueService;
import org.springframework.stereotype.Service;

/**
 * 编码规则明细值(SysCodeRuleValue)表服务实现类
 *
 * @author Youngron
 * @since 2021-10-29 19:53:15
 */
@Service
public class SysCodeRuleValueServiceImpl extends CustomServiceImpl<SysCodeRuleValueMapper, SysCodeRuleValue> implements ISysCodeRuleValueService {

    private final SysCodeRuleValueMapper codeRuleValueMapper;

    public SysCodeRuleValueServiceImpl(SysCodeRuleValueMapper codeRuleValueMapper) {
        this.codeRuleValueMapper = codeRuleValueMapper;
    }

    @Override
    public void updateSeqNumber(String levelValue, SysCodeRuleDetail codeRuleDetail) {
        LambdaQueryWrapper<SysCodeRuleValue> queryWrapper = new LambdaQueryWrapper<SysCodeRuleValue>()
                .eq(SysCodeRuleValue::getRuleDetailId, codeRuleDetail.getRuleDetailId())
                .eq(SysCodeRuleValue::getLevelValue, levelValue);
        SysCodeRuleValue codeRuleValue = this.getOne(queryWrapper);
        if (codeRuleValue == null) {
            codeRuleValue = new SysCodeRuleValue();
            codeRuleValue.setRuleDetailId(codeRuleDetail.getRuleDetailId());
            codeRuleValue.setLevelValue(levelValue);
            codeRuleValue.setCurrentValue(codeRuleDetail.getCurrentValue());
            codeRuleValue.setResetDate(codeRuleDetail.getResetDate());
            this.save(codeRuleValue);
        } else {
            codeRuleValue.setCurrentValue(codeRuleDetail.getCurrentValue());
            codeRuleValue.setResetDate(codeRuleDetail.getResetDate());
            codeRuleValueMapper.updateSeqNumber(codeRuleValue);
        }
    }
}
