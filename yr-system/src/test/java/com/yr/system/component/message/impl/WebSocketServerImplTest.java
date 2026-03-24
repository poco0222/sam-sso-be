/**
 * @file 验证 WebSocketServerImpl 在多连接场景下的注册、关闭与发送行为
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.component.message.impl;

import com.yr.common.core.domain.AjaxResult;
import com.yr.common.enums.MessageStatus;
import com.yr.system.component.message.MessageJsonSerializer;
import com.yr.system.component.message.IMessageListener;
import com.yr.system.component.message.config.WebSocketConfig;
import com.yr.system.domain.entity.SysMsgTemplate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.io.IOException;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WebSocketServerImpl 行为测试。
 */
class WebSocketServerImplTest {

    /**
     * 验证已注册的负数 userId 仍会被视为有效连接标识，而不是被当作“空值”跳过。
     *
     * @throws IOException websocket 发送异常
     */
    @Test
    void shouldBroadcastToRegisteredNegativeUserId() throws IOException {
        IMessageListener messageListener = mock(IMessageListener.class);
        WebSocketServerImpl webSocketServer = createServer(messageListener);
        Session session = mockSessionWithAsyncSend(CompletableFuture.completedFuture(null));

        webSocketServer.onOpen(session, -1L);
        webSocketServer.sendGlobalMsgToUser(-1L, "negative-id");

        verify(session.getAsyncRemote()).sendText("negative-id");
        verify(session.getBasicRemote(), never()).sendText(anyString());
    }

    /**
     * 验证同一用户注册多个 session 后会全部参与广播发送。
     *
     * @throws IOException websocket 发送异常
     */
    @Test
    void shouldRegisterMultipleSessionsForSameUserAndBroadcastToAll() throws IOException {
        IMessageListener messageListener = mock(IMessageListener.class);
        WebSocketServerImpl webSocketServer = createServer(messageListener);
        Session sessionOne = mockSessionWithAsyncSend(CompletableFuture.completedFuture(null));
        Session sessionTwo = mockSessionWithAsyncSend(CompletableFuture.completedFuture(null));

        webSocketServer.onOpen(sessionOne, 100L);
        webSocketServer.onOpen(sessionTwo, 100L);
        webSocketServer.sendGlobalMsgToUser(100L, "phase0-broadcast");

        assertThat(webSocketServer.getSession(100L)).containsExactly(sessionOne, sessionTwo);
        assertThat(webSocketServer.getOnlineCount()).isEqualTo(2);
        verify(sessionOne.getAsyncRemote()).sendText("phase0-broadcast");
        verify(sessionTwo.getAsyncRemote()).sendText("phase0-broadcast");
        verify(sessionOne.getBasicRemote(), never()).sendText(anyString());
        verify(sessionTwo.getBasicRemote(), never()).sendText(anyString());
    }

    /**
     * 验证重复关闭同一个连接时，不应继续递减在线人数。
     */
    @Test
    void shouldKeepOnlineCountStableWhenClosingSameSessionTwice() {
        IMessageListener messageListener = mock(IMessageListener.class);
        WebSocketServerImpl webSocketServer = createServer(messageListener);
        Session session = mockSession();

        webSocketServer.onOpen(session, 200L);
        webSocketServer.onClose();
        assertThat(webSocketServer.getSession(200L)).isEmpty();
        assertThatCode(() -> webSocketServer.sendGlobalMsgToUser(200L, "noop")).doesNotThrowAnyException();

        webSocketServer.onClose();

        assertThat(webSocketServer.getOnlineCount()).isZero();
    }

    /**
     * 验证单个用户异步发送失败时，不会打断同批次其他用户发送。
     *
     */
    @Test
    void shouldContinueSendingWhenAsyncRemoteWriteFailsForOneUser() throws IOException {
        IMessageListener messageListener = mock(IMessageListener.class);
        when(messageListener.sendBefore(any(), eq("admin"))).thenReturn("msg-id");
        MessageJsonSerializer messageJsonSerializer = mock(MessageJsonSerializer.class);
        when(messageJsonSerializer.toJson(any())).thenReturn("serialized-result");
        when(messageJsonSerializer.toJsonQuietly(any())).thenReturn("serialized-result");
        WebSocketServerImpl webSocketServer = createServer(messageListener, messageJsonSerializer);
        Session failedSession = mockSessionWithAsyncSend(CompletableFuture.failedFuture(new IOException("boom")));
        Session successSession = mockSessionWithAsyncSend(CompletableFuture.completedFuture(null));
        SysMsgTemplate template = buildTemplate();

        webSocketServer.onOpen(failedSession, 300L);
        webSocketServer.onOpen(successSession, 301L);
        webSocketServer.send(List.of(300L, 301L), template, "admin");

        verify(messageListener).sendAfter("msg-id", "admin", 300L, MessageStatus.MSG_NOT_SENT_YET.getCode());
        verify(messageListener).sendAfter("msg-id", "admin", 301L, MessageStatus.MSG_HAS_BEEN_SENT.getCode());
        verify(failedSession.getAsyncRemote()).sendText("serialized-result");
        verify(successSession.getAsyncRemote()).sendText("serialized-result");
        verify(failedSession.getBasicRemote(), never()).sendText(anyString());
        verify(successSession.getBasicRemote(), never()).sendText(anyString());
    }

