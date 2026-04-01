/**
 * @file 企业微信登录 state 与日志脱敏契约测试
 * @author PopoY
 * @date 2026-03-31
 */
package com.yr.framework.web.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yr.common.core.redis.RedisCache;
import com.yr.common.exception.CustomException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定企业微信登录链路的 state 闭环与敏感 URL 日志脱敏行为。
 */
class SysLoginServiceWxworkStateContractTest {

    /** 企业微信 corpId 测试值。 */
    private static final String CORP_ID = "corp-id-123";

    /** 企业微信 agentId 测试值。 */
    private static final String AGENT_ID = "100001";

    /** 企业微信 redirectUri 测试值。 */
    private static final String REDIRECT_URI = "https://sso.example.com/auth/wxwork/callback";

    /** 企业微信 corpSecret 测试值。 */
    private static final String CORP_SECRET = "corp-secret-123";

    /**
     * 每个用例后清理 RequestContext，避免 ServletUtils 串用，同时移除日志捕获器。
     */
    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * 验证 pre-login 生成的 state 会先落缓存，再拼入授权地址。
     */
    @Test
    void shouldPersistStateWhenBuildingWxworkPreLoginUrl() {
        SysLoginService service = createService();
        ArgumentCaptor<String> stateKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> stateValueCaptor = ArgumentCaptor.forClass(String.class);

        String authorizeUrl = service.buildWxworkPreLoginUrl();
        String state = UriComponentsBuilder.fromUriString(authorizeUrl.replace("#wechat_redirect", ""))
                .build(true)
                .getQueryParams()
                .getFirst("state");

        verify(stringValueOperations).set(stateKeyCaptor.capture(), stateValueCaptor.capture(), eq(300L), eq(TimeUnit.SECONDS));
        assertThat(state).isNotBlank();
        assertThat(stateKeyCaptor.getValue()).isEqualTo("wxwork:oauth_state:" + state);
        assertThat(stateValueCaptor.getValue()).isEqualTo(state);
        assertThat(authorizeUrl).contains("state=" + state);
    }

    /**
     * 验证缺失 state 时企业微信登录必须返回受控错误。
     */
    @Test
    void shouldRejectWxworkLoginWhenStateIsMissing() {
        SysLoginService service = createService();

        assertThatThrownBy(() -> service.loginByWxworkCode("wx-code-123", null))
                .isInstanceOf(CustomException.class)
                .hasMessage("授权状态无效或已过期");
    }

    /**
     * 验证 state 不匹配时企业微信登录必须返回受控错误。
     */
    @Test
    void shouldRejectWxworkLoginWhenStateDoesNotMatchCache() {
        SysLoginService service = createService();
        String state = "state-123";
        String cacheKey = "wxwork:oauth_state:" + state;

        when(stringValueOperations.getAndDelete(cacheKey)).thenReturn("state-456");

        assertThatThrownBy(() -> service.loginByWxworkCode("wx-code-123", state))
                .isInstanceOf(CustomException.class)
                .hasMessage("授权状态无效或已过期");
    }

    /**
     * 验证 state 在通过校验后即被消费；即使后续登录失败也不能重放。
     */
    @Test
    void shouldConsumeStateWhenWxworkLoginFailsAfterValidation() {
        SysLoginService service = createService();
        String state = "state-123";
        String cacheKey = "wxwork:oauth_state:" + state;

        when(stringValueOperations.getAndDelete(cacheKey)).thenReturn(state);

        assertThatThrownBy(() -> service.loginByWxworkCode(" ", state))
                .isInstanceOf(CustomException.class)
                .hasMessage("授权码无效或已过期");
        verify(stringValueOperations).getAndDelete(cacheKey);
    }

    /**
     * 验证获取 access_token 失败时日志不会泄露 corpsecret。
     */
    @Test
    void shouldRedactCorpSecretWhenAccessTokenRequestFails() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        SysLoginService service = createService(restTemplate);
        ListAppender<ILoggingEvent> listAppender = attachLogAppender();

        when(stringValueOperations.getAndDelete("wxwork:oauth_state:state-123")).thenReturn("state-123");
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(new RestClientException("boom"));

