package com.yr.common.core.domain.entity;

import com.yr.common.annotation.Excel;
import com.yr.common.core.domain.TreeEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 组织信息对象 sys_org
 *
 * @author PopoY
 * @date 2021-09-09
 */
public class SysOrg extends TreeEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 组织ID
     */
    private Long orgId;

    /**
     * 组织编码
     */
    @Excel(name = "组织编码")
    private String orgCode;

    /**
     * 组织名称
     */
    @Excel(name = "组织名称")
    private String orgName;

    /**
     * 领导人
     */
    @Excel(name = "领导人")
    private String leader;

    /**
     * 状态
     */
    @Excel(name = "状态")
    private String status;

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getOrgCode() {
        return orgCode;
    }

    public void setOrgCode(String orgCode) {
        this.orgCode = orgCode;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
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
                .append("orgId", getOrgId())
                .append("parentId", getParentId())
                .append("orgCode", getOrgCode())
                .append("orgName", getOrgName())
                .append("orderNum", getOrderNum())
                .append("leader", getOrgName())
                .append("remark", getRemark())
                .append("status", getStatus())
                .append("createBy", getCreateBy())
                .append("createAt", getCreateAt())
                .append("updateBy", getUpdateBy())
                .append("updateAt", getUpdateAt())
                .toString();
    }
}
