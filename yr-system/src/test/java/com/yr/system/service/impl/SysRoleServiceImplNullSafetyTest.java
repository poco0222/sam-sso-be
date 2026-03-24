/**
 * @file 验证 SysRoleServiceImpl 在角色菜单与数据权限写入时的空值安全契约
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysRole;
import com.yr.system.mapper.SysRoleDeptMapper;
import com.yr.system.mapper.SysRoleMapper;
import com.yr.system.mapper.SysRoleMenuMapper;
import com.yr.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * SysRoleServiceImpl 空值安全测试。
 */
class SysRoleServiceImplNullSafetyTest {

    /**
     * 验证菜单 ID 缺失时会被视为空集合，而不是触发空指针。
     */
    @Test
    void shouldTreatMissingMenuIdsAsEmptyWhenInsertingRoleMenu() {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysRoleMenuMapper roleMenuMapper = mock(SysRoleMenuMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysRoleDeptMapper roleDeptMapper = mock(SysRoleDeptMapper.class);
        SysRoleServiceImpl roleService = buildRoleService(roleMapper, roleMenuMapper, userRoleMapper, roleDeptMapper);
        SysRole role = new SysRole();
        role.setRoleId(10L);
        role.setMenuIds(null);

        assertThatCode(() -> {
            int rows = roleService.insertRoleMenu(role);
            assertThat(rows).isEqualTo(1);
        }).doesNotThrowAnyException();
        verify(roleMenuMapper, never()).batchRoleMenu(org.mockito.ArgumentMatchers.anyList());
    }

    /**
     * 验证部门 ID 缺失时会被视为空集合，而不是触发空指针。
     */
    @Test
    void shouldTreatMissingDeptIdsAsEmptyWhenAuthorizingDataScope() {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysRoleMenuMapper roleMenuMapper = mock(SysRoleMenuMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysRoleDeptMapper roleDeptMapper = mock(SysRoleDeptMapper.class);
        SysRoleServiceImpl roleService = buildRoleService(roleMapper, roleMenuMapper, userRoleMapper, roleDeptMapper);
        SysRole role = new SysRole();
        role.setRoleId(20L);
        role.setDeptIds(null);

        assertThatCode(() -> {
            int rows = roleService.authDataScope(role);
            assertThat(rows).isEqualTo(1);
        }).doesNotThrowAnyException();
        verify(roleDeptMapper, never()).batchRoleDept(org.mockito.ArgumentMatchers.anyList());
    }

    /**
     * 构造最小角色服务实例，便于聚焦空值安全行为。
     *
     * @param roleMapper 角色 Mapper
     * @param roleMenuMapper 角色菜单 Mapper
     * @param userRoleMapper 用户角色 Mapper
     * @param roleDeptMapper 角色部门 Mapper
     * @return 待测服务实例
     */
    private SysRoleServiceImpl buildRoleService(SysRoleMapper roleMapper,
                                                SysRoleMenuMapper roleMenuMapper,
                                                SysUserRoleMapper userRoleMapper,
                                                SysRoleDeptMapper roleDeptMapper) {
        return new SysRoleServiceImpl(
                roleMapper,
                roleMenuMapper,
                userRoleMapper,
                roleDeptMapper,
                mock(com.yr.system.service.ISysRoleService.class)
        );
    }
}
