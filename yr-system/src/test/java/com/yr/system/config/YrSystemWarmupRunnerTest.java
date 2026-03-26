/**
 * @file 验证 yr-system 一期 warmup runner 的最小行为
 * @author PopoY
 * @date 2026-03-26
 */
package com.yr.system.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * YrSystemWarmupRunner 行为测试。
 */
class YrSystemWarmupRunnerTest {

    /**
     * 验证一期 runner 在无额外依赖时也能安全执行。
     */
    @Test
    void shouldRunWithoutLegacyWarmupDependencies() {
        YrSystemWarmupRunner warmupRunner = new YrSystemWarmupRunner();

        assertThatCode(() -> warmupRunner.run(new DefaultApplicationArguments(new String[0])))
                .doesNotThrowAnyException();
    }
}
