package com.yr.common.core.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yr.common.core.domain.BaseEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 值集视图字段配置 sys_lov_field
 *
 * @author Youngron
 * @date 2021-09-13
 */
public class SysLovField extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 值集视图字段ID
     */
    private Long fieldId;

    /**
     * 值集视图ID
     */
    private Long lovId;

    /**
     * 值集视图字段名
     */
    private String fieldName;

    /**
     * 值集视图字段标题
     */
    private String fieldLabel;

    /**
     * 值集视图字段类型
     */
    private String fieldType;

    /**
     * 值集视图字段字典类型
     */
    private String dictType;

    /**
     * 值集视图字段排序
     */
    private Long orderNum;

    /**
     * 值集视图字段是否查询字段（Y是 N否）
     */
    private String isQuery;

    /**
     * 值集视图字段是否表格列（Y是 N否）
     */
    private String isCol;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateAt;

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(Long fieldId) {
        this.fieldId = fieldId;
    }

    @NotNull(message = "关联值集视图不能为空")
    public Long getLovId() {
        return lovId;
    }

    public void setLovId(Long lovId) {
        this.lovId = lovId;
    }

    @NotBlank(message = "字段名不能为空")
    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    @NotBlank(message = "字段标题不能为空")
    public String getFieldLabel() {
        return fieldLabel;
    }

    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel = fieldLabel;
    }

    @NotBlank(message = "字段类型不能为空")
    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getDictType() {
        return dictType;
    }

    public void setDictType(String dictType) {
        this.dictType = dictType;
    }

    public Long getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Long orderNum) {
        this.orderNum = orderNum;
    }

    public String getIsQuery() {
        return isQuery;
    }

    public void setIsQuery(String isQuery) {
        this.isQuery = isQuery;
    }

    public String getIsCol() {
        return isCol;
    }

    public void setIsCol(String isCol) {
        this.isCol = isCol;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public Date getCreateAt() {
        return createAt;
    }

    @Override
    public void setCreateAt(Date createAt) {
        this.createAt = createAt;
    }

    @Override
    public Date getUpdateAt() {
        return updateAt;
    }

    @Override
    public void setUpdateAt(Date updateAt) {
        this.updateAt = updateAt;
    }
}
