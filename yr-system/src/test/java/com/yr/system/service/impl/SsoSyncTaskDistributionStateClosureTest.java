/**
 * @file DISTRIBUTION 任务状态闭环失败测试
 * @author PopoY
 * @date 2026-03-30
 */
package com.yr.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.core.domain.entity.SsoSyncTaskItem;
import com.yr.common.service.MqProducerService;
import com.yr.system.domain.dto.SsoSyncTaskExecutionResult;
import com.yr.system.service.ISsoSyncTaskItemService;
import com.yr.system.service.support.SsoCurrentIdentitySnapshotLoader;
import com.yr.system.service.support.SsoDistributionDispatchResultRecorder;
import com.yr.system.service.support.SsoSyncTaskFailureRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * 通过红灯测试锁定：after-commit（提交后回调）完成后必须把 task/item 终态回写到数据库。
 */
class SsoSyncTaskDistributionStateClosureTest {

    /**
     * 验证发送全部成功时，事务内先落 PENDING，after-commit 再通过 recorder（结果回写器）落 SUCCESS。
     */
    @Test
    void shouldPersistSuccessStateAfterAfterCommitDispatchCompletes() {
        SsoSyncTaskServiceImpl service = spy(new SsoSyncTaskServiceImpl());
        ProbeTransactionManager transactionManager = new ProbeTransactionManager();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        ISsoSyncTaskItemService ssoSyncTaskItemService = mock(ISsoSyncTaskItemService.class);
        SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder = mock(SsoSyncTaskFailureRecorder.class);
        SsoDistributionDispatchResultRecorder ssoDistributionDispatchResultRecorder = mock(SsoDistributionDispatchResultRecorder.class);
        List<String> initialTaskStatuses = new ArrayList<>();
        List<List<String>> initialItemStatuses = new ArrayList<>();
        List<String> finalTaskStatuses = new ArrayList<>();
        List<List<String>> finalItemStatuses = new ArrayList<>();

        ReflectionTestUtils.setField(
                service,
                "ssoIdentityDistributionService",
                buildDistributionService(ssoDistributionDispatchResultRecorder, ssoSyncTaskFailureRecorder, true, true, true, true, true)
        );
        ReflectionTestUtils.setField(service, "ssoSyncTaskItemService", ssoSyncTaskItemService);
        ReflectionTestUtils.setField(service, "ssoSyncTaskFailureRecorder", ssoSyncTaskFailureRecorder);
        doAnswer(invocation -> {
            SsoSyncTask task = invocation.getArgument(0);
            task.setTaskId(31L);
            return null;
        }).when(ssoSyncTaskFailureRecorder).persistNewTask(any(SsoSyncTask.class));
        doAnswer(invocation -> {
            SsoSyncTask task = invocation.getArgument(0);
            initialTaskStatuses.add(task.getStatus());
            return true;
        }).when(service).updateById(any(SsoSyncTask.class));
        doAnswer(invocation -> {
            List<SsoSyncTaskItem> itemList = invocation.getArgument(1);
            initialItemStatuses.add(itemList.stream().map(SsoSyncTaskItem::getStatus).toList());
            return null;
        }).when(ssoSyncTaskItemService).replaceTaskItems(eq(31L), anyList());
        doAnswer(invocation -> {
            SsoSyncTask task = invocation.getArgument(0);
            SsoSyncTaskExecutionResult executionResult = invocation.getArgument(1);
            finalTaskStatuses.add(task.getStatus());
            finalItemStatuses.add(executionResult.getItemList().stream().map(SsoSyncTaskItem::getStatus).toList());
            return null;
        }).when(ssoDistributionDispatchResultRecorder).recordDispatchResult(any(SsoSyncTask.class), any(SsoSyncTaskExecutionResult.class));

        transactionTemplate.executeWithoutResult(status -> service.distributionTask(buildDistributionCommand()));

        assertThat(initialTaskStatuses)
                .as("事务内第一次任务回写应保持 PENDING")
                .containsExactly(SsoSyncTask.STATUS_PENDING);
        assertThat(initialItemStatuses)
                .as("事务内第一次明细落库应保持 PENDING 快照")
                .hasSize(1);
        assertThat(initialItemStatuses.get(0))
                .as("第一次明细落库应保持事务内的 PENDING 快照")
                .allMatch(SsoSyncTask.STATUS_PENDING::equals);
        assertThat(finalTaskStatuses)
                .as("after-commit 最终回写应把任务推进到 SUCCESS")
                .containsExactly(SsoSyncTask.STATUS_SUCCESS);
        assertThat(finalItemStatuses)
                .as("after-commit 最终回写应把明细推进到 SUCCESS")
                .hasSize(1);
        assertThat(finalItemStatuses.get(0))
                .as("最终明细回写应体现 after-commit 成功后的 SUCCESS 终态")
                .allMatch(SsoSyncTask.STATUS_SUCCESS::equals);
    }

