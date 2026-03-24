package com.yr.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yr.common.mybatisplus.entity.CustomEntity;

/**
 * @author Youngron
 * @version V1.0
 * @date 2021-9-14 14:12
 */

@TableName("sys_user_org")
public class SysUserOrg extends CustomEntity {

    /**
     * 表id，主键
     */
    @TableId
    private Long id;

    /**
     * 用户id，sys_user.user_id
     */
    private Long userId;

    /**
     * 组织id，sys_org.org_id
     */
    private Long orgId;

    /**
     * 1表示用户的默认组织
     */
    private Integer isDefault;

    /**
     * 是否激活，0:否；1:是
     */
    private Integer enabled;

    // 非数据库字段
    //---------------------------------

    /**
     * 最后更新人名称
     */
    @TableField(exist = false)
    private String lastUpdateName;
    /**
     * 组织编码
     */
    @TableField(exist = false)
    private String orgCode;
    /**
     * 组织名称
     */
    @TableField(exist = false)
    private String orgName;

    // getter setter
    //---------------------------------

    public String getLastUpdateName() {
        return lastUpdateName;
    }

    public void setLastUpdateName(String lastUpdateName) {
        this.lastUpdateName = lastUpdateName;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }
}
