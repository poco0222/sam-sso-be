/**
 * @file 锁定 SSO 后端高风险安全暴露面契约
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定一期 SSO backend（后端）必须收紧的高风险安全暴露面。
 */
class SsoSecuritySurfaceContractTest {

    /** 当前 Maven module root（模块根目录）。 */
    private static final Path MODULE_ROOT = locateModuleRoot();

    /** backend 仓库根目录。 */
    private static final Path REPOSITORY_ROOT = resolveRepositoryRoot();

    /**
     * 验证安全配置源码不再匿名暴露 Swagger、Druid 与通配 CORS。
     *
     * @throws IOException 读取源码失败时抛出
     */
    @Test
    void shouldNotKeepAnonymousSwaggerDruidOrWildcardCorsExposure() throws IOException {
        String securityConfigSource = Files.readString(
                REPOSITORY_ROOT.resolve("yr-framework/src/main/java/com/yr/framework/config/SecurityConfig.java"),
                StandardCharsets.UTF_8
        );
        String resourcesConfigSource = Files.readString(
                REPOSITORY_ROOT.resolve("yr-framework/src/main/java/com/yr/framework/config/ResourcesConfig.java"),
                StandardCharsets.UTF_8
        );

        assertThat(securityConfigSource).doesNotContain(".antMatchers(\"/druid/**\").anonymous()");
        assertThat(securityConfigSource).doesNotContain(".antMatchers(\"/swagger-ui/**\").anonymous()");
        assertThat(securityConfigSource).doesNotContain(".antMatchers(\"/v3/api-docs/**\").anonymous()");
        assertThat(resourcesConfigSource).doesNotContain("addAllowedOriginPattern(\"*\")");
    }

    /**
     * 验证配置文件不再保留弱默认值与可直接使用的生产密钥兜底。
     *
     * @throws IOException 读取配置文件失败时抛出
     */
    @Test
    void shouldNotKeepWeakDefaultSecretsInApplicationProfiles() throws IOException {
        String localYaml = Files.readString(MODULE_ROOT.resolve("src/main/resources/application-local.yml"), StandardCharsets.UTF_8);
        String devYaml = Files.readString(MODULE_ROOT.resolve("src/main/resources/application-dev.yml"), StandardCharsets.UTF_8);
        String prodYaml = Files.readString(MODULE_ROOT.resolve("src/main/resources/application-prod.yml"), StandardCharsets.UTF_8);

        assertThat(localYaml).doesNotContain("login-password: 123456");
        assertThat(localYaml).doesNotContain("password: Popo0222");
        assertThat(localYaml).doesNotContain("FILE_FTP_PASSWORD:1qazxsw2");
        assertThat(devYaml).doesNotContain("login-password: 123456");
        assertThat(devYaml).doesNotContain("TOKEN_SECRET:abcdefghijklmnopqrstuvwxyz");
        assertThat(devYaml).doesNotContain("FILE_FTP_PASSWORD:1qazxsw2");
        assertThat(prodYaml).doesNotContain("login-password: 123456");
        assertThat(prodYaml).doesNotContain("TOKEN_SECRET:abcdefghijklmnopqrstuvwxyz");
        assertThat(prodYaml).doesNotContain("FILE_FTP_PASSWORD:1qazxsw2");
    }

    /**
     * 验证仅供本地开发使用的 Swagger/Test 控制器不会进入默认 runtime surface（运行时暴露面）。
     *
     * @throws IOException 读取源码失败时抛出
     */
    @Test
    void shouldRestrictToolControllersToLocalProfile() throws IOException {
        String swaggerControllerSource = Files.readString(
                MODULE_ROOT.resolve("src/main/java/com/yr/web/controller/tool/SwaggerController.java"),
                StandardCharsets.UTF_8
        );
        Path testControllerPath = MODULE_ROOT.resolve("src/main/java/com/yr/web/controller/tool/TestController.java");

        assertThat(swaggerControllerSource).contains("@Profile(\"local\")");
        if (Files.exists(testControllerPath)) {
            String testControllerSource = Files.readString(testControllerPath, StandardCharsets.UTF_8);
            assertThat(testControllerSource).contains("@Profile(\"local\")");
        }
    }

    /**
     * 定位当前测试所属的 yr-admin module root（模块根目录）。
     *
     * @return module root 绝对路径
     */
    private static Path locateModuleRoot() {
        String basedir = System.getProperty("basedir");
        if (basedir != null && !basedir.isBlank()) {
            Path basedirPath = Path.of(basedir).toAbsolutePath().normalize();
            if (Files.exists(basedirPath.resolve("pom.xml"))) {
                return basedirPath;
            }
        }

        try {
            Path codeSourcePath = Path.of(SsoSecuritySurfaceContractTest.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI())
                    .toAbsolutePath()
                    .normalize();

            // 从 test-classes 向上回溯，直到定位到包含 pom.xml 的 Maven module root。
            Path current = codeSourcePath;
            while (current != null && !Files.exists(current.resolve("pom.xml"))) {
                current = current.getParent();
            }
            if (current == null) {
                throw new IllegalStateException("无法定位 yr-admin module root（未找到 pom.xml）");
            }
            return current;
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("无法解析测试 CodeSource 路径", ex);
        }
    }

    /**
     * 根据 module root 推断 backend 仓库根目录。
     *
     * @return backend 仓库根目录
     */
    private static Path resolveRepositoryRoot() {
        Path repositoryRoot = MODULE_ROOT.getParent();
        if (repositoryRoot == null) {
            throw new IllegalStateException("无法根据 yr-admin module root 推断 backend 仓库根目录");
        }
        return repositoryRoot.normalize();
    }
}
