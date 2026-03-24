/**
 * @file 锁定 WebMessageServiceImpl 当前发送行为的单元测试
 * @author PopoY
 * @date 2026-03-11
 */
package com.yr.system.component.message.impl;

import com.yr.common.enums.ModeType;
import com.yr.common.enums.MessageType;
import com.yr.common.exception.CustomException;
import com.yr.system.component.message.IWebSocketService;
import com.yr.system.component.message.MessageJsonSerializer;
import com.yr.system.domain.entity.SysMsgTemplate;
import com.yr.system.domain.entity.SysReceiveGroup;
import com.yr.system.domain.entity.SysReceiveGroupObject;
import com.yr.system.service.ISysMsgTemplateService;
import com.yr.system.service.ISysReceiveGroupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * WebMessageServiceImpl 行为测试。
 */
@ExtendWith(MockitoExtension.class)
class WebMessageServiceImplTest {

    /** WebSocket 发送服务桩。 */
    @Mock
    private IWebSocketService messageService;

    /** 模板服务桩。 */
    @Mock
    private ISysMsgTemplateService templateService;

    /** 接收组服务桩。 */
    @Mock
    private ISysReceiveGroupService receiveGroupService;

    /** 异步消息分发服务桩。 */
    @Mock
    private AsyncWebMessageDispatchService asyncDispatchService;

    /** 消息链路统一 JSON serializer（JSON 序列化器）桩。 */
    @Mock
    private MessageJsonSerializer messageJsonSerializer;

    /** 被测试对象。 */
    @InjectMocks
    private WebMessageServiceImpl webMessageService;

    /**
     * 验证直接发送用户消息时会委托给异步分发服务。
     */
    @Test
    void shouldDispatchDirectMessageAsynchronously() {
        List<Long> users = Arrays.asList(1L, 2L);
        SysMsgTemplate sysMsgTemplate = buildMessageTemplate();

        webMessageService.sendMessage(sysMsgTemplate, users);

        verify(messageJsonSerializer).toJsonQuietly(sysMsgTemplate);
        verify(asyncDispatchService).dispatch(users, sysMsgTemplate, "admin");
        verifyNoInteractions(messageService);
    }

    /**
     * 验证接收人为空时仍保持原有异常语义。
     */
    @Test
    void shouldRejectEmptyUsersWhenSendingDirectMessage() {
        SysMsgTemplate sysMsgTemplate = buildMessageTemplate();

        assertThatThrownBy(() -> webMessageService.sendMessage(sysMsgTemplate, Collections.emptyList()))
                .isInstanceOf(CustomException.class)
                .hasMessage("接收人用户集合为");
    }

    /**
     * 验证模板与接收组校验通过后，会走异步分发而不是直接发 websocket。
     */
    @Test
    void shouldDispatchAsyncAfterValidationSucceeds() {
        SysMsgTemplate template = buildMessageTemplate();
        when(templateService.get("TPL")).thenReturn(template);
        when(receiveGroupService.getReceiveGroupList("GROUP")).thenReturn(buildReceiveGroup(1L));

        webMessageService.sendMessage("TPL", "GROUP", Collections.singletonMap("${temp.name}", "ok"));

        verify(asyncDispatchService).dispatch(Collections.singletonList(1L), template, "admin");
        verifyNoInteractions(messageService);
    }

    /**
     * 验证用户列表为空时仍抛出原有业务异常，且不会触发异步分发。
     */
    @Test
    void shouldNotDispatchAsyncWhenUsersEmpty() {
        SysMsgTemplate template = new SysMsgTemplate();
        SysReceiveGroup receiveGroup = new SysReceiveGroup();
        receiveGroup.setReMode(ModeType.USER_GROUP.name());
        receiveGroup.setReCode("GROUP_CODE");
        receiveGroup.setGroupObjectList(Collections.emptyList());
        when(templateService.get("TPL_CODE")).thenReturn(template);
        when(receiveGroupService.getReceiveGroupList("GROUP_CODE")).thenReturn(receiveGroup);

        assertThatThrownBy(() -> webMessageService.sendMessage("TPL_CODE", "GROUP_CODE", null))
                .isInstanceOf(CustomException.class)
                .hasMessage("模组没有用户，请先添加模组或者用户");

        verifyNoInteractions(asyncDispatchService);
        verifyNoInteractions(messageService);
    }

