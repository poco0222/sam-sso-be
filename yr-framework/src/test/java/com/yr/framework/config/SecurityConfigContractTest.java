/**
 * @file Spring Security 配置契约测试
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.framework.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定 Task 5 对 Spring Security 配置基线的迁移目标。
 */
class SecurityConfigContractTest {

    /** SecurityConfig 源码路径。 */
    private static final Path SECURITY_CONFIG_PATH = Path.of("src/main/java/com/yr/framework/config/SecurityConfig.java");

    /**
     * 验证安全配置不再基于 WebSecurityConfigurerAdapter。
     *
     * @throws IOException 读取源码失败时抛出
     */
    @Test
    void shouldUseSecurityFilterChainInsteadOfWebSecurityConfigurerAdapter() throws IOException {
        String securityConfigSource = Files.readString(SECURITY_CONFIG_PATH, StandardCharsets.UTF_8);

        assertThat(securityConfigSource).doesNotContain("extends WebSecurityConfigurerAdapter");
        assertThat(securityConfigSource).doesNotContain("@EnableGlobalMethodSecurity");
        assertThat(securityConfigSource).contains("@EnableMethodSecurity");
        assertThat(securityConfigSource).contains("SecurityFilterChain");
    }

    /**
     * 验证安全配置暴露 AuthenticationManager bean，并保留 PasswordEncoder bean。
     *
     * @throws IOException 读取源码失败时抛出
     */
    @Test
    void shouldExposeAuthenticationManagerAndPasswordEncoderBeans() throws IOException {
        String securityConfigSource = Files.readString(SECURITY_CONFIG_PATH, StandardCharsets.UTF_8);

        assertThat(securityConfigSource).contains("AuthenticationManager");
        assertThat(securityConfigSource).contains("@Bean");
        assertThat(securityConfigSource).contains("bCryptPasswordEncoder");
    }
}
