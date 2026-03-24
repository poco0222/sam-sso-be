/**
 * @file 编码规则明细服务实现，负责明细校验与序列值读取
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.system.constant.CodeRuleConstant;
import com.yr.system.domain.entity.SysCodeRuleDetail;
import com.yr.system.domain.entity.SysCodeRuleValue;
import com.yr.system.mapper.SysCodeRuleDetailMapper;
import com.yr.system.service.ISysCodeRuleDetailService;
import com.yr.system.service.ISysCodeRuleValueService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 编码规则明细(SysCodeRuleDetail)表服务实现类
 *
 * @author PopoY
 * @since 2021-10-29 19:53:14
 */
@Service
public class SysCodeRuleDetailServiceImpl extends CustomServiceImpl<SysCodeRuleDetailMapper, SysCodeRuleDetail> implements ISysCodeRuleDetailService {

    private final SysCodeRuleDetailMapper codeRuleDetailMapper;
    private final ISysCodeRuleValueService codeRuleValueService;

    public SysCodeRuleDetailServiceImpl(SysCodeRuleDetailMapper codeRuleDetailMapper,
                                        ISysCodeRuleValueService codeRuleValueService) {
        this.codeRuleDetailMapper = codeRuleDetailMapper;
        this.codeRuleValueService = codeRuleValueService;
    }

    @Override
    public void validate(SysCodeRuleDetail codeRuleDetail) {
        LambdaQueryWrapper<SysCodeRuleDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysCodeRuleDetail::getRuleLineId, codeRuleDetail.getRuleLineId());
        List<SysCodeRuleDetail> codeRuleDetailList = this.list(queryWrapper);
        if (CollectionUtils.isNotEmpty(codeRuleDetailList)) {
            codeRuleDetailList.forEach(detail -> {
                if (codeRuleDetail.getOrderSeq().equals(detail.getOrderSeq())) {
                    throw new CustomException("序号不能重复");
                }
                if (codeRuleDetail.getFieldType().equals(detail.getFieldType()) && codeRuleDetail.getFieldType().equals(CodeRuleConstant.FieldType.SEQUENCE)) {
                    throw new CustomException("同一规则下，只能有一个序列");
                }
            });
        }
    }

    @Override
    public List<SysCodeRuleDetail> listRuleCodeDetail(String ruleCode, String levelCode, String levelValue) {
        return codeRuleDetailMapper.listRuleCodeDetail(ruleCode, levelCode, levelValue);
    }

    @Override
    public void updateSeqNumber(String levelCode, String levelValue, SysCodeRuleDetail codeRuleDetail) {
        if (CodeRuleConstant.LevelCode.GLOBAL.equals(levelCode) || CodeRuleConstant.LevelCode.CUSTOM.equals(levelCode)) {
            codeRuleDetailMapper.updateSeqNumber(codeRuleDetail);
        } else {
            codeRuleValueService.updateSeqNumber(levelValue, codeRuleDetail);
        }
    }

    @Override
    public SysCodeRuleValue getSysCodeRuleValue(String levelValue, Long ruleDetailId) {
        LambdaQueryWrapper<SysCodeRuleValue> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysCodeRuleValue::getRuleDetailId, ruleDetailId);
        queryWrapper.eq(SysCodeRuleValue::getLevelValue, levelValue);
        SysCodeRuleValue codeRuleValue = codeRuleValueService.getOne(queryWrapper);
        return codeRuleValue == null ? new SysCodeRuleValue() : codeRuleValue;
    }
}
