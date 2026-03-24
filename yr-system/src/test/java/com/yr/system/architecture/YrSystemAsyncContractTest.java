/**
 * @file yr-system 异步消息契约测试
 * @author Codex
 * @date 2026-03-17
 */
package com.yr.system.architecture;

import com.yr.system.component.message.IMessageEntity;
import com.yr.system.component.message.IWebSocketService;
import com.yr.system.component.message.impl.AsyncWebMessageDispatchService;
import com.yr.system.component.message.impl.WebSocketServerImpl;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定站内消息异步分发相关的架构契约。
 */
class YrSystemAsyncContractTest {

    /**
     * 验证接口和实现都不再暴露无效的 async 快捷方法。
     */
    @Test
    void shouldNotExposeAsyncShortcutOnWebSocketService() {
        assertThat(methodNames(IWebSocketService.class))
                .as("IWebSocketService 不应继续暴露 async()")
                .doesNotContain("async");
        assertThat(methodNames(WebSocketServerImpl.class))
                .as("WebSocketServerImpl 不应继续暴露 async()")
                .doesNotContain("async");
    }

    /**
     * 验证异步分发方法显式声明了 Spring Async 注解。
     *
     * @throws NoSuchMethodException 方法不存在时测试失败
     */
    @Test
    void shouldAnnotateAsyncDispatchMethod() throws NoSuchMethodException {
        assertThat(AsyncWebMessageDispatchService.class
                .getDeclaredMethod("dispatch", List.class, IMessageEntity.class, String.class)
                .getAnnotation(Async.class))
                .isNotNull();
    }

    /**
     * 提取目标类型的方法名列表，便于断言接口面。
     *
     * @param targetClass 目标类型
     * @return 方法名集合
     */
    private Iterable<String> methodNames(Class<?> targetClass) {
        return Arrays.stream(targetClass.getDeclaredMethods()).map(Method::getName).toList();
    }
}