    /**
     * 验证发送存在失败时，事务内先落 PENDING，after-commit 再通过 recorder 落 PARTIAL_SUCCESS / FAILED。
     */
    @Test
    void shouldPersistFailureOrPartialSuccessAfterAfterCommitDispatchFails() {
        SsoSyncTaskServiceImpl service = spy(new SsoSyncTaskServiceImpl());
        ProbeTransactionManager transactionManager = new ProbeTransactionManager();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        ISsoSyncTaskItemService ssoSyncTaskItemService = mock(ISsoSyncTaskItemService.class);
        SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder = mock(SsoSyncTaskFailureRecorder.class);
        SsoDistributionDispatchResultRecorder ssoDistributionDispatchResultRecorder = mock(SsoDistributionDispatchResultRecorder.class);
        List<String> initialTaskStatuses = new ArrayList<>();
        List<List<String>> initialItemStatuses = new ArrayList<>();
        List<String> finalTaskStatuses = new ArrayList<>();
        List<List<String>> finalItemStatuses = new ArrayList<>();

        ReflectionTestUtils.setField(
                service,
                "ssoIdentityDistributionService",
                buildDistributionService(ssoDistributionDispatchResultRecorder, ssoSyncTaskFailureRecorder, true, false, true, true, true)
        );
        ReflectionTestUtils.setField(service, "ssoSyncTaskItemService", ssoSyncTaskItemService);
        ReflectionTestUtils.setField(service, "ssoSyncTaskFailureRecorder", ssoSyncTaskFailureRecorder);
        doAnswer(invocation -> {
            SsoSyncTask task = invocation.getArgument(0);
            task.setTaskId(41L);
            return null;
        }).when(ssoSyncTaskFailureRecorder).persistNewTask(any(SsoSyncTask.class));
        doAnswer(invocation -> {
            SsoSyncTask task = invocation.getArgument(0);
            initialTaskStatuses.add(task.getStatus());
            return true;
        }).when(service).updateById(any(SsoSyncTask.class));
        doAnswer(invocation -> {
            List<SsoSyncTaskItem> itemList = invocation.getArgument(1);
            initialItemStatuses.add(itemList.stream().map(SsoSyncTaskItem::getStatus).toList());
            return null;
        }).when(ssoSyncTaskItemService).replaceTaskItems(eq(41L), anyList());
        doAnswer(invocation -> {
            SsoSyncTask task = invocation.getArgument(0);
            SsoSyncTaskExecutionResult executionResult = invocation.getArgument(1);
            finalTaskStatuses.add(task.getStatus());
            finalItemStatuses.add(executionResult.getItemList().stream().map(SsoSyncTaskItem::getStatus).toList());
            return null;
        }).when(ssoDistributionDispatchResultRecorder).recordDispatchResult(any(SsoSyncTask.class), any(SsoSyncTaskExecutionResult.class));

        transactionTemplate.executeWithoutResult(status -> service.distributionTask(buildDistributionCommand()));

        assertThat(initialTaskStatuses)
                .as("事务内第一次任务回写应保持 PENDING")
                .containsExactly(SsoSyncTask.STATUS_PENDING);
        assertThat(initialItemStatuses)
                .as("事务内第一次明细落库应保持 PENDING 快照")
                .hasSize(1);
        assertThat(initialItemStatuses.get(0))
                .as("第一次明细落库应保持事务内的 PENDING 快照")
                .allMatch(SsoSyncTask.STATUS_PENDING::equals);
        assertThat(finalTaskStatuses)
                .as("after-commit 最终回写应把任务推进到 PARTIAL_SUCCESS")
                .containsExactly(SsoSyncTask.STATUS_PARTIAL_SUCCESS);
        assertThat(finalItemStatuses)
                .as("after-commit 最终回写应把失败明细落成 FAILED")
                .hasSize(1);
        assertThat(finalItemStatuses.get(0))
                .as("最终明细回写应体现 after-commit 失败后的 FAILED 终态")
                .contains(SsoSyncTask.STATUS_FAILED);
    }

    /**
     * 构造最小 DISTRIBUTION 命令。
     *
     * @return 命令对象
     */
    private SsoSyncTask buildDistributionCommand() {
        SsoSyncTask command = new SsoSyncTask();
        command.setTargetClientCode("sam-mgmt");
        command.setCreateBy("phase1");
        return command;
    }

    /**
     * 构造测试用 DISTRIBUTION 执行服务。
     *
     * @param ssoDistributionDispatchResultRecorder after-commit 结果回写器
     * @param sendResults MQ send（发送）返回序列
     * @return 执行服务
     */
    private SsoIdentityDistributionServiceImpl buildDistributionService(SsoDistributionDispatchResultRecorder ssoDistributionDispatchResultRecorder,
                                                                        SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder,
                                                                        boolean... sendResults) {
        SsoCurrentIdentitySnapshotLoader snapshotLoader = mock(SsoCurrentIdentitySnapshotLoader.class);
        MqProducerService mqProducerService = mock(MqProducerService.class);

        when(snapshotLoader.loadSnapshot()).thenReturn(SsoDistributionTestFixtures.minimalSnapshot());
        when(mqProducerService.send(any(), any(), any(), any(), any()))
                .thenReturn(sendResults[0], copyTail(sendResults));
        return new SsoIdentityDistributionServiceImpl(
                snapshotLoader,
                mqProducerService,
                new ObjectMapper(),
                ssoDistributionDispatchResultRecorder,
                ssoSyncTaskFailureRecorder
        );
    }

    /**
     * 截取可变参数尾部数组，便于 Mockito（模拟框架）构造返回序列。
     *
     * @param source 原数组
     * @return 从索引 1 开始的尾部数组
     */
    private Boolean[] copyTail(boolean[] source) {
        if (source.length <= 1) {
            return new Boolean[0];
        }
        Boolean[] tail = new Boolean[source.length - 1];
        for (int i = 1; i < source.length; i++) {
            tail[i - 1] = source[i];
        }
        return tail;
    }

    /**
     * 最小事务管理器，用于触发 TransactionSynchronization（事务同步）回调。
     */
    static class ProbeTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            // author: PopoY，测试仅需激活事务生命周期，无需额外行为。
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            // author: PopoY，AbstractPlatformTransactionManager 会在提交时触发 afterCommit 回调。
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            // author: PopoY，本测试不校验回滚细节。
        }
    }
}
