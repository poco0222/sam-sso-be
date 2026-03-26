/**
 * @file 锁定 YrSystemWarmupRunner 的一期依赖边界
 * @author PopoY
 * @date 2026-03-26
 */
package com.yr.system.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * YrSystemWarmupRunner 一期契约测试。
 */
class YrSystemWarmupRunnerContractTest {

    /** YrSystemWarmupRunner 源码路径。 */
    private static final Path SOURCE_PATH = Path.of("src/main/java/com/yr/system/config/YrSystemWarmupRunner.java");

    /**
     * 验证一期 warmup runner 不再依赖 codeRule/config/dict 预热服务。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldNotDependOnLegacyWarmupServices() throws IOException {
        String source = Files.readString(SOURCE_PATH);

        assertThat(source).doesNotContain("ISysCodeRuleService");
        assertThat(source).doesNotContain("ISysConfigService");
        assertThat(source).doesNotContain("ISysDictTypeService");
        assertThat(source).doesNotContain("runWarmup(");
    }
}
