package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yr.common.constant.UserConstants;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysOrg;
import com.yr.common.core.domain.entity.SysRole;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.page.PageDomain;
import com.yr.common.exception.CustomException;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.domain.SysPost;
import com.yr.system.domain.SysUserRole;
import com.yr.system.domain.entity.SysDuty;
import com.yr.system.domain.entity.SysRank;
import com.yr.system.domain.entity.SysUserOrg;
import com.yr.system.domain.entity.SysUserRank;
import com.yr.system.mapper.*;
import com.yr.system.service.*;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yr.system.service.ISysUserDprService;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户 业务层处理
 *
 * @author Youngron
 */
@Service
public class SysUserDprServiceImpl implements ISysUserDprService {
    private static final Logger log = LoggerFactory.getLogger(SysUserDprServiceImpl.class);

    private final SysUserDprMapper userDprMapper;

    public SysUserDprServiceImpl(SysUserDprMapper userDprMapper) {
        this.userDprMapper = userDprMapper;
    }

    @Override
    public List<SysUser> listUserByDeptId(Long deptId) {
        return userDprMapper.listUserByDeptId(deptId);
    }

    @Override
    public List<SysUser> listUserByPostId(Long postId) {
        return userDprMapper.listUserByPostId(postId);
    }

    @Override
    public List<SysUser> listUserByRoleId(Long roleId) {
        return userDprMapper.listUserByRoleId(roleId);
    }


    @Override
    public List<SysUser> listUserByDeptCode(String deptCode) {
        return userDprMapper.listUserByDeptCode(deptCode);
    }

    @Override
    public List<SysUser> listUserByPostCode(String postCode) {
        return userDprMapper.listUserByPostCode(postCode);
    }

    @Override
    public List<SysUser> listUserByRoleKey(String roleKey) {
        return userDprMapper.listUserByRoleKey(roleKey);
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
