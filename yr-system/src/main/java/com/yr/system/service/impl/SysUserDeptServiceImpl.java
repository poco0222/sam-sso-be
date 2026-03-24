/**
 * @file 用户部门关联服务实现
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.common.utils.SecurityUtils;
import com.yr.system.domain.entity.SysUserDept;
import com.yr.system.mapper.SysDeptMapper;
import com.yr.system.mapper.SysUserDeptMapper;
import com.yr.system.mapper.SysUserMapper;
import com.yr.system.service.ISysUserDeptService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-18 11:06
 * @description
 */

@Service
public class SysUserDeptServiceImpl extends CustomServiceImpl<SysUserDeptMapper, SysUserDept> implements ISysUserDeptService {

    private final SysUserDeptMapper sysUserDeptMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysUserMapper sysUserMapper;

    public SysUserDeptServiceImpl(SysUserDeptMapper sysUserDeptMapper,
                                  SysDeptMapper sysDeptMapper,
                                  SysUserMapper sysUserMapper) {
        this.sysUserDeptMapper = sysUserDeptMapper;
        this.sysDeptMapper = sysDeptMapper;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public IPage<SysUser> pageDeptAssignUserByDeptId(int pageNum, int pageSize, Long deptId, SysUser sysUser) {
        return sysUserDeptMapper.pageDeptAssignUserByDeptId(new Page<>(pageNum, pageSize), deptId, sysUser);
    }

    @Override
    public IPage<SysUser> pageDeptUnAssignUserByDeptId(int pageNum, int pageSize, Long deptId, SysUser sysUser) {
        SysDept sysDept = sysDeptMapper.selectDeptById(deptId);
        return sysUserDeptMapper.pageDeptUnAssignUserByDeptId(new Page<>(pageNum, pageSize), deptId, sysDept.getOrgId(), sysUser);
    }

    @Override
    public void addAssignUserDept(Long deptId, Long[] userIds) {
        if (deptId == null || userIds.length <= 0) {
            return;
        }
        List<SysUserDept> insertList = new ArrayList<>(userIds.length);
        for (Long userId : userIds) {
            SysUserDept sysUserDept = new SysUserDept();
            sysUserDept.setUserId(userId);
            sysUserDept.setDeptId(deptId);
            sysUserDept.setEnabled(1);
            insertList.add(sysUserDept);
        }
        this.saveBatch(insertList);
    }

    @Override
    public void removeAssignUserDeptByIds(Long[] ids) {
        if (ids.length <= 0) {
            return;
        }
        LambdaQueryWrapper<SysUserDept> queryWrapper = new LambdaQueryWrapper<SysUserDept>()
                .eq(SysUserDept::getIsDefault, 1)
                .in(SysUserDept::getId, Arrays.asList(ids));
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new CustomException("不能移除用户的默认部门，请先修改用户的默认部门");
        }
        this.removeByIds(Arrays.asList(ids));
    }

    @Override
    public IPage<SysDept> pageDeptByUserId(int pageNum, int pageSize, Long userId) {
        return sysUserDeptMapper.pageDeptByUserId(new Page<>(pageNum, pageSize), userId, SecurityUtils.getOrgId());
    }

    @Override
    public void addDeptForUser(Long userId, Long deptId) {
        LambdaQueryWrapper<SysUserDept> queryWrapper = new LambdaQueryWrapper<SysUserDept>()
                .eq(SysUserDept::getUserId, userId)
                .eq(SysUserDept::getDeptId, deptId);
        long count = this.count(queryWrapper);
        if (count >= 1) {
            throw new CustomException("用户已在该部门中");
        }
        SysUserDept sysUserDept = new SysUserDept();
        sysUserDept.setUserId(userId);
        sysUserDept.setDeptId(deptId);
        sysUserDept.setEnabled(1);
        this.save(sysUserDept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultUserDept(Long userId, Long deptId) {
        // 校验当前关系是否存在
        LambdaQueryWrapper<SysUserDept> queryWrapper = new LambdaQueryWrapper<SysUserDept>()
                .eq(SysUserDept::getUserId, userId)
                .eq(SysUserDept::getDeptId, deptId);
        SysUserDept sysUserDept = this.getOne(queryWrapper);
        if (sysUserDept == null) {
            throw new CustomException("未查询到数据");
        }
        if (sysUserDept.getIsDefault() != null && sysUserDept.getIsDefault().equals(1)) {
            return;
        }
        // 检查用户在当前组织是否有默认部门，如果有，就先取消掉默认部门
        Long sysUserDeptId = sysUserDeptMapper.selectDefaultUserDeptId(userId, SecurityUtils.getOrgId());
        if (sysUserDeptId != null) {
            // 旧默认部门清理失败时必须整体回滚，避免用户丢失稳定默认部门状态。
            if (sysUserDeptMapper.clearDefaultUserDept(sysUserDeptId) != 1) {
                throw new CustomException("设置失败");
            }
        }
        // 更新当前部门为默认部门
        sysUserDept.setIsDefault(1);
        if (!this.updateById(sysUserDept)) {
            throw new CustomException("设置失败");
        }
    }

    @Override
    public List<SysDept> selectDeptByUserIdAndOrgId(Long userId, Long orgId, Boolean defaultDept) {
        return sysUserDeptMapper.selectDeptByUserIdAndOrgId(userId, orgId, defaultDept ? "Y" : "N");
    }



}
