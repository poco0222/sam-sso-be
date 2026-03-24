package com.yr.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yr.common.mybatisplus.entity.CustomEntity;

import java.util.Date;

/**
 * 编码规则明细值(SysCodeRuleValue)表实体类
 *
 * @author Youngron
 * @since 2021-10-29 22:30:51
 */

@TableName("sys_code_rule_value")
public class SysCodeRuleValue extends CustomEntity {

    /**
     * 表ID，主键
     */
    @TableId
    private Long ruleValueId;

    /**
     * 编码规则明细ID，sys_code_rule_detail.id
     */
    private Long ruleDetailId;

    /**
     * 应用层级值
     */
    private String levelValue;

    /**
     * 序列当前值
     */
    private Long currentValue;

    /**
     * 序列上次重置日期
     */
    private Date resetDate;

    /**
     * 组织ID
     */
    private Long orgId;

    // getter setter
    //----------------------------------

    public Long getRuleDetailId() {
        return ruleDetailId;
    }

    public void setRuleDetailId(Long ruleDetailId) {
        this.ruleDetailId = ruleDetailId;
    }

    public String getLevelValue() {
        return levelValue;
    }

    public void setLevelValue(String levelValue) {
        this.levelValue = levelValue;
    }

    public Long getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(Long currentValue) {
        this.currentValue = currentValue;
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

    public Long getRuleValueId() {
        return ruleValueId;
    }

    public void setRuleValueId(Long ruleValueId) {
        this.ruleValueId = ruleValueId;
    }

}
