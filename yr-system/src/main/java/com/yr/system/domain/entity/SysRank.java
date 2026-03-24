package com.yr.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yr.common.mybatisplus.entity.CustomEntity;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-26 18:17
 * @description
 */

@TableName("sys_rank")
public class SysRank extends CustomEntity {

    /**
     * 表id，主键
     */
    @TableId
    private Long id;

    /**
     * 父职级id
     */
    private Long parentId;

    /**
     * 祖级列表
     */
    private String ancestors;

    /**
     * 职级编码
     */
    private String rankCode;

    /**
     * 职级名称
     */
    private String rankName;

    /**
     * 分类（0目录 1职级），只有职级才能分配用户
     */
    private String rankType;

    /**
     * 显示顺序
     */
    private Integer orderNum;

    /**
     * 组织id
     */
    private Long orgId;

    // getter setter
    //--------------------------------------

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

    public String getRankCode() {
        return rankCode;
    }

    public void setRankCode(String rankCode) {
        this.rankCode = rankCode;
    }

    public String getRankName() {
        return rankName;
    }

    public void setRankName(String rankName) {
        this.rankName = rankName;
    }

    public String getRankType() {
        return rankType;
    }

    public void setRankType(String rankType) {
        this.rankType = rankType;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }
}
