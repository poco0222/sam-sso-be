/**
 * @file 客户端密钥存储源码契约测试
 * @author PopoY
 * @date 2026-03-30
 */
package com.yr.system.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定客户端密钥不能继续以明文写入 service 持久化链路。
 */
class SsoClientServiceImplSecretStorageContractTest {

    /** SsoClientServiceImpl 源码路径。 */
    private static final Path SOURCE_PATH = Path.of("src/main/java/com/yr/system/service/impl/SsoClientServiceImpl.java");

    /**
     * 验证 service 源码必须包含密钥哈希逻辑，且不再把原始 secret 直接写回实体持久化。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldHashClientSecretBeforePersisting() throws IOException {
        String normalizedSource = normalizeWhitespace(Files.readString(SOURCE_PATH));

        assertThat(normalizedSource)
                .contains("SecurityUtils.encryptPassword")
                .doesNotContain("ssoClient.setClientSecret(clientSecret);");
    }

    /**
     * 归一化空白，减少源码断言对缩进和换行的敏感度。
     *
     * @param source 原始源码
     * @return 归一化后的源码
     */
    private String normalizeWhitespace(String source) {
        return source.replaceAll("\\s+", " ").trim();
    }
}
