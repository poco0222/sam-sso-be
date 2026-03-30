/**
 * @file 验证用户默认组织切换的事务边界
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.yr.common.exception.CustomException;
import com.yr.system.mapper.SysOrgMapper;
import com.yr.system.mapper.SysUserOrgMapper;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SysUserOrgServiceImpl 事务行为测试。
 */
class SysUserOrgServiceImplTransactionTest {

    /**
     * 验证设置新默认组织失败时会触发事务回滚。
     */
    @Test
    void shouldRollbackWhenSettingNewDefaultOrgFails() {
        ProbeTransactionManager transactionManager = new ProbeTransactionManager();
        SysUserOrgServiceImpl proxiedService = buildProxiedService(transactionManager, mapperReturningSecondUpdateZero());

        assertThatThrownBy(() -> proxiedService.setDefaultUserOrg(10L, 20L))
                .isInstanceOf(CustomException.class)
                .hasMessage("设置失败");

        assertThat(transactionManager.getRollbackCount()).isEqualTo(1);
    }

    /**
     * 构造织入注解事务拦截器的代理对象。
     *
     * @param transactionManager 事务管理器探针
     * @param mapper 用户组织 Mapper 桩
     * @return 代理后的服务对象
     */
    private SysUserOrgServiceImpl buildProxiedService(ProbeTransactionManager transactionManager,
                                                      SysUserOrgMapper mapper) {
        SysUserOrgServiceImpl target = new SysUserOrgServiceImpl(mapper, mock(SysOrgMapper.class));
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(new TransactionInterceptor(transactionManager, new AnnotationTransactionAttributeSource()));
        return (SysUserOrgServiceImpl) proxyFactory.getProxy();
    }

    /**
     * 构造第二次更新返回 0 的 Mapper，模拟设置默认组织失败。
     *
     * @return 用户组织 Mapper 桩
     */
    private SysUserOrgMapper mapperReturningSecondUpdateZero() {
        SysUserOrgMapper mapper = mock(SysUserOrgMapper.class);
        when(mapper.clearDefaultUserOrg(anyLong())).thenReturn(1);
        when(mapper.setDefaultUserOrg(anyLong(), anyLong())).thenReturn(0);
        return mapper;
    }

    /**
     * 最小事务管理器探针，用于记录事务提交和回滚次数。
     */
    private static final class ProbeTransactionManager extends AbstractPlatformTransactionManager {

        /** 记录事务回滚次数。 */
        private int rollbackCount;

        /**
         * 记录事务开启时机。
         *
         * @return 事务对象占位符
         */
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        /**
         * 开启事务时无需真正连接外部资源。
         *
         * @param definition 事务定义
         * @param transaction 事务对象
         */
        @Override
        protected void doBegin(Object transaction, org.springframework.transaction.TransactionDefinition definition) {
            // 单元测试只需要统计事务回滚次数，不需要真实开启数据库事务。
        }

        /**
         * 提交事务时无需额外动作。
         *
         * @param status 事务状态
         */
        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            // 当前用例只验证失败场景，提交阶段无需处理。
        }

        /**
         * 记录事务回滚次数。
         *
         * @param status 事务状态
         */
        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbackCount++;
        }

        /**
         * @return 已记录的事务回滚次数
         */
        private int getRollbackCount() {
            return rollbackCount;
        }
    }
}
