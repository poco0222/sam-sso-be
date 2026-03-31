/**
 * @file 客户端状态修改请求 DTO
 * @author PopoY
 * @date 2026-03-31
 */
package com.yr.web.controller.sso.dto;

import javax.validation.constraints.NotBlank;

/**
 * 客户端状态修改请求，只暴露状态字段。
 */
public class SsoClientStatusUpdateRequest {

    /** 客户端状态。 */
    @NotBlank(message = "status不能为空")
    private String status;

    /**
     * @return 客户端状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status 客户端状态
     */
    public void setStatus(String status) {
        this.status = status;
    }
}
