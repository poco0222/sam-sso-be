/**
 * @file 验证 SysUserServiceImpl 批量删除用户时会同步清理关联表
 * @author Codex
 * @date 2026-03-17
 */
package com.yr.system.service.impl;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户批量删除关联清理契约测试。
 */
class SysUserServiceImplDeleteUserCleanupTest {

    /**
     * 验证批量删用户时会先清理角色与岗位关联，再删除用户主记录。
     */
    @Test
    void shouldDeleteRoleAndPostRelationsBeforeRemovingUsers() {
        Long[] userIds = {10L, 11L};
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserRoleMapper userRoleMapper = mock(SysUserRoleMapper.class);
        SysUserPostMapper userPostMapper = mock(SysUserPostMapper.class);
        SysUserServiceImpl userService = buildUserService(userMapper, userRoleMapper, userPostMapper);

        when(userMapper.deleteUserByIds(userIds)).thenReturn(2);

        int rows = userService.deleteUserByIds(userIds);

        assertThat(rows).isEqualTo(2);
        verify(userRoleMapper).deleteUserRole(userIds);
        verify(userPostMapper).deleteUserPost(userIds);
        verify(userMapper).deleteUserByIds(userIds);
    }

    /**
     * 构造仅关注删除清理行为的用户服务实例。
     *
     * @param userMapper 用户 Mapper
     * @param userRoleMapper 用户角色 Mapper
     * @param userPostMapper 用户岗位 Mapper
     * @return 用户服务实例
     */
    private SysUserServiceImpl buildUserService(SysUserMapper userMapper,
                                                SysUserRoleMapper userRoleMapper,
                                                SysUserPostMapper userPostMapper) {
        return new SysUserServiceImpl(
                userMapper,
                mock(SysRoleMapper.class),
                mock(SysPostMapper.class),
                userRoleMapper,
                userPostMapper,
                mock(ISysUserDeptService.class),
                mock(ISysUserRankService.class),
                mock(ISysOrgService.class),
                mock(ISysUserDutyService.class),
                mock(SysUserWriteService.class),
                mock(SysUserImportService.class),
                mock(SysUserQueryService.class)
        );
    }
}
