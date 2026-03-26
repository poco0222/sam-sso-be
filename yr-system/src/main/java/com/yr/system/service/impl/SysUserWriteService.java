/**
 * @file 用户写入事务服务，负责用户主表及关联关系的一致性提交
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.exception.CustomException;
import com.yr.common.utils.SecurityUtils;
import com.yr.system.domain.entity.SysUserOrg;
import com.yr.system.mapper.SysUserMapper;
import com.yr.system.service.ISysUserOrgService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户写入事务服务。
 *
 * <p>该服务显式承接新增/修改用户的事务边界，避免在导入流程中发生自调用导致事务失效。</p>
 */
@Service
public class SysUserWriteService {

    /** 用户主表持久层。 */
    private final SysUserMapper userMapper;

    /** 用户组织关联服务。 */
    private final ISysUserOrgService sysUserOrgService;

    /**
     * 构造用户写入事务服务。
     *
     * @param userMapper 用户主表持久层
     * @param sysUserOrgService 用户组织关联服务
     */
    public SysUserWriteService(
            SysUserMapper userMapper,
            ISysUserOrgService sysUserOrgService
    ) {
        this.userMapper = userMapper;
        this.sysUserOrgService = sysUserOrgService;
    }

    /**
     * 新增用户及其组织关联。
     *
     * @param user 用户信息
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int insertUser(SysUser user) {
        // 一期只保留用户主表与组织关系写入，避免继续耦合岗位/职务/职级链路。
        int rows = userMapper.insertUser(user);

        SysUserOrg sysUserOrg = new SysUserOrg();
        sysUserOrg.setUserId(user.getUserId());
        sysUserOrg.setOrgId(SecurityUtils.getOrgId());
        LambdaQueryWrapper<SysUserOrg> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUserOrg::getUserId, user.getUserId());
        queryWrapper.eq(SysUserOrg::getIsDefault, 1);
        long count = sysUserOrgService.count(queryWrapper);
        if (count <= 0) {
            sysUserOrg.setIsDefault(1);
        }
        sysUserOrgService.addSysUserOrg(sysUserOrg);
        return rows;
    }

    /**
     * 修改用户主表信息。
     *
     * @param user 用户信息
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateUser(SysUser user) {
        if (user.getUserId() == null) {
            throw new CustomException("用户ID不能为空");
        }

        return userMapper.updateUser(user);
    }
}
