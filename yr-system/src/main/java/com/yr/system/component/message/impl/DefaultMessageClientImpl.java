/**
 * @file 默认消息客户端实现，负责把消息发送委托给 Web 消息服务
 * @author Codex
 * @date 2026-03-16
 */
package com.yr.system.component.message.impl;

import com.yr.system.component.message.IMessageClient;
import com.yr.system.component.message.IMessageEntity;
import com.yr.system.component.message.IWebMessageService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * <p>
 * 默认消息发送客户端
 * </p>
 *
 * @author carl 2022-01-17 11:31 default
 * @version V1.0
 */
@Service
public class DefaultMessageClientImpl implements IMessageClient {

    private final IWebMessageService messageService;

    /**
     * 通过构造器显式注入消息服务，便于单元测试和依赖追踪。
     *
     * @param messageService Web 消息服务
     */
    public DefaultMessageClientImpl(IWebMessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void sendWebMessage(String templateCode, String receiverGroupCode, Map<String, String> args) {
        messageService.sendMessage(templateCode, receiverGroupCode, args);
    }

    @Override
    public void sendWebMessage(String templateCoe, List<Long> users, Map<String, String> args) {
        messageService.sendMessage(templateCoe, users, args);
    }

    @Override
    public void sendWebMessage(IMessageEntity message, List<Long> users) {
        messageService.sendMessage(message, users);
    }
}
