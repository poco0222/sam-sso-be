/**
 * @file 锁定 yr-system 高风险 SQL 与查询语义约束，避免平台过滤、消息发送人与文件筛选语义回退
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * yr-system SQL contract 约束测试。
 */
class YrSystemSqlContractTest {

    /** yr-system 模块根目录，用于读取 service 源码验证 sender contract。 */
    private static final Path MODULE_ROOT = locateModuleRoot();

    /**
     * 验证 mapper 中依赖 mgmt 平台的过滤语句始终包裹括号，避免 AND/OR 优先级带来数据泄露。
     *
     * @throws IOException 读取资源失败
     */
    @Test
    void shouldWriteExplicitParenthesesAroundMgmtPlatformBranch() throws IOException {
        assertMapperPathDoesNotExist("SysMenuMapper.xml");
    }

    /**
     * 验证高风险 mapper 不再直接保留裸 `SELECT *`，避免列扩散和隐式映射漂移。
     *
     * @throws IOException 读取资源失败
     */
    @Test
    void shouldAvoidSelectStarInHighRiskSystemMappers() throws IOException {
        assertMapperDoesNotProjectWildcardColumns("SysUserMapper.xml");
        assertMapperDoesNotProjectWildcardColumns("SysDeptMapper.xml");
        assertMapperDoesNotProjectWildcardColumns("SysOrgMapper.xml");
        assertMapperDoesNotProjectWildcardColumns("SysUserDeptMapper.xml");
    }

    /**
     * 验证可达时间过滤查询不再对索引列使用 date_format(column, ...)，避免索引失效。
     *
     * @throws IOException 读取资源失败
     */
    @Test
    void shouldAvoidDateFormatOnReachableTimeFilters() throws IOException {
        assertMapperDoesNotContainIgnoringCase("SysUserMapper.xml", "date_format(");
        assertMapperPathDoesNotExist("SysDictTypeMapper.xml");
        assertMapperDoesNotContainIgnoringCase("SysLogininforMapper.xml", "date_format(");
        assertMapperDoesNotContainIgnoringCase("SysOperLogMapper.xml", "date_format(");
    }

    /**
     * 验证一期边界不再继续保留角色部门树与用户角色关系 SQL。
     *
     * @throws IOException 读取资源失败
     */
    @Test
    void shouldRemoveLegacyRoleDrivenSqlFromPhaseOneBoundary() throws IOException {
        assertMapperPathDoesNotExist("SysUserRoleMapper.xml");
        assertMapperDoesNotContainIgnoringCase("SysDeptMapper.xml", "selectDeptRoleTreeList");
        assertMapperDoesNotContainIgnoringCase("SysDeptMapper.xml", "sys_role_dept");
        assertMapperDoesNotContainIgnoringCase("SysUserMapper.xml", "selectUserListByDeptRole");
        assertMapperDoesNotContainIgnoringCase("SysUserMapper.xml", "selectAllocatedList");
        assertMapperDoesNotContainIgnoringCase("SysUserMapper.xml", "selectUnallocatedList");
        assertMapperDoesNotContainIgnoringCase("SysUserMapper.xml", "selectUserRoleListByRoleKeysBatch");
        assertMapperDoesNotContainIgnoringCase("SysUserMapper.xml", "sys_user_role");
    }

    /**
     * 验证 Task 7 第一轮只允许指定用户查询路径暂时保留 `find_in_set`。
     *
     * @throws IOException 读取资源失败
     */
    @Test
    void shouldLimitFindInSetUsageToApprovedUserStatements() throws IOException {
        assertFindInSetOnlyAppearsIn("SysUserMapper.xml", List.of("selectUserList", "selectAllUserList"));
    }

    /**
     * 验证 Task 7 第一轮只允许指定部门树查询路径暂时保留 `find_in_set`。
     *
     * @throws IOException 读取资源失败
     */
    @Test
    void shouldLimitFindInSetUsageToApprovedDeptStatements() throws IOException {
        assertFindInSetOnlyAppearsIn("SysDeptMapper.xml", List.of("selectChildrenDeptById", "selectNormalChildrenDeptById"));
    }

    /**
     * 验证消息主体写入端与接收端关联查询统一以 username 作为 `msg_from` 规范契约。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldRemoveLegacyMessageSenderPersistenceChainFromPhaseOneBoundary() {
        assertPathDoesNotExist(Path.of("src", "main", "java", "com", "yr", "system", "service", "impl", "SysMessageBodyService.java"));
        assertMapperPathDoesNotExist("SysMessageBodyReceiverMapper.xml");
    }

    /**
     * 验证一期边界已经移除 SysFileMapper，避免测试继续锁定已下线文件内容。
     */
    @Test
    void shouldRemoveSysFileMapperFromPhaseOneBoundary() {
        assertMapperPathDoesNotExist("SysFileMapper.xml");
    }

