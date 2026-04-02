/**
 * @file UUIDUtils 默认分支行为测试
 * @author PopoY
 * @date 2026-03-11
 */
package com.yr.system.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UUIDUtils 默认分支语义锁定测试。
 */
class UUIDUtilsCharacterizationTest {

    /**
     * 验证不支持的位数仍回退到 32 位 UUID。
     */
    @Test
    void shouldFallbackTo32WhenDigitUnsupported() {
        assertThat(UUIDUtils.getUUID(-1)).hasSize(32);
        assertThat(UUIDUtils.getUUID(0)).hasSize(32);
        assertThat(UUIDUtils.getUUID(99)).hasSize(32);
    }
}
