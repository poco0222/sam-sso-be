/**
 * @file 身份中心 Liquibase 启动契约测试
 * @author PopoY
 * @date 2026-03-24
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
        assertThat(masterContent).doesNotContain("includeAll");
        assertThat(masterContent).doesNotContain("changelog/system/changelog1.0.xml");
    }

    /**
     * 验证 core-console changelog 只保留一期核心表族，并排除 legacy system 表。
     *
     * @throws Exception 读取资源失败时抛出
     */
    @Test
    void shouldKeepOnlyPhaseOneCoreTablesInCoreConsoleChangelog() throws Exception {
        String coreConsoleContent = readClasspathResource("db/liquibase/changelog/sso/changelog1.0-core-console.xml");

        assertThat(coreConsoleContent)
                .contains("CREATE TABLE IF NOT EXISTS `sys_user`")
                .contains("CREATE TABLE IF NOT EXISTS `sys_org`")
                .contains("CREATE TABLE IF NOT EXISTS `sys_dept`")
                .contains("CREATE TABLE IF NOT EXISTS `sys_user_org`")
                .contains("CREATE TABLE IF NOT EXISTS `sys_user_dept`")
                .contains("CREATE TABLE IF NOT EXISTS `sys_role`")
                .contains("CREATE TABLE IF NOT EXISTS `sys_menu`")
                .contains("CREATE TABLE IF NOT EXISTS `sys_role_menu`")
                .contains("CREATE TABLE IF NOT EXISTS `sys_user_role`")
                .contains("CREATE TABLE IF NOT EXISTS `sys_config`")
                .contains("身份管理")
                .doesNotContain("sys_duty")
                .doesNotContain("sys_rank")
                .doesNotContain("sys_attach_")
                .doesNotContain("sys_file")
                .doesNotContain("sys_message_")
                .doesNotContain("sys_receive_group")
                .doesNotContain("sys_region");
    }

    /**
     * 验证 client-sync changelog 声明了一期新增表与对应控制台菜单。
     *
     * @throws Exception 读取资源失败时抛出
     */
    @Test
    void shouldDeclareClientAndSyncBootstrapArtifacts() throws Exception {
        String clientSyncContent = readClasspathResource("db/liquibase/changelog/sso/changelog1.1-client-sync.xml");

        assertThat(clientSyncContent)
                .contains("CREATE TABLE IF NOT EXISTS `sso_client`")
                .contains("CREATE TABLE IF NOT EXISTS `sso_sync_task`")
                .contains("CREATE TABLE IF NOT EXISTS `sso_sync_task_item`")
                .contains("客户端管理")
                .contains("同步任务控制台")
                .doesNotContain("activiti");
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
