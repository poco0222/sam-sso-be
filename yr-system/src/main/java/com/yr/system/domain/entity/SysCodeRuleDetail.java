package com.yr.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yr.common.mybatisplus.entity.CustomEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 编码规则明细(SysCodeRuleDetail)表实体类
 *
 * @author Youngron
 * @since 2021-10-29 22:30:51
 */

@TableName("sys_code_rule_detail")
public class SysCodeRuleDetail extends CustomEntity {

    /**
     * 表ID，主键
     */
    @TableId
    private Long ruleDetailId;

    /**
     * 编码规则行ID，sys_code_rule_line.id
     */
    @NotNull(message = "编码规则行ID不能为空")
    private Long ruleLineId;

    /**
     * 序号，编码规则生成的顺序
     */
    @NotNull(message = "序号不能为空")
    private Integer orderSeq;

    /**
     * 段类型
     */
    @NotBlank(message = "段类型不能为空")
    private String fieldType;

    /**
     * 段值
     */
    private String fieldValue;

    /**
     * 日期格式
     */
    private String dateMask;

    /**
     * 序列位数
     */
    private Long seqLength;

    /**
     * 序列开始值
     */
    private Long startValue;

    /**
     * 序列当前值
     */
    private Long currentValue;

    /**
     * 序列重置频率
     */
    private String resetFrequency;

    /**
     * 序列上次重置日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date resetDate;

    /**
     * 组织ID
     */
    private Long orgId;

    // getter setter
    //----------------------------------

    public Long getRuleLineId() {
        return ruleLineId;
    }

    public void setRuleLineId(Long ruleLineId) {
        this.ruleLineId = ruleLineId;
    }

    public Integer getOrderSeq() {
        return orderSeq;
    }

    public void setOrderSeq(Integer orderSeq) {
        this.orderSeq = orderSeq;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }

    public String getDateMask() {
        return dateMask;
    }

    public void setDateMask(String dateMask) {
        this.dateMask = dateMask;
    }

    public Long getSeqLength() {
        return seqLength;
    }

    public void setSeqLength(Long seqLength) {
        this.seqLength = seqLength;
    }

    public Long getStartValue() {
        return startValue;
    }

    public void setStartValue(Long startValue) {
        this.startValue = startValue;
    }

    public Long getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Long currentValue) {
        this.currentValue = currentValue;
    }

    public String getResetFrequency() {
        return resetFrequency;
    }

    public void setResetFrequency(String resetFrequency) {
        this.resetFrequency = resetFrequency;
    }

    public Date getResetDate() {
        return resetDate;
    }

    public void setResetDate(Date resetDate) {
        this.resetDate = resetDate;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getRuleDetailId() {
        return ruleDetailId;
    }

    public void setRuleDetailId(Long ruleDetailId) {
        this.ruleDetailId = ruleDetailId;
    }

}
