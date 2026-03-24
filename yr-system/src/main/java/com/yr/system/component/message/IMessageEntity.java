package com.yr.system.component.message;

import com.yr.common.enums.MessageType;

/**
 * <p>
 * description
 * </p>
 *
 * @author carl 2022-01-19 13:51
 * @version V1.0
 */
public interface IMessageEntity {
    /**
     * 消息体
     *
     * @return
     */
    String getBody();

    /**
     * 消息标题
     *
     * @return
     */
    String getTitle();

    /**
     * 消息名称
     *
     * @return
     */
    String getName();


    /**
     * 消息类型
     *
     * @return
     */
    MessageType getMessageType();
}
