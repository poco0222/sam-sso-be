/**
 * @file WebSocket 服务端 endpoint 与站内消息发送实现
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.component.message.impl;

import com.yr.common.core.domain.AjaxResult;
import com.yr.common.enums.MessageStatus;
import com.yr.common.enums.MessageType;
import com.yr.common.utils.StringUtils;
import com.yr.system.component.message.IMessageEntity;
import com.yr.system.component.message.IMessageListener;
import com.yr.system.component.message.IWebSocketService;
import com.yr.system.component.message.MessageJsonSerializer;
import com.yr.system.component.message.config.WebSocketConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * 服务端 websocket endpoint。
 *
 * <p>共享状态统一放到 {@link WebSocketConfig.WebSocketSessionRegistry} 中，
 * endpoint 实例只负责连接生命周期回调和参数桥接。</p>
 */
@ServerEndpoint(
        value = "/websocket/message/{userId}",
        configurator = WebSocketConfig.SpringEndpointConfigurator.class
)
@Component
public class WebSocketServerImpl implements IWebSocketService {

    /** WebSocket 服务日志。 */
    private static final Logger logger = LoggerFactory.getLogger(WebSocketServerImpl.class);

    /** 共享会话注册表。 */
    private final WebSocketConfig.WebSocketSessionRegistry sessionRegistry;

    /** 站内消息监听器。 */
    private final IMessageListener messageListener;

    /** 消息链路统一 JSON serializer（JSON 序列化器）。 */
    private final MessageJsonSerializer messageJsonSerializer;

    /** 当前 endpoint 绑定的 websocket 会话。 */
    private Session session;

    /** 当前 endpoint 绑定的用户 ID。 */
    private Long sid;

    /**
     * 构造函数，交由 Spring 和 websocket configurator 统一注入依赖。
     *
     * @param sessionRegistry 会话注册表
     * @param messageListener 消息监听器
     * @param messageJsonSerializer 统一 JSON 序列化器
     */
    public WebSocketServerImpl(
            WebSocketConfig.WebSocketSessionRegistry sessionRegistry,
            IMessageListener messageListener,
            MessageJsonSerializer messageJsonSerializer
    ) {
        this.sessionRegistry = sessionRegistry;
        this.messageListener = messageListener;
        this.messageJsonSerializer = messageJsonSerializer;
    }

    /**
     * 连接建立成功调用的方法。
     *
     * @param session 当前 websocket 会话
     * @param userId  当前连接用户 ID
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        this.sid = userId;
        this.session = session;
        int currentOnlineCount = sessionRegistry.register(userId, session);
        logger.info("有个一个连接！当前在线人数为{}", currentOnlineCount);
        messageListener.open(session, userId, currentOnlineCount);
    }

    /**
     * 连接关闭调用的方法。
     */
    @OnClose
    public void onClose() {
        Session closedSession = this.session;
        Long closedUserId = this.sid;
        boolean removed = sessionRegistry.unregister(closedUserId, closedSession);
        int currentOnlineCount = sessionRegistry.getOnlineCount();

        if (removed) {
            logger.info("有一连接关闭！当前在线人数为 {}", currentOnlineCount);
            messageListener.close(closedSession, closedUserId, currentOnlineCount);
        } else {
            logger.debug("忽略重复关闭或未注册连接 userId={}", closedUserId);
        }

        // 关闭后立即清理 endpoint 本地状态，避免重复 onClose 再次处理旧连接。
        this.session = null;
        this.sid = null;
    }

    /**
     * 收到客户端消息后调用的方法。
     *
     * @param message 客户端发送过来的消息
     * @param session 当前 websocket 会话
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        logger.info("收到来自窗口{}的信息:{} 当前人数:{}", sid, message, getOnlineCount());
        messageListener.message(session, sid, message);
    }

    /**
     * websocket 错误回调。
     *
     * @param error 错误信息
     */
    @OnError
    public void onError(Throwable error) {
        logger.info("onError......{}", error.getMessage());
        messageListener.error(error);
    }

    /**
     * 向当前连接发送文本消息。
     *
     * @param message 文本消息
     * @throws IOException websocket 发送异常
     */
    public void sendMessage(String message) throws IOException {
        sendAsyncText(this.session, message);
    }

    /**
     * 把业务消息包装为前端统一响应体。
     *
     * @param object 消息体
     * @return AjaxResult 结构
     */
    private AjaxResult getMessageObject(Object object) {
        return AjaxResult.success(object);
    }

