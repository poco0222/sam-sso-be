/**
 * @file 用户编辑写边界契约测试
 * @author PopoY
 * @date 2026-03-31
 */
package com.yr.web.controller.system;

import com.yr.common.constant.UserConstants;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.web.controller.system.dto.UpdateUserRequest;
import com.yr.system.service.ISysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定普通用户编辑入口不能继续承担密码 / 状态等高风险字段写入。
 */
class SysUserControllerEditWriteBoundaryContractTest {

    /**
     * 每个用例后清理安全上下文，避免 updateBy 串用。
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 验证普通编辑入口只允许下沉基础资料字段，敏感写字段必须被拒绝或忽略。
     */
    @Test
    void shouldNotForwardSensitiveWriteFieldsWhenEditingUser() {
        SysUserController controller = new SysUserController();
        ISysUserService userService = mock(ISysUserService.class);
        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        UpdateUserRequest request = new UpdateUserRequest();

        setAuthenticatedUser("task2-editor");
        request.setUserId(7L);
        request.setUserName("phase2-user");
        request.setNickName("一期用户");
        request.setEmail("phase2@example.com");
        request.setPhonenumber("13800138000");
        request.setSex("0");
        request.setDeptId(88L);
        request.setRemark("允许保留的普通备注");
        ReflectionTestUtils.setField(controller, "userService", userService);
        doNothing().when(userService).checkUserAllowed(any(SysUser.class));
        when(userService.checkPhoneUnique(any(SysUser.class))).thenReturn(UserConstants.UNIQUE);
        when(userService.checkEmailUnique(any(SysUser.class))).thenReturn(UserConstants.UNIQUE);
        when(userService.updateUser(any(SysUser.class))).thenReturn(1);

        AjaxResult result = controller.edit(request);

        verify(userService).updateUser(userCaptor.capture());
        assertThat(result.get("code")).isEqualTo(200);
        assertThat(userCaptor.getValue().getUserId()).isEqualTo(7L);
        assertThat(userCaptor.getValue().getUserName()).isEqualTo("phase2-user");
        assertThat(userCaptor.getValue().getNickName()).isEqualTo("一期用户");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("phase2@example.com");
        assertThat(userCaptor.getValue().getPhonenumber()).isEqualTo("13800138000");
        assertThat(userCaptor.getValue().getSex()).isEqualTo("0");
        assertThat(userCaptor.getValue().getDeptId()).isEqualTo(88L);
        assertThat(userCaptor.getValue().getRemark()).isEqualTo("允许保留的普通备注");
        assertThat(userCaptor.getValue().getUpdateBy()).isEqualTo("task2-editor");
        assertThat(userCaptor.getValue().getPassword()).as("普通编辑不应透传 password").isNull();
        assertThat(userCaptor.getValue().getStatus()).as("普通编辑不应透传 status").isNull();
        assertThat(userCaptor.getValue().getLoginIp()).as("普通编辑不应透传 loginIp").isNull();
        assertThat(userCaptor.getValue().getLoginDate()).as("普通编辑不应透传 loginDate").isNull();
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
