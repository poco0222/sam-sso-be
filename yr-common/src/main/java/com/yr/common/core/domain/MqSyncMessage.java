package com.yr.common.core.domain;

import java.io.Serializable;

/**
 * MQ同步消息体
 */
public class MqSyncMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 业务唯一键 */
    private String msgKey;

    /** 操作类型: I/U/D/UPSERT */
    private String actionType;

    /** 业务数据JSON */
    private String data;

    /** 时间戳 */
    private Long timestamp;

    public MqSyncMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public MqSyncMessage(String msgKey, String actionType, String data) {
        this.msgKey = msgKey;
        this.actionType = actionType;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMsgKey() { return msgKey; }
    public void setMsgKey(String msgKey) { this.msgKey = msgKey; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
