/**
 * @file 锁定消息模板服务的重复编码校验契约
 * @author Codex
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.yr.common.exception.CustomException;
import com.yr.system.domain.entity.SysMsgTemplate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

/**
 * SysMsgTemplateService 行为测试。
 */
class SysMsgTemplateServiceTest {

    /**
     * 验证新增时遇到重复消息编码会拒绝保存。
     */
    @Test
    void shouldRejectDuplicatedMessageTemplateCodeOnCreate() {
        SysMsgTemplateService service = spy(new SysMsgTemplateService());
        SysMsgTemplate existingTemplate = buildTemplate("MSG_CODE", "已存在模板");
        SysMsgTemplate command = buildTemplate("MSG_CODE", "待新增模板");
        doReturn(existingTemplate).when(service).get("MSG_CODE");
        doReturn(true).when(service).saveOrUpdate(any(SysMsgTemplate.class));

        assertThatThrownBy(() -> service.saveMessageTemplate(command))
                .isInstanceOf(CustomException.class)
                .hasMessage("消息编码已经存在，添加失败");

        verify(service, never()).saveOrUpdate(any(SysMsgTemplate.class));
    }

    /**
     * 构造最小可用的消息模板对象。
     *
     * @param msgCode 模板编码
     * @param msgName 模板名称
     * @return 消息模板
     */
    private SysMsgTemplate buildTemplate(String msgCode, String msgName) {
        SysMsgTemplate template = new SysMsgTemplate();
        template.setMsgCode(msgCode);
        template.setMsgName(msgName);
        template.setTitle("标题-${name}");
        template.setMsgContent("内容-${name}");
        return template;
    }
}
