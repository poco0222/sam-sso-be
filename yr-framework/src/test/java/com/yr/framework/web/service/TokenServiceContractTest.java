/**
 * @file TokenService 令牌密钥契约测试
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.framework.web.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 锁定 JWT（JSON Web Token）签名密钥的 fail-fast（快速失败）行为。
 */
class TokenServiceContractTest {

    /**
     * 验证空白 token.secret 配置不会继续生成可预测签名密钥。
     */
    @Test
    void shouldRejectBlankSigningSecret() {
        TokenService tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", " ");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(tokenService, "getSigningKey"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("token.secret");
    }
}
