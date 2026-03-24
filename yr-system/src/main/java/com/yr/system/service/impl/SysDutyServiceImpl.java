/**
 * @file 职务服务实现
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yr.common.constant.UserConstants;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.domain.entity.SysDuty;
import com.yr.system.domain.entity.SysUserDuty;
import com.yr.system.mapper.SysDutyMapper;
import com.yr.system.service.ISysDutyService;
import com.yr.system.service.ISysUserDutyService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-23 18:28
 * @description
 */

@Service
public class SysDutyServiceImpl extends CustomServiceImpl<SysDutyMapper, SysDuty> implements ISysDutyService {

    private final ISysUserDutyService iSysUserDutyService;

    public SysDutyServiceImpl(ISysUserDutyService iSysUserDutyService) {
        this.iSysUserDutyService = iSysUserDutyService;
    }

    @Override
    public List<SysDuty> listDuty(SysDuty sysDuty) {
        LambdaQueryWrapper<SysDuty> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(sysDuty.getDutyCode()), SysDuty::getDutyCode, sysDuty.getDutyCode());
        queryWrapper.like(StringUtils.isNotBlank(sysDuty.getDutyName()), SysDuty::getDutyName, sysDuty.getDutyName());
        queryWrapper.eq(SysDuty::getOrgId, SecurityUtils.getOrgId());
        queryWrapper.orderByAsc(SysDuty::getParentId)
                .orderByAsc(SysDuty::getOrderNum);
        return this.list(queryWrapper);
    }

    @Override
    public SysDuty addDuty(SysDuty sysDuty) {
        if (sysDuty.getOrgId() == null) {
            sysDuty.setOrgId(SecurityUtils.getOrgId());
        }
        if (UserConstants.NOT_UNIQUE.equals(this.checkDutyCodeUnique(sysDuty))) {
            throw new CustomException("新增失败，编码已存在");
        }

        SysDuty parent = this.getById(sysDuty.getParentId());
        // 父节点不存在时直接失败，避免祖级拼接触发空指针。
        if (parent == null) {
            throw new CustomException("上级职务不存在");
        }
        sysDuty.setAncestors(parent.getAncestors() + "," + sysDuty.getParentId());

        this.save(sysDuty);
        return sysDuty;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDuty updateDuty(SysDuty sysDuty) {
        if (UserConstants.NOT_UNIQUE.equals(this.checkDutyCodeUnique(sysDuty))) {
            throw new CustomException("更新失败，编码已存在");
        }
        if (sysDuty.getId().equals(sysDuty.getParentId())) {
            throw new CustomException("更新失败，上级职务不能是自己");
        }

        SysDuty parent = this.getById(sysDuty.getParentId());
        if (parent == null) {
            throw new CustomException("上级职务不存在");
        }
        SysDuty oldDuty = this.getById(sysDuty.getId());
        if (oldDuty == null) {
            throw new CustomException("职务不存在");
        }
        String newAncestors = parent.getAncestors() + "," + parent.getId();
        String oldAncestors = oldDuty.getAncestors();
        sysDuty.setAncestors(newAncestors);
        this.updateDutyChildrenAncestors(sysDuty.getId(), newAncestors, oldAncestors);

        SysDuty mergedDuty = mergeDutyForUpdate(oldDuty, sysDuty);
        if (!this.updateById(mergedDuty)) {
            throw new CustomException("更新失败，数据可能已经被修改");
        }
        return mergedDuty;
    }

    @Override
    public int deleteDuty(Long id) {
        if (id == null) {
            throw new CustomException("id can't be null");
        }

        SysDuty sysDuty = this.getById(id);
        if (sysDuty == null) {
            throw new CustomException("数据不存在");
        }
        if (sysDuty.getParentId().compareTo(0L) == 0) {
            throw new CustomException("根节点不允许删除");
        }

        // 检查是否存在下级职务
        LambdaQueryWrapper<SysDuty> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysDuty::getParentId, id);
        long childrenCount = this.count(queryWrapper);
        if (childrenCount > 0) {
            throw new CustomException("存在下级职务，不能删除");
        }

        // 检查是否关联了用户
        LambdaQueryWrapper<SysUserDuty> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.eq(SysUserDuty::getDutyId, id);
        long count = iSysUserDutyService.count(queryWrapper2);
        if (count > 0) {
            throw new CustomException("该职务存在关联的用户，不能删除");
        }

        // 删除职务
        return this.removeById(id) ? 1 : 0;
    }

    /**
     * 检查职务编码是否唯一
     *
     * @param duty
     * @return
     */
    private String checkDutyCodeUnique(SysDuty duty) {
        Long dutyId = StringUtils.isNull(duty.getId()) ? -1L : duty.getId();

        LambdaQueryWrapper<SysDuty> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysDuty::getOrgId, duty.getOrgId());
        queryWrapper.eq(SysDuty::getDutyCode, duty.getDutyCode());
        SysDuty info = this.getOne(queryWrapper);
        if (StringUtils.isNotNull(info) && info.getId().compareTo(dutyId) != 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 更新子节点的祖级列表
     *
     * @param dutyId       职务ID
     * @param newAncestors 新的祖级列表
     * @param oldAncestors 需要替换的祖级列表
     */
    private void updateDutyChildrenAncestors(Long dutyId, String newAncestors, String oldAncestors) {
        List<SysDuty> childrenList = getBaseMapper().selectDescendants(dutyId);
        if (CollectionUtils.isNotEmpty(childrenList)) {
            for (SysDuty child : childrenList) {
                child.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
            }
            this.updateBatchById(childrenList);
        }
    }

    /**
     * 合并旧实体与更新命令，避免 partial payload 清空未传字段。
     *
     * @param oldDuty 已存在的职务实体
     * @param updateCommand 本次更新命令
     * @return 可安全持久化的合并结果
     */
    private SysDuty mergeDutyForUpdate(SysDuty oldDuty, SysDuty updateCommand) {
        SysDuty mergedDuty = new SysDuty();
        mergedDuty.setId(oldDuty.getId());
        mergedDuty.setParentId(updateCommand.getParentId() != null ? updateCommand.getParentId() : oldDuty.getParentId());
        mergedDuty.setAncestors(updateCommand.getAncestors() != null ? updateCommand.getAncestors() : oldDuty.getAncestors());
        mergedDuty.setDutyCode(updateCommand.getDutyCode() != null ? updateCommand.getDutyCode() : oldDuty.getDutyCode());
        mergedDuty.setDutyName(updateCommand.getDutyName() != null ? updateCommand.getDutyName() : oldDuty.getDutyName());
        mergedDuty.setOrderNum(updateCommand.getOrderNum() != null ? updateCommand.getOrderNum() : oldDuty.getOrderNum());
        mergedDuty.setOrgId(updateCommand.getOrgId() != null ? updateCommand.getOrgId() : oldDuty.getOrgId());
        return mergedDuty;
    }
}
