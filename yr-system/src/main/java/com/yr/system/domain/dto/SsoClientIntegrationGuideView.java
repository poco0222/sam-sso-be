/**
 * @file 客户端接入说明视图 DTO
 * @author PopoY
 * @date 2026-04-02
 */
package com.yr.system.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;
import java.util.List;

/**
 * 客户端接入治理说明视图。
 */
public class SsoClientIntegrationGuideView {

    /** 客户端主键。 */
    private Long clientId;

    /** 客户端编码。 */
    private String clientCode;

    /** 客户端名称。 */
    private String clientName;

    /** 回调地址白名单原始文本。 */
    private String redirectUris;

    /** 是否允许账号密码登录。 */
    private String allowPasswordLogin;

    /** 是否允许企业微信登录。 */
    private String allowWxworkLogin;

    /** 是否启用主数据同步。 */
    private String syncEnabled;

    /** 客户端状态。 */
    private String status;

    /** 回调地址是否已配置。 */
    private boolean redirectUriConfigured;

    /** 是否至少启用一种登录方式。 */
    private boolean loginMethodConfigured;

    /** 客户端是否已启用。 */
    private boolean clientEnabled;

    /** 是否已启用同步。 */
    private boolean syncEnabledReady;

    /** 授权跳转路径。 */
    private String authorizePath;

    /** 换票路径。 */
    private String exchangePath;

    /** 授权跳转示例。 */
    private String authorizeExample;

    /** 换票请求示例。 */
    private String exchangeRequestExample;

    /** 标准化身份载荷字段清单。 */
    private List<String> identityPayloadFields;

    /** 最近已知密钥操作时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date latestKnownSecretOperationTime;

    /** 密钥轮换提示信息。 */
    private String secretRotationInfo;

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

    public boolean isRedirectUriConfigured() {
        return redirectUriConfigured;
    }

    public void setRedirectUriConfigured(boolean redirectUriConfigured) {
        this.redirectUriConfigured = redirectUriConfigured;
    }

    public boolean isLoginMethodConfigured() {
        return loginMethodConfigured;
    }

    public void setLoginMethodConfigured(boolean loginMethodConfigured) {
        this.loginMethodConfigured = loginMethodConfigured;
    }

    public boolean isClientEnabled() {
        return clientEnabled;
    }

    public void setClientEnabled(boolean clientEnabled) {
        this.clientEnabled = clientEnabled;
    }

    public boolean isSyncEnabledReady() {
        return syncEnabledReady;
    }

    public void setSyncEnabledReady(boolean syncEnabledReady) {
        this.syncEnabledReady = syncEnabledReady;
    }

    public String getAuthorizePath() {
        return authorizePath;
    }

    public void setAuthorizePath(String authorizePath) {
        this.authorizePath = authorizePath;
    }

    public String getExchangePath() {
        return exchangePath;
    }

    public void setExchangePath(String exchangePath) {
        this.exchangePath = exchangePath;
    }

    public String getAuthorizeExample() {
        return authorizeExample;
    }

    public void setAuthorizeExample(String authorizeExample) {
        this.authorizeExample = authorizeExample;
    }

    public String getExchangeRequestExample() {
        return exchangeRequestExample;
    }

    public void setExchangeRequestExample(String exchangeRequestExample) {
        this.exchangeRequestExample = exchangeRequestExample;
    }

    public List<String> getIdentityPayloadFields() {
        return identityPayloadFields;
    }

    public void setIdentityPayloadFields(List<String> identityPayloadFields) {
        this.identityPayloadFields = identityPayloadFields;
    }

    public Date getLatestKnownSecretOperationTime() {
        return latestKnownSecretOperationTime;
    }

    public void setLatestKnownSecretOperationTime(Date latestKnownSecretOperationTime) {
        this.latestKnownSecretOperationTime = latestKnownSecretOperationTime;
    }

    public String getSecretRotationInfo() {
        return secretRotationInfo;
    }

    public void setSecretRotationInfo(String secretRotationInfo) {
        this.secretRotationInfo = secretRotationInfo;
    }
}
