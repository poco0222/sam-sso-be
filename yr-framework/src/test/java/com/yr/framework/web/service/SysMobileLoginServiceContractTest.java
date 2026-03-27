/**
 * @file 移动登录遗留链路契约测试
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.framework.web.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定无调用方的移动登录服务不会继续留在一期主执行路径。
 */
class SysMobileLoginServiceContractTest {

    /** 已下线的移动登录服务源码路径。 */
    private static final Path SOURCE_PATH = Path.of("src/main/java/com/yr/framework/web/service/SysMobileLoginService.java");

    /**
     * 验证无调用方的移动登录服务已从一期 runtime surface 下线。
     */
    @Test
    void shouldRemoveUnusedMobileLoginServiceFromPrimaryRuntimePath() {
        assertThat(Files.exists(SOURCE_PATH))
                .as("无调用方的 SysMobileLoginService 应从一期主执行路径移除")
                .isFalse();
    }
}
