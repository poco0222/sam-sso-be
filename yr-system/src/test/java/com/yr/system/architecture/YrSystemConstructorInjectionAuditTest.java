/**
 * @file 审计安全相关 framework Bean 的字段注入残留
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.system.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定 Task 5 需要迁移到构造器注入的关键安全 Bean 清单。
 */
class YrSystemConstructorInjectionAuditTest {

    /** 需要收口字段注入的安全源码文件。 */
    private static final List<Path> SECURITY_BEAN_SOURCES = List.of(
            Path.of("../yr-framework/src/main/java/com/yr/framework/config/SecurityConfig.java"),
            Path.of("../yr-framework/src/main/java/com/yr/framework/security/filter/JwtAuthenticationTokenFilter.java"),
            Path.of("../yr-admin/src/main/java/com/yr/framework/web/service/UserDetailsServiceImpl.java"),
            Path.of("../yr-framework/src/main/java/com/yr/framework/web/service/PermissionService.java")
    );

    /**
     * 验证当前 Task 5 的构造器注入迁移目标清单保持稳定。
     */
    @Test
    void shouldTrackAllPendingConstructorInjectionTargets() {
        assertThat(SECURITY_BEAN_SOURCES)
                .as("Task 5 安全相关字段注入迁移清单应保持稳定")
                .hasSize(4);
    }

    /**
     * 验证安全相关 Bean 不再保留字段级依赖注入。
     *
     * @throws IOException 读取源码失败时抛出
     */
    @Test
    void shouldAvoidFieldInjectionInSecurityRelatedFrameworkBeans() throws IOException {
        for (Path sourcePath : SECURITY_BEAN_SOURCES) {
            String source = Files.readString(sourcePath, StandardCharsets.UTF_8);

            assertThat(source)
                    .as("%s 不应继续保留字段级 @Autowired", sourcePath.getFileName())
                    .doesNotContain("@Autowired\n    private");
            assertThat(source)
                    .as("%s 不应继续保留字段级 @Resource", sourcePath.getFileName())
                    .doesNotContain("@Resource");
        }
    }
}