    /**
     * 仅用于测试和排查时观察当前 endpoint 绑定会话。
     *
     * @return 当前 endpoint 会话
     */
    public Session getSession() {
        return session;
    }

    /**
     * 获取当前在线连接总数。
     *
     * @return 在线连接总数
     */
    @Override
    public int getOnlineCount() {
        return sessionRegistry.getOnlineCount();
    }

    /**
     * 获取指定用户的 websocket 会话快照。
     *
     * @param userId 用户 ID
     * @return 会话快照
     */
    @Override
    public List<Session> getSession(Long userId) {
        return sessionRegistry.getSessions(userId);
    }

    /**
     * 向指定用户的全部在线连接发送文本。
     *
     * @param receive 接收用户 ID
     * @param text    文本消息
     * @return 是否成功发给该用户的全部在线连接
     */
    private boolean send(Long receive, String text) {
        if (receive == null) {
            return false;
        }

        List<Session> sessions = getSession(receive);
        if (sessions.isEmpty()) {
            return false;
        }

        boolean success = true;
        for (Session currentSession : sessions) {
            if (!sendAsyncTextSafely(currentSession, text, receive)) {
                success = false;
            }
        }
        return success;
    }

    /**
     * 使用容器异步 remote 发送文本，并等待 Future 结果来保留原有成功/失败语义。
     *
     * @param targetSession 目标会话
     * @param text 消息文本
     * @throws IOException websocket 发送异常
     */
    private void sendAsyncText(Session targetSession, String text) throws IOException {
        waitForAsyncSend(targetSession.getAsyncRemote().sendText(text));
    }

    /**
     * 向单个会话安全发送文本，失败时仅记录并返回 false，不打断同批次其他连接。
     *
     * @param targetSession 目标会话
     * @param text 消息文本
     * @param receive 接收用户 ID
     * @return 该会话是否发送成功
     */
    private boolean sendAsyncTextSafely(Session targetSession, String text, Long receive) {
        try {
            sendAsyncText(targetSession, text);
            return true;
        } catch (IOException | RuntimeException ex) {
            logger.error("消息发送失败 userId={}", receive, ex);
            return false;
        }
    }

    /**
     * 等待 AsyncRemote 的 Future 完成，并把线程中断与执行异常统一折叠成 IOException。
     *
     * @param sendFuture websocket 异步发送句柄
     * @throws IOException websocket 发送异常
     */
    private void waitForAsyncSend(Future<Void> sendFuture) throws IOException {
        try {
            sendFuture.get();
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IOException("异步发送被中断", interruptedException);
        } catch (ExecutionException executionException) {
            Throwable cause = executionException.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            throw new IOException("异步发送执行失败", cause);
        }
    }

    /**
     * 向指定用户发送业务消息。
     *
     * @param receive 接收用户 ID
     * @param result  业务消息
     * @return 是否成功发给该用户的全部在线连接
     */
    private boolean send(Long receive, IMessageEntity result) {
        return send(receive, messageJsonSerializer.toJson(getMessageObject(result)));
    }

    /**
     * 批量发送站内消息，并把真实发送结果回写到监听器。
     *
     * @param receives   接收人集合
     * @param result     消息体
     * @param fromUserId 发送人
     */
    @Override
    public void send(List<Long> receives, IMessageEntity result, String fromUserId) {
        String messageId = "";
        boolean shouldSaveMessage = true;

        if (CollectionUtils.isEmpty(receives)) {
            return;
        }

        if (MessageType.WEB_MESSAGE_NOTIFY.name().equals(result.getMessageType().name())) {
            shouldSaveMessage = false;
        }

        if (shouldSaveMessage) {
            messageId = messageListener.sendBefore(result, fromUserId);
        }

        for (Long receive : receives) {
            String status = MessageStatus.MSG_NOT_SENT_YET.getCode();
            try {
                if (send(receive, result)) {
                    status = MessageStatus.MSG_HAS_BEEN_SENT.getCode();
                }
            } catch (RuntimeException ex) {
                logger.error("发送消息失败 user={} body={}", receive, messageJsonSerializer.toJsonQuietly(result), ex);
            }

            if (shouldSaveMessage && StringUtils.isNotBlank(messageId)) {
                messageListener.sendAfter(messageId, fromUserId, receive, status);
            }
        }
    }

    /**
     * 全局消息推送到指定用户的所有在线连接。
     *
     * @param userId 用户 ID
     * @param text   推送内容
     */
    @Override
    public void sendGlobalMsgToUser(Long userId, String text) {
        Optional.ofNullable(userId).ifPresent(targetUserId -> send(targetUserId, text));
    }

}
