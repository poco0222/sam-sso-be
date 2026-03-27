/**
 * @file framework 模块边界与依赖基线契约测试
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.framework.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定 Task 6 对 framework/common 模块边界和生产依赖基线的约束。
 */
class YrFrameworkBuildBoundaryContractTest {

    /** framework 模块 POM。 */
    private static final Path FRAMEWORK_POM_PATH = Path.of("pom.xml");

    /** common 模块 POM。 */
    private static final Path COMMON_POM_PATH = Path.of("../yr-common/pom.xml");

    /** 根 POM。 */
    private static final Path ROOT_POM_PATH = Path.of("../pom.xml");

    /** framework 源码根目录。 */
    private static final Path FRAMEWORK_SOURCE_ROOT = Path.of("src/main/java");

    /**
     * 验证 framework 模块不再直接依赖 yr-system，也不再直接 import 业务系统包。
     *
     * @throws IOException 读取文件失败时抛出
     */
    @Test
    void shouldNotDependOnYrSystemModuleOrImportBusinessPackages() throws IOException {
        String frameworkPom = Files.readString(FRAMEWORK_POM_PATH, StandardCharsets.UTF_8);

        assertThat(frameworkPom).doesNotContain("<artifactId>yr-system</artifactId>");
        try (Stream<Path> sourceFiles = Files.walk(FRAMEWORK_SOURCE_ROOT)) {
            List<Path> javaFiles = sourceFiles
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();

            for (Path javaFile : javaFiles) {
                String source = Files.readString(javaFile, StandardCharsets.UTF_8);
                assertThat(source)
                        .as("%s 不应继续 import com.yr.system.*", javaFile)
                        .doesNotContain("import com.yr.system.");
            }
        }
    }

    /**
     * 验证 yr-common 不再直接声明安全、Redis、MyBatis-Plus、RocketMQ 等重依赖。
     *
     * @throws IOException 读取文件失败时抛出
     */
    @Test
    void shouldKeepYrCommonFreeFromHeavyInfraDependencies() throws IOException {
        String commonPom = Files.readString(COMMON_POM_PATH, StandardCharsets.UTF_8);

        assertThat(commonPom).doesNotContain("<artifactId>spring-boot-starter-security</artifactId>");
        assertThat(commonPom).doesNotContain("<artifactId>spring-boot-starter-data-redis</artifactId>");
        assertThat(commonPom).doesNotContain("<artifactId>mybatis-plus-boot-starter</artifactId>");
        assertThat(commonPom).doesNotContain("<artifactId>rocketmq-spring-boot-starter</artifactId>");
    }

    /**
     * 验证旧版 JSON/JWT/HTTP 依赖不再作为生产依赖基线保留。
     *
     * @throws IOException 读取文件失败时抛出
     */
    @Test
    void shouldNotRetainLegacySerializationJwtAndHttpDependencies() throws IOException {
        String commonPom = Files.readString(COMMON_POM_PATH, StandardCharsets.UTF_8);
        String rootPom = Files.readString(ROOT_POM_PATH, StandardCharsets.UTF_8);

        assertThat(commonPom).doesNotContain("<artifactId>fastjson</artifactId>");
        assertThat(commonPom).doesNotContain("<artifactId>jjwt</artifactId>");
        assertThat(commonPom).doesNotContain("<artifactId>okhttp</artifactId>");
        assertThat(rootPom).doesNotContain("<fastjson.version>");
        assertThat(rootPom).doesNotContain("<jwt.version>0.9.1</jwt.version>");
    }
}
