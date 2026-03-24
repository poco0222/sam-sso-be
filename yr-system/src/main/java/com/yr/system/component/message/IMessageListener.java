package com.yr.system.component.message;

import javax.websocket.Session;
import java.io.Serializable;

/**
 * <p>
 * 消息广播事件
 * </p>
 *
 * @author carl 2022-01-17 11:37
 * @version V1.0
 */
public interface IMessageListener extends Serializable {

    /**
     * 连接后客户端发送过来的消息
     *
     * @param session
     * @param userId
     * @param currentUserSize
     */
    void open(Session session, Long userId, int currentUserSize);

    /**
     * 关闭后广播
     *
     * @param session
     * @param currentUserSize
     * @param userId
     */
    void close(Session session, Long userId, int currentUserSize);

    /**
     * 接收消息
     *
     * @param session
     * @param userId
     * @param message
     */
    void message(Session session, Long userId, String message);

    /**
     * 错误接收
     *
     * @param error
     */
    void error(Throwable error);

    /**
     * 发送消息之后
     *
     * @param messageId  消息对象
     * @param toUserId   接收用户对象
     * @param fromUserId 发送用户对象
     * @param status     状态
     */
    void sendAfter(String messageId, String fromUserId, Long toUserId, String status);

    /**
     * 发送消息之前
     *
     * @param entity
     * @param formUserId
     * @return
     */
    String sendBefore(IMessageEntity entity, String formUserId);
}
