/**
 * @file 锁定 DefaultMessageListenerImpl 的发送前后行为
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.component.message.impl;

import com.yr.common.enums.MessageType;
import com.yr.system.component.message.MessageJsonSerializer;
import com.yr.system.component.message.IMessageEntity;
import com.yr.system.domain.entity.SysMessageBody;
import com.yr.system.domain.entity.SysMessageBodyReceiver;
import com.yr.system.service.ISysMessageBodyReceiverService;
import com.yr.system.service.ISysMessageBodyService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * DefaultMessageListenerImpl 行为测试。
 */
class DefaultMessageListenerImplTest {

    /**
     * 验证 open() 默认发送路径会委托给统一 serializer（序列化器），并将结果发送到 websocket session。
     *
     * @throws Exception websocket 发送异常（用于兼容 sendText 声明的 IOException）
     */
    @Test
    void shouldSendSerializedSuccessMessageOnOpen() throws Exception {
        ISysMessageBodyService messageBodyService = mock(ISysMessageBodyService.class);
        ISysMessageBodyReceiverService receiverService = mock(ISysMessageBodyReceiverService.class);
        MessageJsonSerializer messageJsonSerializer = mock(MessageJsonSerializer.class);
        DefaultMessageListenerImpl listener = new DefaultMessageListenerImpl(messageBodyService, receiverService, messageJsonSerializer);

        Session session = mock(Session.class);
        RemoteEndpoint.Basic basicRemote = mock(RemoteEndpoint.Basic.class);
        when(session.getBasicRemote()).thenReturn(basicRemote);
        when(messageJsonSerializer.toJson(any())).thenReturn("open-success");

        listener.open(session, 1L, 1);

        verify(messageJsonSerializer).toJson(any());
        verify(basicRemote).sendText("open-success");
    }

    /**
     * 验证发送完成后会根据显式非空参数落库接收记录。
     */
    @Test
    void shouldPersistReceiverAfterSendWhenRequiredIdentifiersPresent() {
        ISysMessageBodyService messageBodyService = mock(ISysMessageBodyService.class);
        ISysMessageBodyReceiverService receiverService = mock(ISysMessageBodyReceiverService.class);
        MessageJsonSerializer messageJsonSerializer = mock(MessageJsonSerializer.class);
        DefaultMessageListenerImpl listener = new DefaultMessageListenerImpl(messageBodyService, receiverService, messageJsonSerializer);
        SysMessageBody messageBody = new SysMessageBody();
        messageBody.setId("body-1");
        when(messageBodyService.getById("msg-1")).thenReturn(messageBody);

        listener.sendAfter("msg-1", "sender", 10L, "SUCCESS");

        ArgumentCaptor<SysMessageBodyReceiver> captor = ArgumentCaptor.forClass(SysMessageBodyReceiver.class);
        verify(receiverService).saveOrUpdate(captor.capture());
        SysMessageBodyReceiver persistedReceiver = captor.getValue();
        assertThat(persistedReceiver.getMsgId()).isEqualTo("body-1");
        assertThat(persistedReceiver.getMsgTo()).isEqualTo(10L);
        assertThat(persistedReceiver.getSendStatus()).isEqualTo("SUCCESS");
    }

    /**
     * 验证消息标识为空白时不会触发接收记录写入。
     */
    @Test
    void shouldSkipReceiverPersistenceWhenMessageIdentifierBlank() {
        ISysMessageBodyService messageBodyService = mock(ISysMessageBodyService.class);
        ISysMessageBodyReceiverService receiverService = mock(ISysMessageBodyReceiverService.class);
        MessageJsonSerializer messageJsonSerializer = mock(MessageJsonSerializer.class);
        DefaultMessageListenerImpl listener = new DefaultMessageListenerImpl(messageBodyService, receiverService, messageJsonSerializer);

        listener.sendAfter("   ", "sender", 10L, "SUCCESS");

        verify(messageBodyService, never()).getById(any());
        verifyNoInteractions(receiverService);
    }

    /**
     * 验证发送前会保存消息主体并返回生成后的消息 ID。
     */
    @Test
    void shouldPersistMessageBodyBeforeSendWhenEntityPresent() {
        ISysMessageBodyService messageBodyService = mock(ISysMessageBodyService.class);
        ISysMessageBodyReceiverService receiverService = mock(ISysMessageBodyReceiverService.class);
        MessageJsonSerializer messageJsonSerializer = mock(MessageJsonSerializer.class);
        DefaultMessageListenerImpl listener = new DefaultMessageListenerImpl(messageBodyService, receiverService, messageJsonSerializer);
        IMessageEntity entity = new TestMessageEntity("标题", "名称", "正文");
        doAnswer(invocation -> {
            SysMessageBody body = invocation.getArgument(0);
            body.setId("generated-id");
            return true;
        }).when(messageBodyService).saveOrUpdate(any(SysMessageBody.class));

        String messageId = listener.sendBefore(entity, "sender");

        ArgumentCaptor<SysMessageBody> captor = ArgumentCaptor.forClass(SysMessageBody.class);
        verify(messageBodyService).saveOrUpdate(captor.capture());
        SysMessageBody savedBody = captor.getValue();
        assertThat(savedBody.getMsgTitle()).isEqualTo("标题");
        assertThat(savedBody.getMsgName()).isEqualTo("名称");
        assertThat(savedBody.getMsgBody()).isEqualTo("正文");
        assertThat(savedBody.getMsgFrom()).isEqualTo("sender");
        assertThat(messageId).isEqualTo("generated-id");
    }

    /**
     * 最小消息实体桩。
     */
    private static class TestMessageEntity implements IMessageEntity {
        private final String title;
        private final String name;
        private final String body;

        /**
         * @param title 标题
         * @param name 名称
         * @param body 正文
         */
        private TestMessageEntity(String title, String name, String body) {
            this.title = title;
            this.name = name;
            this.body = body;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getBody() {
            return body;
        }

        @Override
        public MessageType getMessageType() {
            return MessageType.WEB_MESSAGE;
        }
    }
}
