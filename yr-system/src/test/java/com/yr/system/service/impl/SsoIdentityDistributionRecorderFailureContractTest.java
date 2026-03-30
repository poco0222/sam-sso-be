/**
 * @file DISTRIBUTION after-commit recorder 失败闭环契约测试
 * @author PopoY
 * @date 2026-03-30
 */
package com.yr.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.core.domain.entity.SsoSyncTaskItem;
import com.yr.common.service.MqProducerService;
import com.yr.system.domain.dto.SsoSyncTaskExecutionResult;
import com.yr.system.mapper.SsoSyncTaskMapper;
import com.yr.system.service.ISsoSyncTaskItemService;
import com.yr.system.service.support.SsoCurrentIdentitySnapshotLoader;
import com.yr.system.service.support.SsoDistributionDispatchResultRecorder;
import com.yr.system.service.support.SsoSyncTaskFailureRecorder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定：MQ 已发送但 after-commit 最终态回写失败时，系统仍必须留下可恢复的任务级对账失败标记。
 */
@SpringJUnitConfig(classes = SsoIdentityDistributionRecorderFailureContractTest.TestConfig.class)
class SsoIdentityDistributionRecorderFailureContractTest {

    /** 待测 DISTRIBUTION 服务。 */
    @Autowired
    private SsoIdentityDistributionServiceImpl ssoIdentityDistributionService;

    /** 事务探针。 */
    @Autowired
    private ProbeTransactionManager transactionManager;

    /** 当前主数据快照读取器测试桩。 */
    @Autowired
    private SsoCurrentIdentitySnapshotLoader ssoCurrentIdentitySnapshotLoader;

    /** MQ 发送服务测试桩。 */
    @Autowired
    private MqProducerService mqProducerService;

    /** after-commit 结果回写器测试桩。 */
    @Autowired
    private SsoDistributionDispatchResultRecorder ssoDistributionDispatchResultRecorder;

    /** after-commit 失败兜底记录器测试桩。 */
    @Autowired
    private SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder;

    /** 同步任务 Mapper 测试桩。 */
    @Autowired
    private SsoSyncTaskMapper ssoSyncTaskMapper;

    /** 同步任务明细服务测试桩。 */
    @Autowired
    private ISsoSyncTaskItemService ssoSyncTaskItemService;

    /**
     * 每个用例后重置 mock 与事务计数器，避免状态串用。
     */
    @AfterEach
    void tearDown() {
        transactionManager.reset();
        Mockito.reset(
                ssoCurrentIdentitySnapshotLoader,
                mqProducerService,
                ssoDistributionDispatchResultRecorder,
                ssoSyncTaskMapper,
                ssoSyncTaskItemService
        );
    }

    /**
     * 验证 MQ 已经成功发送但 recorder 回写失败时，任务侧仍会留下 reconciliation failure（对账失败）标记。
     *
     * @author PopoY
     */
    @Test
    void shouldLeaveTaskLevelReconciliationMarkerWhenRecorderFailsAfterCommit() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        AtomicReference<SsoSyncTaskExecutionResult> resultRef = new AtomicReference<>();
        SsoSyncTask task = buildTask();

        when(ssoCurrentIdentitySnapshotLoader.loadSnapshot()).thenReturn(SsoDistributionTestFixtures.minimalSnapshot());
        when(mqProducerService.send(any(), any(), any(), any(), any())).thenReturn(true);
        when(ssoSyncTaskMapper.updateById(any(SsoSyncTask.class))).thenReturn(1);
        doThrow(new RuntimeException("dispatch result recorder unavailable"))
                .when(ssoDistributionDispatchResultRecorder)
                .recordDispatchResult(any(SsoSyncTask.class), any(SsoSyncTaskExecutionResult.class));

        catchThrowable(() -> transactionTemplate.executeWithoutResult(status ->
                resultRef.set(ssoIdentityDistributionService.execute(task, null))
        ));

