/**
 * @file 站内消息异步分发服务
 * @author Codex
 * @date 2026-03-17
 */
package com.yr.system.component.message.impl;

import com.yr.system.component.message.IMessageEntity;
import com.yr.system.component.message.IWebSocketService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 负责把站内消息分发委托给 Spring Async 线程池执行。
 */
@Service
public class AsyncWebMessageDispatchService {

    /** websocket 发送服务。 */
    private final IWebSocketService webSocketService;

    /**
     * 构造异步分发服务。
     *
     * @param webSocketService websocket 发送服务
     */
    public AsyncWebMessageDispatchService(IWebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    /**
     * 异步分发站内消息。
     *
     * @param users 接收用户列表
     * @param message 消息内容
     * @param fromUserId 发送人
     */
    @Async("threadPoolTaskExecutor")
    public void dispatch(List<Long> users, IMessageEntity message, String fromUserId) {
        try {
            webSocketService.send(users, message, fromUserId);
        } catch (IOException exception) {
            throw new IllegalStateException("异步分发站内消息失败", exception);
        }
    }
}
