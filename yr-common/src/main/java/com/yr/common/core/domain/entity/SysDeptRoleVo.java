package com.yr.common.core.domain.entity;


import java.util.ArrayList;
import java.util.List;

public class SysDeptRoleVo {
    private static final long serialVersionUID = 1L;
    private String id;
    private String treeName;
    private Long roleId;
    private String nodeType;
    private Long deptId;
    private String accounteUnit;
    private String parentId;
    /**
     * 子部门
     */
    private List<SysDeptRoleVo> children = new ArrayList<SysDeptRoleVo>();

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public List<SysDeptRoleVo> getChildren() {
        return children;
    }

    public void setChildren(List<SysDeptRoleVo> children) {
        this.children = children;
    }

    public String getAccounteUnit() {
        return accounteUnit;
    }

    public void setAccounteUnit(String accounteUnit) {
        this.accounteUnit = accounteUnit;
    }

    public String getTreeName() {
        return treeName;
    }

    public void setTreeName(String treeName) {
        this.treeName = treeName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }
}
