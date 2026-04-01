/**
 * @file 组织状态修改请求 DTO
 * @author PopoY
 * @date 2026-04-01
 */
package com.yr.web.controller.system.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 组织状态修改请求，只暴露状态切换必需字段。
 */
public class SysOrgStatusUpdateRequest {

    /** 组织 ID。 */
    @NotNull(message = "组织ID不能为空")
    private Long orgId;

    /** 目标状态。 */
    @NotBlank(message = "status不能为空")
    @Pattern(regexp = "0|1", message = "status只允许为0或1")
    private String status;

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
