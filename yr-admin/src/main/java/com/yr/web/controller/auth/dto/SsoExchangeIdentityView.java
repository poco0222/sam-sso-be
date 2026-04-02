/**
 * @file 认证接入协议身份载荷视图 DTO
 * @author PopoY
 * @date 2026-04-02
 */
package com.yr.web.controller.auth.dto;

/**
 * 对下游系统暴露的标准化身份载荷。
 */
public class SsoExchangeIdentityView {

    /** 本次换票 ID。 */
    private String exchangeId;

    /** 跟踪 ID。 */
    private String traceId;

    /** 客户端编码。 */
    private String clientCode;

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

    public String getClientCode() {
        return clientCode;
    }

    public void setClientCode(String clientCode) {
        this.clientCode = clientCode;
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
