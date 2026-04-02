/**
 * @file 认证接入协议授权码载荷 DTO
 * @author PopoY
 * @date 2026-04-02
 */
package com.yr.system.domain.dto;

/**
 * 一次性授权码在 Redis 中缓存的身份载荷。
 */
public class SsoAuthorizationCodePayload {

    /** 本次换票 ID。 */
    private String exchangeId;

    /** 跟踪 ID。 */
    private String traceId;

    /** 客户端主键。 */
    private Long clientId;

    /** 客户端编码。 */
    private String clientCode;

    /** 回调地址。 */
    private String redirectUri;

    /** 下游系统 state。 */
    private String state;

    /** 用户 ID。 */
    private Long userId;

    /** 用户账号。 */
    private String username;

    /** 用户昵称。 */
    private String nickName;

    /** 默认组织 ID。 */
    private Long orgId;

    /** 默认组织名称。 */
    private String orgName;

    /** 默认部门 ID。 */
    private Long deptId;

    /** 默认部门名称。 */
    private String deptName;

    public String getExchangeId() {
        return exchangeId;
    }

    public void setExchangeId(String exchangeId) {
        this.exchangeId = exchangeId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }
}
