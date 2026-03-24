package com.yr.common.enums;

/**
 * <p>
 * description mail
 * </p>
 *
 * @author carl 2022-01-19 14:17
 * @version V1.0
 */
public enum MessageType {
    WEB_MESSAGE("站内消息"),
    MAIL_MESSAGE("邮件消息"),
    WEB_MESSAGE_NOTIFY("站内消息通知");

    private String name;

    MessageType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
