/**
 * @file 组织状态修改安全契约测试
 * @author PopoY
 * @date 2026-04-01
 */
package com.yr.system.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定 SysOrg 的专用状态写路径，避免继续复用通用更新 SQL。
 */
class SysOrgServiceImplSafetyTest {

    /** SysOrgServiceImpl 源码路径。 */
    private static final Path SYS_ORG_SERVICE_SOURCE_PATH =
            Path.of("src/main/java/com/yr/system/service/impl/SysOrgServiceImpl.java");

    /** SysOrgMapper XML 路径。 */
    private static final Path SYS_ORG_MAPPER_XML_PATH =
            Path.of("src/main/resources/mapper/system/SysOrgMapper.xml");

    /**
     * 验证 updateOrgStatus 不允许继续直接委托到通用 updateSysOrg。
     *
     * @throws IOException 读取源码失败时抛出
     */
    @Test
    void shouldNotDelegateOrgStatusChangeToGenericUpdatePath() throws IOException {
        String serviceSource = Files.readString(SYS_ORG_SERVICE_SOURCE_PATH, StandardCharsets.UTF_8);
        String updateOrgStatusMethod = extractBetween(
                serviceSource,
                "public int updateOrgStatus(SysOrg sysOrg) {",
                "    /**\n     * 查询是否有子节点数据"
        );

        assertThat(updateOrgStatusMethod).doesNotContain("updateSysOrg(sysOrg)");
        assertThat(updateOrgStatusMethod).contains("updateOrgStatus(");
    }

    /**
     * 验证通用 updateSysOrg SQL 不再允许写入 create_by/create_at。
     *
     * @throws IOException 读取 XML 失败时抛出
     */
    @Test
    void shouldNotUpdateCreateAuditFieldsInGenericOrgUpdateSql() throws IOException {
        String mapperXml = Files.readString(SYS_ORG_MAPPER_XML_PATH, StandardCharsets.UTF_8);
        String updateSysOrgSql = extractBetween(
                mapperXml,
                "<update id=\"updateSysOrg\" parameterType=\"SysOrg\">",
                "    <delete id=\"deleteSysOrgById\""
        );

        assertThat(updateSysOrgSql).doesNotContain("create_by = #{createBy}");
        assertThat(updateSysOrgSql).doesNotContain("create_at = #{createAt}");
    }

    /**
     * 从完整文本中截取两个锚点之间的片段，供源码契约断言复用。
     *
     * @param source 完整源码
     * @param startMarker 起始锚点
     * @param endMarker 结束锚点
     * @return 起止锚点之间的片段
     */
    private String extractBetween(String source, String startMarker, String endMarker) {
        int startIndex = source.indexOf(startMarker);
        int endIndex = source.indexOf(endMarker);
        assertThat(startIndex).as("应找到起始锚点: %s", startMarker).isGreaterThanOrEqualTo(0);
        assertThat(endIndex).as("应找到结束锚点: %s", endMarker).isGreaterThan(startIndex);
        return source.substring(startIndex, endIndex);
    }
}
