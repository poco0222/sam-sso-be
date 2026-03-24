/**
 * @file 锁定 SysUserServiceImpl 角色组拼接行为
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysRole;
import com.yr.system.mapper.SysPostMapper;
import com.yr.system.mapper.SysRoleMapper;
import com.yr.system.mapper.SysUserMapper;
import com.yr.system.mapper.SysUserPostMapper;
import com.yr.system.mapper.SysUserRoleMapper;
import com.yr.system.service.ISysOrgService;
import com.yr.system.service.ISysUserDeptService;
import com.yr.system.service.ISysUserDutyService;
import com.yr.system.service.ISysUserRankService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SysUserServiceImpl 角色组行为测试。
 */
class SysUserServiceImplRoleGroupTest {

    /**
     * 验证角色组会按查询结果顺序拼接，且末尾不带逗号。
     */
    @Test
    void shouldJoinRoleNamesWithoutTrailingComma() {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserServiceImpl service = buildService(roleMapper);
        when(roleMapper.selectRolesByUserName("demo"))
                .thenReturn(List.of(buildRole("管理员"), buildRole("审计员")));

        String roleGroup = service.selectUserRoleGroup("demo");

        assertThat(roleGroup).isEqualTo("管理员,审计员");
    }

    /**
     * 验证没有角色时返回空字符串而不是 null。
     */
    @Test
    void shouldReturnEmptyStringWhenUserHasNoRoles() {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserServiceImpl service = buildService(roleMapper);
        when(roleMapper.selectRolesByUserName("demo")).thenReturn(Collections.emptyList());

        String roleGroup = service.selectUserRoleGroup("demo");

        assertThat(roleGroup).isEmpty();
    }

    /**
     * 构造最小依赖的用户服务实例。
     *
     * @param roleMapper 角色 Mapper
     * @return 用户服务实例
     */
    private SysUserServiceImpl buildService(SysRoleMapper roleMapper) {
        return new SysUserServiceImpl(
                mock(SysUserMapper.class),
                roleMapper,
                mock(SysPostMapper.class),
                mock(SysUserRoleMapper.class),
                mock(SysUserPostMapper.class),
                mock(ISysUserDeptService.class),
                mock(ISysUserRankService.class),
                mock(ISysOrgService.class),
                mock(ISysUserDutyService.class),
                mock(SysUserWriteService.class),
                mock(SysUserImportService.class),
                mock(SysUserQueryService.class)
        );
    }

    /**
     * 构造最小角色对象。
     *
     * @param roleName 角色名称
     * @return 角色对象
     */
    private SysRole buildRole(String roleName) {
        SysRole role = new SysRole();
        role.setRoleName(roleName);
        return role;
    }
}