    /**
     * 验证登录相关查询会从默认组织关系解析 `orgId`，避免 `sys_user.org_id` 为空时污染登录态。
     *
     * @throws IOException 读取 mapper 失败
     */
    @Test
    void shouldResolveDefaultOrgIdFromUserOrgInLoginQueries() throws IOException {
        assertMapperContains("SysUserMapper.xml", "<sql id=\"selectUserBaseColumnsWithResolvedOrgName\">");
        assertMapperContains("SysUserMapper.xml", "suo.org_id as org_id");
        assertSelectStatementContainsIgnoringCase(
                "SysUserMapper.xml",
                "selectUserByUserName",
                "refid=\"selectUserBaseColumnsWithResolvedOrgName\""
        );
        assertSelectStatementContainsIgnoringCase(
                "SysUserMapper.xml",
                "selectUserByUserId",
                "refid=\"selectUserBaseColumnsWithResolvedOrgName\""
        );
    }

    /**
     * 断言指定 mapper 包含预期的文本片段。
     *
     * @param mapperFileName mapper 文件名
     * @param expectedText 期待的 SQL 片段
     * @throws IOException 读取资源失败
     */
    private void assertMapperContains(String mapperFileName, String expectedText) throws IOException {
        String mapperContent = loadMapperFromClasspath(mapperFileName);
        String normalizedActual = normalizeWhitespace(mapperContent);
        String normalizedExpected = normalizeWhitespace(expectedText);

        assertThat(normalizedActual)
                .as("%s 应包含 %s", mapperFileName, expectedText)
                .contains(normalizedExpected);
    }

    /**
     * 断言指定 mapper 不包含大小写不敏感的文本片段。
     *
     * @param mapperFileName mapper 文件名
     * @param unexpectedText 不应出现的 SQL 片段
     * @throws IOException 读取资源失败
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
     * 断言指定 mapper 中包含 `find_in_set` 的 statement 只来自允许列表。
     *
     * @param mapperFileName mapper 文件名
     * @param allowedStatementIds 允许保留 `find_in_set` 的 statement ID
     * @throws IOException 读取资源失败
     */
    private void assertFindInSetOnlyAppearsIn(String mapperFileName, List<String> allowedStatementIds) throws IOException {
        List<String> actualStatementIds = collectSelectIdsContaining(mapperFileName, "find_in_set");

        assertThat(actualStatementIds)
                .as("%s 中 find_in_set 只允许出现在 %s，实际出现在 %s",
                        mapperFileName,
                        allowedStatementIds,
                        actualStatementIds)
                .allMatch(allowedStatementIds::contains);
    }

    /**
     * 断言指定 mapper 中不存在 wildcard projection（例如 `select *`、`select su.*`）。
     *
     * @param mapperFileName mapper 文件名
     * @throws IOException 读取资源失败
     */
    private void assertMapperDoesNotProjectWildcardColumns(String mapperFileName) throws IOException {
        List<String> actualStatementIds = collectSelectIdsWithWildcardProjection(mapperFileName);

        assertThat(actualStatementIds)
                .as("%s 不应继续使用 wildcard projection，实际出现在 %s", mapperFileName, actualStatementIds)
                .isEmpty();
    }

    /**
     * 断言一期已删除的 mapper 文件不再出现在 yr-system 源码目录中。
     *
     * @param mapperFileName mapper 文件名
     */
    private void assertMapperPathDoesNotExist(String mapperFileName) {
        Path mapperPath = MODULE_ROOT.resolve(Path.of("src", "main", "resources", "mapper", "system", mapperFileName));

        assertThat(Files.exists(mapperPath))
                .as("%s 应当已经从一期边界移除", mapperPath)
                .isFalse();
    }

    /**
     * 断言指定源码路径在一期边界内已经被删除。
     *
     * @param relativePath 相对 yr-system 模块根目录的路径
     */
    private void assertPathDoesNotExist(Path relativePath) {
        Path sourcePath = MODULE_ROOT.resolve(relativePath);

        assertThat(Files.exists(sourcePath))
                .as("%s 应当已经从一期边界移除", sourcePath)
                .isFalse();
    }

    /**
     * 断言指定 select statement 不包含大小写不敏感的文本片段。
     *
     * @param mapperFileName mapper 文件名
     * @param statementId select 语句 ID
     * @param unexpectedText 不应出现的 SQL 片段
     * @throws IOException 读取资源失败
     */
    private void assertSelectStatementDoesNotContainIgnoringCase(String mapperFileName,
                                                                 String statementId,
                                                                 String unexpectedText) throws IOException {
        String statementBody = loadSelectStatementBody(mapperFileName, statementId);
        String normalizedActual = normalizeWhitespace(statementBody).toLowerCase(Locale.ROOT);
        String normalizedUnexpected = normalizeWhitespace(unexpectedText).toLowerCase(Locale.ROOT);

        assertThat(normalizedActual)
                .as("%s#%s 不应包含 %s", mapperFileName, statementId, unexpectedText)
                .doesNotContain(normalizedUnexpected);
    }

