package com.yr.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yr.common.mybatisplus.entity.CustomEntity;

import javax.validation.constraints.NotBlank;

/**
 * 编码规则(SysCodeRule)表实体类
 *
 * @author Youngron
 * @since 2021-10-29 22:30:51
 */

@TableName("sys_code_rule")
public class SysCodeRule extends CustomEntity {

    /**
     * 表ID，主键
     */
    @TableId
    private Long ruleId;

    /**
     * 编码规则
     */
    @NotBlank(message = "编码规则不能为空")
    private String ruleCode;

    /**
     * 编码名称
     */
    @NotBlank(message = "编码名称不能为空")
    private String ruleName;

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

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
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

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

}
