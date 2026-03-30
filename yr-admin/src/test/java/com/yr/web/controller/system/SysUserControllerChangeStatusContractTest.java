/**
 * @file 用户状态修改契约测试
 * @author PopoY
 * @date 2026-03-30
 */
package com.yr.web.controller.system;

import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.web.controller.system.dto.ChangeUserStatusRequest;
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
 * 锁定 changeStatus 只能修改用户状态，避免继续透传通用用户字段。
 */
class SysUserControllerChangeStatusContractTest {

    /**
     * 每个用例后清理安全上下文，避免 updateBy 串用。
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 验证 changeStatus 只允许转发 userId/status/updateBy。
     */
    @Test
    void shouldOnlyForwardUserIdAndStatusWhenChangingStatus() {
        SysUserController controller = new SysUserController();
        ISysUserService userService = mock(ISysUserService.class);
        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        ChangeUserStatusRequest request = new ChangeUserStatusRequest();

        setAuthenticatedUser("phase2-operator");
        request.setUserId(7L);
        request.setStatus("1");
        ReflectionTestUtils.setField(controller, "userService", userService);
        when(userService.updateUserStatus(org.mockito.ArgumentMatchers.any(SysUser.class))).thenReturn(1);

        AjaxResult result = controller.changeStatus(request);

        verify(userService).updateUserStatus(userCaptor.capture());
        assertThat(result.get("code")).isEqualTo(200);
        assertThat(userCaptor.getValue().getUserId()).isEqualTo(7L);
        assertThat(userCaptor.getValue().getStatus()).isEqualTo("1");
        assertThat(userCaptor.getValue().getUpdateBy()).isEqualTo("phase2-operator");
        assertThat(userCaptor.getValue().getEmail()).as("状态修改不应透传 email").isNull();
        assertThat(userCaptor.getValue().getDeptId()).as("状态修改不应透传 deptId").isNull();
        assertThat(userCaptor.getValue().getRemark()).as("状态修改不应透传 remark").isNull();
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
