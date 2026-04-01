/**
 * @file 组织编辑请求 DTO
 * @author PopoY
 * @date 2026-04-01
 */
package com.yr.web.controller.system.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * 组织编辑请求，只暴露允许进入通用编辑链路的基础字段。
 */
public class SysOrgUpdateRequest {

    /** 组织 ID。 */
    @NotNull(message = "组织ID不能为空")
    private Long orgId;

    /** 上级组织 ID。 */
    @NotNull(message = "上级组织不能为空")
    private Long parentId;

    /** 组织编码。 */
    @NotBlank(message = "组织编码不能为空")
    private String orgCode;

    /** 组织名称。 */
    @NotBlank(message = "组织名称不能为空")
    private String orgName;

    /** 显示顺序。 */
    @NotNull(message = "显示顺序不能为空")
    private Integer orderNum;

    /** 组织负责人。 */
    private String leader;

    /** 备注。 */
    private String remark;

    /** 组织状态。 */
    @NotBlank(message = "status不能为空")
    @Pattern(regexp = "0|1", message = "status只允许为0或1")
    private String status;

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
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

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
