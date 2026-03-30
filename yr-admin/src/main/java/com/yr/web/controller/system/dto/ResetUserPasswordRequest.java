/**
 * @file 用户密码重置请求 DTO
 * @author PopoY
 * @date 2026-03-30
 */
package com.yr.web.controller.system.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 密码重置请求，只暴露密码重置所需字段。
 */
public class ResetUserPasswordRequest {

    /** 目标用户 ID。 */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 新密码。 */
    @NotBlank(message = "新密码不能为空")
    private String password;

    /** 是否首次登录；默认重置为非首次。 */
    private String firstLogin = "0";

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstLogin() {
        return firstLogin;
    }

    public void setFirstLogin(String firstLogin) {
        this.firstLogin = firstLogin;
    }
}
