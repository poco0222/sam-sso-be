/**
 * @file 认证接入协议授权跳转查询 DTO
 * @author PopoY
 * @date 2026-04-02
 */
package com.yr.web.controller.auth.dto;

import javax.validation.constraints.NotBlank;

/**
 * authorize（授权跳转）入口查询参数。
 */
public class SsoAuthorizeQuery {

    /** 客户端编码。 */
    @NotBlank(message = "clientCode不能为空")
    private String clientCode;

    /** 回调地址。 */
    @NotBlank(message = "redirectUri不能为空")
    private String redirectUri;

    /** 下游系统自带的防重放 state。 */
    @NotBlank(message = "state不能为空")
    private String state;

    public String getClientCode() {
        return clientCode;
    }

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
