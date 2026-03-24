package com.yr.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yr.common.mybatisplus.entity.CustomEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 编码规则行(SysCodeRuleLine)表实体类
 *
 * @author Youngron
 * @since 2021-10-29 22:30:51
 */

@TableName("sys_code_rule_line")
public class SysCodeRuleLine extends CustomEntity {

    /**
     * 表ID，主键
     */
    @TableId
    private Long ruleLineId;

    /**
     * 编码规则ID，sys_code_rule.id
     */
    @NotNull(message = "编码规则ID不能为空")
    private Long ruleId;

    /**
     * 应用层级
     */
    @NotBlank(message = "应用层级CODE不能为空")
    private String levelCode;

    /**
     * 应用层级值
     */
    @NotBlank(message = "应用层级值不能为空")
    private String levelValue;

    /**
     * 是否已使用，0：未使用；1：已使用
     */
    private String usedFlag;

    /**
     * 是否启用，0：禁用；1：启用
     */
    private String enabledFlag;

    /**
     * 描述
     */
    private String description;

    /**
     * 组织ID
     */
    private Long orgId;

    // getter setter
    //----------------------------------

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getLevelCode() {
        return levelCode;
    }

    public void setLevelCode(String levelCode) {
        this.levelCode = levelCode;
    }

    public String getLevelValue() {
        return levelValue;
    }

    public void setLevelValue(String levelValue) {
        this.levelValue = levelValue;
    }

    public String getUsedFlag() {
        return usedFlag;
    }

    public void setUsedFlag(String usedFlag) {
        this.usedFlag = usedFlag;
    }

    public String getEnabledFlag() {
        return enabledFlag;
    }

    public void setEnabledFlag(String enabledFlag) {
        this.enabledFlag = enabledFlag;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Long getRuleLineId() {
        return ruleLineId;
    }

    public void setRuleLineId(Long ruleLineId) {
        this.ruleLineId = ruleLineId;
    }

}
