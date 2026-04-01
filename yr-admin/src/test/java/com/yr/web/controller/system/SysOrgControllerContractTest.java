/**
 * @file 组织状态修改控制器契约测试
 * @author PopoY
 * @date 2026-04-01
 */
package com.yr.web.controller.system;

import com.yr.common.core.domain.entity.SysOrg;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.core.redis.RedisCache;
import com.yr.framework.config.ResourcesConfig;
import com.yr.framework.config.SecurityConfig;
import com.yr.framework.security.filter.JwtAuthenticationTokenFilter;
import com.yr.framework.security.handle.AuthenticationEntryPointImpl;
import com.yr.framework.security.handle.LogoutSuccessHandlerImpl;
import com.yr.system.service.ISysOrgService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 锁定 SysOrg changeStatus 入口只能下沉最小状态写入对象。
 */
@WebMvcTest(
        value = SysOrgController.class,
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
class SysOrgControllerContractTest {

    /** MVC 契约测试入口。 */
    @Autowired
    private MockMvc mockMvc;

    /** 组织服务测试桩。 */
    @MockBean
    private ISysOrgService sysOrgService;

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
     * 每个用例结束后清理安全上下文，避免 updateBy 串用。
     */
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 验证 changeStatus 只允许下沉 orgId/status/updateBy，不能继续透传完整 SysOrg。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldOnlyForwardOrgIdAndStatusWhenChangingOrgStatus() throws Exception {
        ArgumentCaptor<SysOrg> orgCaptor = ArgumentCaptor.forClass(SysOrg.class);
        setAuthenticatedUser("org-status-operator");
        when(sysOrgService.updateOrgStatus(any(SysOrg.class))).thenReturn(1);

        mockMvc.perform(put("/system/org/changeStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orgId":7,
                                  "status":"1",
                                  "orgCode":"HACKED-CODE",
                                  "orgName":"被越权覆盖的组织名",
                                  "parentId":99,
                                  "orderNum":8,
                                  "leader":"malicious-leader",
                                  "remark":"malicious-remark",
                                  "createBy":"malicious-create-by"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(sysOrgService).updateOrgStatus(orgCaptor.capture());
        assertThat(orgCaptor.getValue().getOrgId()).isEqualTo(7L);
        assertThat(orgCaptor.getValue().getStatus()).isEqualTo("1");
        assertThat(orgCaptor.getValue().getUpdateBy()).isEqualTo("org-status-operator");
        assertThat(orgCaptor.getValue().getOrgCode()).as("状态修改不应透传 orgCode").isNull();
        assertThat(orgCaptor.getValue().getOrgName()).as("状态修改不应透传 orgName").isNull();
        assertThat(orgCaptor.getValue().getParentId()).as("状态修改不应透传 parentId").isNull();
        assertThat(orgCaptor.getValue().getOrderNum()).as("状态修改不应透传 orderNum").isNull();
        assertThat(orgCaptor.getValue().getLeader()).as("状态修改不应透传 leader").isNull();
        assertThat(orgCaptor.getValue().getRemark()).as("状态修改不应透传 remark").isNull();
        assertThat(orgCaptor.getValue().getCreateBy()).as("状态修改不应透传 createBy").isNull();
        assertThat(orgCaptor.getValue().getCreateAt()).as("状态修改不应透传 createAt").isNull();
    }

    /**
     * 验证空状态必须在 controller 层返回 400。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldRejectBlankStatusWhenChangingOrgStatus() throws Exception {
        mockMvc.perform(put("/system/org/changeStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orgId":7,
                                  "status":""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(containsString("status不能为空")));
    }

    /**
     * 验证缺少 orgId 必须在 controller 层返回 400。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldRejectMissingOrgIdWhenChangingOrgStatus() throws Exception {
        mockMvc.perform(put("/system/org/changeStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status":"1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(containsString("组织ID不能为空")));
    }

    /**
     * 验证非法状态值必须在 controller 层返回 400。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldRejectInvalidStatusWhenChangingOrgStatus() throws Exception {
        mockMvc.perform(put("/system/org/changeStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orgId":7,
                                  "status":"X"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(containsString("status只允许为0或1")));
    }

    /**
     * 写入最小登录态，让 SecurityUtils.getUsername() 返回稳定操作人。
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
