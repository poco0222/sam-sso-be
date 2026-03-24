package com.yr.common.annotation;

import com.yr.common.enums.MessageType;

import java.lang.annotation.*;

/**
 * <p>
 * 自动发送消息
 * </p>
 * 属性值必须是常量
 *
 * @author carl 2022-01-21 13:38
 * @version V1.0
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoMessage {

    String title() default "";

    String name() default "";

    String body() default "";

    /**
     * 默认发送站内消息通知，不保存记录数据库
     *
     * @return
     */
    MessageType type() default MessageType.WEB_MESSAGE_NOTIFY;
}
