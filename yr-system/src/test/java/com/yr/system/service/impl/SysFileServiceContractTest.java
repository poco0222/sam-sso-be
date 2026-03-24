/**
 * @file 验证文件服务对外契约不再暴露 Web transport 类型
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SysFileService 契约测试。
 */
class SysFileServiceContractTest {

    /** yr-system 模块根目录，用于稳定读取源码。 */
    private static final Path MODULE_ROOT = locateModuleRoot();

    /**
     * 验证服务接口不再直接暴露 MultipartFile / HttpServletResponse，并改用领域命令与下载 payload。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldExposeTransportAgnosticFileServiceContract() throws IOException {
        String interfaceSource = Files.readString(
                MODULE_ROOT.resolve("src/main/java/com/yr/system/service/ISysFileService.java"),
                StandardCharsets.UTF_8
        );

        assertThat(interfaceSource).doesNotContain("MultipartFile");
        assertThat(interfaceSource).doesNotContain("HttpServletResponse");
        assertThat(interfaceSource).contains("SysFileUploadCommand");
        assertThat(interfaceSource).contains("SysFileDownloadPayload");
    }

    /**
     * 基于测试类的 CodeSource 回溯定位 Maven 模块根目录。
     *
     * @return 模块根目录
     */
    private static Path locateModuleRoot() {
        try {
            Path codeSourcePath = Paths.get(SysFileServiceContractTest.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            Path currentPath = Files.isDirectory(codeSourcePath) ? codeSourcePath : codeSourcePath.getParent();
            while (currentPath != null) {
                if (Files.exists(currentPath.resolve("pom.xml"))) {
                    return currentPath;
                }
                currentPath = currentPath.getParent();
            }
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("无法定位 yr-system 模块根目录", exception);
        }
        throw new IllegalStateException("无法定位 yr-system 模块根目录");
    }
}
