/**
 * @file 锁定 SysUserController#getInfo 的一期响应边界
 * @author PopoY
 * @date 2026-03-26
 */
package com.yr.web.controller.system;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SysUserController#getInfo 一期边界契约测试。
 */
class SysUserControllerInfoContractTest {

    /** SysUserController 源码路径。 */
    private static final Path SOURCE_PATH = Path.of("src/main/java/com/yr/web/controller/system/SysUserController.java");

    /**
     * 验证一期用户详情接口不再回传 roles/posts/postIds/roleIds。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldNotExposeLegacyRoleAndPostPayloadsInGetInfo() throws IOException {
        String source = Files.readString(SOURCE_PATH);

        assertThat(source).doesNotContain("ajax.put(\"roles\"");
        assertThat(source).doesNotContain("ajax.put(\"posts\"");
        assertThat(source).doesNotContain("ajax.put(\"postIds\"");
        assertThat(source).doesNotContain("ajax.put(\"roleIds\"");
    }

    /**
     * 验证控制器不再注入岗位与角色服务作为一期用户详情来源。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldNotInjectRoleAndPostServicesForPhaseOneUserInfo() throws IOException {
        String source = Files.readString(SOURCE_PATH);

        assertThat(source).doesNotContain("ISysRoleService");
        assertThat(source).doesNotContain("ISysPostService");
    }
}