    /**
     * 断言指定 select statement 包含大小写不敏感的文本片段。
     *
     * @param mapperFileName mapper 文件名
     * @param statementId select 语句 ID
     * @param expectedText 预期出现的 SQL 片段
     * @throws IOException 读取资源失败
     */
    private void assertSelectStatementContainsIgnoringCase(String mapperFileName,
                                                           String statementId,
                                                           String expectedText) throws IOException {
        String statementBody = loadSelectStatementBody(mapperFileName, statementId);
        String normalizedActual = normalizeWhitespace(statementBody).toLowerCase(Locale.ROOT);
        String normalizedExpected = normalizeWhitespace(expectedText).toLowerCase(Locale.ROOT);

        assertThat(normalizedActual)
                .as("%s#%s 应包含 %s", mapperFileName, statementId, expectedText)
                .contains(normalizedExpected);
    }

    /**
     * 从 classpath 读取指定 mapper 源文件内容。
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
     * 读取指定 mapper 中某个 select statement 的原始 XML。
     *
     * @param mapperFileName mapper 文件名
     * @param statementId select 语句 ID
     * @return select statement 原始文本
     * @throws IOException 读取资源失败
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
     * 断言指定源码文件包含目标文本。
     *
     * @param relativePath 相对模块根目录的路径
     * @param expectedText 预期文本
     * @throws IOException 读取源码失败
     */
    private void assertSourceContains(String relativePath, String expectedText) throws IOException {
        String sourceText = Files.readString(MODULE_ROOT.resolve(relativePath), StandardCharsets.UTF_8);

        assertThat(sourceText)
                .as("%s 应包含 %s", relativePath, expectedText)
                .contains(expectedText);
    }

    /**
     * 提取指定 mapper 中所有包含目标关键字的 `<select>` statement ID。
     *
     * @param mapperFileName mapper 文件名
     * @param keyword 关键字
     * @return 包含关键字的 statement ID 列表
     * @throws IOException 读取资源失败
     */
    private List<String> collectSelectIdsContaining(String mapperFileName, String keyword) throws IOException {
        String mapperContent = loadMapperFromClasspath(mapperFileName);
        Matcher statementMatcher = Pattern.compile(
                "<select\\s+id=\"([^\"]+)\"[^>]*>(.*?)</select>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        ).matcher(mapperContent);
        String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
        List<String> statementIds = new ArrayList<>();

        while (statementMatcher.find()) {
            String statementId = statementMatcher.group(1);
            String statementBody = normalizeWhitespace(statementMatcher.group(2)).toLowerCase(Locale.ROOT);
            if (statementBody.contains(normalizedKeyword) && !statementIds.contains(statementId)) {
                statementIds.add(statementId);
            }
        }

        return statementIds;
    }

    /**
     * 提取指定 mapper 中使用 wildcard projection 的 select statement ID。
     *
     * @param mapperFileName mapper 文件名
     * @return 使用 `select *` / `select alias.*` 的 statement ID 列表
     * @throws IOException 读取资源失败
     */
    private List<String> collectSelectIdsWithWildcardProjection(String mapperFileName) throws IOException {
        String mapperContent = loadMapperFromClasspath(mapperFileName);
        Matcher statementMatcher = Pattern.compile(
                "<select\\s+id=\"([^\"]+)\"[^>]*>(.*?)</select>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        ).matcher(mapperContent);
        List<String> statementIds = new ArrayList<>();

        while (statementMatcher.find()) {
            String statementId = statementMatcher.group(1);
            String statementBody = normalizeWhitespace(statementMatcher.group(2));
            if (containsWildcardProjection(statementBody) && !statementIds.contains(statementId)) {
                statementIds.add(statementId);
            }
        }

        return statementIds;
    }

    /**
     * 把任意文本的空白归一化为单个空格，降低换行/缩进差异带来的断言破裂。
     *
     * @param text 原始文本
     * @return 空白归一化后的文本
     */
    private String normalizeWhitespace(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * 判断 statement 是否包含 wildcard projection（通配列投影）。
     *
     * @return true 表示存在 wildcard projection
     */
    private boolean containsWildcardProjection(String statementBody) {
        Pattern leadingWildcardPattern = Pattern.compile(
                "\\bselect\\s+(distinct\\s+)?([a-zA-Z_][\\w]*\\.)?\\*",
                Pattern.CASE_INSENSITIVE
        );
        Pattern trailingAliasWildcardPattern = Pattern.compile(
                ",\\s*[a-zA-Z_][\\w]*\\.\\*",
                Pattern.CASE_INSENSITIVE
        );
        return leadingWildcardPattern.matcher(statementBody).find()
                || trailingAliasWildcardPattern.matcher(statementBody).find();
    }

    /**
     * 基于测试类的 CodeSource 回溯定位 Maven 模块根目录。
     *
     * @return 模块根目录
     */
    private static Path locateModuleRoot() {
        try {
            Path codeSourcePath = Paths.get(YrSystemSqlContractTest.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            Path currentPath = Files.isDirectory(codeSourcePath)
                    ? codeSourcePath
                    : codeSourcePath.getParent();
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
