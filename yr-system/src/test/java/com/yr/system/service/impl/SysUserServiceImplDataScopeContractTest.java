/**
 * @file 验证 SysUserServiceImpl 用户列表与角色分配查询必须声明数据范围过滤契约
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.annotation.DataScope;
import com.yr.common.core.domain.entity.SysUser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SysUserServiceImpl 数据范围契约测试。
 */
class SysUserServiceImplDataScopeContractTest {

    /**
     * 验证用户列表查询方法保留部门与用户两个维度的数据范围注解。
     *
     * @throws NoSuchMethodException 当目标方法签名变化时抛出
     */
    @Test
    void shouldDeclareDataScopeOnSelectUserList() throws NoSuchMethodException {
        Method method = SysUserServiceImpl.class.getMethod("selectUserList", SysUser.class);
        DataScope dataScope = method.getAnnotation(DataScope.class);

        assertThat(dataScope).isNotNull();
        assertThat(dataScope.deptAlias()).isEqualTo("d");
        assertThat(dataScope.userAlias()).isEqualTo("u");
    }

    /**
     * 验证角色用户已分配/未分配查询同样保留 DataScope 注解，避免直接绕过权限切面。
     *
     * @throws NoSuchMethodException 当目标方法签名变化时抛出
     */
    @Test
    void shouldDeclareDataScopeOnRoleAllocationQueries() throws NoSuchMethodException {
        assertDataScopeAliases(SysUserServiceImpl.class.getMethod("selectAllocatedList", SysUser.class));
        assertDataScopeAliases(SysUserServiceImpl.class.getMethod("selectUnallocatedList", SysUser.class));
    }

    /**
     * 验证角色用户已分配/未分配 SQL 都显式接入 `${params.dataScope}`，防止注解存在但 SQL 未消费。
     *
     * @throws IOException 读取 mapper 失败
     */
    @Test
    void shouldConsumeDataScopePlaceholderInRoleAllocationMappers() throws IOException {
        assertSelectStatementContainsIgnoringCase("SysUserMapper.xml", "selectAllocatedList", "${params.dataScope}");
        assertSelectStatementContainsIgnoringCase("SysUserMapper.xml", "selectUnallocatedList", "${params.dataScope}");
    }

    /**
     * 断言给定方法上的 DataScope 别名保持一致。
     *
     * @param method 待断言的方法
     */
    private void assertDataScopeAliases(Method method) {
        DataScope dataScope = method.getAnnotation(DataScope.class);

        assertThat(dataScope)
                .as("%s 应声明 DataScope 注解", method.getName())
                .isNotNull();
        assertThat(dataScope.deptAlias()).isEqualTo("d");
        assertThat(dataScope.userAlias()).isEqualTo("u");
    }

    /**
     * 断言指定 select statement 包含大小写不敏感的目标文本。
     *
     * @param mapperFileName mapper 文件名
     * @param statementId select ID
     * @param expectedText 期待文本
     * @throws IOException 读取 mapper 失败
     */
    private void assertSelectStatementContainsIgnoringCase(String mapperFileName,
                                                           String statementId,
                                                           String expectedText) throws IOException {
        String statementBody = normalizeWhitespace(loadSelectStatementBody(mapperFileName, statementId)).toLowerCase(Locale.ROOT);
        String normalizedExpected = normalizeWhitespace(expectedText).toLowerCase(Locale.ROOT);

        assertThat(statementBody)
                .as("%s#%s 应包含 %s", mapperFileName, statementId, expectedText)
                .contains(normalizedExpected);
    }

    /**
     * 读取指定 mapper 中某个 select statement 的原始 XML。
     *
     * @param mapperFileName mapper 文件名
     * @param statementId select 语句 ID
     * @return 原始 XML 片段
     * @throws IOException 读取 mapper 失败
     */
    private String loadSelectStatementBody(String mapperFileName, String statementId) throws IOException {
        String mapperContent = loadMapperFromClasspath(mapperFileName);
        Matcher matcher = Pattern.compile(
                "<select\\s+id=\"" + Pattern.quote(statementId) + "\"[^>]*>(.*?)</select>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        ).matcher(mapperContent);
        if (!matcher.find()) {
            throw new IllegalStateException(mapperFileName + " 中不存在 select#" + statementId);
        }
        return matcher.group(1);
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
