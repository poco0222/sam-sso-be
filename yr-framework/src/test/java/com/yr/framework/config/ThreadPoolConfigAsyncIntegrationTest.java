/**
 * @file ThreadPoolConfig 异步线程池集成测试
 * @author Codex
 * @date 2026-03-17
 */
package com.yr.framework.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证线程池配置是否真正启用了 Spring Async。
 */
@SpringJUnitConfig(classes = {ThreadPoolConfig.class, ThreadPoolConfigAsyncIntegrationTest.AsyncProbeService.class})
class ThreadPoolConfigAsyncIntegrationTest {

    @Autowired
    private AsyncProbeService asyncProbeService;

    /**
     * 验证异步方法会切换到配置的线程池线程执行。
     */
    @Test
    void shouldRunAsyncMethodOnConfiguredThreadPool() {
        String callerThread = Thread.currentThread().getName();

        String asyncThread = asyncProbeService.captureThreadName().join();

        assertThat(asyncThread).startsWith("yr-async-");
        assertThat(asyncThread).isNotEqualTo(callerThread);
    }

    /**
     * 测试探针服务，用于捕获异步执行时的线程名称。
     */
    @Service
    static class AsyncProbeService {

        /**
         * 返回当前执行线程名称，便于断言是否切换到了异步线程池。
         *
         * @return 当前线程名称
         */
        @Async("threadPoolTaskExecutor")
        public CompletableFuture<String> captureThreadName() {
            return CompletableFuture.completedFuture(Thread.currentThread().getName());
        }
    }
}
