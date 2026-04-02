/**
 * @file 认证接入协议换票控制器契约测试
 * @author PopoY
 * @date 2026-04-02
 */
package com.yr.auth;

import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.redis.RedisCache;
import com.yr.framework.config.ResourcesConfig;
import com.yr.framework.config.SecurityConfig;
import com.yr.framework.security.filter.JwtAuthenticationTokenFilter;
import com.yr.framework.security.handle.AuthenticationEntryPointImpl;
import com.yr.framework.security.handle.LogoutSuccessHandlerImpl;
import com.yr.system.domain.dto.SsoAuthorizationCodePayload;
import com.yr.system.service.ISsoAuthorizationCodeService;
import com.yr.web.controller.auth.SsoCodeExchangeController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.filter.CorsFilter;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 锁定认证接入协议 exchange（换票）入口的最小 HTTP 契约。
 */
@WebMvcTest(
        value = SsoCodeExchangeController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        },
        excludeFilters = {
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ResourcesConfig.class),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class SsoCodeExchangeControllerContractTest {

    /** MVC 契约测试入口。 */
    @Autowired
    private MockMvc mockMvc;

    /** 认证接入协议服务测试桩。 */
    @MockBean
    private ISsoAuthorizationCodeService ssoAuthorizationCodeService;

    /** 以下均为安全链路所需测试桩，避免控制器契约测试被基础设施依赖干扰。 */
    @MockBean
    private RedisCache redisCache;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private AuthenticationEntryPointImpl authenticationEntryPoint;

    @MockBean
    private LogoutSuccessHandlerImpl logoutSuccessHandler;

    @MockBean
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    @MockBean
    private CorsFilter corsFilter;

    @MockBean
    private ResourcesConfig resourcesConfig;

    /**
     * 验证 clientSecret 缺失时，exchange 入口会在 controller 层直接返回 400。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldRejectBlankClientSecretWhenExchangingCode() throws Exception {
        mockMvc.perform(post("/auth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientCode":"sam-mgmt",
                                  "clientSecret":"",
                                  "code":"code-123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(containsString("clientSecret不能为空")));
    }

    /**
     * 验证换票成功时返回标准化 identity payload，而不是下游业务 token。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldReturnIdentityPayloadWithoutBusinessJwtWhenExchangeSucceeds() throws Exception {
        SsoAuthorizationCodePayload payload = new SsoAuthorizationCodePayload();
        payload.setExchangeId("exchange-001");
        payload.setTraceId("trace-001");
        payload.setClientCode("sam-mgmt");
        payload.setUserId(9L);
        payload.setUsername("phase2-user");
        payload.setNickName("Phase 2 User");
        payload.setOrgId(18L);
        payload.setOrgName("Youngron");
        payload.setDeptId(108L);
        payload.setDeptName("IAM");

        when(ssoAuthorizationCodeService.exchangeIdentity(
                eq("sam-mgmt"),
                eq("secret-plain"),
                eq("code-123")
        )).thenReturn(payload);

        mockMvc.perform(post("/auth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientCode":"sam-mgmt",
                                  "clientSecret":"secret-plain",
                                  "code":"code-123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exchangeId").value("exchange-001"))
                .andExpect(jsonPath("$.data.traceId").value("trace-001"))
                .andExpect(jsonPath("$.data.clientCode").value("sam-mgmt"))
                .andExpect(jsonPath("$.data.userId").value(9L))
                .andExpect(jsonPath("$.data.username").value("phase2-user"))
                .andExpect(jsonPath("$.data.orgId").value(18L))
                .andExpect(jsonPath("$.data.deptId").value(108L))
                .andExpect(jsonPath("$.data.token").doesNotExist());
    }
}
