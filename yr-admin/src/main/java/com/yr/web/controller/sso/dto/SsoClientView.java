/**
 * @file 客户端列表视图 DTO
 * @author PopoY
 * @date 2026-03-30
 */
package com.yr.web.controller.sso.dto;

import java.util.Date;

/**
 * 客户端常规查询视图，不包含 clientSecret。
 */
public class SsoClientView {

    /** 客户端主键。 */
    private Long clientId;

    /** 客户端编码。 */
    private String clientCode;

    /** 客户端名称。 */
    private String clientName;

    /** 回调地址列表。 */
    private String redirectUris;

    /** 是否允许账号密码登录。 */
    private String allowPasswordLogin;

    /** 是否允许企业微信登录。 */
    private String allowWxworkLogin;

    /** 是否启用主数据同步。 */
    private String syncEnabled;

    /** 客户端状态。 */
    private String status;

    /** 创建人。 */
    private String createBy;

    /** 创建时间。 */
    private Date createTime;

    /** 更新人。 */
    private String updateBy;

    /** 更新时间。 */
    private Date updateTime;

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public String getClientCode() {
        return clientCode;
    }

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(String redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String getAllowPasswordLogin() {
        return allowPasswordLogin;
    }

    public void setAllowPasswordLogin(String allowPasswordLogin) {
        this.allowPasswordLogin = allowPasswordLogin;
    }

    public String getAllowWxworkLogin() {
        return allowWxworkLogin;
    }

    public void setAllowWxworkLogin(String allowWxworkLogin) {
        this.allowWxworkLogin = allowWxworkLogin;
    }

    public String getSyncEnabled() {
        return syncEnabled;
    }

    public void setSyncEnabled(String syncEnabled) {
        this.syncEnabled = syncEnabled;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
