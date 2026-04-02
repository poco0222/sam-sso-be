/**
 * @file 认证接入协议换票请求 DTO
 * @author PopoY
 * @date 2026-04-02
 */
package com.yr.web.controller.auth.dto;

import javax.validation.constraints.NotBlank;

/**
 * exchange（换票）入口请求体。
 */
public class SsoCodeExchangeRequest {

    /** 客户端编码。 */
    @NotBlank(message = "clientCode不能为空")
    private String clientCode;

    /** 客户端密钥。 */
    @NotBlank(message = "clientSecret不能为空")
    private String clientSecret;

    /** 一次性授权码。 */
    @NotBlank(message = "code不能为空")
    private String code;

    public String getClientCode() {
        return clientCode;
    }

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
