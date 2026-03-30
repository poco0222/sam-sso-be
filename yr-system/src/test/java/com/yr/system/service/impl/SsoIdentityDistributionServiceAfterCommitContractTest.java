/**
 * @file DISTRIBUTION after-commit 契约测试
 * @author PopoY
 * @date 2026-03-30
 */
package com.yr.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.service.MqProducerService;
import com.yr.system.service.support.SsoCurrentIdentitySnapshotLoader;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定 MQ 外发必须在事务提交后发生，避免 DB 回滚时消息已出队。
 */
@SpringJUnitConfig(classes = SsoIdentityDistributionServiceAfterCommitContractTest.TestConfig.class)
class SsoIdentityDistributionServiceAfterCommitContractTest {

    @Autowired
    private SsoIdentityDistributionServiceImpl ssoIdentityDistributionService;

    @Autowired
    private ProbeTransactionManager transactionManager;

    @Autowired
    private SsoCurrentIdentitySnapshotLoader ssoCurrentIdentitySnapshotLoader;

    @Autowired
    private MqProducerService mqProducerService;

    /**
     * 每个用例后重置 mock 与事务计数器，避免状态串用。
     */
    @AfterEach
    void tearDown() {
        transactionManager.reset();
        Mockito.reset(ssoCurrentIdentitySnapshotLoader, mqProducerService);
    }

    /**
     * 验证外层事务在任务明细写库失败并回滚时，MQ 发送不得先发生。
     */
    @Test
    void shouldNotSendMqBeforeOuterTransactionCommits() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        when(ssoCurrentIdentitySnapshotLoader.loadSnapshot()).thenReturn(SsoDistributionTestFixtures.minimalSnapshot());
        when(mqProducerService.send(any(), any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            ssoIdentityDistributionService.execute(buildTask(), null);
            throw new IllegalStateException("task item persistence failed");
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("task item persistence failed");

        assertThat(transactionManager.getRollbackCount()).isGreaterThanOrEqualTo(1);
        verify(mqProducerService, never()).send(any(), any(), any(), any(), any());
    }

    /**
     * 构造最小分发任务。
     *
     * @return 同步任务
     */
    private SsoSyncTask buildTask() {
        SsoSyncTask task = new SsoSyncTask();
        task.setTaskId(88L);
        task.setBatchNo("DIST-88");
        task.setTargetClientCode("sam-mgmt");
        task.setCreateBy("phase1");
        return task;
    }

    /**
     * 测试专用 Spring 配置，只加载 after-commit 契约所需的最小 Bean。
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
         * @return Spring 管理的分发服务
         */
        @Bean
        SsoIdentityDistributionServiceImpl ssoIdentityDistributionService(
                SsoCurrentIdentitySnapshotLoader ssoCurrentIdentitySnapshotLoader,
                MqProducerService mqProducerService,
                ObjectMapper objectMapper
        ) {
            return new SsoIdentityDistributionServiceImpl(
                    ssoCurrentIdentitySnapshotLoader,
                    mqProducerService,
                    objectMapper
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
