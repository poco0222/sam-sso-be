/**
 * @file 编码规则行服务实现，负责规则行唯一性校验与使用标记更新
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.system.constant.CodeRuleConstant;
import com.yr.system.domain.entity.SysCodeRuleLine;
import com.yr.system.mapper.SysCodeRuleLineMapper;
import com.yr.system.service.ISysCodeRuleLineService;
import org.springframework.stereotype.Service;

/**
 * 编码规则行(SysCodeRuleLine)表服务实现类
 *
 * @author PopoY
 * @since 2021-10-29 19:53:14
 */
@Service
public class SysCodeRuleLineServiceImpl extends CustomServiceImpl<SysCodeRuleLineMapper, SysCodeRuleLine> implements ISysCodeRuleLineService {

    @Override
    public void insertGlobalLine(Long codeRuleId) {
        SysCodeRuleLine sysCodeRuleLine = new SysCodeRuleLine();
        sysCodeRuleLine.setRuleId(codeRuleId);
        sysCodeRuleLine.setLevelCode(CodeRuleConstant.LevelCode.GLOBAL);
        sysCodeRuleLine.setLevelValue(CodeRuleConstant.LevelCode.GLOBAL);
        sysCodeRuleLine.setUsedFlag(CodeRuleConstant.UsedFlag.NO);
        sysCodeRuleLine.setEnabledFlag(CodeRuleConstant.EnabledFlag.DISABLED);
        this.save(sysCodeRuleLine);
    }

    @Override
    public SysCodeRuleLine insertCodeRuleLine(SysCodeRuleLine codeRuleLine) {
        LambdaQueryWrapper<SysCodeRuleLine> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysCodeRuleLine::getRuleId, codeRuleLine.getRuleId());
        queryWrapper.eq(SysCodeRuleLine::getLevelCode, codeRuleLine.getLevelCode());
        queryWrapper.eq(SysCodeRuleLine::getLevelValue, codeRuleLine.getLevelValue());
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new CustomException("数据重复");
        }
        this.save(codeRuleLine);
        return this.getById(codeRuleLine.getRuleLineId());
    }

    @Override
    public SysCodeRuleLine updateCodeRuleLine(SysCodeRuleLine codeRuleLine, SysCodeRuleLine oldCodeRuleLine) {
        // 校验唯一约束
        if (oldCodeRuleLine != null && !oldCodeRuleLine.getLevelValue().equals(codeRuleLine.getLevelValue())) {
            LambdaQueryWrapper<SysCodeRuleLine> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysCodeRuleLine::getRuleId, codeRuleLine.getRuleId());
            queryWrapper.eq(SysCodeRuleLine::getLevelCode, codeRuleLine.getLevelCode());
            queryWrapper.eq(SysCodeRuleLine::getLevelValue, codeRuleLine.getLevelValue());
            queryWrapper.ne(SysCodeRuleLine::getRuleLineId, codeRuleLine.getRuleLineId());
            long count = this.count(queryWrapper);
            if (count > 0) {
                throw new CustomException("数据重复");
            }
        }
        // 不更新这个字段
        codeRuleLine.setUsedFlag(null);
        this.updateById(codeRuleLine);
        return this.getById(codeRuleLine.getRuleLineId());
    }

    @Override
    public void updateCodeRuleLineUsedFlag(Long ruleLineId) {
        LambdaUpdateWrapper<SysCodeRuleLine> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(SysCodeRuleLine::getUsedFlag, "1");
        updateWrapper.eq(SysCodeRuleLine::getRuleLineId, ruleLineId);
        this.update(updateWrapper);
    }
}
