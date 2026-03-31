/**
 * @file 用户写入边界源码契约测试
 * @author PopoY
 * @date 2026-03-30
 */
package com.yr.system.service.impl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定密码重置与状态修改不得继续复用通用 updateUser 写法。
 */
class SysUserServiceImplWriteBoundaryContractTest {

    /** SysUserServiceImpl 源码路径。 */
    private static final Path SERVICE_SOURCE_PATH = Path.of("src/main/java/com/yr/system/service/impl/SysUserServiceImpl.java");

    /** SysUserMapper 源码路径。 */
    private static final Path MAPPER_SOURCE_PATH = Path.of("src/main/java/com/yr/system/mapper/SysUserMapper.java");

    /** SysUserMapper XML 路径。 */
    private static final Path MAPPER_XML_PATH = Path.of("src/main/resources/mapper/system/SysUserMapper.xml");

    /**
     * 验证密码重置与状态修改不再直接调用通用 updateUser。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldNotUseGenericUpdateUserForPasswordResetOrStatusWrite() throws IOException {
        String normalizedSource = normalizeWhitespace(Files.readString(SERVICE_SOURCE_PATH));

        assertThat(normalizedSource)
                .doesNotContain("public int updateUserStatus(SysUser user) { return userMapper.updateUser(user); }")
                .doesNotContain("public int resetPwd(SysUser user) { return userMapper.updateUser(user); }");
    }

    /**
     * 验证 Mapper 接口声明了专用的状态更新方法，供 service 收口到专用 SQL。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldDeclareDedicatedMapperMethodForStatusWrite() throws IOException {
        String normalizedMapper = normalizeWhitespace(Files.readString(MAPPER_SOURCE_PATH));

        assertThat(normalizedMapper).contains("int updateUserStatus(SysUser user);");
    }

    /**
     * 验证 profile 更新不再继续复用通用 updateUser SQL。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldUseDedicatedWritePathForProfileUpdate() throws IOException {
        String serviceSource = Files.readString(SERVICE_SOURCE_PATH);
        String normalizedMapper = normalizeWhitespace(Files.readString(MAPPER_SOURCE_PATH));
        String profileUpdateBlock = normalizeWhitespace(extractUpdateBlock(Files.readString(MAPPER_XML_PATH), "updateUserProfile"));
        String normalizedProfileMethod = normalizeWhitespace(extractMethodBlock(serviceSource, "public int updateUserProfile(SysUser user)"));

        assertThat(normalizedProfileMethod).contains("return sysUserWriteService.updateUserProfile(user);");
        assertThat(normalizedProfileMethod).doesNotContain("return userMapper.updateUser(user);");
        assertThat(normalizedMapper).contains("int updateUserProfile(SysUser user);");
        assertThat(profileUpdateBlock).contains("nick_name = #{nickName}");
        assertThat(profileUpdateBlock).contains("email = #{email}");
        assertThat(profileUpdateBlock).contains("phonenumber = #{phonenumber}");
        assertThat(profileUpdateBlock).contains("sex = #{sex}");
        assertThat(profileUpdateBlock).contains("login_ip = #{loginIp}");
        assertThat(profileUpdateBlock).contains("login_date = #{loginDate}");
        assertThat(profileUpdateBlock).doesNotContain("user_name = #{userName}");
        assertThat(profileUpdateBlock).doesNotContain("dept_id = #{deptId}");
        assertThat(profileUpdateBlock).doesNotContain("password = #{password}");
        assertThat(profileUpdateBlock).doesNotContain("status = #{status}");
        assertThat(profileUpdateBlock).doesNotContain("remark = #{remark}");
    }

    /**
     * 归一化空白，减少源码断言对缩进和换行的敏感度。
     *
     * @param source 原始源码
     * @return 归一化后的源码
     */
    private String normalizeWhitespace(String source) {
        return source.replaceAll("\\s+", " ").trim();
    }

    /**
     * 从 mapper XML 中提取指定 update 节点，避免对整个文件做误判。
     *
     * @param mapperXml 原始 mapper XML
     * @param updateId update 节点 ID
     * @return 对应 update 节点源码
     */
    private String extractUpdateBlock(String mapperXml, String updateId) {
        String startToken = "<update id=\"" + updateId + "\"";
        int startIndex = mapperXml.indexOf(startToken);
        int endIndex = mapperXml.indexOf("</update>", startIndex);
        return mapperXml.substring(startIndex, endIndex + "</update>".length());
    }

    /**
     * 提取指定方法体源码，避免直接把整行实现写死到断言里。
     *
     * @param source 原始源码
     * @param methodSignature 方法签名
     * @return 方法体源码
     */
    private String extractMethodBlock(String source, String methodSignature) {
        int signatureIndex = source.indexOf(methodSignature);
        int blockStart = source.indexOf('{', signatureIndex);
        int depth = 0;
        for (int index = blockStart; index < source.length(); index++) {
            char currentChar = source.charAt(index);
            if (currentChar == '{') {
                depth++;
            } else if (currentChar == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(signatureIndex, index + 1);
                }
            }
        }
        throw new IllegalStateException("无法提取方法体: " + methodSignature);
    }
}
