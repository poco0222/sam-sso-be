/**
 * @file 身份中心 Liquibase 启动契约测试
 * @author PopoY
 * @date 2026-03-26
 */
package com.yr.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定身份中心一期 Liquibase bootstrap 入口与核心表边界。
 */
class SsoLiquibaseBootstrapContractTest {

    /**
     * 验证 master.xml 只按固定顺序指向新的 SSO changelog。
     *
     * @throws Exception 读取资源失败时抛出
     */
    @Test
    void shouldPointMasterToSsoChangelogs() throws Exception {
        String masterContent = readClasspathResource("db/liquibase/master.xml");

        assertThat(masterContent).contains("changelog/sso/changelog1.0-core-console.xml");
        assertThat(masterContent).contains("changelog/sso/changelog1.1-client-sync.xml");
        assertThat(masterContent).contains("changelog/sso/changelog1.2-login-audit.xml");
        assertThat(masterContent).contains("changelog/sso/changelog1.3-operation-audit.xml");
        assertThat(masterContent).doesNotContain("includeAll");
        assertThat(masterContent).doesNotContain("changelog/system/changelog1.0.xml");
    }

    /**
     * 验证 core-console changelog 只保留一期核心表族，并排除 legacy system 表与控制台菜单基线。
     *
     * @throws Exception 读取资源失败时抛出
     */
    @Test
    void shouldKeepOnlyPhaseOneCoreTablesInCoreConsoleChangelog() throws Exception {
        String coreConsoleContent = readClasspathResource("db/liquibase/changelog/sso/changelog1.0-core-console.xml");

        assertThat(coreConsoleContent)
                .contains("<validCheckSum>8:a783bb25ed537fc2de1ab216d3895bd5</validCheckSum>")
                .contains("CREATE TABLE IF NOT EXISTS `sys_user`")
                .contains("CREATE TABLE IF NOT EXISTS `sys_org`")
                .contains("CREATE TABLE IF NOT EXISTS `sys_dept`")
                .contains("CREATE TABLE IF NOT EXISTS `sys_user_org`")
                .contains("CREATE TABLE IF NOT EXISTS `sys_user_dept`")
                .doesNotContain("CREATE TABLE IF NOT EXISTS `sys_role`")
                .doesNotContain("CREATE TABLE IF NOT EXISTS `sys_menu`")
                .doesNotContain("CREATE TABLE IF NOT EXISTS `sys_role_menu`")
                .doesNotContain("CREATE TABLE IF NOT EXISTS `sys_user_role`")
                .doesNotContain("CREATE TABLE IF NOT EXISTS `sys_config`")
                .doesNotContain("INSERT IGNORE INTO `sys_menu`")
                .doesNotContain("身份管理")
                .doesNotContain("sys_duty")
                .doesNotContain("sys_rank")
                .doesNotContain("sys_attach_")
                .doesNotContain("sys_file")
                .doesNotContain("sys_message_")
                .doesNotContain("sys_receive_group")
                .doesNotContain("sys_region");
    }

    /**
     * 验证 client-sync changelog 声明了一期新增表，不再依赖 legacy 动态菜单 seed。
     *
     * @throws Exception 读取资源失败时抛出
     */
    @Test
    void shouldDeclareClientAndSyncBootstrapArtifacts() throws Exception {
        String clientSyncContent = readClasspathResource("db/liquibase/changelog/sso/changelog1.1-client-sync.xml");

        assertThat(clientSyncContent)
                .contains("<validCheckSum>8:4163bb1671bc08c8e0b0fbde608da5dc</validCheckSum>")
                .contains("CREATE TABLE IF NOT EXISTS `sso_client`")
                .contains("CREATE TABLE IF NOT EXISTS `sso_sync_task`")
                .contains("CREATE TABLE IF NOT EXISTS `sso_sync_task_item`")
                .contains("CREATE TABLE IF NOT EXISTS `mq_message_log`")
                .contains("`msg_key`")
                .doesNotContain("INSERT IGNORE INTO `sys_menu`")
                .doesNotContain("客户端管理")
                .doesNotContain("同步任务控制台")
                .doesNotContain("activiti");
    }

    /**
     * 验证登录审计 changelog 会补齐 `sys_logininfor` 表，避免本地新库登录后异步日志持续报错。
     *
     * @throws Exception 读取资源失败时抛出
     */
    @Test
    void shouldDeclareLoginAuditBootstrapArtifacts() throws Exception {
        String loginAuditContent = readClasspathResource("db/liquibase/changelog/sso/changelog1.2-login-audit.xml");

        assertThat(loginAuditContent)
                .contains("CREATE TABLE IF NOT EXISTS `sys_logininfor`")
                .contains("`info_id`")
                .contains("`user_name`")
                .contains("`login_time`");
    }

    /**
     * 验证操作审计 changelog 会补齐 `sys_oper_log` 表，避免本地新库触发带 `@Log` 的写接口后异步日志持续报错。
     *
     * @throws Exception 读取资源失败时抛出
     */
    @Test
    void shouldDeclareOperationAuditBootstrapArtifacts() throws Exception {
        String operationAuditContent = readClasspathResource("db/liquibase/changelog/sso/changelog1.3-operation-audit.xml");

        assertThat(operationAuditContent)
                .contains("CREATE TABLE IF NOT EXISTS `sys_oper_log`")
                .contains("`oper_id`")
                .contains("`title`")
                .contains("`oper_time`");
    }

    /**
     * 读取 classpath 资源内容。
     *
     * @param resourcePath 资源相对路径
     * @return 资源文本
     * @throws Exception 读取资源失败时抛出
     */
    private String readClasspathResource(String resourcePath) throws Exception {
        return StreamUtils.copyToString(
                new ClassPathResource(resourcePath).getInputStream(),
                StandardCharsets.UTF_8
        );
    }
}
