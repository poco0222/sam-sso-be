package com.yr.common.core.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yr.common.annotation.Excel;
import com.yr.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;

/**
 * 值集视图配置 sys_lov_type
 *
 * @author Youngron
 * @date 2021-09-13
 */
public class SysLovType extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 值集视图编号
     */
    @Excel(name = "值集视图主键", cellType = Excel.ColumnType.NUMERIC)
    private Long lovId;

    /**
     * 值集视图名称
     */
    @Excel(name = "值集视图名称")
    private String lovName;

    /**
     * 值集视图类型
     */
    @Excel(name = "值集视图类型")
    private String lovType;

    /**
     * 数据请求接口
     */
    @Excel(name = "数据请求接口")
    private String url;

    /**
     * 默认值字段名
     */
    @Excel(name = "默认值字段名")
    private String valueField;

    /**
     * 默认显示字段名
     */
    @Excel(name = "默认显示字段名")
    private String displayField;

    /**
     * 默认父级字段名
     */
    @Excel(name = "默认父级字段名")
    private String parentField;

    /**
     * 默认分页大小
     */
    @Excel(name = "默认分页大小")
    private Long pageSize;

    /**
     * 状态（0正常 1停用）
     */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /**
     * 字段
     */
    private List<SysLovField> fields;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "创建时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date createAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date updateAt;

    public Long getLovId() {
        return lovId;
    }

    public void setLovId(Long lovId) {
        this.lovId = lovId;
    }

    @NotBlank(message = "视图名称不能为空")
    @Size(min = 0, max = 30, message = "视图名称长度不能超过30个字符")
    public String getLovName() {
        return lovName;
    }

    public void setLovName(String lovName) {
        this.lovName = lovName;
    }

    @NotBlank(message = "视图类型不能为空")
    @Size(min = 2, max = 50, message = "视图类型长度不能少于2个字符且不能超过50个字符")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*[A-Z0-9]$", message = "视图类型需以大写字母开头，以大写字母或数字结尾，中间可以包含下划线")
    public String getLovType() {
        return lovType;
    }

    public void setLovType(String lovType) {
        this.lovType = lovType;
    }

    @NotBlank(message = "数据请求接口不能为空")
    @Size(min = 0, max = 200, message = "数据请求接口长度不能超过200个字符")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @NotBlank(message = "值字段名不能为空")
    @Size(min = 0, max = 50, message = "值字段名长度不能超过50个字符")
    public String getValueField() {
        return valueField;
    }

    public void setValueField(String valueField) {
        this.valueField = valueField;
    }

    @NotBlank(message = "显示字段名不能为空")
    @Size(min = 0, max = 50, message = "显示字段名长度不能超过50个字符")
    public String getDisplayField() {
        return displayField;
    }

    public void setDisplayField(String displayField) {
        this.displayField = displayField;
    }

    @Size(min = 0, max = 50, message = "父级字段名长度不能超过50个字符")
    public String getParentField() {
        return parentField;
    }

    public void setParentField(String parentField) {
        this.parentField = parentField;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<SysLovField> getFields() {
        return fields;
    }

    public void setFields(List<SysLovField> fields) {
        this.fields = fields;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("lovId", getLovId())
                .append("lovName", getLovName())
                .append("lovType", getLovType())
                .append("url", getUrl())
                .append("valueField", getValueField())
                .append("displayField", getDisplayField())
                .append("parentField", getParentField())
                .append("pageSize", getPageSize())
                .append("createBy", getCreateBy())
                .append("createAt", getCreateAt())
                .append("updateBy", getUpdateBy())
                .append("updateAt", getUpdateAt())
                .toString();
    }
}
