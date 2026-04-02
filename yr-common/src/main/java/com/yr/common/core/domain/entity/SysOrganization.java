package com.yr.common.core.domain.entity;

import com.yr.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 组织对象 sys_organization
 *
 * @author PopoY
 */
public class SysOrganization extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 组织ID
     */
    private Long organizationId;

    /**
     * 组织编码
     */
    private String organizationCode;

    /**
     * 组织名称
     */
    private String organizationName;

    /**
     * 状态（0禁用 1启用）
     */
    private String status;

    public SysOrganization() {
    }

    public SysOrganization(Long organizationId) {
        this.organizationId = organizationId;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Long organizationId) {
        this.organizationId = organizationId;
    }

    @NotBlank(message = "组织编码不能为空")
    @Size(min = 0, max = 30, message = "组织编码长度不能超过30个字符")
    public String getOrganizationCode() {
        return organizationCode;
    }

    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    @NotBlank(message = "组织名称不能为空")
    @Size(min = 0, max = 30, message = "组织名称长度不能超过30个字符")
    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("organizationId", getOrganizationId())
                .append("organizationCode", getOrganizationCode())
                .append("organizationName", getOrganizationName())
                .append("status", getStatus())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .toString();
    }
}
