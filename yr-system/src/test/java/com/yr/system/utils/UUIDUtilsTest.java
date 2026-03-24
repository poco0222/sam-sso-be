/**
 * @file 锁定 UUIDUtils 当前行为的特征测试
 * @author Codex
 * @date 2026-03-11
 */
package com.yr.system.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UUIDUtils 行为快照测试。
 */
class UUIDUtilsTest {

    /**
     * 验证支持的位数会返回固定长度结果。
     */
    @Test
    void shouldReturnExpectedLengthForSupportedDigits() {
        assertThat(UUIDUtils.getUUID(8)).hasSize(8);
        assertThat(UUIDUtils.getUUID(16)).hasSize(16);
        assertThat(UUIDUtils.getUUID(22)).hasSize(22);
        assertThat(UUIDUtils.getUUID(32)).hasSize(32);
    }

    /**
     * 验证不支持的位数仍会回退到 32 位 UUID。
     */
    @Test
    void shouldFallbackTo32WhenDigitUnsupported() {
        assertThat(UUIDUtils.getUUID(-1)).hasSize(32);
        assertThat(UUIDUtils.getUUID(0)).hasSize(32);
        assertThat(UUIDUtils.getUUID(99)).hasSize(32);
    }
}
