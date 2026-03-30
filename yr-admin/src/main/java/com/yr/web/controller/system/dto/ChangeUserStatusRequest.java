/**
 * @file 用户状态修改请求 DTO
 * @author PopoY
 * @date 2026-03-30
 */
package com.yr.web.controller.system.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 用户状态修改请求，只暴露状态切换所需字段。
 */
public class ChangeUserStatusRequest {

    /** 目标用户 ID。 */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 目标状态。 */
    @NotBlank(message = "用户状态不能为空")
    private String status;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
