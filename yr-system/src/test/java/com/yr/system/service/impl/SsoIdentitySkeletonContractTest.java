/**
 * @file 锁定身份中心一期 client 与 sync-task skeleton 契约
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SsoClient;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.system.domain.dto.SsoIdentityImportExecutionResult;
import com.yr.system.domain.dto.SsoSyncTaskExecutionResult;
import com.yr.system.service.ISsoClientService;
import com.yr.system.service.ISsoIdentityDistributionService;
import com.yr.system.service.ISsoIdentityImportService;
import com.yr.system.service.ISsoSyncTaskItemService;
import com.yr.system.service.support.SsoSyncTaskFailureRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 身份中心一期 skeleton 契约测试。
 */
class SsoIdentitySkeletonContractTest {

    /**
     * 验证客户端实体只保留一期允许的两类登录开关字段。
     */
    @Test
    void shouldKeepOnlyPhaseOneLoginSwitchesOnClientEntity() {
        List<String> fieldNames = Arrays.stream(SsoClient.class.getDeclaredFields())
                .map(Field::getName)
                .toList();

        assertThat(fieldNames).contains("allowPasswordLogin", "allowWxworkLogin");
        assertThat(fieldNames).doesNotContain("oauthProviderType");
        assertThat(fieldNames.stream()
                .filter(fieldName -> fieldName.startsWith("allow") && fieldName.endsWith("Login"))
                .toList()).containsExactlyInAnyOrder("allowPasswordLogin", "allowWxworkLogin");
    }

    /**
     * 验证同步任务实体显式保留一期允许的任务类型与状态常量。
     */
    @Test
    void shouldExposeSupportedTaskTypesAndStatuses() {
        List<String> fieldNames = Arrays.stream(SsoSyncTask.class.getDeclaredFields())
                .map(Field::getName)
                .toList();

        assertThat(fieldNames).contains(
                "TASK_TYPE_INIT_IMPORT",
                "TASK_TYPE_DISTRIBUTION",
                "TASK_TYPE_COMPENSATION",
                "STATUS_PENDING",
                "STATUS_RUNNING",
                "STATUS_SUCCESS",
                "STATUS_FAILED",
                "STATUS_PARTIAL_SUCCESS"
        );
    }

    /**
     * 验证 INIT_IMPORT 任务会写入旧 ID 继承与主权转移契约。
     */
    @Test
    void shouldLockInitImportOwnershipTransferContract() {
        SsoSyncTaskServiceImpl service = spy(new SsoSyncTaskServiceImpl());
        ISsoIdentityImportService ssoIdentityImportService = mock(ISsoIdentityImportService.class);
        ISsoSyncTaskItemService ssoSyncTaskItemService = mock(ISsoSyncTaskItemService.class);
        SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder = mock(SsoSyncTaskFailureRecorder.class);
        ReflectionTestUtils.setField(service, "ssoSyncTaskFailureRecorder", ssoSyncTaskFailureRecorder);
        ReflectionTestUtils.setField(service, "ssoIdentityImportService", ssoIdentityImportService);
        ReflectionTestUtils.setField(service, "ssoSyncTaskItemService", ssoSyncTaskItemService);
        doReturn(true).when(service).updateById(any(SsoSyncTask.class));
        when(ssoIdentityImportService.execute(any(SsoSyncTask.class), any())).thenReturn(buildPendingImportResult());
        SsoSyncTask command = new SsoSyncTask();
        command.setTargetClientCode("sam-mgmt");

        SsoSyncTask createdTask = service.initImportTask(command);

        assertThat(createdTask.getTaskType()).isEqualTo("INIT_IMPORT");
        assertThat(createdTask.getStatus()).isEqualTo("PENDING");
        assertThat(createdTask.getTriggerType()).isEqualTo("MANUAL");
        assertThat(createdTask.getRetryCount()).isZero();
        assertThat(createdTask.getBatchNo()).startsWith("INIT-");
        assertThat(createdTask.getIdStrategy()).isEqualTo("INHERIT_SOURCE_ID");
        assertThat(createdTask.getOwnershipTransferStatus()).isEqualTo("TRANSFERRED");
        assertThat(createdTask.getSourceBatchNo()).isNotBlank();
        assertThat(createdTask.getImportSnapshotAt()).isNotNull();
        assertThat(createdTask.getPayloadJson())
                .contains("org")
                .contains("dept")
                .contains("user")
                .contains("user_org_relation")
                .contains("user_dept_relation")
                .contains("userId+orgId")
                .contains("userId+deptId");
        verify(ssoSyncTaskFailureRecorder).persistNewTask(command);
    }

