/**
 * @file 企业微信登录接口契约测试
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.login;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yr.framework.web.service.SysLoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 锁定企业微信登录控制器的一期接口契约。
 */
class WxworkLoginControllerContractTest {

    /** JSON 消息转换器使用的对象映射器。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 企业微信登录控制器依赖的登录服务测试桩。 */
    private final SysLoginService loginService = mock(SysLoginService.class);

    /** 控制器契约测试入口。 */
    private MockMvc mockMvc;

    /**
     * 初始化待测控制器；如果控制器尚未落地，就让测试明确红灯。
     */
    @BeforeEach
    void setUp() {
        Object controller = instantiateWxworkAuthController();
        ReflectionTestUtils.setField(controller, "loginService", loginService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    /**
     * 验证企业微信登录成功时直接返回 token 字段，避免一期 auth surface 漂移。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldReturnTokenWhenWxworkLoginSucceeds() throws Exception {
        when(loginService.loginByWxworkCode("wx-code-123", "state-123")).thenReturn("token-123");

        mockMvc.perform(post("/auth/wxwork/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"wx-code-123\",\"state\":\"state-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-123"));
    }

    /**
     * 验证预登录接口会返回 authorizeUrl，避免前端自行拼企业微信授权参数。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldReturnAuthorizeUrlWhenWxworkPreLoginSucceeds() throws Exception {
        when(loginService.buildWxworkPreLoginUrl()).thenReturn("https://open.weixin.qq.com/connect/oauth2/authorize?state=state-123#wechat_redirect");

        mockMvc.perform(get("/auth/wxwork/pre-login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizeUrl").value("https://open.weixin.qq.com/connect/oauth2/authorize?state=state-123#wechat_redirect"));
    }

    /**
     * 通过反射实例化目标控制器，让缺失实现时也能给出清晰的契约红灯。
     *
     * @return 控制器实例
     */
    private Object instantiateWxworkAuthController() {
        try {
            Class<?> controllerType = Class.forName("com.yr.web.controller.auth.WxworkAuthController");
            return controllerType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("WxworkAuthController 尚未实现", ex);
        }
    }
}
