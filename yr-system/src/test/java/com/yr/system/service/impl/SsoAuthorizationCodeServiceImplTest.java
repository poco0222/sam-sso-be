/**
 * @file 认证接入协议授权码服务测试
 * @author PopoY
 * @date 2026-04-02
 */
package com.yr.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yr.common.core.domain.entity.SsoClient;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.exception.CustomException;
import com.yr.common.utils.SecurityUtils;
import com.yr.system.domain.dto.SsoAuthorizationCodePayload;
import com.yr.system.service.ISsoClientService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定认证接入协议 authorize / exchange 的核心业务语义。
 */
class SsoAuthorizationCodeServiceImplTest {

    /**
     * 验证非法 clientCode 会被 authorize 入口拒绝。
     */
    @Test
    void shouldRejectUnknownClientCodeWhenAuthorizing() {
        ISsoClientService ssoClientService = mock(ISsoClientService.class);
        SsoAuthorizationCodeServiceImpl service = createService(ssoClientService, mock(StringRedisTemplate.class));

        when(ssoClientService.selectSsoClientByCode("sam-mgmt")).thenReturn(null);

        assertThatThrownBy(() -> service.issueAuthorizeRedirectUrl(
                "sam-mgmt",
                "https://downstream.example.com/callback",
                "state-123",
                buildLoginUser()
        )).isInstanceOf(CustomException.class)
                .hasMessage("clientCode无效或客户端不存在");
    }

    /**
     * 验证 redirectUri 不在白名单内时会被 authorize 入口拒绝。
     */
    @Test
    void shouldRejectRedirectUriOutsideWhitelistWhenAuthorizing() {
        ISsoClientService ssoClientService = mock(ISsoClientService.class);
        SsoAuthorizationCodeServiceImpl service = createService(ssoClientService, mock(StringRedisTemplate.class));
        SsoClient ssoClient = buildAuthorizeClient("https://allowed.example.com/callback");

        when(ssoClientService.selectSsoClientByCode("sam-mgmt")).thenReturn(ssoClient);

        assertThatThrownBy(() -> service.issueAuthorizeRedirectUrl(
                "sam-mgmt",
                "https://downstream.example.com/callback",
                "state-123",
                buildLoginUser()
        )).isInstanceOf(CustomException.class)
                .hasMessage("redirectUri不在白名单内");
    }

    /**
     * 验证 redirectUri 已携带冲突 state 时，authorize 入口会按 state 不匹配拒绝。
     */
    @Test
    void shouldRejectAuthorizeWhenRedirectUriContainsMismatchedState() {
        ISsoClientService ssoClientService = mock(ISsoClientService.class);
        SsoAuthorizationCodeServiceImpl service = createService(ssoClientService, mock(StringRedisTemplate.class));
        SsoClient ssoClient = buildAuthorizeClient("https://downstream.example.com/callback?state=other");

        when(ssoClientService.selectSsoClientByCode("sam-mgmt")).thenReturn(ssoClient);

        assertThatThrownBy(() -> service.issueAuthorizeRedirectUrl(
                "sam-mgmt",
                "https://downstream.example.com/callback?state=other",
                "state-123",
                buildLoginUser()
        )).isInstanceOf(CustomException.class)
                .hasMessage("state不匹配");
    }

