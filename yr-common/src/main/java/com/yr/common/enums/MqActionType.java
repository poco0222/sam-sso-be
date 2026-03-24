package com.yr.common.enums;

/**
 * MQ消息操作类型
 */
public enum MqActionType {
    /** 新增 */
    I("I", "新增"),
    /** 修改 */
    U("U", "修改"),
    /** 删除 */
    D("D", "删除");

    private final String code;
    private final String desc;

    MqActionType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