        assertThat(transactionManager.getCommitCount()).isGreaterThanOrEqualTo(1);
        assertThat(resultRef.get()).isNotNull();
        assertThat(resultRef.get().getItemList())
                .as("MQ 已成功发送时，item 级发送结果仍应保持 SUCCESS")
                .allSatisfy(item -> assertThat(item.getStatus()).isEqualTo(SsoSyncTask.STATUS_SUCCESS));
        assertThat(task.getStatus())
                .as("task 级状态不应继续停留在纯 SUCCESS，而应留下可查询的对账失败标记")
                .isIn(SsoSyncTask.STATUS_FAILED, SsoSyncTask.STATUS_PARTIAL_SUCCESS);
        assertThat(task.getResultSummary())
                .as("task 级摘要应显式提示 dispatch state reconciliation failed")
                .contains("dispatch state reconciliation failed");
        verify(mqProducerService, times(5)).send(any(), any(), any(), any(), any());
        verify(ssoDistributionDispatchResultRecorder, times(1)).recordDispatchResult(any(SsoSyncTask.class), any(SsoSyncTaskExecutionResult.class));
        verify(ssoSyncTaskMapper, times(1)).updateById(any(SsoSyncTask.class));
        verify(ssoSyncTaskItemService, times(1))
                .updateDispatchResult(eq(99L), argThat(itemList -> itemList != null
                        && !itemList.isEmpty()
                        && itemList.stream().map(SsoSyncTaskItem::getStatus).allMatch(SsoSyncTask.STATUS_SUCCESS::equals)));
    }

    /**
     * 构造最小分发任务。
     *
     * @return 同步任务
     */
    private SsoSyncTask buildTask() {
        SsoSyncTask task = new SsoSyncTask();
        task.setTaskId(99L);
        task.setBatchNo("DIST-99");
        task.setTargetClientCode("sam-mgmt");
        task.setCreateBy("phase2");
        return task;
    }

    /**
     * 测试专用 Spring 配置，只加载 recorder failure 契约所需的最小 Bean。
     */
    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    static class TestConfig {

        /**
         * @return 事务探针
         */
        @Bean
        ProbeTransactionManager transactionManager() {
            return new ProbeTransactionManager();
        }

        /**
         * @return 当前主数据快照读取器 mock
         */
        @Bean
        SsoCurrentIdentitySnapshotLoader ssoCurrentIdentitySnapshotLoader() {
            return mock(SsoCurrentIdentitySnapshotLoader.class);
        }

        /**
         * @return MQ 发送服务 mock
         */
        @Bean
        MqProducerService mqProducerService() {
            return mock(MqProducerService.class);
        }

        /**
         * @return after-commit 结果回写器 mock
         */
        @Bean
        SsoDistributionDispatchResultRecorder ssoDistributionDispatchResultRecorder() {
            return mock(SsoDistributionDispatchResultRecorder.class);
        }

        /**
         * @return 同步任务 Mapper mock
         */
        @Bean
        SsoSyncTaskMapper ssoSyncTaskMapper() {
            return mock(SsoSyncTaskMapper.class);
        }

        /**
         * @return 同步任务明细服务 mock
         */
        @Bean
        ISsoSyncTaskItemService ssoSyncTaskItemService() {
            return mock(ISsoSyncTaskItemService.class);
        }

        /**
         * @param ssoSyncTaskMapper 同步任务 Mapper
         * @param ssoSyncTaskItemService 同步任务明细服务
         * @return after-commit 失败兜底记录器
         */
        @Bean
        SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder(SsoSyncTaskMapper ssoSyncTaskMapper,
                                                              ISsoSyncTaskItemService ssoSyncTaskItemService) {
            return new SsoSyncTaskFailureRecorder(ssoSyncTaskMapper, ssoSyncTaskItemService);
        }

        /**
         * @return ObjectMapper
         */
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        /**
         * @param ssoCurrentIdentitySnapshotLoader 当前主数据快照读取器
         * @param mqProducerService MQ 发送服务
         * @param objectMapper JSON 序列化器
         * @param ssoDistributionDispatchResultRecorder after-commit 结果回写器
         * @return Spring 管理的分发服务
         */
        @Bean
        SsoIdentityDistributionServiceImpl ssoIdentityDistributionService(
                SsoCurrentIdentitySnapshotLoader ssoCurrentIdentitySnapshotLoader,
                MqProducerService mqProducerService,
                ObjectMapper objectMapper,
                SsoDistributionDispatchResultRecorder ssoDistributionDispatchResultRecorder,
                SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder
        ) {
            return new SsoIdentityDistributionServiceImpl(
                    ssoCurrentIdentitySnapshotLoader,
                    mqProducerService,
                    objectMapper,
                    ssoDistributionDispatchResultRecorder,
                    ssoSyncTaskFailureRecorder
            );
        }
    }

    /**
     * 测试专用事务管理器，用于记录事务开启、提交与回滚次数。
     */
    static class ProbeTransactionManager extends AbstractPlatformTransactionManager {

        /** 已开启事务次数。 */
        private final AtomicInteger beginCount = new AtomicInteger();

        /** 已提交事务次数。 */
        private final AtomicInteger commitCount = new AtomicInteger();

        /** 已回滚事务次数。 */
        private final AtomicInteger rollbackCount = new AtomicInteger();

        /**
         * 重置事务计数器，便于多个测试复用同一 bean。
         */
        void reset() {
            beginCount.set(0);
            commitCount.set(0);
            rollbackCount.set(0);
        }

        /**
         * @return 已提交事务次数
         */
        int getCommitCount() {
            return commitCount.get();
        }

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            beginCount.incrementAndGet();
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commitCount.incrementAndGet();
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbackCount.incrementAndGet();
        }
    }
}
