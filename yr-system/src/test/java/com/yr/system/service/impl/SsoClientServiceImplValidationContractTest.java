/**
 * @file 客户端写入校验契约测试
 * @author PopoY
 * @date 2026-03-31
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SsoClient;
import com.yr.common.exception.CustomException;
import com.yr.system.mapper.SsoClientMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 锁定客户端状态与布尔型开关的 allowed value（允许值）校验。
 */
class SsoClientServiceImplValidationContractTest {

    /**
     * 验证实体调试输出不会把 clientSecret 直接带入日志上下文。
     */
    @Test
    void shouldExcludeClientSecretFromEntityToString() {
        SsoClient ssoClient = buildValidClient();
        ssoClient.setClientSecret("secret-123");

        assertThat(ssoClient.toString())
                .doesNotContain("secret-123")
                .doesNotContain("clientSecret");
    }

    /**
     * 验证 changeStatus 不允许写入非法状态值。
     */
    @Test
    void shouldRejectInvalidStatusWhenChangingClientStatus() {
        SsoClientServiceImpl service = new SsoClientServiceImpl();

        assertThatThrownBy(() -> service.changeStatus(7L, "X"))
                .isInstanceOf(CustomException.class)
                .hasMessage("status只允许为0或1");
    }

    /**
     * 验证新增客户端时 status 必须收窄到允许值。
     */
    @Test
    void shouldRejectInvalidStatusWhenCreatingClient() {
        SsoClientServiceImpl service = new SsoClientServiceImpl();
        SsoClient ssoClient = buildValidClient();
        ssoClient.setStatus("X");

        assertThatThrownBy(() -> service.insertSsoClient(ssoClient))
                .isInstanceOf(CustomException.class)
                .hasMessage("status只允许为0或1");
    }

    /**
     * 验证 allowPasswordLogin 必须收窄到 Y/N。
     */
    @Test
    void shouldRejectInvalidAllowPasswordLoginWhenCreatingClient() {
        SsoClientServiceImpl service = new SsoClientServiceImpl();
        SsoClient ssoClient = buildValidClient();
        ssoClient.setAllowPasswordLogin("X");

        assertThatThrownBy(() -> service.insertSsoClient(ssoClient))
                .isInstanceOf(CustomException.class)
                .hasMessage("allowPasswordLogin只允许为Y或N");
    }

    /**
     * 验证 allowWxworkLogin 必须收窄到 Y/N。
     */
    @Test
    void shouldRejectInvalidAllowWxworkLoginWhenCreatingClient() {
        SsoClientServiceImpl service = new SsoClientServiceImpl();
        SsoClient ssoClient = buildValidClient();
        ssoClient.setAllowWxworkLogin("X");

        assertThatThrownBy(() -> service.insertSsoClient(ssoClient))
                .isInstanceOf(CustomException.class)
                .hasMessage("allowWxworkLogin只允许为Y或N");
    }

    /**
     * 验证 syncEnabled 必须收窄到 Y/N。
     */
    @Test
    void shouldRejectInvalidSyncEnabledWhenCreatingClient() {
        SsoClientServiceImpl service = new SsoClientServiceImpl();
        SsoClient ssoClient = buildValidClient();
        ssoClient.setSyncEnabled("X");

        assertThatThrownBy(() -> service.insertSsoClient(ssoClient))
                .isInstanceOf(CustomException.class)
                .hasMessage("syncEnabled只允许为Y或N");
    }

    /**
     * 验证非法 redirectUris 必须统一映射到受控业务错误。
     */
    @Test
    void shouldMapInvalidRedirectUrisToControlledBusinessError() {
        SsoClientServiceImpl service = new SsoClientServiceImpl();
        SsoClient ssoClient = buildValidClient();
        ssoClient.setRedirectUris("://broken-uri");

        assertThatThrownBy(() -> service.insertSsoClient(ssoClient))
                .isInstanceOf(CustomException.class)
                .hasMessage("redirectUris中存在非法地址");
    }

    /**
     * 验证 redirectUris 不允许使用非 http/https 的危险协议。
     */
    @Test
    void shouldRejectUnsupportedRedirectUriScheme() {
        SsoClientServiceImpl service = new SsoClientServiceImpl();
        SsoClient ssoClient = buildValidClient();
        ssoClient.setRedirectUris("javascript:alert('xss')");

        assertThatThrownBy(() -> service.insertSsoClient(ssoClient))
                .isInstanceOf(CustomException.class)
                .hasMessage("redirectUris中存在非法地址");
    }

    /**
     * 验证 redirectUris 不允许使用 host 为空的 http/https 地址。
     */
    @Test
    void shouldRejectRedirectUriWithoutHost() {
        SsoClientServiceImpl service = new SsoClientServiceImpl();
        SsoClient ssoClient = buildValidClient();
        ssoClient.setRedirectUris("https:///callback");

        assertThatThrownBy(() -> service.insertSsoClient(ssoClient))
                .isInstanceOf(CustomException.class)
                .hasMessage("redirectUris中存在非法地址");
    }

    /**
     * 验证轮换客户端密钥时如果数据库更新失败，必须返回受控业务错误。
     */
    @Test
    void shouldRejectRotateClientSecretWhenPersistenceFails() {
        SsoClientMapper mapper = mock(SsoClientMapper.class);
        SsoClientServiceImpl service = new SsoClientServiceImpl();
        SsoClient ssoClient = buildValidClient();
        ssoClient.setClientId(7L);

        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(mapper.selectById(7L)).thenReturn(ssoClient);
        when(mapper.updateById(any(SsoClient.class))).thenReturn(0);

        assertThatThrownBy(() -> service.rotateClientSecret(7L))
                .isInstanceOf(CustomException.class)
                .hasMessage("轮换客户端密钥失败");
    }

    /**
     * 构造最小有效客户端，供单项非法值测试复用。
     *
     * @return 最小有效客户端
     */
    private SsoClient buildValidClient() {
        SsoClient ssoClient = new SsoClient();
        ssoClient.setClientCode("sam-mgmt");
        ssoClient.setClientName("SAM 管理后台");
        ssoClient.setRedirectUris("https://sso.example.com/callback");
        ssoClient.setAllowPasswordLogin("Y");
        ssoClient.setAllowWxworkLogin("N");
        ssoClient.setSyncEnabled("Y");
        ssoClient.setStatus("0");
        ssoClient.setCreateBy("phase5-operator");
        return ssoClient;
    }
}
