/**
 * @file 客户端密钥签发结果 DTO
 * @author PopoY
 * @date 2026-03-30
 */
package com.yr.system.domain.dto;

/**
 * 承载创建/轮换后的一次性密钥展示结果。
 */
public class SsoClientSecretIssueResult {

    /** 客户端主键。 */
    private Long clientId;

    /** 客户端编码。 */
    private String clientCode;

    /** 一次性展示的客户端密钥。 */
    private String clientSecret;

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

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
