package com.yr.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yr.common.mybatisplus.entity.CustomEntity;

/**
 * 附件表(SysAttachment)表实体类
 *
 * @author Youngron
 * @since 2021-12-30 10:48:34
 */

@TableName("sys_attachment")
public class SysAttachment extends CustomEntity {

    /**
     * 表ID，主键
     */
    @TableId
    private Long id;

    /**
     * 附件目录ID，sys_attach_category.id
     */
    private Long categoryId;

    /**
     * 附件名称
     */
    private String attachName;

    /**
     * 业务主键
     */
    private String businessId;

    /**
     * 字典值，对应sys_attach_category.leaf_dict_code
     */
    private String businessType;

    /**
     * 组织ID
     */
    private Long orgId;

    // getter setter
    //----------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getAttachName() {
        return attachName;
    }

    public void setAttachName(String attachName) {
        this.attachName = attachName;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

}
