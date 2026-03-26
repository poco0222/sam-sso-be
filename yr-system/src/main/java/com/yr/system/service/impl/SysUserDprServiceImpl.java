/**
 * @file 用户 DPR 查询实现，一期只保留部门维度的派生查询
 * @author PopoY
 * @date 2026-03-26
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysUser;
import com.yr.system.mapper.SysUserDprMapper;
import org.springframework.stereotype.Service;
import com.yr.system.service.ISysUserDprService;

import java.util.List;

/**
 * 用户 业务层处理
 *
 * @author Youngron
 */
@Service
public class SysUserDprServiceImpl implements ISysUserDprService {
    private final SysUserDprMapper userDprMapper;

    public SysUserDprServiceImpl(SysUserDprMapper userDprMapper) {
        this.userDprMapper = userDprMapper;
    }

    @Override
    public List<SysUser> listUserByDeptId(Long deptId) {
        return userDprMapper.listUserByDeptId(deptId);
    }

    @Override
    public List<SysUser> listUserByDeptCode(String deptCode) {
        return userDprMapper.listUserByDeptCode(deptCode);
    }

    @Override
    public List<SysUser> listUserByDeptIdWithAncestors(Long deptId) {
        return userDprMapper.listUserByDeptIdWithAncestors(deptId);
    }

    @Override
    public List<SysUser> listUserByDeptIdWithDescenders(Long deptId) {
        return userDprMapper.listUserByDeptIdWithDescenders(deptId);
    }

    @Override
    public List<SysUser> listUserByDeptCodeWithAncestors(String deptCode) {
        return userDprMapper.listUserByDeptCodeWithAncestors(deptCode);
    }

    @Override
    public List<SysUser> listUserByDeptCodeWithDescenders(String deptCode) {
        return userDprMapper.listUserByDeptCodeWithDescenders(deptCode);
    }
}
