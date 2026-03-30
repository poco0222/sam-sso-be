/**
 * @file 客户端控制器契约测试
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.web.controller.sso;

import com.yr.common.core.redis.RedisCache;
import com.yr.common.core.domain.entity.SsoClient;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.framework.config.ResourcesConfig;
import com.yr.framework.config.SecurityConfig;
import com.yr.framework.security.filter.JwtAuthenticationTokenFilter;
import com.yr.framework.security.handle.AuthenticationEntryPointImpl;
import com.yr.framework.security.handle.LogoutSuccessHandlerImpl;
import com.yr.system.domain.dto.SsoClientSecretIssueResult;
import com.yr.system.service.ISsoClientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.filter.CorsFilter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
     * 每个用例结束后清理安全上下文，避免操作人信息污染其他测试。
     */
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

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
        ssoClient.setClientSecret("phase1-secret");
        when(ssoClientService.selectSsoClientList(any(SsoClient.class))).thenReturn(List.of(ssoClient));

        mockMvc.perform(get("/sso/client/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].clientCode").value("sam-mgmt"))
                .andExpect(jsonPath("$.rows[0].clientSecret").doesNotExist());
    }

    /**
     * 验证客户端新增接口在成功时返回 200。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldCreateClient() throws Exception {
        setAuthenticatedUser("phase1-operator");
        SsoClientSecretIssueResult issueResult = new SsoClientSecretIssueResult();
        issueResult.setClientId(7L);
        issueResult.setClientCode("sam-mgmt");
        issueResult.setClientSecret("secret-created");
        when(ssoClientService.insertSsoClient(any(SsoClient.class))).thenReturn(issueResult);

        mockMvc.perform(post("/sso/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientCode":"sam-mgmt",
                                  "clientName":"SAM 管理后台",
                                  "status":"0"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.clientSecret").value("secret-created"));
    }

    /**
     * 验证新增客户端缺少 clientCode 时必须被输入校验拦截。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldRejectBlankClientCodeWhenCreatingClient() throws Exception {
        setAuthenticatedUser("phase3-operator");

        mockMvc.perform(post("/sso/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientCode":"",
                                  "clientName":"SAM 管理后台",
                                  "status":"0"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(containsString("clientCode不能为空")));
    }

    /**
     * 验证客户端修改接口在成功时返回 200。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldUpdateClient() throws Exception {
        setAuthenticatedUser("phase1-operator");
        when(ssoClientService.updateSsoClient(any(SsoClient.class))).thenReturn(1);

        mockMvc.perform(put("/sso/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId":7,
                                  "clientCode":"sam-mgmt",
                                  "clientName":"SAM 管理后台",
                                  "status":"0"
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

    /**
     * 验证客户端控制器改为构造器注入，不再依赖字段级 @Autowired。
     *
     * @throws NoSuchFieldException 字段不存在时抛出
     */
    @Test
    void shouldUseConstructorInjectionForClientService() throws NoSuchFieldException {
        Constructor<?>[] constructors = SsoClientController.class.getDeclaredConstructors();
        Field serviceField = SsoClientController.class.getDeclaredField("ssoClientService");

        assertThat(constructors).singleElement().satisfies(constructor ->
                assertThat(constructor.getParameterTypes()).containsExactly(ISsoClientService.class)
        );
        assertThat(serviceField.getAnnotation(Autowired.class))
                .as("SsoClientController 不应继续使用字段级 @Autowired")
                .isNull();
    }

    /**
     * 验证新增客户端在缺少操作人上下文时会 fail-fast，而不是静默写入 null。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldFailFastWhenOperatorContextMissing() throws Exception {
        SsoClientSecretIssueResult issueResult = new SsoClientSecretIssueResult();
        issueResult.setClientId(7L);
        issueResult.setClientCode("sam-mgmt");
        issueResult.setClientSecret("secret-created");
        when(ssoClientService.insertSsoClient(any(SsoClient.class))).thenReturn(issueResult);

        mockMvc.perform(post("/sso/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientCode":"sam-mgmt",
                                  "clientName":"SAM 管理后台",
                                  "status":"0"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("获取用户账户异常"));
    }

    /**
     * 写入最小安全上下文，让控制器能解析当前操作人。
     *
     * @param username 当前用户名
     */
    private void setAuthenticatedUser(String username) {
        SysUser currentUser = new SysUser();
        currentUser.setUserId(7L);
        currentUser.setUserName(username);
        LoginUser loginUser = new LoginUser(currentUser, java.util.Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(loginUser, null));
    }
}
