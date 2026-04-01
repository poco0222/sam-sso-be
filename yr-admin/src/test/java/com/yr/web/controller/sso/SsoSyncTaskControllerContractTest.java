/**
 * @file 同步任务控制器契约测试
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.web.controller.sso;

import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.core.redis.RedisCache;
import com.yr.framework.config.ResourcesConfig;
import com.yr.framework.config.SecurityConfig;
import com.yr.framework.security.filter.JwtAuthenticationTokenFilter;
import com.yr.framework.security.handle.AuthenticationEntryPointImpl;
import com.yr.framework.security.handle.LogoutSuccessHandlerImpl;
import com.yr.system.service.ISsoSyncTaskService;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.filter.CorsFilter;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
     * 每个用例后清理安全上下文，避免操作人串用。
     */
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

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
        setAuthenticatedUser("phase5-operator");
        when(ssoSyncTaskService.initImportTask(any(SsoSyncTask.class))).thenReturn(task);

        mockMvc.perform(post("/sso/sync-task/init-import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(11));
    }

    /**
     * 验证初始化导入入口不会透传服务端管理字段。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldNotForwardServerManagedFieldsWhenInitImportingTask() throws Exception {
        SsoSyncTask task = new SsoSyncTask();
        task.setTaskId(11L);
        org.mockito.ArgumentCaptor<SsoSyncTask> taskCaptor = org.mockito.ArgumentCaptor.forClass(SsoSyncTask.class);
        setAuthenticatedUser("phase5-operator");
        when(ssoSyncTaskService.initImportTask(any(SsoSyncTask.class))).thenReturn(task);

        mockMvc.perform(post("/sso/sync-task/init-import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taskType":"INIT_IMPORT",
                                  "status":"SUCCESS",
                                  "batchNo":"INIT-FROM-CLIENT",
                                  "payloadJson":"{\\"force\\":true}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(11));

        verify(ssoSyncTaskService).initImportTask(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getTargetClientCode()).as("initImport 不应允许客户端自带 targetClientCode").isNull();
        assertThat(taskCaptor.getValue().getSourceBatchNo()).as("initImport 不应允许客户端自带 sourceBatchNo").isNull();
        assertThat(taskCaptor.getValue().getTaskType()).as("initImport 不应允许客户端自带 taskType").isNull();
        assertThat(taskCaptor.getValue().getStatus()).as("initImport 不应允许客户端自带 status").isNull();
        assertThat(taskCaptor.getValue().getBatchNo()).as("initImport 不应允许客户端自带 batchNo").isNull();
        assertThat(taskCaptor.getValue().getPayloadJson()).as("initImport 不应允许客户端自带 payloadJson").isNull();
    }

    /**
     * 验证手工触发分发任务接口返回任务标识。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldCreateDistributionTask() throws Exception {
        SsoSyncTask task = new SsoSyncTask();
        task.setTaskId(21L);
        setAuthenticatedUser("phase5-operator");
        when(ssoSyncTaskService.distributionTask(any(SsoSyncTask.class))).thenReturn(task);

        mockMvc.perform(post("/sso/sync-task/distribution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetClientCode":"sam-mgmt"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(21));
    }

    /**
     * 验证分发任务入口不会透传服务端管理字段。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldNotForwardServerManagedFieldsWhenCreatingDistributionTask() throws Exception {
        SsoSyncTask task = new SsoSyncTask();
        task.setTaskId(21L);
        org.mockito.ArgumentCaptor<SsoSyncTask> taskCaptor = org.mockito.ArgumentCaptor.forClass(SsoSyncTask.class);
        setAuthenticatedUser("phase5-operator");
        when(ssoSyncTaskService.distributionTask(any(SsoSyncTask.class))).thenReturn(task);

        mockMvc.perform(post("/sso/sync-task/distribution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetClientCode":"sam-mgmt",
                                  "taskType":"DISTRIBUTION",
                                  "status":"SUCCESS",
                                  "batchNo":"DIST-FROM-CLIENT",
                                  "payloadJson":"{\\"force\\":true}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(21));

        verify(ssoSyncTaskService).distributionTask(taskCaptor.capture());
        assertThat(taskCaptor.getValue().getTargetClientCode()).isEqualTo("sam-mgmt");
        assertThat(taskCaptor.getValue().getTaskType()).as("distribution 不应允许客户端自带 taskType").isNull();
        assertThat(taskCaptor.getValue().getStatus()).as("distribution 不应允许客户端自带 status").isNull();
        assertThat(taskCaptor.getValue().getBatchNo()).as("distribution 不应允许客户端自带 batchNo").isNull();
        assertThat(taskCaptor.getValue().getPayloadJson()).as("distribution 不应允许客户端自带 payloadJson").isNull();
    }

    /**
     * 验证分发任务缺少目标客户端编码时必须在 controller 层返回 400。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldRejectBlankTargetClientCodeWhenCreatingDistributionTask() throws Exception {
        mockMvc.perform(post("/sso/sync-task/distribution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetClientCode":""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("targetClientCode不能为空"));
    }

    /**
     * 验证创建同步任务在缺少操作人上下文时会 fail-fast，而不是静默写入空 createBy。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldFailFastWhenOperatorContextMissingForDistributionTask() throws Exception {
        SsoSyncTask task = new SsoSyncTask();
        task.setTaskId(21L);
        when(ssoSyncTaskService.distributionTask(any(SsoSyncTask.class))).thenReturn(task);

        mockMvc.perform(post("/sso/sync-task/distribution")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetClientCode":"sam-mgmt"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value("获取用户账户异常"));
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

    /**
     * 验证任务详情接口返回 item 明细与统计字段，供 console 直接渲染。
     *
     * @throws Exception MockMvc 调用失败时抛出
     */
    @Test
    void shouldReturnTaskDetailWithItemList() throws Exception {
        SsoSyncTask task = new SsoSyncTask();
        task.setTaskId(11L);
        task.setStatus("PARTIAL_SUCCESS");
        task.setTotalItemCount(5L);
        task.setSuccessItemCount(4L);
        task.setFailedItemCount(1L);

        com.yr.common.core.domain.entity.SsoSyncTaskItem item = new com.yr.common.core.domain.entity.SsoSyncTaskItem();
        item.setItemId(101L);
        item.setEntityType("user");
        item.setStatus("FAILED");
        item.setSourceId("2001");
        item.setMsgKey("DIST:11:user:2001");
        attachMessageLog(item);
        task.setItemList(List.of(item));

        when(ssoSyncTaskService.selectSsoSyncTaskById(eq(11L))).thenReturn(task);

        mockMvc.perform(get("/sso/sync-task/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value(11))
                .andExpect(jsonPath("$.data.totalItemCount").value(5))
                .andExpect(jsonPath("$.data.failedItemCount").value(1))
                .andExpect(jsonPath("$.data.itemList[0].itemId").value(101))
                .andExpect(jsonPath("$.data.itemList[0].entityType").value("user"))
                .andExpect(jsonPath("$.data.itemList[0].status").value("FAILED"))
                .andExpect(jsonPath("$.data.itemList[0].msgKey").value("DIST:11:user:2001"))
                .andExpect(jsonPath("$.data.itemList[0].messageLog.sendStatus").value(1))
                .andExpect(jsonPath("$.data.itemList[0].messageLog.topic").value("sso-identity-distribution"));
    }

    /**
     * 通过反射挂接 messageLog 视图，保证控制器契约测试在字段缺失时先红灯。
     *
     * @param item 同步任务条目
     */
    private void attachMessageLog(com.yr.common.core.domain.entity.SsoSyncTaskItem item) {
        Field messageLogField = ReflectionUtils.findField(item.getClass(), "messageLog");
        if (messageLogField == null) {
            throw new IllegalStateException("SsoSyncTaskItem.messageLog 字段尚未落地");
        }
        try {
            messageLogField.setAccessible(true);
            Object messageLog = messageLogField.getType().getDeclaredConstructor().newInstance();
            ReflectionTestUtils.setField(messageLog, "sendStatus", 1);
            ReflectionTestUtils.setField(messageLog, "topic", "sso-identity-distribution");
            messageLogField.set(item, messageLog);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("构造 SsoSyncTaskItem.messageLog 测试视图失败", exception);
        }
    }

    /**
     * 写入最小登录态，让控制器能解析当前操作人。
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
