/**
 * @file 验证 AsyncWebMessageDispatchService 真实走独立线程池执行
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.component.message.impl;

import com.yr.system.component.message.IMessageEntity;
import com.yr.system.component.message.IWebSocketService;
import com.yr.system.domain.entity.SysMsgTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import javax.websocket.Session;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AsyncWebMessageDispatchService 线程池行为测试。
 */
@SpringJUnitConfig(classes = {
        AsyncWebMessageDispatchServiceThreadingTest.AsyncThreadPoolTestConfig.class,
        AsyncWebMessageDispatchService.class,
        AsyncWebMessageDispatchServiceThreadingTest.DispatchProbeWebSocketService.class
})
class AsyncWebMessageDispatchServiceThreadingTest {

    @Autowired
    private AsyncWebMessageDispatchService dispatchService;

    @Autowired
    private DispatchProbeWebSocketService probeWebSocketService;

    /**
     * 验证 dispatch 会切换到 threadPoolTaskExecutor，而不是在调用线程内同步执行。
     */
    @Test
    void shouldDispatchOnConfiguredAsyncThreadPool() {
        String callerThread = Thread.currentThread().getName();
        SysMsgTemplate message = new SysMsgTemplate();

        dispatchService.dispatch(List.of(1L), message, "admin");

        String asyncThread = probeWebSocketService.awaitThreadName().join();
        assertThat(asyncThread).startsWith("yr-async-");
        assertThat(asyncThread).isNotEqualTo(callerThread);
    }

    /**
     * 为测试场景提供最小可用的异步线程池配置，避免依赖其他模块的 Spring 配置可见性。
     */
    @Configuration
    @EnableAsync
    static class AsyncThreadPoolTestConfig {

        /**
         * 提供与生产环境同名的异步线程池 bean。
         *
         * @return 线程池执行器
         */
        @Bean(name = "threadPoolTaskExecutor")
        ThreadPoolTaskExecutor threadPoolTaskExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setQueueCapacity(8);
            executor.setThreadNamePrefix("yr-async-");
            executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
            executor.initialize();
            return executor;
        }
    }

    /**
     * 线程探针版 websocket 服务，用于记录异步 dispatch 的真实执行线程。
     */
    @Service
    static class DispatchProbeWebSocketService implements IWebSocketService {

        /** 记录 dispatch 实际执行线程名。 */
        private final CompletableFuture<String> threadNameFuture = new CompletableFuture<>();

        /**
         * 等待并返回执行线程名。
         *
         * @return 执行线程名
         */
        CompletableFuture<String> awaitThreadName() {
            return threadNameFuture;
        }

        @Override
        public void send(List<Long> receive, IMessageEntity result, String fromUserId) throws IOException {
            threadNameFuture.complete(Thread.currentThread().getName());
        }

        @Override
        public int getOnlineCount() {
            return 0;
        }

        @Override
        public List<Session> getSession(Long userId) {
            return Collections.emptyList();
        }

        @Override
        public void sendGlobalMsgToUser(Long userId, String text) {
            threadNameFuture.complete(Thread.currentThread().getName());
        }
    }
}
