/**
 * @file 系统信息接口脱敏契约测试
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.web.controller.common;

import com.yr.common.core.redis.RedisCache;
import com.yr.framework.config.ResourcesConfig;
import com.yr.framework.config.SecurityConfig;
import com.yr.framework.interceptor.LoginFailInterceptor;
import com.yr.framework.interceptor.RepeatSubmitInterceptor;
import com.yr.framework.security.filter.JwtAuthenticationTokenFilter;
import com.yr.framework.security.handle.AuthenticationEntryPointImpl;
import com.yr.framework.security.handle.LogoutSuccessHandlerImpl;
import com.yr.framework.web.service.TokenService;
import com.yr.web.domain.DatabaseInfo;
import com.yr.web.domain.RedisInfo;
import com.yr.web.service.ServerTimeScheduleService;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 `/getSystemInfo` 只返回显式脱敏后的视图对象。
 */
@WebMvcTest(
        value = SystemInfoController.class,
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
class SystemInfoControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RedisInfo redisInfo;

    @MockBean
    private DatabaseInfo databaseInfo;

    @MockBean
    private ServerTimeScheduleService serverTimeScheduleService;

    /**
     * 供 MVC 拦截器链使用的 Redis 测试桩，避免控制器契约测试被基础设施依赖干扰。
     */
    @MockBean
    private RedisCache redisCache;

    /**
     * 登录失败拦截器测试桩，避免 `@WebMvcTest` 在启动时装配真实限流逻辑。
     */
    @MockBean
    private LoginFailInterceptor loginFailInterceptor;

    /**
     * 防重复提交拦截器测试桩，避免控制器契约测试装配抽象拦截器实现。
     */
    @MockBean
    private RepeatSubmitInterceptor repeatSubmitInterceptor;

    /**
     * MVC 资源配置测试桩，避免静态资源映射依赖真实 Spring 上下文工具类。
     */
    @MockBean
    private ResourcesConfig resourcesConfig;

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
     * JWT 过滤器依赖的 token 服务测试桩。
     */
    @MockBean
    private TokenService tokenService;

    /**
     * 验证系统信息接口不会把数据库或 Redis 配置中的敏感字段回传给前端。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldNotExposeDatabaseOrRedisSecrets() throws Exception {
        DatabaseInfo dbInfo = new DatabaseInfo();
        dbInfo.setHost("127.0.0.1");
        dbInfo.setPort(3306);
        dbInfo.setUsername("root");
        dbInfo.setPassword("Popo0222");
        dbInfo.setBasename("sso");

        RedisInfo cacheInfo = new RedisInfo();
        cacheInfo.setHost("127.0.0.1");
        cacheInfo.setPort(6379);
        cacheInfo.setDatabase("0");
        cacheInfo.setPassword("redis-secret");
        cacheInfo.setTimeout("20s");

        org.mockito.BDDMockito.given(databaseInfo.getHost()).willReturn(dbInfo.getHost());
        org.mockito.BDDMockito.given(databaseInfo.getPort()).willReturn(dbInfo.getPort());
        org.mockito.BDDMockito.given(databaseInfo.getUsername()).willReturn(dbInfo.getUsername());
        org.mockito.BDDMockito.given(databaseInfo.getPassword()).willReturn(dbInfo.getPassword());
        org.mockito.BDDMockito.given(databaseInfo.getBasename()).willReturn(dbInfo.getBasename());
        org.mockito.BDDMockito.given(redisInfo.getHost()).willReturn(cacheInfo.getHost());
        org.mockito.BDDMockito.given(redisInfo.getPort()).willReturn(cacheInfo.getPort());
        org.mockito.BDDMockito.given(redisInfo.getDatabase()).willReturn(cacheInfo.getDatabase());
        org.mockito.BDDMockito.given(redisInfo.getPassword()).willReturn(cacheInfo.getPassword());
        org.mockito.BDDMockito.given(redisInfo.getTimeout()).willReturn(cacheInfo.getTimeout());

        String systemInfoJson = mockMvc.perform(get("/getSystemInfo"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(systemInfoJson).contains("127.0.0.1");
        assertThat(systemInfoJson).doesNotContain("password");
        assertThat(systemInfoJson).doesNotContain("secret");
    }
}
