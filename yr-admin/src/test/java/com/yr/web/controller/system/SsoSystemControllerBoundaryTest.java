/**
 * @file 一期后台系统控制器边界契约测试
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.web.controller.system;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定一期后台 system controller keep-list，避免非一期接口继续暴露。
 */
class SsoSystemControllerBoundaryTest {

    /** yr-admin module root（模块根目录），用于稳定解析源码目录。 */
    private static final Path MODULE_ROOT = locateModuleRoot();

    /** system controller 目录。 */
    private static final Path SYSTEM_CONTROLLER_DIR = MODULE_ROOT.resolve("src/main/java/com/yr/web/controller/system");

    /** 一期必须保留的 controller 文件名集合。 */
    private static final Set<String> REQUIRED_CONTROLLERS = Set.of(
            "SysLoginController.java",
            "SysProfileController.java",
            "SysOrgController.java",
            "SysDeptController.java",
            "SysUserController.java",
            "SysUserOrgController.java",
            "SysUserDeptController.java",
            "SysRoleController.java",
            "SysMenuController.java",
            "SysConfigController.java"
    );

    /** 一期明确要求退出的 legacy controller 文件名集合。 */
    private static final Set<String> FORBIDDEN_CONTROLLERS = Set.of(
            "SysAttachCategoryController.java",
            "SysAttachmentController.java",
            "SysCodeRuleController.java",
            "SysCodeRuleDetailController.java",
            "SysCodeRuleLineController.java",
            "SysCodeRuleValueController.java",
            "SysDictDataController.java",
            "SysDictTypeController.java",
            "SysDutyController.java",
            "SysFileController.java",
            "SysLovConfigController.java",
            "SysLovFieldController.java",
            "SysLovTypeController.java",
            "SysMessageController.java",
            "SysMobileLoginController.java",
            "SysMobileUserCOntroller.java",
            "SysMsgTemplateController.java",
            "SysNoticeController.java",
            "SysPostController.java",
            "SysRankController.java",
            "SysReceiveGroupController.java",
            "SysRegionController.java",
            "SysUserDutyController.java",
            "SysUserRankController.java"
    );

    /** 使用前缀拦截整类 controller 家族回流。 */
    private static final List<String> FORBIDDEN_PREFIXES = List.of("SysCodeRule", "SysDict", "SysLov");

    /**
     * 验证一期 keep-list controller 仍然全部存在，避免在裁撤过程中误删控制台核心入口。
     *
     * @throws IOException 读取目录失败时抛出
     */
    @Test
    void shouldKeepPhase1SystemControllers() throws IOException {
        Set<String> actualControllerNames = listSystemControllerFileNames();

        assertThat(actualControllerNames)
                .as("一期 system controller keep-list 不应缺失")
                .containsAll(REQUIRED_CONTROLLERS);
    }

    /**
     * 验证计划明确排除的 legacy controller 已经退出 system API surface。
     *
     * @throws IOException 读取目录失败时抛出
     */
    @Test
    void shouldNotKeepLegacySystemControllersOutsidePhase1Scope() throws IOException {
        Set<String> actualControllerNames = listSystemControllerFileNames();

        assertThat(actualControllerNames)
                .as("非一期 system controller 不应继续留在 system API surface")
                .doesNotContainAnyElementsOf(FORBIDDEN_CONTROLLERS);

        // 用前缀额外兜底，避免同家族 controller 改名后绕过明确名单。
        assertThat(actualControllerNames.stream().noneMatch(this::matchesForbiddenPrefix))
                .as("SysCodeRule* / SysDict* / SysLov* controller 不应继续暴露")
                .isTrue();
    }

    /**
     * 列出 system controller 目录下的所有 Java 文件名。
     *
     * @return controller 文件名集合
     * @throws IOException 读取目录失败时抛出
     */
    private Set<String> listSystemControllerFileNames() throws IOException {
        try (Stream<Path> pathStream = Files.list(SYSTEM_CONTROLLER_DIR)) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(fileName -> fileName.endsWith(".java"))
                    .collect(Collectors.toSet());
        }
    }

    /**
     * 判断文件名是否命中禁止前缀集合。
     *
     * @param fileName controller 文件名
     * @return 如果命中禁止前缀则返回 true
     */
    private boolean matchesForbiddenPrefix(String fileName) {
        return FORBIDDEN_PREFIXES.stream().anyMatch(fileName::startsWith);
    }

    /**
     * 定位当前测试所属的 yr-admin module root。
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
            Path codeSourcePath = Path.of(SsoSystemControllerBoundaryTest.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI())
                    .toAbsolutePath()
                    .normalize();

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
}
