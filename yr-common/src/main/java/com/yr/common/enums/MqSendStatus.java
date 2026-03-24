package com.yr.common.enums;

/**
 * MQ消息发送状态
 */
public enum MqSendStatus {
    /** 待发送 */
    PENDING(0, "待发送"),
    /** 发送成功 */
    SUCCESS(1, "发送成功"),
    /** 发送失败 */
    FAILED(2, "发送失败");

    private final int code;
    private final String desc;

    MqSendStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
