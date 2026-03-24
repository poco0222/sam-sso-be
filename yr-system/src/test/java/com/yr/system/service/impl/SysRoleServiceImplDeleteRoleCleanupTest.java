/**
 * @file 验证 SysRoleServiceImpl 批量删除角色时会同步清理部门关联
 * @author PopoY
 * @date 2026-03-17
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysRole;
import com.yr.system.mapper.SysRoleDeptMapper;
import com.yr.system.mapper.SysRoleMapper;
import com.yr.system.mapper.SysRoleMenuMapper;
import com.yr.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 角色批量删除关联清理契约测试。
 */
class SysRoleServiceImplDeleteRoleCleanupTest {

    /**
     * 验证批量删角色时会同步清理菜单关联和部门关联。
     */
    @Test
    void shouldDeleteDeptRelationsBeforeRemovingRoles() {
        Long[] roleIds = {10L, 11L};
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysRoleMenuMapper roleMenuMapper = mock(SysRoleMenuMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysRoleDeptMapper roleDeptMapper = mock(SysRoleDeptMapper.class);
        SysRoleServiceImpl roleService = new SysRoleServiceImpl(
                roleMapper,
                roleMenuMapper,
                userRoleMapper,
                roleDeptMapper,
                mock(com.yr.system.service.ISysRoleService.class)
        );

        when(roleMapper.selectRoleById(10L)).thenReturn(buildRole(10L, "角色A"));
        when(roleMapper.selectRoleById(11L)).thenReturn(buildRole(11L, "角色B"));
        when(userRoleMapper.countUserRoleByRoleId(10L)).thenReturn(0);
        when(userRoleMapper.countUserRoleByRoleId(11L)).thenReturn(0);
        when(roleMapper.deleteRoleByIds(roleIds)).thenReturn(2);

        int rows = roleService.deleteRoleByIds(roleIds);

        assertThat(rows).isEqualTo(2);
        verify(roleMenuMapper).deleteRoleMenu(roleIds);
        verify(roleDeptMapper).deleteRoleDept(roleIds);
        verify(roleMapper).deleteRoleByIds(roleIds);
    }

    /**
     * 构造测试角色，避免命中管理员保护分支。
     *
     * @param roleId 角色 ID
     * @param roleName 角色名称
     * @return 角色对象
     */
    private SysRole buildRole(Long roleId, String roleName) {
        SysRole role = new SysRole();
        role.setRoleId(roleId);
        role.setRoleName(roleName);
        return role;
    }
}
