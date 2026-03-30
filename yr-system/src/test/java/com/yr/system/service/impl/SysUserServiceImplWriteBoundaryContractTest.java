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
     * 归一化空白，减少源码断言对缩进和换行的敏感度。
     *
     * @param source 原始源码
     * @return 归一化后的源码
     */
    private String normalizeWhitespace(String source) {
        return source.replaceAll("\\s+", " ").trim();
    }
}
