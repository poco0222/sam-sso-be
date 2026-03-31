/**
 * @file 用户密码重置契约测试
 * @author PopoY
 * @date 2026-03-30
 */
package com.yr.web.controller.system;

import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.utils.SecurityUtils;
import com.yr.web.controller.system.dto.ResetUserPasswordRequest;
import com.yr.system.service.ISysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定密码重置入口的最小输入契约，避免 over-posting 字段透传到 service。
 */
class SysUserControllerPasswordResetContractTest {

    /**
     * 每个用例后清理安全上下文，避免 updateBy 读取串用。
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 验证 resetPwd 只允许把密码与首次登录标志传给 service，其他敏感字段必须被拦截。
     */
    @Test
    void shouldOnlyForwardPasswordAndFirstLoginFieldsWhenResettingPassword() {
        SysUserController controller = new SysUserController();
        ISysUserService userService = mock(ISysUserService.class);
        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        ResetUserPasswordRequest request = new ResetUserPasswordRequest();

        setAuthenticatedUser("phase1-operator");
        request.setUserId(7L);
        request.setPassword("Plain@123");
        request.setFirstLogin("0");
        ReflectionTestUtils.setField(controller, "userService", userService);
        when(userService.resetPwd(org.mockito.ArgumentMatchers.any(SysUser.class))).thenReturn(1);

        AjaxResult result = controller.resetPwd(request);

        verify(userService).resetPwd(userCaptor.capture());
        assertThat(result.get("code")).isEqualTo(200);
        assertThat(userCaptor.getValue().getUserId()).isEqualTo(7L);
        assertThat(userCaptor.getValue().getFirstLogin()).isEqualTo("0");
        assertThat(userCaptor.getValue().getUpdateBy()).isEqualTo("phase1-operator");
        assertThat(SecurityUtils.matchesPassword("Plain@123", userCaptor.getValue().getPassword())).isTrue();
        assertThat(userCaptor.getValue().getStatus()).as("密码重置不应透传 status").isNull();
        assertThat(userCaptor.getValue().getEmail()).as("密码重置不应透传 email").isNull();
        assertThat(userCaptor.getValue().getDeptId()).as("密码重置不应透传 deptId").isNull();
        assertThat(userCaptor.getValue().getLoginIp()).as("密码重置不应透传 loginIp").isNull();
        assertThat(userCaptor.getValue().getLoginDate()).as("密码重置不应透传 loginDate").isNull();
        assertThat(userCaptor.getValue().getRemark()).as("密码重置不应透传 remark").isNull();
    }

    /**
     * 写入最小登录态，让 SecurityUtils.getUsername() 返回稳定操作人。
     *
     * @param username 当前用户名
     */
    private void setAuthenticatedUser(String username) {
        SysUser currentUser = new SysUser();
        currentUser.setUserId(99L);
        currentUser.setUserName(username);
        LoginUser loginUser = new LoginUser(currentUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList())
        );
    }
}