    /**
     * 验证接收人列表为空时会直接短路，不触发消息落库与回写。
     */
    @Test
    void shouldShortCircuitWhenReceivesListIsNull() {
        IMessageListener messageListener = mock(IMessageListener.class);
        WebSocketServerImpl webSocketServer = createServer(messageListener);
        SysMsgTemplate template = buildTemplate();

        webSocketServer.send(null, template, "admin");

        verify(messageListener, never()).sendBefore(any(), anyString());
        verify(messageListener, never()).sendAfter(anyString(), anyString(), any(), anyString());
    }

    /**
     * 验证发送业务消息时会先用 AjaxResult.success(result) 包装，再交给统一 serializer（序列化器）输出文本。
     *
     * @throws IOException websocket 发送异常
     */
    @Test
    void shouldSerializeAjaxResultWrapperWhenSendingMessage() throws IOException {
        IMessageListener messageListener = mock(IMessageListener.class);
        MessageJsonSerializer messageJsonSerializer = mock(MessageJsonSerializer.class);
        when(messageJsonSerializer.toJson(any())).thenReturn("serialized-result");
        when(messageJsonSerializer.toJsonQuietly(any())).thenReturn("serialized-result");
        WebSocketServerImpl webSocketServer = createServer(messageListener, messageJsonSerializer);
        Session session = mockSessionWithAsyncSend(CompletableFuture.completedFuture(null));
        SysMsgTemplate template = buildTemplate();

        webSocketServer.onOpen(session, 400L);
        webSocketServer.send(Collections.singletonList(400L), template, "admin");

        verify(session.getAsyncRemote()).sendText("serialized-result");
        verify(session.getBasicRemote(), never()).sendText(anyString());
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messageJsonSerializer).toJson(captor.capture());
        Object wrapper = captor.getValue();
        assertThat(wrapper).isInstanceOf(AjaxResult.class);
        AjaxResult ajaxResult = (AjaxResult) wrapper;
        assertThat(ajaxResult.get(AjaxResult.DATA_TAG)).isSameAs(template);
    }

    /**
     * 构造依赖注入完成的 WebSocketServerImpl。
     *
     * @param messageListener 消息监听器桩
     * @return 被测试对象
     */
    private WebSocketServerImpl createServer(IMessageListener messageListener) {
        WebSocketConfig.WebSocketSessionRegistry sessionRegistry = new WebSocketConfig.WebSocketSessionRegistry();
        MessageJsonSerializer messageJsonSerializer = mock(MessageJsonSerializer.class);
        when(messageJsonSerializer.toJson(any())).thenReturn("serialized-default");
        when(messageJsonSerializer.toJsonQuietly(any())).thenReturn("serialized-default");
        return new WebSocketServerImpl(sessionRegistry, messageListener, messageJsonSerializer);
    }

    /**
     * 构造依赖注入完成的 WebSocketServerImpl，允许自定义 serializer（序列化器）以便断言输入输出。
     *
     * @param messageListener      消息监听器桩
     * @param messageJsonSerializer 统一 JSON 序列化器
     * @return 被测试对象
     */
    private WebSocketServerImpl createServer(IMessageListener messageListener, MessageJsonSerializer messageJsonSerializer) {
        WebSocketConfig.WebSocketSessionRegistry sessionRegistry = new WebSocketConfig.WebSocketSessionRegistry();
        return new WebSocketServerImpl(sessionRegistry, messageListener, messageJsonSerializer);
    }

    /**
     * 创建带有 BasicRemote 的 Session mock。
     *
     * @return 会话对象
     */
    private Session mockSession() {
        return mockSessionWithAsyncSend(CompletableFuture.completedFuture(null));
    }

    /**
     * 创建同时暴露 BasicRemote 和 AsyncRemote 的 Session mock。
     *
     * @param sendResult AsyncRemote 发送结果
     * @return 会话对象
     */
    private Session mockSessionWithAsyncSend(Future<Void> sendResult) {
        Session session = mock(Session.class);
        RemoteEndpoint.Basic basicRemote = mock(RemoteEndpoint.Basic.class);
        RemoteEndpoint.Async asyncRemote = mock(RemoteEndpoint.Async.class);
        when(session.getBasicRemote()).thenReturn(basicRemote);
        when(session.getAsyncRemote()).thenReturn(asyncRemote);
        when(asyncRemote.sendText(anyString())).thenReturn(sendResult);
        return session;
    }

    /**
     * 构造最小可发送的消息模板。
     *
     * @return 消息模板
     */
    private SysMsgTemplate buildTemplate() {
        SysMsgTemplate template = new SysMsgTemplate();
        template.setMsgName("测试消息");
        template.setMsgContent("消息内容");
        template.setTitle("标题");
        return template;
    }
}
