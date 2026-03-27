/**
 * @file 同步任务服务测试
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yr.common.core.domain.MqMessageLog;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.core.domain.entity.SsoSyncTaskItem;
import com.yr.common.exception.CustomException;
import com.yr.common.mapper.MqMessageLogMapper;
import com.yr.system.domain.dto.SsoIdentityImportExecutionResult;
import com.yr.system.domain.dto.SsoSyncTaskExecutionResult;
import com.yr.system.service.ISsoIdentityDistributionService;
import com.yr.system.service.ISsoIdentityImportService;
import com.yr.system.service.ISsoSyncTaskItemService;
import com.yr.system.service.support.SsoSyncTaskFailureRecorder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 retry / compensate 的最小业务语义，便于后续 rehearsal（演练）继续收敛。
 */
class SsoSyncTaskServiceImplTest {

    /**
     * 验证列表查询构造出的 wrapper 在 JDK 17 下仍可安全求值，避免 lambda 元数据反射炸掉控制台列表接口。
     */
    @Test
    void selectSsoSyncTaskListShouldBuildJvmFriendlyWrapper() {
        SsoSyncTaskServiceImpl service = spy(new SsoSyncTaskServiceImpl());
        AtomicReference<String> sqlSegmentRef = new AtomicReference<>();
        SsoSyncTask query = new SsoSyncTask();

        query.setTaskId(11L);
        query.setTaskType(SsoSyncTask.TASK_TYPE_DISTRIBUTION);
        query.setStatus(SsoSyncTask.STATUS_FAILED);
        query.setTargetClientCode("sam-mgmt");
        doAnswer(invocation -> {
            Wrapper<SsoSyncTask> wrapper = invocation.getArgument(0);
            sqlSegmentRef.set(wrapper.getSqlSegment());
            return List.of();
        }).when(service).list(any());

        assertThatCode(() -> service.selectSsoSyncTaskList(query))
                .doesNotThrowAnyException();
        assertThat(sqlSegmentRef.get())
                .contains("task_id")
                .contains("task_type")
                .contains("status")
                .contains("target_client_code")
                .contains("ORDER BY");
    }

