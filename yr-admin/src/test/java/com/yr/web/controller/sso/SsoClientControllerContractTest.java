/**
 * @file 客户端控制器契约测试
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.web.controller.sso;

import com.yr.common.core.redis.RedisCache;
import com.yr.common.core.domain.entity.SsoClient;
import com.yr.framework.config.ResourcesConfig;
import com.yr.framework.config.SecurityConfig;
import com.yr.framework.security.filter.JwtAuthenticationTokenFilter;
import com.yr.framework.security.handle.AuthenticationEntryPointImpl;
import com.yr.framework.security.handle.LogoutSuccessHandlerImpl;
import com.yr.system.service.ISsoClientService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 锁定客户端控制台的一期 HTTP 契约。
 */
@WebMvcTest(
        value = SsoClientController.class,
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
class SsoClientControllerContractTest {

    /** MVC 契约测试入口。 */
    @Autowired
    private MockMvc mockMvc;

    /** 客户端服务测试桩。 */
    @MockBean
    private ISsoClientService ssoClientService;

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
     * 验证客户端列表接口返回稳定的 rows 结构。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldListClients() throws Exception {
        SsoClient ssoClient = new SsoClient();
        ssoClient.setClientId(7L);
        ssoClient.setClientCode("sam-mgmt");
        ssoClient.setClientName("SAM 管理后台");
        when(ssoClientService.selectSsoClientList(any(SsoClient.class))).thenReturn(List.of(ssoClient));

        mockMvc.perform(get("/sso/client/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].clientCode").value("sam-mgmt"));
    }

    /**
     * 验证客户端新增接口在成功时返回 200。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldCreateClient() throws Exception {
        when(ssoClientService.insertSsoClient(any(SsoClient.class))).thenReturn(1);

        mockMvc.perform(post("/sso/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientCode":"sam-mgmt",
                                  "clientName":"SAM 管理后台"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 验证客户端修改接口在成功时返回 200。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldUpdateClient() throws Exception {
        when(ssoClientService.updateSsoClient(any(SsoClient.class))).thenReturn(1);

        mockMvc.perform(put("/sso/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId":7,
                                  "clientCode":"sam-mgmt",
                                  "clientName":"SAM 管理后台"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /**
     * 验证轮换密钥接口直接返回新的 clientSecret。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldRotateClientSecret() throws Exception {
        when(ssoClientService.rotateClientSecret(eq(7L))).thenReturn("secret-rotated");

        mockMvc.perform(put("/sso/client/7/rotate-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientSecret").value("secret-rotated"));
    }
}
