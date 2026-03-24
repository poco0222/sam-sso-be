package com.yr.common.enums;

/**
 * <p>
 * 接收消息模组类型
 * </p>
 *
 * @author carl 2022-01-06 14:03
 * @version V1.0
 */
public enum ModeType {

    USER_GROUP("用户组");

    private String name;

    ModeType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
