/**
 * @file 客户端接入说明契约测试
 * @author PopoY
 * @date 2026-04-02
 */
package com.yr.system.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yr.common.core.domain.entity.SsoClient;
import com.yr.common.exception.CustomException;
import com.yr.system.mapper.SsoClientMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 锁定客户端接入治理说明的最小导出契约，避免控制台继续只停留在 CRUD 页。
 */
class SsoClientIntegrationGuideContractTest {

    /** 通用对象映射器，用于把反射结果转成稳定 Map 断言。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证已就绪客户端可以导出接入说明、接入前检查与最近密钥信息。
     */
    @Test
    void shouldBuildIntegrationGuideForReadyClient() {
        SsoClientMapper mapper = mock(SsoClientMapper.class);
        SsoClientServiceImpl service = new SsoClientServiceImpl();
        Date createTime = new Date(1_744_000_000_000L);
        Date updateTime = new Date(1_744_086_400_000L);
        SsoClient ssoClient = buildClient();
        ssoClient.setClientId(7L);
        ssoClient.setRedirectUris("https://downstream.example.com/callback\nhttps://downstream.example.com/alt");
        ssoClient.setCreateTime(createTime);
        ssoClient.setUpdateTime(updateTime);

        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(mapper.selectById(7L)).thenReturn(ssoClient);

        Map<String, Object> guide = invokeGuideAsMap(service, 7L);

        assertThat(guide)
                .containsEntry("clientId", 7L)
                .containsEntry("clientCode", "sam-mgmt")
                .containsEntry("clientName", "SAM 管理后台")
                .containsEntry("authorizePath", "/auth/authorize")
                .containsEntry("exchangePath", "/auth/exchange")
                .containsEntry("redirectUriConfigured", true)
                .containsEntry("loginMethodConfigured", true)
                .containsEntry("clientEnabled", true)
                .containsEntry("syncEnabledReady", true)
                .doesNotContainKey("clientSecret");

        assertThat(guide.get("latestKnownSecretOperationTime"))
                .as("接入说明应至少暴露最近已知密钥操作时间")
                .isNotNull();
        assertThat((String) guide.get("secretRotationInfo"))
                .contains("未单独记录密钥轮换时间");
        assertThat((String) guide.get("authorizeExample"))
                .contains("clientCode=sam-mgmt")
                .contains("redirectUri=https://downstream.example.com/callback")
                .contains("state=");
        assertThat((String) guide.get("exchangeRequestExample"))
                .contains("\"clientCode\":\"sam-mgmt\"")
                .contains("\"clientSecret\":\"<创建或轮换后获取的一次性明文>\"")
                .contains("\"code\":\"<浏览器回跳得到的一次性code>\"");
        assertThat((List<String>) guide.get("identityPayloadFields"))
                .contains("userId", "userName", "nickName", "orgInfo", "deptInfo", "traceId", "exchangeId");
    }

    /**
     * 验证未就绪客户端的接入说明会明确暴露缺失项，而不是把治理状态伪装成“可接入”。
     */
    @Test
    void shouldExposeGovernanceGapsForUnreadyClient() {
        SsoClientMapper mapper = mock(SsoClientMapper.class);
        SsoClientServiceImpl service = new SsoClientServiceImpl();
        SsoClient ssoClient = buildClient();
        ssoClient.setClientId(9L);
        ssoClient.setRedirectUris("");
        ssoClient.setAllowPasswordLogin("N");
        ssoClient.setAllowWxworkLogin("N");
        ssoClient.setSyncEnabled("N");
        ssoClient.setStatus("1");
        ssoClient.setCreateTime(new Date(1_744_000_000_000L));

        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(mapper.selectById(9L)).thenReturn(ssoClient);

        Map<String, Object> guide = invokeGuideAsMap(service, 9L);

        assertThat(guide)
                .containsEntry("redirectUriConfigured", false)
                .containsEntry("loginMethodConfigured", false)
                .containsEntry("clientEnabled", false)
                .containsEntry("syncEnabledReady", false);
        assertThat((String) guide.get("authorizeExample"))
                .contains("your-app.example.com");
    }

    /**
     * 验证查询不存在的客户端时会返回受控业务错误。
     */
    @Test
    void shouldRejectMissingClientWhenBuildingIntegrationGuide() {
        SsoClientMapper mapper = mock(SsoClientMapper.class);
        SsoClientServiceImpl service = new SsoClientServiceImpl();

        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(mapper.selectById(88L)).thenReturn(null);

        assertThatThrownBy(() -> invokeGuideAsMap(service, 88L))
                .isInstanceOf(CustomException.class)
                .hasMessage("客户端不存在");
    }

    /**
     * 通过反射调用目标方法；在方法尚未实现时给出清晰失败信息。
     *
     * @param service 服务实现
     * @param clientId 客户端ID
     * @return 归一化后的接入说明 Map
     */
    private Map<String, Object> invokeGuideAsMap(SsoClientServiceImpl service, Long clientId) {
        try {
            Object result = ReflectionTestUtils.invokeMethod(service, "buildIntegrationGuide", clientId);
            return objectMapper.convertValue(result, new TypeReference<Map<String, Object>>() {
            });
        } catch (IllegalStateException exception) {
            fail("SsoClientServiceImpl 尚未提供 buildIntegrationGuide(Long) 契约", exception);
            return Map.of();
        }
    }

    /**
     * 构造最小有效客户端，供接入说明契约测试复用。
     *
     * @return 最小有效客户端
     */
    private SsoClient buildClient() {
        SsoClient ssoClient = new SsoClient();
        ssoClient.setClientCode("sam-mgmt");
        ssoClient.setClientName("SAM 管理后台");
        ssoClient.setAllowPasswordLogin("Y");
        ssoClient.setAllowWxworkLogin("N");
        ssoClient.setSyncEnabled("Y");
        ssoClient.setStatus("0");
        ssoClient.setCreateBy("phase2-operator");
        return ssoClient;
    }
}
