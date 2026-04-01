/**
 * @file 组织状态修改请求 DTO
 * @author PopoY
 * @date 2026-04-01
 */
package com.yr.web.controller.system.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yr.common.utils.StringUtils;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 组织状态修改请求，只暴露状态切换必需字段。
 */
public class SysOrgStatusUpdateRequest {

    /** 组织 ID。 */
    @NotNull(message = "组织ID不能为空")
    private Long orgId;

    /** 目标状态。 */
    @NotBlank(message = "status不能为空")
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

    /**
     * 校验状态值只允许为一期约定的启停标识。
     * 这里显式跳过 blank（空白）场景，让 `@NotBlank` 优先产出“status不能为空”，
     * 避免空字符串被模式校验抢先覆盖成 allowed-value（允许值）错误。
     *
     * @return `status` 为空时交给 `@NotBlank` 处理；非空时仅允许 `0/1`
     * @author PopoY
     */
    @JsonIgnore
    @AssertTrue(message = "status只允许为0或1")
    public boolean isStatusValueAllowed() {
        if (StringUtils.isBlank(status)) {
            return true;
        }
        return "0".equals(status) || "1".equals(status);
    }
}
