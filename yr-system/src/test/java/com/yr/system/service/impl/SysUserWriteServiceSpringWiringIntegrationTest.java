/**
 * @file 验证 SysUser 导入链路在 Spring 上下文中会通过事务代理执行
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.system.domain.entity.SysUserOrg;
import com.yr.system.mapper.SysUserMapper;
import com.yr.system.service.ISysUserOrgService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Spring 上下文下的用户写入代理集成测试。
 */
@SpringJUnitConfig(classes = SysUserWriteServiceSpringWiringIntegrationTest.TestConfig.class)
class SysUserWriteServiceSpringWiringIntegrationTest {

    @Autowired
    private SysUserWriteService sysUserWriteService;

    @Autowired
    private SysUserImportService sysUserImportService;

    @Autowired
    private ProbeTransactionManager transactionManager;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private ISysUserOrgService userOrgService;

    /**
     * 每个用例结束后清理安全上下文和 mock 状态，避免测试相互污染。
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        transactionManager.reset();
        Mockito.reset(userMapper, userOrgService);
    }

    /**
     * 验证导入链路会通过 Spring 管理的事务代理进入写服务。
     */
    @Test
    void shouldUseTransactionalSpringProxyWhenImportingUser() {
        AtomicBoolean userInsertInTransaction = new AtomicBoolean(false);
        AtomicBoolean userOrgWriteInTransaction = new AtomicBoolean(false);
        SysUser user = buildUser("spring-integration", null);

        setAuthenticatedUser(77L, 88L, "spring-tester");
        when(userMapper.selectUserByUserName(anyString())).thenReturn(null);
        when(userMapper.insertUser(any(SysUser.class))).thenAnswer(invocation -> {
            userInsertInTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
            SysUser insertedUser = invocation.getArgument(0);
            insertedUser.setUserId(501L);
            return 1;
        });
        when(userOrgService.count(any(Wrapper.class))).thenAnswer(invocation -> {
            userOrgWriteInTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
            return 0L;
        });
        doAnswer(invocation -> {
            userOrgWriteInTransaction.set(userOrgWriteInTransaction.get() && TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(userOrgService).addSysUserOrg(any(SysUserOrg.class));

        String result = sysUserImportService.importUser(List.of(user), false, "spring-tester");

        assertThat(AopUtils.isAopProxy(sysUserWriteService)).isTrue();
        assertThat(result).contains("数据已全部导入成功").contains("spring-integration");
        assertThat(transactionManager.getBeginCount()).isEqualTo(1);
        assertThat(transactionManager.getCommitCount()).isEqualTo(1);
        assertThat(transactionManager.getRollbackCount()).isZero();
        assertThat(userInsertInTransaction.get()).isTrue();
        assertThat(userOrgWriteInTransaction.get()).isTrue();
        verify(userMapper).insertUser(any(SysUser.class));
        verify(userOrgService).addSysUserOrg(any(SysUserOrg.class));
    }

    /**
     * 构造导入用户命令。
     *
     * @param userName 用户账号
     * @param rankId 职级 ID
     * @return 用户命令
     */
    private SysUser buildUser(String userName, Long rankId) {
        SysUser user = new SysUser();
        user.setUserName(userName);
        user.setNickName("测试用户");
        user.setRankId(rankId);
        return user;
    }

    /**
     * 写入测试所需的安全上下文，供 SecurityUtils 获取用户与组织信息。
     *
     * @param userId 用户 ID
     * @param orgId 组织 ID
     * @param userName 用户账号
     */
    private void setAuthenticatedUser(Long userId, Long orgId, String userName) {
        SysUser authenticatedUser = new SysUser();
        authenticatedUser.setUserId(userId);
        authenticatedUser.setOrgId(orgId);
        authenticatedUser.setUserName(userName);
        LoginUser loginUser = new LoginUser(authenticatedUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(loginUser, null));
    }

    /**
     * 测试专用 Spring 配置，只加载导入链路所需的最小 Bean 集合。
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
         * @return 用户 Mapper mock
         */
        @Bean
        SysUserMapper userMapper() {
            return mock(SysUserMapper.class);
        }

        /**
         * @return 用户组织服务 mock
         */
        @Bean
        ISysUserOrgService userOrgService() {
            return mock(ISysUserOrgService.class);
        }

        /**
         * @return Spring 管理的用户写入服务
         */
        @Bean
        SysUserWriteService sysUserWriteService(SysUserMapper userMapper,
                                                ISysUserOrgService userOrgService) {
            return new SysUserWriteService(userMapper, userOrgService);
        }

        /**
         * @return Spring 管理的用户导入服务
         */
        @Bean
        SysUserImportService sysUserImportService(SysUserMapper userMapper,
                                                  SysUserWriteService sysUserWriteService) {
            return new SysUserImportService("Init@123", userMapper, sysUserWriteService);
        }
    }

    /**
     * 测试用事务管理器，记录事务开启、提交与回滚次数。
     */
    static class ProbeTransactionManager extends AbstractPlatformTransactionManager {

        /** 已开启事务次数。 */
        private final AtomicInteger beginCount = new AtomicInteger();
        /** 已提交事务次数。 */
        private final AtomicInteger commitCount = new AtomicInteger();
        /** 已回滚事务次数。 */
        private final AtomicInteger rollbackCount = new AtomicInteger();

        /**
         * 重置计数器，便于多个测试复用同一 bean。
         */
        void reset() {
            beginCount.set(0);
            commitCount.set(0);
            rollbackCount.set(0);
        }

        /**
         * @return 开启事务次数
         */
        int getBeginCount() {
            return beginCount.get();
        }

        /**
         * @return 提交事务次数
         */
        int getCommitCount() {
            return commitCount.get();
        }

        /**
         * @return 回滚事务次数
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
