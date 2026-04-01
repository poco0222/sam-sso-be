/**
 * @file 用户组织关联控制器契约测试
 * @author PopoY
 * @date 2026-04-01
 */
package com.yr.web.controller.system;

import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.core.redis.RedisCache;
import com.yr.framework.config.ResourcesConfig;
import com.yr.framework.config.SecurityConfig;
import com.yr.framework.security.filter.JwtAuthenticationTokenFilter;
import com.yr.framework.security.handle.AuthenticationEntryPointImpl;
import com.yr.framework.security.handle.LogoutSuccessHandlerImpl;
import com.yr.system.domain.entity.SysUserOrg;
import com.yr.system.service.ISysUserOrgService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.filter.CorsFilter;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 锁定 SysUserOrg 的新增与启停入口只能接收最小写对象。
 */
@WebMvcTest(
        value = SysUserOrgController.class,
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
class SysUserOrgControllerContractTest {

    /** MVC 契约测试入口。 */
    @Autowired
    private MockMvc mockMvc;

    /** 用户组织服务测试桩。 */
    @MockBean
    private ISysUserOrgService sysUserOrgService;

    /** 以下均为安全链路所需测试桩，避免契约测试被基础设施依赖干扰。 */
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
     * 每个用例结束后清理安全上下文，避免污染其它测试。
     */
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 验证新增用户组织时 controller 只下沉 userId/orgId。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldOnlyForwardUserIdAndOrgIdWhenCreatingUserOrg() throws Exception {
        ArgumentCaptor<SysUserOrg> relationCaptor = ArgumentCaptor.forClass(SysUserOrg.class);
        setAuthenticatedUser("user-org-operator");

        mockMvc.perform(post("/system/user-org")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId":10,
                                  "orgId":20,
                                  "isDefault":1,
                                  "enabled":0,
                                  "createBy":99,
                                  "objectVersionNumber":7
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysUserOrgService).addSysUserOrg(relationCaptor.capture());
        assertThat(relationCaptor.getValue().getUserId()).isEqualTo(10L);
        assertThat(relationCaptor.getValue().getOrgId()).isEqualTo(20L);
        assertThat(relationCaptor.getValue().getIsDefault()).as("新增入口不应透传 isDefault").isNull();
        assertThat(relationCaptor.getValue().getEnabled()).as("新增入口不应透传 enabled").isNull();
        assertThat(relationCaptor.getValue().getCreateBy()).as("新增入口不应透传 createBy").isNull();
        assertThat(relationCaptor.getValue().getObjectVersionNumber()).as("新增入口不应透传版本字段").isNull();
    }

    /**
     * 验证新增用户组织缺少 userId 时必须返回 400。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldRejectMissingUserIdWhenCreatingUserOrg() throws Exception {
        mockMvc.perform(post("/system/user-org")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orgId":20
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(containsString("用户ID不能为空")));
    }

    /**
     * 验证新增用户组织缺少 orgId 时必须返回 400。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldRejectMissingOrgIdWhenCreatingUserOrg() throws Exception {
        mockMvc.perform(post("/system/user-org")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId":10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(containsString("组织ID不能为空")));
    }

    /**
     * 验证 changeEnabled 只允许下沉 id/enabled。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldOnlyForwardIdAndEnabledWhenChangingUserOrgEnabled() throws Exception {
        when(sysUserOrgService.changeEnabledById(7L, 1)).thenReturn(1);

        mockMvc.perform(put("/system/user-org/changeEnabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id":7,
                                  "enabled":1,
                                  "userId":10,
                                  "orgId":20,
                                  "isDefault":1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysUserOrgService).changeEnabledById(7L, 1);
    }

    /**
     * 验证 changeEnabled 缺少 id 时必须返回 400。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldRejectMissingIdWhenChangingUserOrgEnabled() throws Exception {
        mockMvc.perform(put("/system/user-org/changeEnabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled":1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(containsString("主键ID不能为空")));
    }

    /**
     * 验证 changeEnabled 非法 enabled 值必须返回 400。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldRejectInvalidEnabledWhenChangingUserOrgEnabled() throws Exception {
        mockMvc.perform(put("/system/user-org/changeEnabled")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id":7,
                                  "enabled":2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(containsString("enabled只允许为0或1")));
    }

    /**
     * 写入最小登录态，便于后续需要 SecurityUtils 时复用。
     *
     * @param username 当前用户名
     */
    private void setAuthenticatedUser(String username) {
        SysUser currentUser = new SysUser();
        currentUser.setUserId(99L);
        currentUser.setUserName(username);
        LoginUser loginUser = new LoginUser(currentUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList())
        );
    }
}
