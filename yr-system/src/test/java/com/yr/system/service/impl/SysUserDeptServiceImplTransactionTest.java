/**
 * @file 验证用户默认部门切换的事务边界
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.exception.CustomException;
import com.yr.system.domain.entity.SysUserDept;
import com.yr.system.mapper.SysDeptMapper;
import com.yr.system.mapper.SysUserDeptMapper;
import com.yr.system.mapper.SysUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SysUserDeptServiceImpl 事务行为测试。
 */
class SysUserDeptServiceImplTransactionTest {

    /**
     * 每个测试结束后清理安全上下文，避免组织信息污染其他用例。
     */
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 验证设置默认部门方法声明了事务边界。
     *
     * @throws NoSuchMethodException 方法不存在时抛出
     */
    @Test
    void shouldDeclareTransactionalOnSetDefaultUserDept() throws NoSuchMethodException {
        Method method = SysUserDeptServiceImpl.class.getMethod("setDefaultUserDept", Long.class, Long.class);

        assertThat(method.getAnnotation(Transactional.class)).isNotNull();
    }

    /**
     * 验证设置新默认部门失败时会触发事务回滚。
     */
    @Test
    void shouldRollbackWhenUpdatingNewDefaultDeptFails() {
        setAuthenticatedOrg(30L);
        ProbeTransactionManager transactionManager = new ProbeTransactionManager();
        SysUserDeptServiceImpl proxiedService = buildProxiedService(transactionManager, mapperWithSecondUpdateFailure());

        assertThatThrownBy(() -> proxiedService.setDefaultUserDept(10L, 20L))
                .isInstanceOf(CustomException.class)
                .hasMessage("设置失败");

        assertThat(transactionManager.getRollbackCount()).isEqualTo(1);
    }

    /**
     * 构造织入注解事务拦截器的代理对象。
     *
     * @param transactionManager 事务管理器探针
     * @param mapper 用户部门 Mapper 桩
     * @return 代理后的服务对象
     */
    private SysUserDeptServiceImpl buildProxiedService(ProbeTransactionManager transactionManager,
                                                       SysUserDeptMapper mapper) {
        SysUserDeptServiceImpl target = new SysUserDeptServiceImpl(mapper, mock(SysDeptMapper.class), mock(SysUserMapper.class));
        injectBaseMapper(target, mapper);

        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(new TransactionInterceptor(transactionManager, new AnnotationTransactionAttributeSource()));
        return (SysUserDeptServiceImpl) proxyFactory.getProxy();
    }

    /**
     * 手动补齐 MyBatis-Plus 基类 mapper，保证继承方法走到同一个 mock 上。
     *
     * @param target 服务对象
     * @param mapper 用户部门 Mapper 桩
     */
    private void injectBaseMapper(SysUserDeptServiceImpl target, SysUserDeptMapper mapper) {
        try {
            Field serviceImplField = ServiceImpl.class.getDeclaredField("baseMapper");
            serviceImplField.setAccessible(true);
            serviceImplField.set(target, mapper);

            Field customServiceField = CustomServiceImpl.class.getDeclaredField("baseMapper");
            customServiceField.setAccessible(true);
            customServiceField.set(target, mapper);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalStateException("注入基类 mapper 失败", exception);
        }
    }

    /**
     * 构造第二次更新失败的 Mapper，模拟默认部门切换在最后一步失败。
     *
     * @return 用户部门 Mapper 桩
     */
    private SysUserDeptMapper mapperWithSecondUpdateFailure() {
        SysUserDeptMapper mapper = mock(SysUserDeptMapper.class);
        SysUserDept relation = new SysUserDept();
        relation.setId(100L);
        relation.setUserId(10L);
        relation.setDeptId(20L);
        relation.setIsDefault(0);

        when(mapper.selectOne(any())).thenReturn(relation);
        when(mapper.selectDefaultUserDeptId(10L, 30L)).thenReturn(99L);
        when(mapper.clearDefaultUserDept(anyLong())).thenReturn(1);
        when(mapper.updateById(any(SysUserDept.class))).thenReturn(0);
        return mapper;
    }

    /**
     * 为测试设置最小登录上下文，供 SecurityUtils.getOrgId() 使用。
     *
     * @param orgId 当前组织 ID
     */
    private void setAuthenticatedOrg(Long orgId) {
        SysUser currentUser = new SysUser();
        currentUser.setUserId(999L);
        currentUser.setUserName("phase2-operator");
        currentUser.setOrgId(orgId);
        LoginUser loginUser = new LoginUser(currentUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(loginUser, null));
    }

    /**
     * 最小事务管理器探针，用于记录事务回滚次数。
     */
    private static final class ProbeTransactionManager extends AbstractPlatformTransactionManager {

        /** 记录事务回滚次数。 */
        private int rollbackCount;

        /**
         * @return 虚拟事务对象
         */
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        /**
         * 单元测试无需真实开启数据库事务。
         *
         * @param transaction 虚拟事务对象
         * @param definition 事务定义
         */
        @Override
        protected void doBegin(Object transaction, org.springframework.transaction.TransactionDefinition definition) {
            // 这里仅验证事务是否触发回滚，不需要外部资源。
        }

        /**
         * 当前用例只验证失败路径，提交阶段无需处理。
         *
         * @param status 事务状态
         */
        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            // 无需额外动作。
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
