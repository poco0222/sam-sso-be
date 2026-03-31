/**
 * @file 个人资料写边界契约测试
 * @author PopoY
 * @date 2026-03-31
 */
package com.yr.web.controller.system;

import com.yr.common.constant.UserConstants;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.framework.web.service.TokenService;
import com.yr.web.controller.system.dto.UpdateProfileRequest;
import com.yr.system.service.ISysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定个人资料入口只能修改自助资料字段，禁止越界写入用户其它属性。
 */
class SysProfileControllerWriteBoundaryContractTest {

    /**
     * 每个用例后清理 RequestContext（请求上下文），避免 ServletUtils 读取串用。
     */
    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * 验证 profile 更新只允许下沉个人资料字段，其余字段必须被拒绝或忽略。
     */
    @Test
    void shouldOnlyForwardProfileFieldsWhenUpdatingProfile() {
        SysProfileController controller = new SysProfileController();
        ISysUserService userService = mock(ISysUserService.class);
        TokenService tokenService = mock(TokenService.class);
        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        UpdateProfileRequest request = new UpdateProfileRequest();
        LoginUser loginUser = createLoginUser(9L, "profile-owner");

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        request.setNickName("新昵称");
        request.setPhonenumber("13900139000");
        request.setEmail("profile@example.com");
        request.setSex("1");

        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "tokenService", tokenService);
        when(tokenService.getLoginUser(any())).thenReturn(loginUser);
        when(userService.checkPhoneUnique(any(SysUser.class))).thenReturn(UserConstants.UNIQUE);
        when(userService.checkEmailUnique(any(SysUser.class))).thenReturn(UserConstants.UNIQUE);
        when(userService.updateUserProfile(any(SysUser.class))).thenReturn(1);

        AjaxResult result = controller.updateProfile(request);

        verify(userService).updateUserProfile(userCaptor.capture());
        assertThat(result.get("code")).isEqualTo(200);
        assertThat(userCaptor.getValue().getUserId()).isEqualTo(9L);
        assertThat(userCaptor.getValue().getNickName()).isEqualTo("新昵称");
        assertThat(userCaptor.getValue().getPhonenumber()).isEqualTo("13900139000");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("profile@example.com");
        assertThat(userCaptor.getValue().getSex()).isEqualTo("1");
        assertThat(userCaptor.getValue().getPassword()).as("profile 不应透传 password").isNull();
        assertThat(userCaptor.getValue().getUserName()).as("profile 不应透传 userName").isNull();
        assertThat(userCaptor.getValue().getDeptId()).as("profile 不应透传 deptId").isNull();
        assertThat(userCaptor.getValue().getStatus()).as("profile 不应透传 status").isNull();
        assertThat(userCaptor.getValue().getRemark()).as("profile 不应透传 remark").isNull();
        assertThat(userCaptor.getValue().getLoginIp()).as("profile 不应透传 loginIp").isNull();
        assertThat(userCaptor.getValue().getLoginDate()).as("profile 不应透传 loginDate").isNull();
    }

    /**
     * 构造当前登录用户，供 profile 入口绑定 userId。
     *
     * @param userId 当前用户 ID
     * @param username 当前用户名
     * @return 登录态对象
     */
    private LoginUser createLoginUser(Long userId, String username) {
        SysUser currentUser = new SysUser();
        currentUser.setUserId(userId);
        currentUser.setUserName(username);
        return new LoginUser(currentUser, Collections.emptySet());
    }
}
