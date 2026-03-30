/**
 * @file 切换组织安全回归测试
 * @author PopoY
 * @date 2026-03-30
 */
package com.yr.framework.web.service;

import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.core.redis.RedisCache;
import com.yr.common.exception.CustomException;
import com.yr.common.core.domain.entity.SysOrg;
import com.yr.system.mapper.SysOrgMapper;
import com.yr.system.mapper.SysUserOrgMapper;
import com.yr.system.service.ISysUserOrgService;
import com.yr.system.service.ISysUserService;
import com.yr.system.service.impl.SysUserOrgServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;

/**
 * 锁定 changeOrg 的一期安全边界，避免继续接受任意 orgId 或漏删旧 token。
 */
class SysLoginServiceChangeOrgSecurityTest {

    /**
     * 每个用例后清理安全上下文，避免旧 token 和登录态污染其他测试。
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 验证切组织成功时必须删除当前登录态对应的旧 token cache key。
     */
    @Test
    void shouldDeleteCurrentLoginTokenCacheKeyWhenChangeOrgSucceeds() {
        SysLoginService service = new SysLoginService();
        TokenService tokenService = mock(TokenService.class);
        RedisCache redisCache = mock(RedisCache.class);
        ISysUserService userService = mock(ISysUserService.class);
        ISysUserOrgService sysUserOrgService = mock(ISysUserOrgService.class);
        SysPermissionService permissionService = mock(SysPermissionService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        SysUser currentUser = buildUser(7L, "phase1-user", 11L);
        SysUser switchedUser = buildUser(7L, "phase1-user", 88L);

        setAuthenticatedUser(currentUser, "legacy-token");
        when(sysUserOrgService.hasActiveOrgMembership(7L, 88L)).thenReturn(true);
        when(userService.selectUserByUserName("phase1-user", 88L)).thenReturn(switchedUser);
        when(permissionService.getMenuPermission(switchedUser)).thenReturn(Collections.emptySet());
        when(permissionService.getRolePermission(switchedUser)).thenReturn(Collections.emptySet());
        when(tokenService.createToken(any(LoginUser.class))).thenReturn("new-token");
        ReflectionTestUtils.setField(service, "tokenService", tokenService);
        ReflectionTestUtils.setField(service, "redisCache", redisCache);
        ReflectionTestUtils.setField(service, "userService", userService);
        ReflectionTestUtils.setField(service, "sysUserOrgService", sysUserOrgService);
        ReflectionTestUtils.setField(service, "permissionService", permissionService);
        ReflectionTestUtils.setField(service, "userDetailsService", userDetailsService);

        String token = service.changeOrg(88L);

        assertThat(token).isEqualTo("new-token");
        verify(tokenService).delLoginUser("legacy-token");
        verify(tokenService).createToken(any(LoginUser.class));
    }

    /**
     * 验证非归属组织无法解析到目标登录上下文时，必须拒绝继续签发 token。
     */
    @Test
    void shouldRejectSigningTokenWhenTargetOrgDoesNotBelongToCurrentUser() {
        SysLoginService service = new SysLoginService();
        TokenService tokenService = mock(TokenService.class);
        RedisCache redisCache = mock(RedisCache.class);
        ISysUserService userService = mock(ISysUserService.class);
        ISysUserOrgService sysUserOrgService = mock(ISysUserOrgService.class);
        SysPermissionService permissionService = mock(SysPermissionService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        SysUser currentUser = buildUser(7L, "phase1-user", 11L);

        setAuthenticatedUser(currentUser, "legacy-token");
        when(sysUserOrgService.hasActiveOrgMembership(7L, 999L)).thenReturn(false);
        ReflectionTestUtils.setField(service, "tokenService", tokenService);
        ReflectionTestUtils.setField(service, "redisCache", redisCache);
        ReflectionTestUtils.setField(service, "userService", userService);
        ReflectionTestUtils.setField(service, "sysUserOrgService", sysUserOrgService);
        ReflectionTestUtils.setField(service, "permissionService", permissionService);
        ReflectionTestUtils.setField(service, "userDetailsService", userDetailsService);

        assertThatThrownBy(() -> service.changeOrg(999L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("目标组织");
        verify(redisCache, never()).deleteObject(anyString());
        verify(tokenService, never()).createToken(any(LoginUser.class));
    }

    /**
     * 验证目标组织已停用时，即便 membership（归属关系）仍启用也必须拒绝签发 token。
     *
     * @author PopoY
     */
    @Test
    void shouldRejectDisabledTargetOrgEvenWhenMembershipIsEnabled() {
        SysLoginService service = new SysLoginService();
        TokenService tokenService = mock(TokenService.class);
        RedisCache redisCache = mock(RedisCache.class);
        ISysUserService userService = mock(ISysUserService.class);
        ISysUserOrgService sysUserOrgService = buildUserOrgServiceForDisabledOrg();
        SysPermissionService permissionService = mock(SysPermissionService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        SysUser currentUser = buildUser(7L, "phase1-user", 11L);

        // author: PopoY，关系表 enabled=1 且 org.status=1，模拟“成员关系存在但目标组织已停用”的场景。
        setAuthenticatedUser(currentUser, "legacy-token");
        when(permissionService.getMenuPermission(any(SysUser.class))).thenReturn(Collections.emptySet());
        when(permissionService.getRolePermission(any(SysUser.class))).thenReturn(Collections.emptySet());
        when(tokenService.createToken(any(LoginUser.class))).thenReturn("new-token");
        ReflectionTestUtils.setField(service, "tokenService", tokenService);
        ReflectionTestUtils.setField(service, "redisCache", redisCache);
        ReflectionTestUtils.setField(service, "userService", userService);
        ReflectionTestUtils.setField(service, "sysUserOrgService", sysUserOrgService);
        ReflectionTestUtils.setField(service, "permissionService", permissionService);
        ReflectionTestUtils.setField(service, "userDetailsService", userDetailsService);

        assertThatThrownBy(() -> service.changeOrg(88L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("停用");
        verify(tokenService, never()).createToken(any(LoginUser.class));
        verify(userService, never()).selectUserByUserName(anyString(), anyLong());
    }

    /**
     * 构造最小用户对象，便于锁定 changeOrg 的安全行为。
     *
     * @param userId 用户 ID
     * @param userName 用户名
     * @param orgId 当前组织 ID
     * @return 最小用户对象
     */
    private SysUser buildUser(Long userId, String userName, Long orgId) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName(userName);
        user.setOrgId(orgId);
        user.setPassword("encoded-password");
        return user;
    }

    /**
     * 写入当前登录态，供 SecurityUtils 在 changeOrg 中读取用户名和旧 token。
     *
     * @param currentUser 当前登录用户
     * @param token 当前登录 token
     */
    private void setAuthenticatedUser(SysUser currentUser, String token) {
        LoginUser loginUser = new LoginUser(currentUser, Collections.emptySet());
        loginUser.setToken(token);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList())
        );
    }

    /**
     * 构造显式包含 `org.status = 1` 信号的组织归属服务桩，避免测试只绑定控制器文案。
     *
     * @return 带停用组织校验语义的组织归属服务
     */
    private ISysUserOrgService buildUserOrgServiceForDisabledOrg() {
        SysUserOrgMapper sysUserOrgMapper = mock(SysUserOrgMapper.class);
        SysOrgMapper sysOrgMapper = mock(SysOrgMapper.class);
        SysUserOrgServiceImpl userOrgService = spy(new SysUserOrgServiceImpl(sysUserOrgMapper, sysOrgMapper));
        SysOrg disabledOrg = new SysOrg();

        disabledOrg.setOrgId(88L);
        disabledOrg.setOrgName("已停用组织");
        disabledOrg.setStatus("1");
        doReturn(1L).when(userOrgService).count(any());
        when(sysOrgMapper.selectSysOrgById(88L)).thenReturn(disabledOrg);
        return userOrgService;
    }
}
