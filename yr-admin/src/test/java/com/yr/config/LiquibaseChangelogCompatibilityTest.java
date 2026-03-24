/**
 * @file Liquibase 初始化脚本兼容性回归测试
 * @author Codex
 * @date 2026-03-10
 */
package com.yr.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

/**
 * Liquibase 初始化脚本兼容性测试。
 */
class LiquibaseChangelogCompatibilityTest {

    /**
     * 验证初始化 changelog 对已有库具备幂等保护，避免启用 Liquibase 时重复建表或重复插入初始化菜单。
     *
     * @throws Exception 读取资源失败时抛出
     */
    @Test
    void shouldKeepBootstrapChangelogIdempotentForExistingSchema() throws Exception {
        String changelogContent = StreamUtils.copyToString(
                new ClassPathResource("db/liquibase/changelog/system/changelog1.0.xml").getInputStream(),
                StandardCharsets.UTF_8
        );

        Assertions.assertAll(
                () -> assertContains(changelogContent, "CREATE TABLE IF NOT EXISTS `sys_duty`"),
                () -> assertContains(changelogContent, "CREATE TABLE IF NOT EXISTS `sys_user_duty`"),
                () -> assertContains(changelogContent, "CREATE TABLE IF NOT EXISTS `sys_rank`"),
                () -> assertContains(changelogContent, "CREATE TABLE IF NOT EXISTS `sys_user_rank`"),
                () -> assertContains(changelogContent, "CREATE TABLE IF NOT EXISTS `sys_attach_category`"),
                () -> assertContains(changelogContent, "CREATE TABLE IF NOT EXISTS `sys_attachment`"),
                () -> assertContains(changelogContent, "CREATE TABLE IF NOT EXISTS `sys_file`"),
                () -> assertContains(changelogContent, "INSERT IGNORE INTO `sys_duty`"),
                () -> assertContains(changelogContent, "INSERT IGNORE INTO `sys_rank`"),
                () -> assertContains(changelogContent, "<preConditions onFail=\"MARK_RAN\" onError=\"MARK_RAN\">"),
                () -> assertContains(changelogContent, "system:attach:upload")
        );
    }

    /**
     * 断言 changelog 文本包含指定片段。
     *
     * @param changelogContent changelog 原文
     * @param expectedFragment 期望出现的关键片段
     */
    private static void assertContains(String changelogContent, String expectedFragment) {
        Assertions.assertTrue(
                changelogContent.contains(expectedFragment),
                "Liquibase 初始化脚本缺少兼容性保护片段: " + expectedFragment
        );
    }
}
