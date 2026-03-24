/**
 * @file 锁定 yr-system mapper 常见高风险 SQL 拼装模式，避免尾逗号、非法 LIKE 与 WHERE AND 回归
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * yr-system SQL syntax contract 约束测试。
 */
class YrSystemSqlSyntaxContractTest {

    /**
     * 验证共享 SQL 片段不再保留会导致语法错误的尾逗号。
     *
     * @throws IOException 读取 mapper 失败
     */
    @Test
    void shouldAvoidDanglingCommaBeforeFromClause() throws IOException {
        assertMapperDoesNotContainIgnoringCase("SysDeptMapper.xml", ", from");
    }

    /**
     * 验证接收组与消息模板分页查询不再使用非法 LIKE 拼装。
     *
     * @throws IOException 读取 mapper 失败
     */
    @Test
    void shouldAvoidInvalidLikePlaceholderConcatenation() throws IOException {
        assertMapperDoesNotContainIgnoringCase("SysReceiveGroupMapper.xml", "like '%' #{");
        assertMapperDoesNotContainIgnoringCase("SysMsgTemplateMapper.xml", "like '%' #{");
    }

    /**
     * 验证用户分组信息查询不再生成 `WHERE AND` 这种非法语法。
     *
     * @throws IOException 读取 mapper 失败
     */
    @Test
    void shouldAvoidWhereAndPatternInModeUserGroupQuery() throws IOException {
        assertMapperDoesNotContainIgnoringCase("SysUserMapper.xml", "where and");
        assertSelectStatementContains("SysUserMapper.xml", "queryModeUserGroupInformationCollection", "<where>");
        assertSelectStatementDoesNotContainIgnoringCase(
                "SysUserMapper.xml",
                "queryModeUserGroupInformationCollection",
                "from sys_user su where"
        );
    }

    /**
     * 断言指定 mapper 不包含大小写不敏感的文本片段。
     *
     * @param mapperFileName mapper 文件名
     * @param unexpectedText 不应出现的 SQL 片段
     * @throws IOException 读取 mapper 失败
     */
    private void assertMapperDoesNotContainIgnoringCase(String mapperFileName, String unexpectedText) throws IOException {
        String mapperContent = loadMapperFromClasspath(mapperFileName);
        String normalizedActual = normalizeWhitespace(mapperContent).toLowerCase(Locale.ROOT);
        String normalizedUnexpected = normalizeWhitespace(unexpectedText).toLowerCase(Locale.ROOT);

        assertThat(normalizedActual)
                .as("%s 不应包含 %s", mapperFileName, unexpectedText)
                .doesNotContain(normalizedUnexpected);
    }

    /**
     * 从 classpath 读取指定 mapper 内容。
     *
     * @param mapperFileName mapper 文件名
     * @return mapper 文本
     * @throws IOException 读取失败
     */
    private String loadMapperFromClasspath(String mapperFileName) throws IOException {
        try (InputStream stream = Objects.requireNonNull(
                getClass().getClassLoader().getResourceAsStream("mapper/system/" + mapperFileName),
                "classpath mapper/system/" + mapperFileName + " 不存在")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 断言指定 select statement 包含目标文本。
     *
     * @param mapperFileName mapper 文件名
     * @param statementId select 语句 ID
     * @param expectedText 期待文本
     * @throws IOException 读取 mapper 失败
     */
    private void assertSelectStatementContains(String mapperFileName, String statementId, String expectedText) throws IOException {
        String statementBody = loadSelectStatementBody(mapperFileName, statementId);

        assertThat(statementBody)
                .as("%s#%s 应包含 %s", mapperFileName, statementId, expectedText)
                .contains(expectedText);
    }

    /**
     * 断言指定 select statement 不包含大小写不敏感的文本片段。
     *
     * @param mapperFileName mapper 文件名
     * @param statementId select 语句 ID
     * @param unexpectedText 禁止文本
     * @throws IOException 读取 mapper 失败
     */
    private void assertSelectStatementDoesNotContainIgnoringCase(String mapperFileName,
                                                                 String statementId,
                                                                 String unexpectedText) throws IOException {
        String statementBody = normalizeWhitespace(loadSelectStatementBody(mapperFileName, statementId)).toLowerCase(Locale.ROOT);
        String normalizedUnexpected = normalizeWhitespace(unexpectedText).toLowerCase(Locale.ROOT);

        assertThat(statementBody)
                .as("%s#%s 不应包含 %s", mapperFileName, statementId, unexpectedText)
                .doesNotContain(normalizedUnexpected);
    }

    /**
     * 读取指定 mapper 中某个 select statement 的原始 XML 片段。
     *
     * @param mapperFileName mapper 文件名
     * @param statementId select 语句 ID
     * @return select statement 原始 XML
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
     * 把空白归一化为单个空格，避免缩进差异影响断言。
     *
     * @param text 原始文本
     * @return 归一化后的文本
     */
    private String normalizeWhitespace(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
}
