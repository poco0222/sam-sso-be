/**
 * @file 客户端选项视图 DTO
 * @author PopoY
 * @date 2026-04-01
 */
package com.yr.web.controller.sso.dto;

/**
 * 分发任务客户端下拉选项，只暴露最小展示字段。
 */
public class SsoClientOptionView {

    /** 客户端编码。 */
    private String clientCode;

    /** 客户端名称。 */
    private String clientName;

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
}
