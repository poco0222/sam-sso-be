/**
 * @file AsyncWebMessageDispatchService 行为测试
 * @author Codex
 * @date 2026-03-17
 */
package com.yr.system.component.message.impl;

import com.yr.system.component.message.IWebSocketService;
import com.yr.system.domain.entity.SysMsgTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.verify;

/**
 * 验证异步分发服务会把消息转交给 websocket 服务。
 */
@ExtendWith(MockitoExtension.class)
class AsyncWebMessageDispatchServiceTest {

    /** WebSocket 发送服务桩。 */
    @Mock
    private IWebSocketService webSocketService;

    /** 被测试对象。 */
    @InjectMocks
    private AsyncWebMessageDispatchService dispatchService;

    /**
     * 验证异步分发服务会调用 websocket 服务完成发送。
     *
     * @throws Exception 用于兼容后续分发方法可能声明的异常
     */
    @Test
    void shouldDelegateSendToWebSocketService() throws Exception {
        SysMsgTemplate message = new SysMsgTemplate();
        List<Long> users = List.of(1L, 2L);

        dispatchService.dispatch(users, message, "admin");

        verify(webSocketService).send(users, message, "admin");
    }
}
