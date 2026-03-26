/**
 * @file 验证 SysUserServiceImpl 一期边界内不再保留角色筛选与 DataScope 旧契约
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysUser;
import com.yr.system.service.ISysUserService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SysUserServiceImpl 一期边界契约测试。
 */
class SysUserServiceImplDataScopeContractTest {

    /**
     * 验证一期用户列表查询不再依赖角色数据权限切面。
     *
     * @throws NoSuchMethodException 当目标方法签名变化时抛出
     */
    @Test
    void shouldNotDeclareDataScopeOnSelectUserList() throws NoSuchMethodException {
        Method method = SysUserServiceImpl.class.getMethod("selectUserList", SysUser.class);
        assertThat(method.getAnnotationsByType(com.yr.common.annotation.DataScope.class))
                .as("selectUserList 不应继续依赖 DataScope 注解")
                .isEmpty();
    }

    /**
     * 验证一期用户服务契约不再继续暴露基于角色的用户筛选接口。
     */
    @Test
    void shouldNotKeepLegacyRoleDrivenUserQueryContracts() {
        assertThat(Arrays.stream(ISysUserService.class.getMethods()).map(Method::getName).toList())
                .as("ISysUserService 不应继续暴露基于角色的用户筛选方法")
                .doesNotContain("selectUserListByDeptRole");
    }

    /**
     * 验证一期用户查询 SQL 不再继续依赖角色分配与用户角色关系表。
     *
     * @throws IOException 读取 mapper 失败
     */
    @Test
    void shouldNotKeepLegacyRoleDrivenUserQueryMappers() throws IOException {
        String mapperContent = normalizeWhitespace(loadMapperFromClasspath("SysUserMapper.xml")).toLowerCase(Locale.ROOT);

        assertThat(mapperContent)
                .as("SysUserMapper.xml 不应继续保留角色分配、角色筛选与职级联表 SQL")
                .doesNotContain("select id=\"selectallocatedlist\"")
                .doesNotContain("select id=\"selectunallocatedlist\"")
                .doesNotContain("select id=\"selectuserlistbydeptrole\"")
                .doesNotContain("select id=\"selectuserrolelistbyrolekeysbatch\"")
                .doesNotContain("sys_user_rank")
                .doesNotContain("sys_rank")
                .doesNotContain("sys_user_role");
    }

    /**
     * 从 classpath 读取 mapper 源文件。
     *
     * @param mapperFileName mapper 文件名
     * @return mapper 文本
     * @throws IOException 读取 mapper 失败
     */
    private String loadMapperFromClasspath(String mapperFileName) throws IOException {
        try (InputStream stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("mapper/system/" + mapperFileName),
                "classpath mapper/system/" + mapperFileName + " 不存在")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 归一化空白，降低缩进差异对断言的影响。
     *
     * @param text 原始文本
     * @return 归一化文本
     */
    private String normalizeWhitespace(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
}