    /**
     * 验证 DISTRIBUTION 任务会写入 full-batch snapshot upsert 契约。
     */
    @Test
    void shouldLockDistributionTaskContract() {
        SsoSyncTaskServiceImpl service = spy(new SsoSyncTaskServiceImpl());
        ISsoClientService ssoClientService = mock(ISsoClientService.class);
        ISsoIdentityDistributionService ssoIdentityDistributionService = mock(ISsoIdentityDistributionService.class);
        ISsoSyncTaskItemService ssoSyncTaskItemService = mock(ISsoSyncTaskItemService.class);
        SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder = mock(SsoSyncTaskFailureRecorder.class);
        when(ssoClientService.selectSsoClientByCode(anyString()))
                .thenAnswer(invocation -> buildEnabledDistributionClient(invocation.getArgument(0)));
        ReflectionTestUtils.setField(service, "ssoClientService", ssoClientService);
        ReflectionTestUtils.setField(service, "ssoSyncTaskFailureRecorder", ssoSyncTaskFailureRecorder);
        ReflectionTestUtils.setField(service, "ssoIdentityDistributionService", ssoIdentityDistributionService);
        ReflectionTestUtils.setField(service, "ssoSyncTaskItemService", ssoSyncTaskItemService);
        doReturn(true).when(service).updateById(any(SsoSyncTask.class));
        when(ssoIdentityDistributionService.execute(any(SsoSyncTask.class), any())).thenReturn(buildPendingDistributionResult());
        SsoSyncTask command = new SsoSyncTask();
        command.setTargetClientCode("sam-mgmt");

        SsoSyncTask createdTask = service.distributionTask(command);

        assertThat(createdTask.getTaskType()).isEqualTo("DISTRIBUTION");
        assertThat(createdTask.getStatus()).isEqualTo("PENDING");
        assertThat(createdTask.getTriggerType()).isEqualTo("MANUAL");
        assertThat(createdTask.getRetryCount()).isZero();
        assertThat(createdTask.getBatchNo()).startsWith("DIST-");
        assertThat(createdTask.getSourceBatchNo()).startsWith("LOCAL-");
        assertThat(createdTask.getImportSnapshotAt()).isNotNull();
        assertThat(createdTask.getPayloadJson())
                .contains("\"deliveryMode\":\"FULL_BATCH_SNAPSHOT\"")
                .contains("\"mqActionType\":\"UPSERT\"")
                .contains("\"sourceSystem\":\"local_sam_empty\"")
                .contains("org")
                .contains("dept")
                .contains("user")
                .contains("user_org_relation")
                .contains("user_dept_relation");
        verify(ssoSyncTaskFailureRecorder).persistNewTask(command);
    }

    /**
     * 构造 INIT_IMPORT 的最小待执行结果，避免本测试与真实执行器绑定。
     *
     * @return INIT_IMPORT 待执行结果
     */
    private SsoIdentityImportExecutionResult buildPendingImportResult() {
        SsoIdentityImportExecutionResult result = new SsoIdentityImportExecutionResult();
        result.setItemList(List.of());
        result.setTotalItemCount(0);
        result.setSuccessItemCount(0);
        result.setFailedItemCount(0);
        result.setStatus(SsoSyncTask.STATUS_PENDING);
        result.setResultSummary("init import queued");
        return result;
    }

    /**
     * 构造 DISTRIBUTION 的最小待执行结果，避免本测试与真实执行器绑定。
     *
     * @return DISTRIBUTION 待执行结果
     */
    private SsoSyncTaskExecutionResult buildPendingDistributionResult() {
        SsoSyncTaskExecutionResult result = new SsoSyncTaskExecutionResult();
        result.setItemList(List.of());
        result.setTotalItemCount(0);
        result.setSuccessItemCount(0);
        result.setFailedItemCount(0);
        result.setStatus(SsoSyncTask.STATUS_PENDING);
        result.setResultSummary("distribution queued");
        return result;
    }

    /**
     * 构造默认可用于 DISTRIBUTION 的合法客户端。
     *
     * @param clientCode 客户端编码
     * @return 合法客户端
     */
    private SsoClient buildEnabledDistributionClient(String clientCode) {
        SsoClient client = new SsoClient();
        client.setClientCode(clientCode);
        client.setClientName("test-" + clientCode);
        client.setStatus("0");
        client.setSyncEnabled("Y");
        client.setAllowPasswordLogin("Y");
        client.setAllowWxworkLogin("Y");
        return client;
    }
}