    /**
     * 验证模板字段为 null 时会保持原值，不会阻断消息发送。
     */
    @Test
    void shouldKeepNullFieldsWhenResolvingTemplate() {
        SysMsgTemplate template = buildMessageTemplate();
        Map<String, String> args = new HashMap<>();
        args.put("${temp.name}", "已替换名称");
        template.setTitle(null);
        template.setMsgName("模板-${temp.name}");
        when(templateService.get("TPL_NULL")).thenReturn(template);
        when(receiveGroupService.getReceiveGroupList("GROUP_NULL")).thenReturn(buildReceiveGroup(1L));

        webMessageService.sendMessage("TPL_NULL", "GROUP_NULL", args);

        assertThat(template.getTitle()).isNull();
        assertThat(template.getMsgName()).isEqualTo("模板-已替换名称");
        verify(asyncDispatchService).dispatch(Collections.singletonList(1L), template, "admin");
        verifyNoInteractions(messageService);
    }

    /**
     * 验证非字符串字段在模板解析阶段保持原值。
     */
    @Test
    void shouldKeepNonStringFieldsUnchangedWhenResolvingTemplate() {
        SysMsgTemplate template = buildMessageTemplate();
        template.setMessageType(MessageType.WEB_MESSAGE_NOTIFY);
        when(templateService.get("TPL_ENUM")).thenReturn(template);
        when(receiveGroupService.getReceiveGroupList("GROUP_ENUM")).thenReturn(buildReceiveGroup(2L));

        webMessageService.sendMessage("TPL_ENUM", "GROUP_ENUM", Collections.singletonMap("${temp.name}", "ignored"));

        assertThat(template.getMessageType()).isEqualTo(MessageType.WEB_MESSAGE_NOTIFY);
        verify(asyncDispatchService).dispatch(Collections.singletonList(2L), template, "admin");
        verifyNoInteractions(messageService);
    }

    /**
     * 验证占位符未命中时保留原始模板内容。
     */
    @Test
    void shouldKeepOriginalContentWhenPlaceholderMissing() {
        SysMsgTemplate template = buildMessageTemplate();
        template.setMsgContent("消息-${temp.missing}");
        when(templateService.get("TPL_MISSING")).thenReturn(template);
        when(receiveGroupService.getReceiveGroupList("GROUP_MISSING")).thenReturn(buildReceiveGroup(3L));

        webMessageService.sendMessage("TPL_MISSING", "GROUP_MISSING", Collections.singletonMap("${temp.name}", "unused"));

        assertThat(template.getMsgContent()).isEqualTo("消息-${temp.missing}");
        verify(asyncDispatchService).dispatch(Collections.singletonList(3L), template, "admin");
        verifyNoInteractions(messageService);
    }

    /**
     * 验证模板解析只作用于允许替换的文本字段，不会改动模板编码和参数定义。
     */
    @Test
    void shouldOnlyResolveAllowedTemplateFields() {
        SysMsgTemplate template = buildMessageTemplate();
        template.setMsgName("名称-${temp.name}");
        template.setTitle("标题-${temp.name}");
        template.setMsgContent("内容-${temp.name}");
        template.setMsgCode("CODE-${temp.name}");
        template.setMsgParams("{\"name\":\"${temp.name}\"}");
        when(templateService.get("TPL_FIELDS")).thenReturn(template);
        when(receiveGroupService.getReceiveGroupList("GROUP_FIELDS")).thenReturn(buildReceiveGroup(4L));

        webMessageService.sendMessage("TPL_FIELDS", "GROUP_FIELDS", Collections.singletonMap("${temp.name}", "已替换"));

        assertThat(template.getMsgName()).isEqualTo("名称-已替换");
        assertThat(template.getTitle()).isEqualTo("标题-已替换");
        assertThat(template.getMsgContent()).isEqualTo("内容-已替换");
        assertThat(template.getMsgCode()).isEqualTo("CODE-${temp.name}");
        assertThat(template.getMsgParams()).isEqualTo("{\"name\":\"${temp.name}\"}");
        verify(asyncDispatchService).dispatch(Collections.singletonList(4L), template, "admin");
        verifyNoInteractions(messageService);
    }

    /**
     * 构造最小可用的消息模板对象。
     *
     * @return 消息模板
     */
    private SysMsgTemplate buildMessageTemplate() {
        SysMsgTemplate sysMsgTemplate = new SysMsgTemplate();
        sysMsgTemplate.setMsgName("测试消息");
        sysMsgTemplate.setMsgContent("消息内容");
        sysMsgTemplate.setTitle("标题");
        return sysMsgTemplate;
    }

    /**
     * 构造最小接收组对象。
     *
     * @param userId 接收用户 ID
     * @return 接收组对象
     */
    private SysReceiveGroup buildReceiveGroup(Long userId) {
        SysReceiveGroupObject receiveGroupObject = new SysReceiveGroupObject();
        receiveGroupObject.setReObjectId(userId);

        SysReceiveGroup receiveGroup = new SysReceiveGroup();
        receiveGroup.setReMode(ModeType.USER_GROUP.name());
        receiveGroup.setReCode("GROUP_CODE");
        receiveGroup.setGroupObjectList(Collections.singletonList(receiveGroupObject));
        return receiveGroup;
    }
}
