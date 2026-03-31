/**
 * @file production profile 安全基线契约测试
 * @author PopoY
 * @date 2026-03-31
 */
package com.yr.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定 `application-prod.yml` 的 production safety baseline（生产安全基线）。
 */
class ProdProfileSafetyContractTest {

    /** 当前 yr-admin module root（模块根目录）。 */
    private static final Path MODULE_ROOT = locateModuleRoot();

    /** production profile 配置文件路径。 */
    private static final Path APPLICATION_PROD_PATH = MODULE_ROOT.resolve("src/main/resources/application-prod.yml");

    /**
     * 验证 prod profile（生产配置）默认关闭 demo 开关。
     *
     * @throws IOException 读取配置失败时抛出
     */
    @Test
    void shouldDisableDemoModeByDefaultInProdProfile() throws IOException {
        PropertySourcesPropertyResolver propertyResolver = loadProdPropertyResolver();

        assertThat(propertyResolver.getProperty("yr.demoEnabled")).isEqualTo("false");
    }

    /**
     * 验证 prod profile 不会开启 devtools（开发热重载）重启开关。
     *
     * @throws IOException 读取配置失败时抛出
     */
    @Test
    void shouldDisableDevtoolsRestartInProdProfile() throws IOException {
        PropertySourcesPropertyResolver propertyResolver = loadProdPropertyResolver();

        assertThat(propertyResolver.getProperty("spring.devtools.restart.enabled")).isEqualTo("false");
    }

    /**
     * 验证生产上传与日志路径不再使用模板默认值，而是完全依赖环境变量注入。
     *
     * @throws IOException 读取配置失败时抛出
     */
    @Test
    void shouldRequireEnvironmentDrivenUploadAndLogPathsInProdProfile() throws IOException {
        PropertySourcesPropertyResolver propertyResolver = loadProdPropertyResolver();

        assertThat(propertyResolver.getProperty("yr.profile")).isBlank();
        assertThat(propertyResolver.getProperty("logging.file.path")).isBlank();
        assertThat(propertyResolver.getProperty("file.constantPath")).isBlank();
    }

    /**
     * 验证 prod profile 不会为 `token.secret` 提供可复用的硬编码默认值，缺失时应继续 fail-fast。
     *
     * @throws IOException 读取配置失败时抛出
     */
    @Test
    void shouldNotProvideFallbackSigningSecretInProdProfile() throws IOException {
        PropertySourcesPropertyResolver propertyResolver = loadProdPropertyResolver();

        assertThat(propertyResolver.getProperty("token.secret")).isBlank();
    }

    /**
     * 加载 `application-prod.yml`，并仅按文件内默认值解析占位符，避免受机器环境变量干扰。
     *
     * @return 基于 prod 配置文件的属性解析器
     * @throws IOException 读取配置失败时抛出
     */
    private static PropertySourcesPropertyResolver loadProdPropertyResolver() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        MutablePropertySources propertySources = new MutablePropertySources();
        List<PropertySource<?>> propertySourceList = loader.load(
                "application-prod",
                new FileSystemResource(APPLICATION_PROD_PATH)
        );

        for (PropertySource<?> propertySource : propertySourceList) {
            propertySources.addLast(propertySource);
        }
        return new PropertySourcesPropertyResolver(propertySources);
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
            Path codeSourcePath = Path.of(ProdProfileSafetyContractTest.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI())
                    .toAbsolutePath()
                    .normalize();

            // 从 test-classes 向上回溯，直到命中包含 pom.xml 的 Maven module root。
            Path currentPath = codeSourcePath;
            while (currentPath != null && !Files.exists(currentPath.resolve("pom.xml"))) {
                currentPath = currentPath.getParent();
            }
            if (currentPath == null) {
                throw new IllegalStateException("无法定位 yr-admin module root（未找到 pom.xml）");
            }
            return currentPath;
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("无法解析测试 CodeSource 路径", ex);
        }
    }
}
