/**
 * @file 锁定身份中心一期 client 与 sync-task skeleton 契约
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SsoClient;
import com.yr.common.core.domain.entity.SsoSyncTask;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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
        doReturn(true).when(service).save(any(SsoSyncTask.class));
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
        verify(service).save(command);
    }
}
