package com.yr.common.mybatisplus.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 1050696985@qq.com
 * @version V1.0
 * @Date 2021-8-9 10:36
 * @description 审计字段基类
 */

public class CustomEntity {

    public static final String FIELD_CREATE_BY = "createBy";
    public static final String FIELD_CREATE_AT = "createAt";
    public static final String FIELD_UPDATE_BY = "updateBy";
    public static final String FIELD_UPDATE_AT = "updateAt";
    public static final String FIELD_OBJECT_VERSION_NUMBER = "objectVersionNumber";

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateAt;

    @Version
    private Long objectVersionNumber;

    /**
     * 请求参数，数据权限会用到 http://doc.ruoyi.vip/ruoyi/document/htsc.html#数据权限
     */
    @TableField(exist = false)
    private Map<String, Object> params;

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public Date getCreateAt() {
        return createAt;
    }

    public void setCreateAt(Date createAt) {
        this.createAt = createAt;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(Date updateAt) {
        this.updateAt = updateAt;
    }

    public Long getObjectVersionNumber() {
        return objectVersionNumber;
    }

    public void setObjectVersionNumber(Long objectVersionNumber) {
        this.objectVersionNumber = objectVersionNumber;
    }

    public Map<String, Object> getParams() {
        if (params == null) {
            params = new HashMap<>(1);
        }
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "CustomEntity{" +
                "createBy=" + createBy +
                ", createAt=" + createAt +
                ", updateBy=" + updateBy +
                ", updateAt=" + updateAt +
                ", objectVersionNumber=" + objectVersionNumber +
                ", params=" + params +
                '}';
    }
}
