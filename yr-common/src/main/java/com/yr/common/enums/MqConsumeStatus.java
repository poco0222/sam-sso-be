package com.yr.common.enums;

/**
 * MQ消息消费状态
 */
public enum MqConsumeStatus {
    /** 未消费 */
    PENDING(0, "未消费"),
    /** 消费成功 */
    SUCCESS(1, "消费成功"),
    /** 消费失败 */
    FAILED(2, "消费失败");

    private final int code;
    private final String desc;

    MqConsumeStatus(int code, String desc) {
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