    /**
     * 验证重试会累加重试次数，并把执行结果回写到原任务。
     */
    @Test
    void retryTaskShouldReinvokeImporterAndIncrementRetry() {
        SsoSyncTaskServiceImpl service = spy(new SsoSyncTaskServiceImpl());
        ISsoIdentityImportService ssoIdentityImportService = mock(ISsoIdentityImportService.class);
        ISsoSyncTaskItemService ssoSyncTaskItemService = mock(ISsoSyncTaskItemService.class);
        SsoSyncTask existingTask = buildExistingTask();
        SsoIdentityImportExecutionResult executionResult = buildExecutionResult("SUCCESS", 2, 2, 0);

        ReflectionTestUtils.setField(service, "ssoIdentityImportService", ssoIdentityImportService);
        ReflectionTestUtils.setField(service, "ssoSyncTaskItemService", ssoSyncTaskItemService);
        doReturn(existingTask).when(service).getById(11L);
        doReturn(true).when(service).updateById(any(SsoSyncTask.class));
        when(ssoIdentityImportService.execute(eq(existingTask), eq(null))).thenReturn(executionResult);

        SsoSyncTask result = service.retryTask(11L);

        assertThat(result.getRetryCount()).isEqualTo(1);
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getTotalItemCount()).isEqualTo(2);
        assertThat(result.getSuccessItemCount()).isEqualTo(2);
        assertThat(result.getFailedItemCount()).isZero();
        verify(ssoIdentityImportService).execute(existingTask, null);
        verify(ssoSyncTaskItemService).replaceTaskItems(eq(11L), eq(executionResult.getItemList()));
    }

    /**
     * 验证补偿任务只会基于失败明细构造 scoped payload，并执行补偿导入。
     */
    @Test
    void compensateTaskShouldBuildCompensationPayloadFromFailedItems() {
        SsoSyncTaskServiceImpl service = spy(new SsoSyncTaskServiceImpl());
        ISsoIdentityImportService ssoIdentityImportService = mock(ISsoIdentityImportService.class);
        ISsoSyncTaskItemService ssoSyncTaskItemService = mock(ISsoSyncTaskItemService.class);
        SsoSyncTask sourceTask = buildExistingTask();
        SsoSyncTaskItem failedUserItem = buildItem("user", "301", "FAILED");
        SsoSyncTaskItem failedDeptItem = buildItem("dept", "201", "FAILED");
        SsoIdentityImportExecutionResult executionResult = buildExecutionResult("SUCCESS", 2, 2, 0);
        ArgumentCaptor<SsoSyncTask> savedTaskCaptor = ArgumentCaptor.forClass(SsoSyncTask.class);

        ReflectionTestUtils.setField(service, "ssoIdentityImportService", ssoIdentityImportService);
        ReflectionTestUtils.setField(service, "ssoSyncTaskItemService", ssoSyncTaskItemService);
        doReturn(sourceTask).when(service).getById(11L);
        doAnswer(invocation -> {
            SsoSyncTask task = invocation.getArgument(0);
            task.setTaskId(12L);
            return true;
        }).when(service).save(any(SsoSyncTask.class));
        doReturn(true).when(service).updateById(any(SsoSyncTask.class));
        when(ssoSyncTaskItemService.selectFailedByTaskId(11L)).thenReturn(List.of(failedUserItem, failedDeptItem));
        when(ssoIdentityImportService.execute(any(SsoSyncTask.class), eq(List.of(failedUserItem, failedDeptItem))))
                .thenReturn(executionResult);

        SsoSyncTask result = service.compensateTask(11L);

        verify(service).save(savedTaskCaptor.capture());
        assertThat(savedTaskCaptor.getValue().getTaskType()).isEqualTo("COMPENSATION");
        assertThat(savedTaskCaptor.getValue().getBatchNo()).startsWith("COMP-");
        assertThat(savedTaskCaptor.getValue().getPayloadJson())
                .contains("\"sourceTaskId\":11")
                .contains("\"entityType\":\"user\"")
                .contains("\"sourceId\":\"301\"")
                .contains("\"entityType\":\"dept\"")
                .contains("\"sourceId\":\"201\"");
        assertThat(result.getTaskId()).isEqualTo(12L);
        assertThat(result.getItemList()).hasSize(2);
    }

    /**
     * 验证重试执行器抛错时，任务会落成 FAILED 并保留异常摘要。
     */
    @Test
    void retryTaskShouldMarkTaskFailedWhenImporterThrows() {
        SsoSyncTaskServiceImpl service = spy(new SsoSyncTaskServiceImpl());
        ISsoIdentityImportService ssoIdentityImportService = mock(ISsoIdentityImportService.class);
        ISsoSyncTaskItemService ssoSyncTaskItemService = mock(ISsoSyncTaskItemService.class);
        SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder = mock(SsoSyncTaskFailureRecorder.class);
        SsoSyncTask existingTask = buildExistingTask();

        ReflectionTestUtils.setField(service, "ssoIdentityImportService", ssoIdentityImportService);
        ReflectionTestUtils.setField(service, "ssoSyncTaskItemService", ssoSyncTaskItemService);
        ReflectionTestUtils.setField(service, "ssoSyncTaskFailureRecorder", ssoSyncTaskFailureRecorder);
        doReturn(existingTask).when(service).getById(11L);
        doReturn(true).when(service).updateById(any(SsoSyncTask.class));
        doThrow(new RuntimeException("legacy source unavailable")).when(ssoIdentityImportService).execute(eq(existingTask), eq(null));
        doAnswer(invocation -> {
            SsoSyncTask failedTask = invocation.getArgument(0);
            RuntimeException exception = invocation.getArgument(1);

            failedTask.setStatus(SsoSyncTask.STATUS_FAILED);
            failedTask.setResultSummary(exception.getMessage());
            failedTask.setExecuteAt(new Date());
            return null;
        }).when(ssoSyncTaskFailureRecorder).recordFailure(eq(existingTask), any(RuntimeException.class));

        assertThatThrownBy(() -> service.retryTask(11L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("legacy source unavailable");
        verify(ssoSyncTaskFailureRecorder).recordFailure(eq(existingTask), any(RuntimeException.class));
        assertThat(existingTask.getStatus()).isEqualTo(SsoSyncTask.STATUS_FAILED);
        assertThat(existingTask.getResultSummary()).contains("legacy source unavailable");
    }

    /**
     * 验证手工全量分发会构造 DISTRIBUTION 任务并调用 distribution executor。
     */
    @Test
    void distributionTaskShouldBuildDistributionPayloadAndInvokeExecutor() {
        SsoSyncTaskServiceImpl service = spy(new SsoSyncTaskServiceImpl());
        ISsoIdentityDistributionService ssoIdentityDistributionService = mock(ISsoIdentityDistributionService.class);
        ISsoSyncTaskItemService ssoSyncTaskItemService = mock(ISsoSyncTaskItemService.class);
        SsoSyncTaskExecutionResult executionResult = buildExecutionResult("SUCCESS", 2, 2, 0);
        ArgumentCaptor<SsoSyncTask> savedTaskCaptor = ArgumentCaptor.forClass(SsoSyncTask.class);

        ReflectionTestUtils.setField(service, "ssoIdentityDistributionService", ssoIdentityDistributionService);
        ReflectionTestUtils.setField(service, "ssoSyncTaskItemService", ssoSyncTaskItemService);
        doAnswer(invocation -> {
            SsoSyncTask task = invocation.getArgument(0);
            task.setTaskId(21L);
            return true;
        }).when(service).save(any(SsoSyncTask.class));
        doReturn(true).when(service).updateById(any(SsoSyncTask.class));
        when(ssoIdentityDistributionService.execute(any(SsoSyncTask.class), eq(null))).thenReturn(executionResult);

        SsoSyncTask command = new SsoSyncTask();
        command.setTargetClientCode("sam-mgmt");
        SsoSyncTask result = service.distributionTask(command);

        verify(service).save(savedTaskCaptor.capture());
        assertThat(savedTaskCaptor.getValue().getTaskType()).isEqualTo("DISTRIBUTION");
        assertThat(savedTaskCaptor.getValue().getBatchNo()).startsWith("DIST-");
        assertThat(savedTaskCaptor.getValue().getSourceBatchNo()).startsWith("LOCAL-");
        assertThat(savedTaskCaptor.getValue().getPayloadJson())
                .contains("\"deliveryMode\":\"FULL_BATCH_SNAPSHOT\"")
                .contains("\"mqActionType\":\"UPSERT\"")
                .contains("\"sourceSystem\":\"local_sam_empty\"");
        assertThat(result.getTaskId()).isEqualTo(21L);
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getItemList()).hasSize(2);
        verify(ssoIdentityDistributionService).execute(any(SsoSyncTask.class), eq(null));
        verify(ssoSyncTaskItemService).replaceTaskItems(eq(21L), eq(executionResult.getItemList()));
    }

    /**
     * 验证详情查询会按持久化 msgKey 批量挂接最新 MQ 履历视图。
     */
    @Test
    void selectSsoSyncTaskByIdShouldAttachLatestMessageLogByMsgKey() {
        SsoSyncTaskServiceImpl service = spy(new SsoSyncTaskServiceImpl());
        ISsoSyncTaskItemService ssoSyncTaskItemService = mock(ISsoSyncTaskItemService.class);
        MqMessageLogMapper mqMessageLogMapper = mock(MqMessageLogMapper.class);
        SsoSyncTask existingTask = buildExistingTask();
        SsoSyncTaskItem distributionItem = buildItem("user", "301", "SUCCESS");
        MqMessageLog mqMessageLog = new MqMessageLog();

        distributionItem.setMsgKey("DIST:11:user:301");
        mqMessageLog.setMsgKey("DIST:11:user:301");
        mqMessageLog.setTopic("sso-identity-distribution");
        mqMessageLog.setTag("sam-mgmt");
        mqMessageLog.setSendStatus(1);

        ReflectionTestUtils.setField(service, "ssoSyncTaskItemService", ssoSyncTaskItemService);
        ReflectionTestUtils.setField(service, "mqMessageLogMapper", mqMessageLogMapper);
        doReturn(existingTask).when(service).getById(11L);
        when(ssoSyncTaskItemService.selectByTaskId(11L)).thenReturn(List.of(distributionItem));
        when(mqMessageLogMapper.selectLatestByMsgKeys(List.of("DIST:11:user:301"))).thenReturn(List.of(mqMessageLog));

        SsoSyncTask result = service.selectSsoSyncTaskById(11L);

        assertThat(result.getItemList()).singleElement().satisfies(item -> {
            assertThat(item.getMsgKey()).isEqualTo("DIST:11:user:301");
            Object messageLog = ReflectionTestUtils.getField(item, "messageLog");
            assertThat(messageLog).isNotNull();
            assertThat(ReflectionTestUtils.getField(messageLog, "sendStatus")).isEqualTo(1);
            assertThat(ReflectionTestUtils.getField(messageLog, "topic")).isEqualTo("sso-identity-distribution");
        });
    }

    /**
     * 验证旧链路在持久化 msgKey 为空时，仍会从 detailJson 回填 msgKey 并挂接最新 MQ 履历。
     */
    @Test
    void selectSsoSyncTaskByIdShouldHydrateMsgKeyFromDetailJsonBeforeAttachingMessageLog() {
        SsoSyncTaskServiceImpl service = spy(new SsoSyncTaskServiceImpl());
        ISsoSyncTaskItemService ssoSyncTaskItemService = mock(ISsoSyncTaskItemService.class);
        MqMessageLogMapper mqMessageLogMapper = mock(MqMessageLogMapper.class);
        SsoSyncTask existingTask = buildExistingTask();
        SsoSyncTaskItem oldChainItem = buildItem("user", "301", "FAILED");
        MqMessageLog mqMessageLog = new MqMessageLog();

        oldChainItem.setDetailJson("{\"msgKey\":\"DIST:11:user:301\",\"payload\":{\"userId\":301}}");
        mqMessageLog.setMsgKey("DIST:11:user:301");
        mqMessageLog.setTopic("sso-identity-distribution");
        mqMessageLog.setTag("sam-mgmt");
        mqMessageLog.setSendStatus(1);
        mqMessageLog.setConsumeStatus(0);

        ReflectionTestUtils.setField(service, "ssoSyncTaskItemService", ssoSyncTaskItemService);
        ReflectionTestUtils.setField(service, "mqMessageLogMapper", mqMessageLogMapper);
        doReturn(existingTask).when(service).getById(11L);
        when(ssoSyncTaskItemService.selectByTaskId(11L)).thenReturn(List.of(oldChainItem));
        when(mqMessageLogMapper.selectLatestByMsgKeys(List.of("DIST:11:user:301"))).thenReturn(List.of(mqMessageLog));

        SsoSyncTask result = service.selectSsoSyncTaskById(11L);

        verify(mqMessageLogMapper).selectLatestByMsgKeys(List.of("DIST:11:user:301"));
        assertThat(result.getItemList()).singleElement().satisfies(item -> {
            assertThat(item.getMsgKey()).isEqualTo("DIST:11:user:301");
            Object messageLog = ReflectionTestUtils.getField(item, "messageLog");
            assertThat(messageLog).isNotNull();
            assertThat(ReflectionTestUtils.getField(messageLog, "sendStatus")).isEqualTo(1);
            assertThat(ReflectionTestUtils.getField(messageLog, "consumeStatus")).isEqualTo(0);
            assertThat(ReflectionTestUtils.getField(messageLog, "topic")).isEqualTo("sso-identity-distribution");
            assertThat(ReflectionTestUtils.getField(messageLog, "tag")).isEqualTo("sam-mgmt");
        });
    }

    /**
     * 验证无失败明细时不会创建空补偿任务。
     */
    @Test
    void compensateTaskShouldRejectWhenNoFailedItems() {
        SsoSyncTaskServiceImpl service = spy(new SsoSyncTaskServiceImpl());
        ISsoSyncTaskItemService ssoSyncTaskItemService = mock(ISsoSyncTaskItemService.class);

        ReflectionTestUtils.setField(service, "ssoSyncTaskItemService", ssoSyncTaskItemService);
        doReturn(buildExistingTask()).when(service).getById(11L);
        when(ssoSyncTaskItemService.selectFailedByTaskId(11L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.compensateTask(11L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("没有失败明细");
    }

    /**
     * 构造现有任务。
     *
     * @return 现有任务
     */
    private SsoSyncTask buildExistingTask() {
        SsoSyncTask task = new SsoSyncTask();
        task.setTaskId(11L);
        task.setTaskType("INIT_IMPORT");
        task.setSourceBatchNo("SRC-001");
        task.setStatus("FAILED");
        task.setRetryCount(0);
        task.setIdStrategy(SsoSyncTask.ID_STRATEGY_INHERIT_SOURCE_ID);
        task.setOwnershipTransferStatus(SsoSyncTask.OWNERSHIP_TRANSFERRED);
        task.setCreateBy("tester");
        return task;
    }

    /**
     * 构造最小执行结果。
     *
     * @param status 任务状态
     * @param totalItemCount 明细总数
     * @param successItemCount 成功数
     * @param failedItemCount 失败数
     * @return 执行结果
     */
    private SsoIdentityImportExecutionResult buildExecutionResult(String status,
                                                                  int totalItemCount,
                                                                  int successItemCount,
                                                                  int failedItemCount) {
        SsoIdentityImportExecutionResult executionResult = new SsoIdentityImportExecutionResult();
        executionResult.setStatus(status);
        executionResult.setResultSummary("summary");
        executionResult.setTotalItemCount(totalItemCount);
        executionResult.setSuccessItemCount(successItemCount);
        executionResult.setFailedItemCount(failedItemCount);
        executionResult.setItemList(List.of(
                buildItem("user", "301", "SUCCESS"),
                buildItem("dept", "201", "SUCCESS")
        ));
        return executionResult;
    }

    /**
     * 构造最小任务明细。
     *
     * @param entityType 实体类型
     * @param sourceId 来源 ID
     * @param status 状态
     * @return 任务明细
     */
    private SsoSyncTaskItem buildItem(String entityType, String sourceId, String status) {
        SsoSyncTaskItem item = new SsoSyncTaskItem();
        item.setEntityType(entityType);
        item.setSourceId(sourceId);
        item.setStatus(status);
        return item;
    }
}
