/**
 * @file 切换组织安全回归测试
 * @author PopoY
 * @date 2026-03-30
 */
package com.yr.framework.web.service;

import com.yr.common.constant.Constants;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.core.redis.RedisCache;
import com.yr.common.exception.CustomException;
import com.yr.system.service.ISysUserOrgService;
import com.yr.system.service.ISysUserService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(sysUserOrgService.hasEnabledOrgMembership(7L, 88L)).thenReturn(true);
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
        when(sysUserOrgService.hasEnabledOrgMembership(7L, 999L)).thenReturn(false);
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
}
