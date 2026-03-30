/**
 * @file 客户端更新请求 DTO
 * @author PopoY
 * @date 2026-03-30
 */
package com.yr.web.controller.sso.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 客户端更新请求，只暴露普通编辑所需字段。
 */
public class SsoClientUpdateRequest {

    /** 客户端主键。 */
    @NotNull(message = "clientId不能为空")
    private Long clientId;

    /** 客户端编码。 */
    @NotBlank(message = "clientCode不能为空")
    private String clientCode;

    /** 客户端名称。 */
    @NotBlank(message = "clientName不能为空")
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
    @NotBlank(message = "status不能为空")
    private String status;

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
}
