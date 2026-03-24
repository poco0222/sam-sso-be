package com.yr.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yr.common.mybatisplus.entity.CustomEntity;

/**
 * 系统区域表(SysRegion)表实体类
 *
 * @author Youngron
 * @since 2021-10-20 18:48:34
 */
@TableName("sys_region")
public class SysRegion extends CustomEntity {

    /**
     * 表ID，主键
     */
    @TableId
    private Long id;

    /**
     * 父节点ID
     */
    private Long parentId;

    /**
     * 祖级列表
     */
    private String ancestors;

    /**
     * 统计用区划代码
     */
    private String regionCode;

    /**
     * 地区名称
     */
    private String regionName;

    /**
     * 地区缩写
     */
    private String regionShortName;

    /**
     * 地区级别 1-省、自治区、直辖市 2-地级市、地区、自治州、盟 3-市辖区、县级市、县
     */
    private Integer regionLevel;

    /**
     * 是否存在子节点
     */
    private Boolean hasChildren;

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

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getRegionShortName() {
        return regionShortName;
    }

    public void setRegionShortName(String regionShortName) {
        this.regionShortName = regionShortName;
    }

    public Integer getRegionLevel() {
        return regionLevel;
    }

    public void setRegionLevel(Integer regionLevel) {
        this.regionLevel = regionLevel;
    }

    public Boolean getHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(Boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

}
