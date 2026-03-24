package com.yr.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yr.common.mybatisplus.entity.CustomEntity;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-23 18:26
 * @description
 */

@TableName("sys_duty")
public class SysDuty extends CustomEntity {

    /**
     * 表id，主键
     */
    @TableId
    private Long id;

    /**
     * 父职务id
     */
    private Long parentId;

    /**
     * 祖级列表
     */
    private String ancestors;

    /**
     * 职务编码
     */
    private String dutyCode;

    /**
     * 职务名称
     */
    private String dutyName;

    /**
     * 显示顺序
     */
    private Integer orderNum;

    /**
     * 组织id
     */
    private Long orgId;

    // 非数据库字段
    //-----------------------------------

    /**
     * sys_user_duty.id
     */
    @TableField(exist = false)
    private Long userDutyId;

    // getter setter
    //-----------------------------------

    public Long getUserDutyId() {
        return userDutyId;
    }

    public void setUserDutyId(Long userDutyId) {
        this.userDutyId = userDutyId;
    }

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

    public String getDutyCode() {
        return dutyCode;
    }

    public void setDutyCode(String dutyCode) {
        this.dutyCode = dutyCode;
    }

    public String getDutyName() {
        return dutyName;
    }

    public void setDutyName(String dutyName) {
        this.dutyName = dutyName;
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
