package com.yr.common.constant;

/**
 * 工单状态常量
 *
 * @author PopoY
 * @date 2026-02-04
 */
public class OrderStateConstants {

    /**
     * 初始状态
     */
    public static final String INIT = "0";

    /**
     * 待加工
     */
    public static final String PENDING = "1";

    /**
     * 正在加工
     */
    public static final String PROCESSING = "2";

    /**
     * 部分加工
     */
    public static final String PARTIAL = "3";

    /**
     * 加工完成
     */
    public static final String COMPLETED = "4";

    /**
     * 暂停加工
     */
    public static final String PAUSED = "6";

    /**
     * 已禁用
     */
    public static final String DISABLED = "9";

    private OrderStateConstants() {
        // 私有构造函数，防止实例化
    }
}
