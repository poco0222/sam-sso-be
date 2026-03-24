/**
 * @file 同步任务控制器契约测试
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.web.controller.sso;

import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.core.redis.RedisCache;
import com.yr.framework.config.ResourcesConfig;
import com.yr.framework.config.SecurityConfig;
import com.yr.framework.security.filter.JwtAuthenticationTokenFilter;
import com.yr.framework.security.handle.AuthenticationEntryPointImpl;
import com.yr.framework.security.handle.LogoutSuccessHandlerImpl;
import com.yr.system.service.ISsoSyncTaskService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 锁定同步任务控制台的一期 HTTP 契约。
 */
@WebMvcTest(
        value = SsoSyncTaskController.class,
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
class SsoSyncTaskControllerContractTest {

    /** MVC 契约测试入口。 */
    @Autowired
    private MockMvc mockMvc;

    /** 同步任务服务测试桩。 */
    @MockBean
    private ISsoSyncTaskService ssoSyncTaskService;

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
     * 验证同步任务列表接口返回稳定的 rows 结构。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldListSyncTasks() throws Exception {
        SsoSyncTask ssoSyncTask = new SsoSyncTask();
        ssoSyncTask.setTaskId(11L);
        ssoSyncTask.setTaskType("INIT_IMPORT");
        when(ssoSyncTaskService.selectSsoSyncTaskList(any(SsoSyncTask.class))).thenReturn(List.of(ssoSyncTask));

        mockMvc.perform(get("/sso/sync-task/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].taskType").value("INIT_IMPORT"));
    }

    /**
     * 验证初始化导入任务接口返回任务标识。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldInitImportTask() throws Exception {
        SsoSyncTask task = new SsoSyncTask();
        task.setTaskId(11L);
        when(ssoSyncTaskService.initImportTask(any(SsoSyncTask.class))).thenReturn(task);

        mockMvc.perform(post("/sso/sync-task/init-import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetClientCode":"sam-mgmt",
                                  "taskType":"INIT_IMPORT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(11));
    }

    /**
     * 验证重试任务接口返回任务标识。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldRetryTask() throws Exception {
        SsoSyncTask task = new SsoSyncTask();
        task.setTaskId(11L);
        when(ssoSyncTaskService.retryTask(eq(11L))).thenReturn(task);

        mockMvc.perform(post("/sso/sync-task/11/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(11));
    }

    /**
     * 验证补偿任务接口返回任务标识。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldCompensateTask() throws Exception {
        SsoSyncTask task = new SsoSyncTask();
        task.setTaskId(11L);
        when(ssoSyncTaskService.compensateTask(eq(11L))).thenReturn(task);

        mockMvc.perform(post("/sso/sync-task/11/compensate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(11));
    }
}
