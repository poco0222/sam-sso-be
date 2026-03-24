/**
 * @file 消息模板分页查询 VO，保持与自身语义一致的 PageVo 泛型声明
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.domain.vo;

/**
 * 消息模板分页查询 VO。
 */
public class SysMsgTemplateVo extends PageVo<SysMsgTemplateVo> {
    /**
     * 消息模板ID
     */
    private Long id;

    /**
     * 消息编码
     */
    private String msgCode;

    /**
     * 消息模板名称
     */
    private String msgName;

    /**
     * 消息模板内容
     */
    private String msgContent;

    /***
     * 消息模板参数
     */
    private String msgParams;

    /**
     * 状态
     */
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMsgCode() {
        return msgCode;
    }

    public void setMsgCode(String msgCode) {
        this.msgCode = msgCode;
    }

    public String getMsgName() {
        return msgName;
    }

    public void setMsgName(String msgName) {
        this.msgName = msgName;
    }

    public String getMsgContent() {
        return msgContent;
    }

    public void setMsgContent(String msgContent) {
        this.msgContent = msgContent;
    }

    public String getMsgParams() {
        return msgParams;
    }

    public void setMsgParams(String msgParams) {
        this.msgParams = msgParams;
    }
}
