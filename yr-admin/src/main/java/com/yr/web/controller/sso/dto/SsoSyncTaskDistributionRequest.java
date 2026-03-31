/**
 * @file 分发任务请求 DTO
 * @author PopoY
 * @date 2026-03-31
 */
package com.yr.web.controller.sso.dto;

import javax.validation.constraints.NotBlank;

/**
 * 分发任务请求，只暴露目标客户端编码。
 */
public class SsoSyncTaskDistributionRequest {

    /** 目标客户端编码。 */
    @NotBlank(message = "targetClientCode不能为空")
    private String targetClientCode;

    /**
     * @return 目标客户端编码
     */
    public String getTargetClientCode() {
        return targetClientCode;
    }

    /**
     * @param targetClientCode 目标客户端编码
     */
    public void setTargetClientCode(String targetClientCode) {
        this.targetClientCode = targetClientCode;
    }
}
