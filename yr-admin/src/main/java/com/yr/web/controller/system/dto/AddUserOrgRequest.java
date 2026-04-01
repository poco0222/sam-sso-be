/**
 * @file 用户组织新增请求 DTO
 * @author PopoY
 * @date 2026-04-01
 */
package com.yr.web.controller.system.dto;

import javax.validation.constraints.NotNull;

/**
 * 用户组织新增请求，只暴露 userId/orgId 两个必要字段。
 */
public class AddUserOrgRequest {

    /** 用户 ID。 */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /** 组织 ID。 */
    @NotNull(message = "组织ID不能为空")
    private Long orgId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }
}
