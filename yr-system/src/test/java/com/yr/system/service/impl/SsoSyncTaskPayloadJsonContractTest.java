/**
 * @file 锁定同步任务 payload JSON 契约
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.system.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.core.domain.entity.SsoSyncTaskItem;
import com.yr.system.service.ISsoSyncTaskItemService;
import com.yr.system.service.support.SsoSyncTaskFailureRecorder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicLong;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定 INIT_IMPORT / DISTRIBUTION / COMPENSATION 的 payload 仍是合法 JSON。
 */
class SsoSyncTaskPayloadJsonContractTest {

    /**
     * 验证补偿 payload 在包含引号与反斜杠时仍是合法 JSON，并保留原始值。
     */
    @Test
    void compensateTaskShouldSerializeSpecialCharactersIntoValidJsonPayload() {
        SsoSyncTaskServiceImpl service = spy(new SsoSyncTaskServiceImpl());
        ISsoSyncTaskItemService ssoSyncTaskItemService = mock(ISsoSyncTaskItemService.class);
        SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder = mock(SsoSyncTaskFailureRecorder.class);
        ArgumentCaptor<SsoSyncTask> persistedTaskCaptor = ArgumentCaptor.forClass(SsoSyncTask.class);
        SsoSyncTask sourceTask = buildExistingTask();
        SsoSyncTaskItem failedItem = buildItem("user\"type", "30\\1/\"A", SsoSyncTask.STATUS_FAILED);

        ReflectionTestUtils.setField(service, "ssoSyncTaskItemService", ssoSyncTaskItemService);
        ReflectionTestUtils.setField(service, "ssoSyncTaskFailureRecorder", ssoSyncTaskFailureRecorder);
        doReturn(sourceTask).when(service).getById(11L);
        doAnswer(invocation -> {
            SsoSyncTask persistedTask = invocation.getArgument(0);
            persistedTask.setTaskId(12L);
            return null;
        }).when(ssoSyncTaskFailureRecorder).persistNewTask(any(SsoSyncTask.class));
        when(ssoSyncTaskItemService.selectFailedByTaskId(11L)).thenReturn(List.of(failedItem));

        service.compensateTask(11L);

        verify(ssoSyncTaskFailureRecorder).persistNewTask(persistedTaskCaptor.capture());
        String payloadJson = persistedTaskCaptor.getValue().getPayloadJson();

        assertThatCode(() -> JSON.parseObject(payloadJson))
                .as("补偿 payload 必须保持为合法 JSON")
                .doesNotThrowAnyException();
        JSONObject payload = JSON.parseObject(payloadJson);
        JSONArray failedItems = payload.getJSONArray("failedItems");
        JSONObject firstFailedItem = failedItems.getJSONObject(0);

        assertThat(payload.getLong("sourceTaskId")).isEqualTo(11L);
        assertThat(firstFailedItem.getString("entityType")).isEqualTo("user\"type");
        assertThat(firstFailedItem.getString("sourceId")).isEqualTo("30\\1/\"A");
    }

    /**
     * 验证 INIT_IMPORT 与 DISTRIBUTION payload 至少保持为可解析的结构化 JSON。
     */
    @Test
    void initImportAndDistributionPayloadShouldRemainValidJsonObjects() {
        SsoSyncTaskServiceImpl service = spy(new SsoSyncTaskServiceImpl());
        AtomicLong taskIdSeed = new AtomicLong(100L);
        SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder = mock(SsoSyncTaskFailureRecorder.class);
        SsoSyncTask initCommand = new SsoSyncTask();
        SsoSyncTask distributionCommand = new SsoSyncTask();

        distributionCommand.setTargetClientCode("sam-mgmt");
        ReflectionTestUtils.setField(service, "ssoSyncTaskFailureRecorder", ssoSyncTaskFailureRecorder);
        doAnswer(invocation -> {
            SsoSyncTask persistedTask = invocation.getArgument(0);
            persistedTask.setTaskId(taskIdSeed.getAndIncrement());
            return null;
        }).when(ssoSyncTaskFailureRecorder).persistNewTask(any(SsoSyncTask.class));

        service.initImportTask(initCommand);
        service.distributionTask(distributionCommand);

        assertThatCode(() -> JSON.parseObject(initCommand.getPayloadJson())).doesNotThrowAnyException();
        assertThatCode(() -> JSON.parseObject(distributionCommand.getPayloadJson())).doesNotThrowAnyException();

        JSONObject initPayload = JSON.parseObject(initCommand.getPayloadJson());
        JSONObject distributionPayload = JSON.parseObject(distributionCommand.getPayloadJson());

        assertThat(initPayload.getJSONArray("entityScopes"))
                .contains("org", "dept", "user", "user_org_relation", "user_dept_relation");
        assertThat(distributionPayload.getString("deliveryMode")).isEqualTo("FULL_BATCH_SNAPSHOT");
        assertThat(distributionPayload.getString("mqActionType")).isEqualTo("UPSERT");
    }

    /**
     * 构造已有同步任务，模拟补偿来源任务。
     *
     * @return 来源任务
     */
    private SsoSyncTask buildExistingTask() {
        SsoSyncTask task = new SsoSyncTask();
        task.setTaskId(11L);
        task.setTaskType(SsoSyncTask.TASK_TYPE_INIT_IMPORT);
        task.setSourceBatchNo("SRC-001");
        task.setStatus(SsoSyncTask.STATUS_FAILED);
        task.setRetryCount(0);
        task.setIdStrategy(SsoSyncTask.ID_STRATEGY_INHERIT_SOURCE_ID);
        task.setOwnershipTransferStatus(SsoSyncTask.OWNERSHIP_TRANSFERRED);
        task.setCreateBy("tester");
        return task;
    }

    /**
     * 构造最小失败明细，便于验证 JSON 转义。
     *
     * @param entityType 实体类型
     * @param sourceId 来源主键
     * @param status 明细状态
     * @return 失败明细
     */
    private SsoSyncTaskItem buildItem(String entityType, String sourceId, String status) {
        SsoSyncTaskItem item = new SsoSyncTaskItem();
        item.setEntityType(entityType);
        item.setSourceId(sourceId);
        item.setStatus(status);
        return item;
    }
}
