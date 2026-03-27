/**
 * @file 验证同步任务失败状态会通过独立事务持久化
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.system.mapper.SsoSyncTaskMapper;
import com.yr.system.service.ISsoIdentityImportService;
import com.yr.system.service.ISsoSyncTaskItemService;
import com.yr.system.service.support.SsoSyncTaskFailureRecorder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 使用最小 Spring 事务上下文验证 FAILED 状态会在独立事务中提交。
 */
@SpringJUnitConfig(classes = SsoSyncTaskFailurePersistenceTest.TestConfig.class)
class SsoSyncTaskFailurePersistenceTest {

    @Autowired
    private SsoSyncTaskServiceImpl ssoSyncTaskService;

    @Autowired
    private SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder;

    @Autowired
    private ProbeTransactionManager transactionManager;

    @Autowired
    private SsoSyncTaskMapper ssoSyncTaskMapper;

    @Autowired
    private ISsoIdentityImportService ssoIdentityImportService;

    @Autowired
    private ISsoSyncTaskItemService ssoSyncTaskItemService;

    /**
     * 每个用例结束后重置事务探针与 mock，避免状态污染。
     */
    @AfterEach
    void tearDown() {
        transactionManager.reset();
        Mockito.reset(ssoSyncTaskMapper, ssoIdentityImportService, ssoSyncTaskItemService);
    }

    /**
     * 验证 retryTask 执行器抛错时，会触发外层回滚 + 内层失败状态提交。
     */
    @Test
    void shouldPersistFailedStatusInRequiresNewTransactionWhenRetryExecutionThrows() {
        List<String> persistedStatuses = new ArrayList<>();
        List<String> persistedSummaries = new ArrayList<>();
        SsoSyncTask existingTask = buildExistingTask();

        when(ssoSyncTaskMapper.selectById(11L)).thenReturn(existingTask);
        when(ssoSyncTaskMapper.updateById(any(SsoSyncTask.class))).thenAnswer(invocation -> {
            SsoSyncTask persistedTask = invocation.getArgument(0);
            persistedStatuses.add(persistedTask.getStatus());
            persistedSummaries.add(persistedTask.getResultSummary());
            return 1;
        });
        when(ssoIdentityImportService.execute(eq(existingTask), eq(null)))
                .thenThrow(new RuntimeException("legacy \"source\" unavailable / dept"));

        assertThat(AopUtils.isAopProxy(ssoSyncTaskService)).isTrue();
        assertThat(AopUtils.isAopProxy(ssoSyncTaskFailureRecorder)).isTrue();

        assertThatThrownBy(() -> ssoSyncTaskService.retryTask(11L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("legacy \"source\" unavailable / dept");

        assertThat(transactionManager.getBeginCount()).isEqualTo(2);
        assertThat(transactionManager.getCommitCount()).isEqualTo(1);
        assertThat(transactionManager.getRollbackCount()).isEqualTo(1);
        assertThat(persistedStatuses).containsExactly(SsoSyncTask.STATUS_RUNNING, SsoSyncTask.STATUS_FAILED);
        assertThat(persistedSummaries).last().satisfies(summary ->
                assertThat(summary).contains("legacy \"source\" unavailable / dept")
        );
    }

    /**
     * 构造已有同步任务，模拟数据库中已存在的 retry 目标。
     *
     * @return 已存在任务
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
     * 测试专用 Spring 配置，只加载同步任务事务验证所需的最小 Bean 集合。
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
         * @return 同步任务 Mapper mock
         */
        @Bean
        SsoSyncTaskMapper ssoSyncTaskMapper() {
            return mock(SsoSyncTaskMapper.class);
        }

        /**
         * @return INIT_IMPORT 执行器 mock
         */
        @Bean
        ISsoIdentityImportService ssoIdentityImportService() {
            return mock(ISsoIdentityImportService.class);
        }

        /**
         * @return 任务明细服务 mock
         */
        @Bean
        ISsoSyncTaskItemService ssoSyncTaskItemService() {
            return mock(ISsoSyncTaskItemService.class);
        }

        /**
         * @param ssoSyncTaskMapper 同步任务 Mapper
         * @return 失败状态记录器
         */
        @Bean
        SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder(SsoSyncTaskMapper ssoSyncTaskMapper) {
            return new SsoSyncTaskFailureRecorder(ssoSyncTaskMapper);
        }

        /**
         * @param ssoSyncTaskMapper 同步任务 Mapper
         * @param ssoIdentityImportService INIT_IMPORT 执行器
         * @param ssoSyncTaskItemService 任务明细服务
         * @param ssoSyncTaskFailureRecorder 失败状态记录器
         * @return Spring 管理的同步任务服务
         */
        @Bean
        SsoSyncTaskServiceImpl ssoSyncTaskService(SsoSyncTaskMapper ssoSyncTaskMapper,
                                                  ISsoIdentityImportService ssoIdentityImportService,
                                                  ISsoSyncTaskItemService ssoSyncTaskItemService,
                                                  SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder) {
            SsoSyncTaskServiceImpl service = new SsoSyncTaskServiceImpl();
            injectBaseMapper(service, ssoSyncTaskMapper);
            ReflectionTestUtils.setField(service, "ssoIdentityImportService", ssoIdentityImportService);
            ReflectionTestUtils.setField(service, "ssoSyncTaskItemService", ssoSyncTaskItemService);
            ReflectionTestUtils.setField(service, "ssoSyncTaskFailureRecorder", ssoSyncTaskFailureRecorder);
            return service;
        }

        /**
         * 手动补齐 MyBatis-Plus 基类 mapper，确保继承方法命中同一个 mock。
         *
         * @param target 服务对象
         * @param mapper 同步任务 Mapper
         */
        private void injectBaseMapper(SsoSyncTaskServiceImpl target, SsoSyncTaskMapper mapper) {
            try {
                Field serviceImplField = ServiceImpl.class.getDeclaredField("baseMapper");
                serviceImplField.setAccessible(true);
                serviceImplField.set(target, mapper);

                Field customServiceField = CustomServiceImpl.class.getDeclaredField("baseMapper");
                customServiceField.setAccessible(true);
                customServiceField.set(target, mapper);
            } catch (NoSuchFieldException | IllegalAccessException exception) {
                throw new IllegalStateException("注入同步任务基类 mapper 失败", exception);
            }
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
         * @return 已开启事务次数
         */
        int getBeginCount() {
            return beginCount.get();
        }

        /**
         * @return 已提交事务次数
         */
        int getCommitCount() {
            return commitCount.get();
        }

        /**
         * @return 已回滚事务次数
         */
        int getRollbackCount() {
            return rollbackCount.get();
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
