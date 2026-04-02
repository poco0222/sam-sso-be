/**
 * @file 认证接入协议授权跳转控制器契约测试
 * @author PopoY
 * @date 2026-04-02
 */
package com.yr.auth;

import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.core.redis.RedisCache;
import com.yr.framework.config.ResourcesConfig;
import com.yr.framework.config.SecurityConfig;
import com.yr.framework.security.filter.JwtAuthenticationTokenFilter;
import com.yr.framework.security.handle.AuthenticationEntryPointImpl;
import com.yr.framework.security.handle.LogoutSuccessHandlerImpl;
import com.yr.system.service.ISsoAuthorizationCodeService;
import com.yr.web.controller.auth.SsoAuthorizeController;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 锁定认证接入协议 authorize（授权跳转）入口的最小 HTTP 契约。
 */
@WebMvcTest(
        value = SsoAuthorizeController.class,
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
class SsoAuthorizeControllerContractTest {

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
     * 每个用例结束后清理安全上下文，避免登录态串用。
     */
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 验证 state 缺失时，authorize 入口会在 controller 层直接返回 400。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldRejectBlankStateWhenAuthorizing() throws Exception {
        mockMvc.perform(get("/auth/authorize")
                        .param("clientCode", "sam-mgmt")
                        .param("redirectUri", "https://downstream.example.com/callback")
                        .param("state", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(containsString("state不能为空")));
    }

    /**
     * 验证匿名用户访问 authorize 时，会被重定向到当前登录页并保留原始授权请求。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldRedirectAnonymousAuthorizeRequestToLoginPage() throws Exception {
        mockMvc.perform(get("/auth/authorize")
                        .param("clientCode", "sam-mgmt")
                        .param("redirectUri", "https://downstream.example.com/callback")
                        .param("state", "state-123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/login?redirect=")))
                .andExpect(header().string("Location", containsString("clientCode%3Dsam-mgmt")))
                .andExpect(header().string("Location", containsString("state%3Dstate-123")));
    }

    /**
     * 验证已登录用户访问 authorize 时，会按服务层返回的回调地址执行浏览器跳转。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldRedirectAuthenticatedUserToDownstreamCallback() throws Exception {
        LoginUser loginUser = setAuthenticatedUser("phase2-user");
        when(ssoAuthorizationCodeService.issueAuthorizeRedirectUrl(
                eq("sam-mgmt"),
                eq("https://downstream.example.com/callback"),
                eq("state-123"),
                eq(loginUser)
        )).thenReturn("https://downstream.example.com/callback?code=code-123&state=state-123");

        mockMvc.perform(get("/auth/authorize")
                        .param("clientCode", "sam-mgmt")
                        .param("redirectUri", "https://downstream.example.com/callback")
                        .param("state", "state-123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://downstream.example.com/callback?code=code-123&state=state-123"));
    }

    /**
     * 构造最小登录态，供授权跳转测试复用。
     *
     * @param username 当前用户名
     */
    private LoginUser setAuthenticatedUser(String username) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(9L);
        sysUser.setUserName(username);
        sysUser.setNickName("Phase 2 User");
        sysUser.setOrgId(18L);
        sysUser.setOrgName("Youngron");
        sysUser.setDeptId(108L);
        sysUser.setDeptName("IAM");
        LoginUser loginUser = new LoginUser(sysUser, java.util.Set.of(), java.util.Set.of(), List.of());

        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(loginUser, null, "ROLE_USER")
        );
        return loginUser;
    }
}
