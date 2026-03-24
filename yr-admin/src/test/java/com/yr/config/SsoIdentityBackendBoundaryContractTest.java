/**
 * @file 身份中心后端边界契约测试
 * @author PopoY
 * @date 2026-03-24
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
 * 锁定身份中心一期后端边界，确保 Activiti 模块与运行时配置被完整裁掉。
 */
class SsoIdentityBackendBoundaryContractTest {

    /** 当前 Maven module root（模块根目录），用于稳定解析 yr-admin 内部相对路径。 */
    private static final Path MODULE_ROOT = locateModuleRoot();

    /** 当前 backend 仓库根目录，用于访问聚合 pom 与旧模块目录。 */
    private static final Path REPOSITORY_ROOT = resolveRepositoryRoot();

    /**
     * 验证身份中心一期后端不再保留 Activiti 聚合模块、入口依赖、运行时配置与模块目录。
     *
     * @throws IOException 读取文件失败时抛出
     */
    @Test
    void shouldNotKeepActivitiModuleOrRuntimeConfig() throws IOException {
        assertFileDoesNotContain(REPOSITORY_ROOT.resolve("pom.xml"), "<module>yr-activiti7</module>");
        assertFileDoesNotContain(REPOSITORY_ROOT.resolve("pom.xml"), "<artifactId>yr-activiti7</artifactId>");
        assertFileDoesNotContain(MODULE_ROOT.resolve("pom.xml"), "<artifactId>yr-activiti7</artifactId>");
        assertFileDoesNotContain(MODULE_ROOT.resolve("src/main/resources/application-local.yml"), "activiti:");
        assertFileDoesNotContain(MODULE_ROOT.resolve("src/main/resources/application-dev.yml"), "activiti:");
        assertFileDoesNotContain(MODULE_ROOT.resolve("src/main/resources/application-prod.yml"), "activiti:");
        assertFileDoesNotContain(MODULE_ROOT.resolve("src/main/resources/logback-spring.xml"), "org.activiti");
        assertPathDoesNotExist(REPOSITORY_ROOT.resolve("yr-activiti7"));
    }

    /**
     * 断言给定文件不包含目标文本。
     *
     * @param filePath 要检查的文件路径
     * @param forbiddenText 禁止保留的文本
     * @throws IOException 读取文件失败时抛出
     */
    private void assertFileDoesNotContain(Path filePath, String forbiddenText) throws IOException {
        String sourceText = Files.readString(filePath, StandardCharsets.UTF_8);

        assertThat(sourceText)
                .as("%s 不应再包含 %s", filePath, forbiddenText)
                .doesNotContain(forbiddenText);
    }

    /**
     * 断言给定路径已经被移除。
     *
     * @param targetPath 目标路径
     */
    private void assertPathDoesNotExist(Path targetPath) {
        assertThat(Files.exists(targetPath))
                .as("%s 应当已经被移除", targetPath)
                .isFalse();
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
            Path codeSourcePath = Path.of(SsoIdentityBackendBoundaryContractTest.class
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
