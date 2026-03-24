package com.yr.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.yr.common.core.domain.BaseEntity;
import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * 部门表 sys_dept
 *
 * @author Youngron
 */
public class SysDept extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 父部门ID
     */
    private Long parentId;

    /**
     * 祖级列表
     */
    private String ancestors;

    /**
     * 部门编码
     */
    private String deptCode;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 显示顺序
     */
    private String orderNum;

    /**
     * 负责人
     */
    private String leader;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 部门状态:0正常,1停用
     */
    private String status;

    /**
     * 删除标志（0代表存在 2代表删除）
     */
    private String delFlag;

    /**
     * 组织ID
     */
    private Long orgId;

    /**
     * 父部门名称
     */
    private String parentName;

    /**
     * 是否为用户默认部门 sys_user_dept.is_default 1表示用户的默认部门
     */
    private Integer isUserDefaultDept;

    /**
     * 用户部门关系表ID
     */
    private Long userDeptId;

    /**
     * 客户标识
     */
    private String isClientFlag;

    /**
     * 子部门
     */
    private List<SysDept> children = new ArrayList<SysDept>();

    /*
   核算单位 （0：是 1否）
   */
    private String accounteUnit;

    /**
     * 父部门ID
     */
    @TableField(exist = false)
    private Long deviceParentId;

    // getter setter
    //--------------------------


    public Long getDeviceParentId() {
        return deviceParentId;
    }

    public void setDeviceParentId(Long deviceParentId) {
        this.deviceParentId = deviceParentId;
    }

    public String getIsClientFlag() {
        return isClientFlag;
    }

    public void setIsClientFlag(String isClientFlag) {
        this.isClientFlag = isClientFlag;
    }

    public String getAccounteUnit() {
        return accounteUnit;
    }

    public void setAccounteUnit(String accounteUnit) {
        this.accounteUnit = accounteUnit;
    }

    public Long getUserDeptId() {
        return userDeptId;
    }

    public void setUserDeptId(Long userDeptId) {
        this.userDeptId = userDeptId;
    }

    public Integer getIsUserDefaultDept() {
        return isUserDefaultDept;
    }

    public void setIsUserDefaultDept(Integer isUserDefaultDept) {
        this.isUserDefaultDept = isUserDefaultDept;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getAncestors() {
        return ancestors;
    }

    public void setAncestors(String ancestors) {
        this.ancestors = ancestors;
    }

    @NotBlank(message = "部门名称不能为空")
    @Size(min = 0, max = 30, message = "部门名称长度不能超过30个字符")
    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    @NotBlank(message = "显示顺序不能为空")
    public String getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(String orderNum) {
        this.orderNum = orderNum;
    }

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    @Size(min = 0, max = 11, message = "联系电话长度不能超过11个字符")
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Email(message = "邮箱格式不正确")
    @Size(min = 0, max = 50, message = "邮箱长度不能超过50个字符")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(String delFlag) {
        this.delFlag = delFlag;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public List<SysDept> getChildren() {
        return children;
    }

    public void setChildren(List<SysDept> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return "SysDept{" +
                "deptId=" + deptId +
                ", parentId=" + parentId +
                ", ancestors='" + ancestors + '\'' +
                ", deptCode='" + deptCode + '\'' +
                ", deptName='" + deptName + '\'' +
                ", orderNum='" + orderNum + '\'' +
                ", leader='" + leader + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", status='" + status + '\'' +
                ", delFlag='" + delFlag + '\'' +
                ", orgId=" + orgId +
                ", parentName='" + parentName + '\'' +
                ", isUserDefaultDept=" + isUserDefaultDept +
                ", userDeptId=" + userDeptId +
                ", isClientFlag='" + isClientFlag + '\'' +
                ", children=" + children +
                ", accounteUnit='" + accounteUnit + '\'' +
                '}';
    }
}
