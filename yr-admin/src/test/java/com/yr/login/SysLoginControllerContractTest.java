/**
 * @file 标准登录接口契约测试
 * @author PopoY
 * @date 2026-03-10
 */
package com.yr.login;

import com.yr.common.core.redis.RedisCache;
import com.yr.framework.config.ResourcesConfig;
import com.yr.framework.config.SecurityConfig;
import com.yr.framework.security.filter.JwtAuthenticationTokenFilter;
import com.yr.framework.security.handle.AuthenticationEntryPointImpl;
import com.yr.framework.security.handle.LogoutSuccessHandlerImpl;
import com.yr.framework.web.service.SysLoginService;
import com.yr.framework.web.service.SysPermissionService;
import com.yr.framework.web.service.TokenService;
import com.yr.web.service.PhaseOneConsoleRouteService;
import com.yr.web.controller.system.SysLoginController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.filter.CorsFilter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 `/login` 在认证成功时返回稳定的 token 字段结构。
 */
@WebMvcTest(
        value = SysLoginController.class,
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
class SysLoginControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SysLoginService loginService;

    @MockBean
    private PhaseOneConsoleRouteService routeService;

    @MockBean
    private SysPermissionService permissionService;

    @MockBean
    private TokenService tokenService;

    /**
     * 供 MVC 拦截器链使用的 Redis 测试桩，避免控制器契约测试被基础设施依赖干扰。
     */
    @MockBean
    private RedisCache redisCache;

    /**
     * 安全配置依赖的用户明细服务测试桩。
     */
    @MockBean
    private UserDetailsService userDetailsService;

    /**
     * 安全配置依赖的未认证处理器测试桩。
     */
    @MockBean
    private AuthenticationEntryPointImpl authenticationEntryPoint;

    /**
     * 安全配置依赖的退出处理器测试桩。
     */
    @MockBean
    private LogoutSuccessHandlerImpl logoutSuccessHandler;

    /**
     * 安全配置依赖的 JWT 过滤器测试桩。
     */
    @MockBean
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    /**
     * 安全配置依赖的跨域过滤器测试桩。
     */
    @MockBean
    private CorsFilter corsFilter;

    /**
     * MVC 资源配置测试桩，避免静态资源映射依赖真实 Spring 上下文工具类。
     */
    @MockBean
    private ResourcesConfig resourcesConfig;

    /**
     * 验证登录成功时响应体直接返回 token，避免升级后接口结构漂移。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldReturnTokenWhenLoginSucceeds() throws Exception {
        when(loginService.login(eq("admin"), anyString(), any(), any(), eq("mgmt")))
                .thenReturn("token-123");

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"cipher\",\"platform\":\"mgmt\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-123"));
    }

    /**
     * 验证一期后端不再保留 `/loginByFree` 免登录入口，避免 legacy 登录旁路继续暴露。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldNotExposeLegacyFreeLoginEndpoint() throws Exception {
        mockMvc.perform(post("/loginByFree")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"admin\"}"))
                .andExpect(status().isNotFound());
    }

    /**
     * 验证一期后端不再保留 `/loginMobile` 登录入口，避免重复认证面继续存在。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldNotExposeLegacyMobileLoginEndpoint() throws Exception {
        mockMvc.perform(post("/loginMobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"cipher\"}"))
                .andExpect(status().isNotFound());
    }

    /**
     * 验证一期后端不再保留 legacy `/sso/login` 单向登录入口。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldNotExposeLegacySsoLoginEndpoint() throws Exception {
        mockMvc.perform(get("/sso/login")
                        .param("userAccount", "admin")
                        .param("userName", "管理员")
                        .param("timeStamp", "123456")
                        .param("token", "legacy-token"))
                .andExpect(status().isNotFound());
    }
}
