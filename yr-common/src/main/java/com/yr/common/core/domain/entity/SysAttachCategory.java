package com.yr.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yr.common.mybatisplus.entity.CustomEntity;

import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

/**
 * 附件目录表(SysAttachCategory)表实体类
 *
 * @author Youngron
 * @since 2021-12-30 10:41:35
 */
@TableName("sys_attach_category")
public class SysAttachCategory extends CustomEntity {
    /**
     * 叶子节点
     */
    public static final String LEAF_CATEGORY = "1";

    /**
     * 表ID，主键
     */
    @TableId
    private Long id;

    /**
     * 父级目录ID
     */
    private Long parentId;

    /**
     * 祖级列表
     */
    private String ancestors;

    /**
     * 目录名称
     */
    @NotBlank(message = "目录名称不能为空")
    private String categoryName;

    /**
     * 描述
     */
    private String description;

    /**
     * 目录层级，自动计算
     */
    private Integer categoryLevel;

    /**
     * 是否叶子节点，0：目录，1：叶子节点
     */
    @NotBlank(message = "是否叶子节点不能为空")
    private String leafFlag;

    /**
     * 叶子节点编码，唯一（代码里上传附件时用到的标识）
     */
    private String leafCode;

    /**
     * 字典编码，用于区分同一叶子节点下不同类型的附件
     */
    private String leafDictCode;

    /**
     * 允许上传的附件类型，叶子节点必填
     */
    private String allowedFileType;

    /**
     * 允许上传的附件最大长度，叶子节点必填
     */
    private Long allowedFileSize;

    /**
     * 组织ID
     */
    private Long orgId;

    // 非数据库字段
    //----------------------------------

    /**
     * 允许上传的附件最大长度，常见格式
     */
    @TableField(exist = false)
    private String allowedFileSizeDesc;

    @TableField(exist = false)
    private List<SysAttachCategory> children = new ArrayList<>();

    // getter setter
    //----------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCategoryLevel() {
        return categoryLevel;
    }

    public void setCategoryLevel(Integer categoryLevel) {
        this.categoryLevel = categoryLevel;
    }

    public String getLeafFlag() {
        return leafFlag;
    }

    public void setLeafFlag(String leafFlag) {
        this.leafFlag = leafFlag;
    }

    public String getLeafCode() {
        return leafCode;
    }

    public void setLeafCode(String leafCode) {
        this.leafCode = leafCode;
    }

    public String getLeafDictCode() {
        return leafDictCode;
    }

    public void setLeafDictCode(String leafDictCode) {
        this.leafDictCode = leafDictCode;
    }

    public String getAllowedFileType() {
        return allowedFileType;
    }

    public void setAllowedFileType(String allowedFileType) {
        this.allowedFileType = allowedFileType;
    }

    public Long getAllowedFileSize() {
        return allowedFileSize;
    }

    public void setAllowedFileSize(Long allowedFileSize) {
        this.allowedFileSize = allowedFileSize;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getAllowedFileSizeDesc() {
        return allowedFileSizeDesc;
    }

    public void setAllowedFileSizeDesc(String allowedFileSizeDesc) {
        this.allowedFileSizeDesc = allowedFileSizeDesc;
    }

    public List<SysAttachCategory> getChildren() {
        return children;
    }

    public void setChildren(List<SysAttachCategory> children) {
        this.children = children;
    }
}