        assertThatThrownBy(() -> service.loginByWxworkCode("wx-code-123", "state-123"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("获取企业微信 access_token失败");

        String logs = joinLogs(listAppender.list);
        assertThat(logs).contains("获取企业微信 access_token失败");
        assertThat(logs).doesNotContain(CORP_SECRET);
        assertThat(logs).contains("corpsecret=[REDACTED]");

        detachLogAppender(listAppender);
    }

    /**
     * 验证获取用户身份失败时日志不会泄露 access_token。
     */
    @Test
    void shouldRedactAccessTokenWhenUserInfoRequestFails() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        SysLoginService service = createService(restTemplate);
        ListAppender<ILoggingEvent> listAppender = attachLogAppender();

        when(stringValueOperations.getAndDelete("wxwork:oauth_state:state-123")).thenReturn("state-123");
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("{\"errcode\":0,\"access_token\":\"access-token-123\",\"expires_in\":7200}")
                .thenThrow(new RestClientException("boom"));

        assertThatThrownBy(() -> service.loginByWxworkCode("wx-code-123", "state-123"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("获取企业微信用户身份失败");

        String logs = joinLogs(listAppender.list);
        assertThat(logs).contains("获取企业微信用户身份失败");
        assertThat(logs).doesNotContain("access-token-123");
        assertThat(logs).doesNotContain("wx-code-123");
        assertThat(logs).contains("access_token=[REDACTED]");
        assertThat(logs).contains("code=[REDACTED]");

        detachLogAppender(listAppender);
    }

    /**
     * 验证企业微信 HTTP 客户端会设置显式 timeout，并在同一次登录流程中复用已构建实例。
     */
    @Test
    void shouldConfigureTimeoutsAndReuseRestTemplateAcrossWxworkRequests() {
        SysLoginService service = new SysLoginService();
        RestTemplate restTemplate = mock(RestTemplate.class);
        RestTemplateBuilder restTemplateBuilder = mock(RestTemplateBuilder.class);
        String accessTokenUrl = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=corp-id-123&corpsecret=corp-secret-123";
        String userInfoUrl = "https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo?access_token=access-token-123&code=wx-code-123";

        when(restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(5))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.setReadTimeout(Duration.ofSeconds(5))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("{\"errcode\":0,\"access_token\":\"access-token-123\",\"expires_in\":7200}")
                .thenReturn("{\"errcode\":0,\"userid\":\"wx-user-1\"}");
        ReflectionTestUtils.setField(service, "restTemplateBuilder", restTemplateBuilder);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());

        ReflectionTestUtils.invokeMethod(service, "requestWxworkJson", accessTokenUrl, "获取企业微信 access_token");
        ReflectionTestUtils.invokeMethod(service, "requestWxworkJson", userInfoUrl, "获取企业微信用户身份");

        verify(restTemplateBuilder).setConnectTimeout(Duration.ofSeconds(5));
        verify(restTemplateBuilder).setReadTimeout(Duration.ofSeconds(5));
        verify(restTemplateBuilder, times(1)).build();
        verify(restTemplate, times(2)).getForObject(anyString(), eq(String.class));
    }

    /**
     * 创建待测服务，并注入默认 HTTP 客户端桩。
     *
     * @return 待测服务
     */
    private SysLoginService createService() {
        return createService(mock(RestTemplate.class));
    }

    /**
     * 创建待测服务并注入给定的 RestTemplate 桩。
     *
     * @param restTemplate HTTP 客户端测试桩
     * @return 待测服务
     */
    private SysLoginService createService(RestTemplate restTemplate) {
        SysLoginService service = new SysLoginService();
        RestTemplateBuilder restTemplateBuilder = mock(RestTemplateBuilder.class);
        Environment environment = mock(Environment.class);

        when(restTemplateBuilder.setConnectTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.setReadTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        when(stringRedisTemplate.opsForValue()).thenReturn(stringValueOperations);
        when(environment.getProperty("wxwork.corp-id")).thenReturn(CORP_ID);
        when(environment.getProperty("wxwork.agent-id")).thenReturn(AGENT_ID);
        when(environment.getProperty("wxwork.redirect-uri")).thenReturn(REDIRECT_URI);
        when(environment.getProperty("wxwork.corp-secret")).thenReturn(CORP_SECRET);

        ReflectionTestUtils.setField(service, "redisCache", redisCache);
        ReflectionTestUtils.setField(service, "userService", userService);
        ReflectionTestUtils.setField(service, "userDetailsService", userDetailsService);
        ReflectionTestUtils.setField(service, "tokenService", tokenService);
        ReflectionTestUtils.setField(service, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(service, "restTemplateBuilder", restTemplateBuilder);
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(service, "environment", environment);

        // 登录成功链路在本测试类里不是重点，只需要有稳定的 request 上下文避免登录信息记录报空。
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        return service;
    }

    /** Redis 缓存测试桩。 */
    private final RedisCache redisCache = mock(RedisCache.class);

    /** StringRedisTemplate 测试桩。 */
    private final StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);

    /** 字符串 ValueOperations（值操作）测试桩。 */
    private final ValueOperations<String, String> stringValueOperations = createStringValueOperationsMock();

    /** 用户服务测试桩。 */
    private final com.yr.system.service.ISysUserService userService = mock(com.yr.system.service.ISysUserService.class);

    /** 用户明细服务测试桩。 */
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);

    /** Token 服务测试桩。 */
    private final TokenService tokenService = mock(TokenService.class);

    /**
     * 创建带泛型的 ValueOperations 测试桩，避免字段初始化处散落未经检查的 mock 转换。
     *
     * @return ValueOperations 测试桩
     */
    @SuppressWarnings("unchecked")
    private ValueOperations<String, String> createStringValueOperationsMock() {
        return mock(ValueOperations.class);
    }

    /**
     * 挂接 SysLoginService 的日志捕获器。
     *
     * @return ListAppender 日志捕获器
     */
    private ListAppender<ILoggingEvent> attachLogAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(SysLoginService.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        return listAppender;
    }

    /**
     * 卸载日志捕获器，避免影响后续用例。
     *
     * @param listAppender 当前日志捕获器
     */
    private void detachLogAppender(ListAppender<ILoggingEvent> listAppender) {
        Logger logger = (Logger) LoggerFactory.getLogger(SysLoginService.class);
        logger.detachAppender(listAppender);
        listAppender.stop();
    }

    /**
     * 拼接日志文本，便于集中断言。
     *
     * @param events 日志事件
     * @return 拼接后的日志文本
     */
    private String joinLogs(List<ILoggingEvent> events) {
        return events.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (left, right) -> left + "\n" + right);
    }
}
