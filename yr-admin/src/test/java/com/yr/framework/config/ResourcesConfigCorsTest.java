/**
 * @file 跨域资源配置回归测试
 * @author Codex
 * @date 2026-03-12
 */
package com.yr.framework.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证全局跨域配置在允许携带凭证时仍能正确处理具体来源地址。
 */
class ResourcesConfigCorsTest {

    /**
     * 回归覆盖 Spring Boot 2.7 升级后 `/logout` 因跨域来源校验抛异常的问题。
     */
    @Test
    void shouldAllowConcreteOriginWhenCredentialsAreEnabled() {
        ResourcesConfig resourcesConfig = new ResourcesConfig();
        CorsFilter corsFilter = resourcesConfig.corsFilter();
        UrlBasedCorsConfigurationSource configSource =
                (UrlBasedCorsConfigurationSource) ReflectionTestUtils.getField(corsFilter, "configSource");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/logout");
        request.addHeader("Origin", "http://localhost:80");
        CorsConfiguration corsConfiguration = configSource.getCorsConfiguration(request);

        String allowedOrigin = assertDoesNotThrow(
                () -> corsConfiguration.checkOrigin("http://localhost:80")
        );

        assertEquals("http://localhost:80", allowedOrigin);
    }
}
