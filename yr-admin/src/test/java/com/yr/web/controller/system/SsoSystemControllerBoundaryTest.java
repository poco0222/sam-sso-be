/**
 * @file 一期后台系统控制器边界契约测试
 * @author PopoY
 * @date 2026-03-26
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

    /** controller 根目录。 */
    private static final Path CONTROLLER_ROOT = MODULE_ROOT.resolve("src/main/java/com/yr/web/controller");

    /** system controller 目录。 */
    private static final Path SYSTEM_CONTROLLER_DIR = CONTROLLER_ROOT.resolve("system");

    /** sso controller 目录。 */
    private static final Path SSO_CONTROLLER_DIR = CONTROLLER_ROOT.resolve("sso");

    /** monitor controller 目录。 */
    private static final Path MONITOR_CONTROLLER_DIR = CONTROLLER_ROOT.resolve("monitor");

    /** 一期必须保留的 system controller 文件名集合。 */
    private static final Set<String> REQUIRED_SYSTEM_CONTROLLERS = Set.of(
            "SysLoginController.java",
            "SysProfileController.java",
            "SysOrgController.java",
            "SysDeptController.java",
            "SysUserController.java",
            "SysUserOrgController.java",
            "SysUserDeptController.java"
    );

    /** 一期必须保留的 sso controller 文件名集合。 */
    private static final Set<String> REQUIRED_SSO_CONTROLLERS = Set.of(
            "SsoClientController.java",
            "SsoSyncTaskController.java"
    );

    /** 一期必须保留的 monitor controller 文件名集合。 */
    private static final Set<String> REQUIRED_MONITOR_CONTROLLERS = Set.of(
            "SysLogininforController.java"
    );

    /** 一期明确要求退出的 legacy system controller 文件名集合。 */
    private static final Set<String> FORBIDDEN_SYSTEM_CONTROLLERS = Set.of(
            "SysAttachCategoryController.java",
            "SysAttachmentController.java",
            "SysCodeRuleController.java",
            "SysCodeRuleDetailController.java",
            "SysCodeRuleLineController.java",
            "SysCodeRuleValueController.java",
            "SysConfigController.java",
            "SysDictDataController.java",
            "SysDictTypeController.java",
            "SysDutyController.java",
            "SysFileController.java",
            "SysIndexController.java",
            "SysLovConfigController.java",
            "SysLovFieldController.java",
            "SysLovTypeController.java",
            "SysMenuController.java",
            "SysMessageController.java",
            "SysMobileLoginController.java",
            "SysMobileUserCOntroller.java",
            "SysMsgTemplateController.java",
            "SysNoticeController.java",
            "SysPostController.java",
            "SysRankController.java",
            "SysReceiveGroupController.java",
            "SysRegionController.java",
            "SysRoleController.java",
            "SysUserDutyController.java",
            "SysUserRankController.java"
    );

    /** 一期明确要求退出的 legacy monitor controller 文件名集合。 */
    private static final Set<String> FORBIDDEN_MONITOR_CONTROLLERS = Set.of(
            "CacheController.java",
            "ServerController.java",
            "SysOperlogController.java",
            "SysUserOnlineController.java"
    );

    /** 使用前缀拦截整类 controller 家族回流。 */
    private static final List<String> FORBIDDEN_PREFIXES = List.of("SysCodeRule", "SysDict", "SysLov");

    /**
     * 验证一期 keep-list controller 仍然全部存在，避免在裁撤过程中误删控制台核心入口。
     *
     * @throws IOException 读取目录失败时抛出
     */
    @Test
    void shouldKeepPhase1ControllerFamilies() throws IOException {
        Set<String> actualSystemControllerNames = listControllerFileNames(SYSTEM_CONTROLLER_DIR);
        Set<String> actualSsoControllerNames = listControllerFileNames(SSO_CONTROLLER_DIR);
        Set<String> actualMonitorControllerNames = listControllerFileNames(MONITOR_CONTROLLER_DIR);

        assertThat(actualSystemControllerNames)
                .as("一期 system controller keep-list 不应缺失，也不应继续保留超边界入口")
                .containsExactlyInAnyOrderElementsOf(REQUIRED_SYSTEM_CONTROLLERS);
        assertThat(actualSsoControllerNames)
                .as("一期 sso controller keep-list 不应缺失客户端与同步任务控制台入口")
                .containsExactlyInAnyOrderElementsOf(REQUIRED_SSO_CONTROLLERS);
        assertThat(actualMonitorControllerNames)
                .as("一期 monitor controller 仅允许保留登录审计入口")
                .containsExactlyInAnyOrderElementsOf(REQUIRED_MONITOR_CONTROLLERS);
    }

    /**
     * 验证计划明确排除的 legacy controller 已经退出 system API surface。
     *
     * @throws IOException 读取目录失败时抛出
     */
    @Test
    void shouldNotKeepLegacySystemControllersOutsidePhase1Scope() throws IOException {
        Set<String> actualSystemControllerNames = listControllerFileNames(SYSTEM_CONTROLLER_DIR);
        Set<String> actualMonitorControllerNames = listControllerFileNames(MONITOR_CONTROLLER_DIR);

        assertThat(actualSystemControllerNames)
                .as("非一期 system controller 不应继续留在 system API surface")
                .doesNotContainAnyElementsOf(FORBIDDEN_SYSTEM_CONTROLLERS);

        assertThat(actualMonitorControllerNames)
                .as("非一期 monitor controller 不应继续留在身份中心一期边界内")
                .doesNotContainAnyElementsOf(FORBIDDEN_MONITOR_CONTROLLERS);

        // 用前缀额外兜底，避免同家族 controller 改名后绕过明确名单。
        assertThat(actualSystemControllerNames.stream().noneMatch(this::matchesForbiddenPrefix))
                .as("SysCodeRule* / SysDict* / SysLov* controller 不应继续暴露")
                .isTrue();
    }

    /**
     * 验证一期保留 controller 不再继续暴露基于角色的旧筛选入口。
     *
     * @throws IOException 读取源码失败时抛出
     */
    @Test
    void shouldNotExposeLegacyRoleDrivenEndpointsInsideRetainedControllers() throws IOException {
        String deptControllerSource = Files.readString(SYSTEM_CONTROLLER_DIR.resolve("SysDeptController.java"));
        String userControllerSource = Files.readString(SYSTEM_CONTROLLER_DIR.resolve("SysUserController.java"));

        assertThat(deptControllerSource)
                .as("SysDeptController 不应继续暴露角色部门树旧入口")
                .doesNotContain("deptRoletreeselect")
                .doesNotContain("roleDeptTreeselect");

        assertThat(userControllerSource)
                .as("SysUserController 不应继续暴露基于角色的用户筛选旧入口")
                .doesNotContain("selectUserListByDeptRole");
    }

    /**
     * 列出 system controller 目录下的所有 Java 文件名。
     *
     * @return controller 文件名集合
     * @throws IOException 读取目录失败时抛出
     */
    private Set<String> listControllerFileNames(Path controllerDirectory) throws IOException {
        try (Stream<Path> pathStream = Files.list(controllerDirectory)) {
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
