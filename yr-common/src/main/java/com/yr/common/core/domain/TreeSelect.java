/**
 * @file 一期边界下的树形选择节点
 * @author PopoY
 * @date 2026-03-26
 */
package com.yr.common.core.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.yr.common.core.domain.entity.SysDept;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Treeselect树结构实体类
 */
public class TreeSelect implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 节点ID
     */
    private Long id;

    private String strId;

    /**
     * 节点名称
     */
    private String label;

    private String nodeType;

    private String deptCode;

    private boolean showTips;
    private Long deptId;

    /**
     * 额外的信息
     */
    private Map<String, Object> extra;

    /**
     * 子节点
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<TreeSelect> children;

    public TreeSelect() {

    }

    public TreeSelect(SysDept dept) {
        this.id = dept.getDeptId();
        this.label = dept.getDeptName();
        this.deptCode = dept.getDeptCode();
        this.children = dept.getChildren().stream().map(TreeSelect::new).collect(Collectors.toList());
    }

//    public TreeSelect(SamMouldInfo samMouldInfo) {
//        this.strId = samMouldInfo.getId();
//        this.label = StringUtils.isEmpty(samMouldInfo.getCode()) ? samMouldInfo.getId() : samMouldInfo.getCode();
//        this.nodeType = samMouldInfo.getNodeType();
//        this.showTips = StringUtils.isNotNull(samMouldInfo.getFileCount()) && samMouldInfo.getFileCount().compareTo(0) > 0;
//        this.children = samMouldInfo.getChildren().stream().map(TreeSelect::new).collect(Collectors.toList());
//    }


    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }

    public String getStrId() {
        return strId;
    }

    public void setStrId(String strId) {
        this.strId = strId;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public boolean isShowTips() {
        return showTips;
    }

    public void setShowTips(boolean showTips) {
        this.showTips = showTips;
    }

    public List<TreeSelect> getChildren() {
        return children;
    }

    public void setChildren(List<TreeSelect> children) {
        this.children = children;
    }
}
