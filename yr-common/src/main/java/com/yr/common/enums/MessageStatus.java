package com.yr.common.enums;

/**
 * <p>
 * description mail
 * </p>
 *
 * @author carl 2022-01-19 14:17
 * @version V1.0
 */
public enum MessageStatus {
    MSG_UNREAD("0", "未读"),
    MSG_HAVE_READ("1", "已读"),
    MSG_NOT_SENT_YET("1", "未发送"),
    MSG_HAS_BEEN_SENT("0", "已发送");

    private String code;

    private String name;

    MessageStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
