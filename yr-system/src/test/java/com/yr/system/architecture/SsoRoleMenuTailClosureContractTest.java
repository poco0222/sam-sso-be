/**
 * @file 一期 role/menu 尾链收口契约测试
 * @author PopoY
 * @date 2026-03-26
 */
package com.yr.system.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定一期后端 role/menu 尾链收口边界，防止历史字段与接口回流。
 */
class SsoRoleMenuTailClosureContractTest {

    /** 当前 yr-system module root（模块根目录）。 */
    private static final Path MODULE_ROOT = locateModuleRoot();

    /** backend 仓库根目录。 */
    private static final Path REPOSITORY_ROOT = resolveRepositoryRoot();

    /**
     * 验证 SysUser 不再暴露一期已裁剪的角色与职级相关字段。
     *
     * @throws IOException 读取源码失败时抛出
     */
    @Test
    void shouldNotExposeLegacyRoleAndRankFieldsOnSysUser() throws IOException {
        Path sysUserPath = REPOSITORY_ROOT.resolve(
                "yr-common/src/main/java/com/yr/common/core/domain/entity/SysUser.java"
        );

        assertSourceDoesNotContain(sysUserPath, "private List<SysRole> roles;");
        assertSourceDoesNotContain(sysUserPath, "private Long[] roleIds;");
        assertSourceDoesNotContain(sysUserPath, "private Long[] postIds;");
        assertSourceDoesNotContain(sysUserPath, "private Long roleId;");
        assertSourceDoesNotContain(sysUserPath, "private Long rankId;");
        assertSourceDoesNotContain(sysUserPath, "private String rankName;");
    }

    /**
     * 验证 TreeSelect 不再保留 SysMenu 依赖构造器。
     *
     * @throws IOException 读取源码失败时抛出
     */
    @Test
    void shouldNotDependOnSysMenuInTreeSelect() throws IOException {
        Path treeSelectPath = REPOSITORY_ROOT.resolve(
                "yr-common/src/main/java/com/yr/common/core/domain/TreeSelect.java"
        );
        Path sysMenuPath = REPOSITORY_ROOT.resolve(
                "yr-common/src/main/java/com/yr/common/core/domain/entity/SysMenu.java"
        );

        assertSourceDoesNotContain(treeSelectPath, "import com.yr.common.core.domain.entity.SysMenu;");
        assertSourceDoesNotContain(treeSelectPath, "TreeSelect(SysMenu menu)");
        assertSourceDoesNotContain(treeSelectPath, "private Long roleId;");
        assertPathDoesNotExist(sysMenuPath);
    }

    /**
     * 验证 PermissionService 不再依赖 SysRole 或 getRoles 进行角色判断。
     *
     * @throws IOException 读取源码失败时抛出
     */
    @Test
    void shouldNotDependOnSysRoleTraversalInPermissionService() throws IOException {
        Path permissionServicePath = REPOSITORY_ROOT.resolve(
                "yr-framework/src/main/java/com/yr/framework/web/service/PermissionService.java"
        );
        Path sysRolePath = REPOSITORY_ROOT.resolve(
                "yr-common/src/main/java/com/yr/common/core/domain/entity/SysRole.java"
        );

        assertSourceDoesNotContain(permissionServicePath, "import com.yr.common.core.domain.entity.SysRole;");
        assertSourceDoesNotContain(permissionServicePath, "getRoles()");
        assertSourceDoesNotContain(permissionServicePath, "for (SysRole");
        assertPathDoesNotExist(sysRolePath);
    }

    /**
     * 验证用户服务层不再保留基于 roleIds 的用户角色分配入口。
     *
     * @throws IOException 读取源码失败时抛出
     */
    @Test
    void shouldNotExposeInsertUserAuthRoleIdsEntry() throws IOException {
        Path userServiceInterfacePath = REPOSITORY_ROOT.resolve(
                "yr-system/src/main/java/com/yr/system/service/ISysUserService.java"
        );
        Path userServiceImplPath = REPOSITORY_ROOT.resolve(
                "yr-system/src/main/java/com/yr/system/service/impl/SysUserServiceImpl.java"
        );

        assertSourceDoesNotContain(userServiceInterfacePath, "insertUserAuth(Long userId, Long[] roleIds)");
        assertSourceDoesNotContain(userServiceImplPath, "insertUserAuth(Long userId, Long[] roleIds)");
    }

    /**
     * 断言源码文件不包含指定文本。
     *
     * @param filePath 文件路径
     * @param unexpectedText 不应出现的文本
     * @throws IOException 读取源码失败时抛出
     */
    private void assertSourceDoesNotContain(Path filePath, String unexpectedText) throws IOException {
        String source = Files.readString(filePath, StandardCharsets.UTF_8);

        assertThat(source)
                .as("%s 不应包含 %s", filePath, unexpectedText)
                .doesNotContain(unexpectedText);
    }

    /**
     * 断言指定路径已经删除。
     *
     * @param filePath 目标路径
     */
    private void assertPathDoesNotExist(Path filePath) {
        assertThat(Files.exists(filePath))
                .as("%s 应在一期尾单收口后删除", filePath)
                .isFalse();
    }

    /**
     * 定位当前测试所属的 yr-system module root。
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
            Path codeSourcePath = Path.of(SsoRoleMenuTailClosureContractTest.class
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
                throw new IllegalStateException("无法定位 yr-system module root（未找到 pom.xml）");
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
            throw new IllegalStateException("无法根据 yr-system module root 推断 backend 仓库根目录");
        }
        return repositoryRoot.normalize();
    }
}