    /**
     * 验证 authorize 成功时会落 Redis 一次性授权码，并带着 code/state 回跳到下游回调地址。
     *
     * @throws Exception JSON 断言失败时抛出
     */
    @Test
    void shouldPersistOneTimeCodeAndRedirectToDownstreamCallbackWhenAuthorizing() throws Exception {
        ISsoClientService ssoClientService = mock(ISsoClientService.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        SsoAuthorizationCodeServiceImpl service = createService(ssoClientService, stringRedisTemplate);
        SsoClient ssoClient = buildAuthorizeClient("https://downstream.example.com/callback?scene=prod");
        ArgumentCaptor<String> cacheKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cacheValueCaptor = ArgumentCaptor.forClass(String.class);

        when(ssoClientService.selectSsoClientByCode("sam-mgmt")).thenReturn(ssoClient);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        String redirectUrl = service.issueAuthorizeRedirectUrl(
                "sam-mgmt",
                "https://downstream.example.com/callback?scene=prod",
                "state-123",
                buildLoginUser()
        );

        verify(valueOperations).set(cacheKeyCaptor.capture(), cacheValueCaptor.capture(), eq(300L), eq(TimeUnit.SECONDS));
        String code = redirectUrl.replaceAll("^.*[?&]code=([^&]+).*$", "$1");
        assertThat(redirectUrl).contains("https://downstream.example.com/callback?scene=prod");
        assertThat(redirectUrl).contains("code=" + code);
        assertThat(redirectUrl).contains("state=state-123");
        assertThat(cacheKeyCaptor.getValue()).isEqualTo("sso:auth:code:" + code);

        SsoAuthorizationCodePayload payload = new ObjectMapper().readValue(cacheValueCaptor.getValue(), SsoAuthorizationCodePayload.class);
        assertThat(payload.getClientId()).isEqualTo(7L);
        assertThat(payload.getClientCode()).isEqualTo("sam-mgmt");
        assertThat(payload.getUserId()).isEqualTo(9L);
        assertThat(payload.getUsername()).isEqualTo("phase2-user");
        assertThat(payload.getOrgId()).isEqualTo(18L);
        assertThat(payload.getDeptId()).isEqualTo(108L);
    }

    /**
     * 验证过期授权码会被 exchange 入口拒绝。
     */
    @Test
    void shouldRejectExpiredAuthorizationCodeWhenExchanging() {
        ISsoClientService ssoClientService = mock(ISsoClientService.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        SsoAuthorizationCodeServiceImpl service = createService(ssoClientService, stringRedisTemplate);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("sso:auth:code:code-123")).thenReturn(null);

        assertThatThrownBy(() -> service.exchangeIdentity("sam-mgmt", "secret-plain", "code-123"))
                .isInstanceOf(CustomException.class)
                .hasMessage("授权码无效或已过期");
    }

    /**
     * 验证授权码只能使用一次。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void shouldAllowAuthorizationCodeOnlyOnce() throws Exception {
        ISsoClientService ssoClientService = mock(ISsoClientService.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        SsoAuthorizationCodeServiceImpl service = createService(ssoClientService, stringRedisTemplate);
        SsoClient persistedClient = buildPersistedClient();
        String payloadJson = buildPayloadJson();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("sso:auth:code:code-123"))
                .thenReturn(payloadJson)
                .thenReturn(null);
        when(ssoClientService.getById(anyLong())).thenReturn(persistedClient);

        SsoAuthorizationCodePayload firstExchange = service.exchangeIdentity("sam-mgmt", "secret-plain", "code-123");

        assertThat(firstExchange.getUserId()).isEqualTo(9L);
        assertThatThrownBy(() -> service.exchangeIdentity("sam-mgmt", "secret-plain", "code-123"))
                .isInstanceOf(CustomException.class)
                .hasMessage("授权码无效或已过期");
    }

    /**
     * 验证换票成功时返回标准化 identity payload，而不是下游业务 token。
     *
     * @throws Exception JSON 构造失败时抛出
     */
    @Test
    void shouldReturnIdentityPayloadWithoutDownstreamJwtWhenExchangeSucceeds() throws Exception {
        ISsoClientService ssoClientService = mock(ISsoClientService.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        SsoAuthorizationCodeServiceImpl service = createService(ssoClientService, stringRedisTemplate);
        SsoClient persistedClient = buildPersistedClient();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("sso:auth:code:code-123")).thenReturn(buildPayloadJson());
        when(ssoClientService.getById(7L)).thenReturn(persistedClient);

        SsoAuthorizationCodePayload payload = service.exchangeIdentity("sam-mgmt", "secret-plain", "code-123");

        assertThat(payload.getExchangeId()).isEqualTo("exchange-001");
        assertThat(payload.getTraceId()).isEqualTo("trace-001");
        assertThat(payload.getClientCode()).isEqualTo("sam-mgmt");
        assertThat(payload.getUserId()).isEqualTo(9L);
        assertThat(payload.getUsername()).isEqualTo("phase2-user");
        assertThat(payload.getOrgId()).isEqualTo(18L);
        assertThat(payload.getDeptId()).isEqualTo(108L);
    }

    /**
     * 创建待测服务实例。
     *
     * @param ssoClientService 客户端服务测试桩
     * @param stringRedisTemplate Redis 测试桩
     * @return 待测服务
     */
    private SsoAuthorizationCodeServiceImpl createService(ISsoClientService ssoClientService,
                                                          StringRedisTemplate stringRedisTemplate) {
        return new SsoAuthorizationCodeServiceImpl(ssoClientService, stringRedisTemplate, new ObjectMapper());
    }

    /**
     * 构造 authorize 阶段可用的客户端。
     *
     * @param redirectUri 白名单回调地址
     * @return 最小有效客户端
     */
    private SsoClient buildAuthorizeClient(String redirectUri) {
        SsoClient ssoClient = new SsoClient();
        ssoClient.setClientId(7L);
        ssoClient.setClientCode("sam-mgmt");
        ssoClient.setRedirectUris(redirectUri);
        ssoClient.setStatus("0");
        ssoClient.setAllowPasswordLogin("Y");
        ssoClient.setAllowWxworkLogin("Y");
        return ssoClient;
    }

    /**
     * 构造持久化后的客户端实体，供 exchange 阶段校验 clientSecret 使用。
     *
     * @return 最小有效客户端
     */
    private SsoClient buildPersistedClient() {
        SsoClient ssoClient = buildAuthorizeClient("https://downstream.example.com/callback");
        ssoClient.setClientSecret(SecurityUtils.encryptPassword("secret-plain"));
        return ssoClient;
    }

    /**
     * 构造最小登录态。
     *
     * @return 登录用户
     */
    private LoginUser buildLoginUser() {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(9L);
        sysUser.setUserName("phase2-user");
        sysUser.setNickName("Phase 2 User");
        sysUser.setOrgId(18L);
        sysUser.setOrgName("Youngron");
        sysUser.setDeptId(108L);
        sysUser.setDeptName("IAM");
        return new LoginUser(sysUser, Set.of(), Set.of(), List.of());
    }

    /**
     * 构造成功换票所需的授权码载荷 JSON。
     *
     * @return JSON 文本
     * @throws Exception JSON 序列化失败时抛出
     */
    private String buildPayloadJson() throws Exception {
        SsoAuthorizationCodePayload payload = new SsoAuthorizationCodePayload();
        payload.setExchangeId("exchange-001");
        payload.setTraceId("trace-001");
        payload.setClientId(7L);
        payload.setClientCode("sam-mgmt");
        payload.setRedirectUri("https://downstream.example.com/callback");
        payload.setState("state-123");
        payload.setUserId(9L);
        payload.setUsername("phase2-user");
        payload.setNickName("Phase 2 User");
        payload.setOrgId(18L);
        payload.setOrgName("Youngron");
        payload.setDeptId(108L);
        payload.setDeptName("IAM");
        return new ObjectMapper().writeValueAsString(payload);
    }
}
