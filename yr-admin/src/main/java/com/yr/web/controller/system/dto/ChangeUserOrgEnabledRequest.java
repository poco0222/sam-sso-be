/**
 * @file 用户组织启停请求 DTO
 * @author PopoY
 * @date 2026-04-01
 */
package com.yr.web.controller.system.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 用户组织启停请求，只暴露 id/enabled 两个必要字段。
 */
public class ChangeUserOrgEnabledRequest {

    /** 关联主键 ID。 */
    @NotNull(message = "主键ID不能为空")
    private Long id;

    /** 启停状态。 */
    @NotNull(message = "enabled不能为空")
    @Min(value = 0, message = "enabled只允许为0或1")
    @Max(value = 1, message = "enabled只允许为0或1")
    private Integer enabled;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }
}
