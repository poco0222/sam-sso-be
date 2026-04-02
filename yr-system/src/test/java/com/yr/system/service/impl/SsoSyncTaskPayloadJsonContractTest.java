/**
 * @file 锁定同步任务 payload JSON 契约
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.system.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yr.common.core.domain.entity.SsoClient;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.core.domain.entity.SsoSyncTaskItem;
import com.yr.system.domain.dto.SsoIdentityImportExecutionResult;
import com.yr.system.domain.dto.SsoSyncTaskExecutionResult;
import com.yr.system.service.ISsoClientService;
import com.yr.system.service.ISsoIdentityDistributionService;
import com.yr.system.service.ISsoIdentityImportService;
import com.yr.system.service.ISsoSyncTaskItemService;
import com.yr.system.service.support.SsoSyncTaskFailureRecorder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    /** JSON 解析器。 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    /**
     * 验证补偿 payload 在包含引号与反斜杠时仍是合法 JSON，并保留原始值。
     */
    @Test
    void compensateTaskShouldSerializeSpecialCharactersIntoValidJsonPayload() {
        SsoSyncTaskServiceImpl service = spy(new SsoSyncTaskServiceImpl());
        ISsoIdentityImportService ssoIdentityImportService = mock(ISsoIdentityImportService.class);
        ISsoSyncTaskItemService ssoSyncTaskItemService = mock(ISsoSyncTaskItemService.class);
        SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder = mock(SsoSyncTaskFailureRecorder.class);
        ArgumentCaptor<SsoSyncTask> persistedTaskCaptor = ArgumentCaptor.forClass(SsoSyncTask.class);
        SsoSyncTask sourceTask = buildExistingTask();
        SsoSyncTaskItem failedItem = buildItem("user\"type", "30\\1/\"A", SsoSyncTask.STATUS_FAILED);

        ReflectionTestUtils.setField(service, "ssoIdentityImportService", ssoIdentityImportService);
        ReflectionTestUtils.setField(service, "ssoSyncTaskItemService", ssoSyncTaskItemService);
        ReflectionTestUtils.setField(service, "ssoSyncTaskFailureRecorder", ssoSyncTaskFailureRecorder);
        doReturn(sourceTask).when(service).getById(11L);
        doReturn(true).when(service).updateById(any(SsoSyncTask.class));
        doAnswer(invocation -> {
            SsoSyncTask persistedTask = invocation.getArgument(0);
            persistedTask.setTaskId(12L);
            return null;
        }).when(ssoSyncTaskFailureRecorder).persistNewTask(any(SsoSyncTask.class));
        when(ssoSyncTaskItemService.selectFailedByTaskId(11L)).thenReturn(List.of(failedItem));
        when(ssoIdentityImportService.execute(any(SsoSyncTask.class), any())).thenReturn(buildPendingImportResult());

        service.compensateTask(11L);

        verify(ssoSyncTaskFailureRecorder).persistNewTask(persistedTaskCaptor.capture());
        String payloadJson = persistedTaskCaptor.getValue().getPayloadJson();

        assertThatCode(() -> OBJECT_MAPPER.readTree(payloadJson))
                .as("补偿 payload 必须保持为合法 JSON")
                .doesNotThrowAnyException();
        JsonNode payload = readJson(payloadJson);
        JsonNode firstFailedItem = payload.path("failedItems").get(0);

        assertThat(payload.path("sourceTaskId").asLong()).isEqualTo(11L);
        assertThat(firstFailedItem.path("entityType").asText()).isEqualTo("user\"type");
        assertThat(firstFailedItem.path("sourceId").asText()).isEqualTo("30\\1/\"A");
    }

    /**
     * 验证 INIT_IMPORT 与 DISTRIBUTION payload 至少保持为可解析的结构化 JSON。
     */
    @Test
    void initImportAndDistributionPayloadShouldRemainValidJsonObjects() {
        SsoSyncTaskServiceImpl service = spy(new SsoSyncTaskServiceImpl());
        ISsoClientService ssoClientService = mock(ISsoClientService.class);
        ISsoIdentityImportService ssoIdentityImportService = mock(ISsoIdentityImportService.class);
        ISsoIdentityDistributionService ssoIdentityDistributionService = mock(ISsoIdentityDistributionService.class);
        ISsoSyncTaskItemService ssoSyncTaskItemService = mock(ISsoSyncTaskItemService.class);
        AtomicLong taskIdSeed = new AtomicLong(100L);
        SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder = mock(SsoSyncTaskFailureRecorder.class);
        SsoSyncTask initCommand = new SsoSyncTask();
        SsoSyncTask distributionCommand = new SsoSyncTask();

        distributionCommand.setTargetClientCode("sam-mgmt");
        when(ssoClientService.selectSsoClientByCode(anyString()))
                .thenAnswer(invocation -> buildEnabledDistributionClient(invocation.getArgument(0)));
        ReflectionTestUtils.setField(service, "ssoClientService", ssoClientService);
        ReflectionTestUtils.setField(service, "ssoIdentityImportService", ssoIdentityImportService);
        ReflectionTestUtils.setField(service, "ssoIdentityDistributionService", ssoIdentityDistributionService);
        ReflectionTestUtils.setField(service, "ssoSyncTaskItemService", ssoSyncTaskItemService);
        ReflectionTestUtils.setField(service, "ssoSyncTaskFailureRecorder", ssoSyncTaskFailureRecorder);
        doReturn(true).when(service).updateById(any(SsoSyncTask.class));
        doAnswer(invocation -> {
            SsoSyncTask persistedTask = invocation.getArgument(0);
            persistedTask.setTaskId(taskIdSeed.getAndIncrement());
            return null;
        }).when(ssoSyncTaskFailureRecorder).persistNewTask(any(SsoSyncTask.class));
        when(ssoIdentityImportService.execute(any(SsoSyncTask.class), any())).thenReturn(buildPendingImportResult());
        when(ssoIdentityDistributionService.execute(any(SsoSyncTask.class), any())).thenReturn(buildPendingDistributionResult());

        service.initImportTask(initCommand);
        service.distributionTask(distributionCommand);

        assertThatCode(() -> OBJECT_MAPPER.readTree(initCommand.getPayloadJson())).doesNotThrowAnyException();
        assertThatCode(() -> OBJECT_MAPPER.readTree(distributionCommand.getPayloadJson())).doesNotThrowAnyException();

        JsonNode initPayload = readJson(initCommand.getPayloadJson());
        JsonNode distributionPayload = readJson(distributionCommand.getPayloadJson());

        assertThat(StreamSupport.stream(initPayload.path("entityScopes").spliterator(), false).map(JsonNode::asText).toList())
                .contains("org", "dept", "user", "user_org_relation", "user_dept_relation");
        assertThat(distributionPayload.path("deliveryMode").asText()).isEqualTo("FULL_BATCH_SNAPSHOT");
        assertThat(distributionPayload.path("mqActionType").asText()).isEqualTo("UPSERT");
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

    /**
     * 读取 JSON（JavaScript 对象表示法）字符串。
     *
     * @param json JSON 字符串
     * @return JSON 树节点
     */
    private JsonNode readJson(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception ex) {
            throw new IllegalStateException("测试 JSON 解析失败", ex);
        }
    }

    /**
     * 构造 INIT_IMPORT 的最小待执行结果，避免 payload 契约测试依赖真实执行器。
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
     * 构造 DISTRIBUTION 的最小待执行结果，避免 payload 契约测试依赖真实执行器。
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
