/**
 * @file 抽象消息监听器，负责 websocket open 阶段默认响应
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.component.message;

import com.yr.common.core.domain.AjaxResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.io.IOException;

/**
 * 抽象的消息接收处理器。
 *
 * <p>历史上此处使用 fastjson（JSON 序列化库：Fastjson），当前统一迁移为 Jackson（JSON 序列化库：Jackson）。</p>
 *
 * @author PopoY
 * @since 2022-01-17（origin: carl）
 */
public abstract class AbstractMessageListener implements IMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMessageListener.class);

    /** 统一 JSON 序列化器。 */
    private final MessageJsonSerializer messageJsonSerializer;

    /**
     * 由子类通过构造器注入统一 serializer（序列化器）。
     *
     * @param messageJsonSerializer 统一 JSON 序列化器
     */
    protected AbstractMessageListener(MessageJsonSerializer messageJsonSerializer) {
        this.messageJsonSerializer = messageJsonSerializer;
    }

    @Override
    public void open(Session session, Long userId, int currentUserSize) {
        // 默认给客户端发送一条“成功”响应，保持历史行为一致。
        try {
            session.getBasicRemote().sendText(messageJsonSerializer.toJson(AjaxResult.success()));
        } catch (IOException | RuntimeException ex) {
            logger.error("发送消息失败 userId={}", userId, ex);
        }
    }
}
