/**
 * @file 验证 SysUser 导入链路的事务边界与逐条处理策略
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.exception.CustomException;
import com.yr.system.domain.entity.SysRank;
import com.yr.system.domain.entity.SysUserOrg;
import com.yr.system.domain.entity.SysUserRank;
import com.yr.system.mapper.SysPostMapper;
import com.yr.system.mapper.SysRoleMapper;
import com.yr.system.mapper.SysUserMapper;
import com.yr.system.mapper.SysUserPostMapper;
import com.yr.system.mapper.SysUserRoleMapper;
import com.yr.system.service.ISysConfigService;
import com.yr.system.service.ISysOrgService;
import com.yr.system.service.ISysRankService;
import com.yr.system.service.ISysUserOrgService;
import com.yr.system.service.ISysUserDeptService;
import com.yr.system.service.ISysUserDutyService;
import com.yr.system.service.ISysUserRankService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.NameMatchTransactionAttributeSource;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysUser 导入事务行为测试。
 */
class SysUserServiceImplTransactionTest {

    /**
     * 每个用例后清理安全上下文，避免测试相互污染。
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 验证用户主表、组织关联和职级关联都在同一个事务中写入。
     */
    @Test
    void shouldWriteUserOrgAndRankWithinSameTransaction() {
        ProbeTransactionManager transactionManager = new ProbeTransactionManager();
        SysUserMapper userMapper = mock(SysUserMapper.class);
        ISysRankService rankService = mock(ISysRankService.class);
        ISysUserOrgService userOrgService = mock(ISysUserOrgService.class);
        ISysUserRankService userRankService = mock(ISysUserRankService.class);
        AtomicBoolean userInsertInTx = new AtomicBoolean(false);
        AtomicBoolean userOrgInsertInTx = new AtomicBoolean(false);
        AtomicBoolean userRankInsertInTx = new AtomicBoolean(false);
        SysUserWriteService target = new SysUserWriteService(userMapper, rankService, userOrgService, userRankService);
        SysUserWriteService proxy = createWriteServiceProxy(target, transactionManager);
        SysUser user = buildUser("phase1-insert", 1L);
        SysRank sysRank = new SysRank();
        sysRank.setId(1L);
        sysRank.setRankType("LEAF");

        setAuthenticatedOrg(88L);
        when(rankService.getById(1L)).thenReturn(sysRank);
        when(userMapper.insertUser(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser insertedUser = invocation.getArgument(0);
            userInsertInTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            insertedUser.setUserId(101L);
            return 1;
        });
        when(userOrgService.count(any(Wrapper.class))).thenAnswer(invocation -> {
            userOrgInsertInTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            return 0L;
        });
        doAnswer(invocation -> {
            userOrgInsertInTx.set(userOrgInsertInTx.get() && TransactionSynchronizationManager.isActualTransactionActive());
            return null;
        }).when(userOrgService).addSysUserOrg(any(SysUserOrg.class));
        when(userRankService.save(any(SysUserRank.class))).thenAnswer(invocation -> {
            userRankInsertInTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            return true;
        });

        proxy.insertUser(user);

        ArgumentCaptor<SysUserOrg> userOrgCaptor = ArgumentCaptor.forClass(SysUserOrg.class);
        ArgumentCaptor<SysUserRank> userRankCaptor = ArgumentCaptor.forClass(SysUserRank.class);
        verify(userOrgService).addSysUserOrg(userOrgCaptor.capture());
        verify(userRankService).save(userRankCaptor.capture());
        assertThat(userInsertInTx.get()).isTrue();
        assertThat(userOrgInsertInTx.get()).isTrue();
        assertThat(userRankInsertInTx.get()).isTrue();
        assertThat(transactionManager.getBeginCount()).isEqualTo(1);
        assertThat(transactionManager.getCommitCount()).isEqualTo(1);
        assertThat(transactionManager.getRollbackCount()).isZero();
        assertThat(userOrgCaptor.getValue().getUserId()).isEqualTo(101L);
        assertThat(userOrgCaptor.getValue().getOrgId()).isEqualTo(88L);
        assertThat(userOrgCaptor.getValue().getIsDefault()).isEqualTo(1);
        assertThat(userRankCaptor.getValue().getUserId()).isEqualTo(101L);
        assertThat(userRankCaptor.getValue().getRankId()).isEqualTo(1L);
    }

    /**
     * 验证批量导入采用逐条事务策略，一条失败不会回滚其他已成功的数据。
     */
    @Test
    void shouldCommitSuccessfulRowsWhenAnotherImportedUserFails() {
        ProbeTransactionManager transactionManager = new ProbeTransactionManager();
        SysUserMapper userMapper = mock(SysUserMapper.class);
        ISysRankService rankService = mock(ISysRankService.class);
        ISysUserOrgService userOrgService = mock(ISysUserOrgService.class);
        ISysUserRankService userRankService = mock(ISysUserRankService.class);
        ISysConfigService configService = mock(ISysConfigService.class);
        SysUserWriteService writeTarget = new SysUserWriteService(userMapper, rankService, userOrgService, userRankService);
        SysUserWriteService writeProxy = createWriteServiceProxy(writeTarget, transactionManager);
        SysUserImportService importService = new SysUserImportService(configService, userMapper, writeProxy);
        SysUserServiceImpl userService = buildUserService(userMapper, writeProxy, importService);
        SysUser successUser = buildUser("phase1-success", 1L);
        SysUser failureUser = buildUser("phase1-failure", null);
        SysRank sysRank = new SysRank();
        sysRank.setId(1L);
        sysRank.setRankType("LEAF");

        setAuthenticatedOrg(66L);
        when(configService.selectConfigByKey("sys.user.initPassword")).thenReturn("Init@123");
        when(userMapper.selectUserByUserName(anyString())).thenReturn(null);
        when(rankService.getById(1L)).thenReturn(sysRank);
        when(userMapper.insertUser(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser insertedUser = invocation.getArgument(0);
            insertedUser.setUserId(202L);
            return 1;
        });
        when(userOrgService.count(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> null).when(userOrgService).addSysUserOrg(any(SysUserOrg.class));
        when(userRankService.save(any(SysUserRank.class))).thenReturn(true);

        String result = userService.importUser(Arrays.asList(successUser, failureUser), false, "phase1");

        assertThat(result)
                .contains("导入完成！成功 1 条，失败 1 条")
                .contains("账号 phase1-success 导入成功")
                .contains("账号 phase1-failure 导入失败：职级不能为空");
        assertThat(transactionManager.getBeginCount()).isEqualTo(2);
        assertThat(transactionManager.getCommitCount()).isEqualTo(1);
        assertThat(transactionManager.getRollbackCount()).isEqualTo(1);
        verify(userMapper).insertUser(argThat(user -> "phase1-success".equals(user.getUserName())));
        verify(userMapper, never()).insertUser(argThat(user -> "phase1-failure".equals(user.getUserName())));
    }

    /**
     * 验证系统异常会中止剩余导入，但不会回滚此前已经成功提交的用户。
     */
    @Test
    void shouldStopImportingRemainingUsersWhenUnexpectedSystemExceptionOccurs() {
        ProbeTransactionManager transactionManager = new ProbeTransactionManager();
        SysUserMapper userMapper = mock(SysUserMapper.class);
        ISysRankService rankService = mock(ISysRankService.class);
        ISysUserOrgService userOrgService = mock(ISysUserOrgService.class);
        ISysUserRankService userRankService = mock(ISysUserRankService.class);
        ISysConfigService configService = mock(ISysConfigService.class);
        SysUserWriteService writeTarget = new SysUserWriteService(userMapper, rankService, userOrgService, userRankService);
        SysUserWriteService writeProxy = createWriteServiceProxy(writeTarget, transactionManager);
        SysUserImportService importService = new SysUserImportService(configService, userMapper, writeProxy);
        SysUserServiceImpl userService = buildUserService(userMapper, writeProxy, importService);
        SysUser successUser = buildUser("phase4-success", 1L);
        SysUser brokenUser = buildUser("phase4-broken", 1L);
        SysUser untouchedUser = buildUser("phase4-untouched", 1L);
        SysRank sysRank = new SysRank();
        sysRank.setId(1L);
        sysRank.setRankType("LEAF");

        setAuthenticatedOrg(66L);
        when(configService.selectConfigByKey("sys.user.initPassword")).thenReturn("Init@123");
        when(userMapper.selectUserByUserName(anyString())).thenReturn(null);
        when(rankService.getById(1L)).thenReturn(sysRank);
        when(userMapper.insertUser(argThat(user -> user != null && "phase4-success".equals(user.getUserName())))).thenAnswer(invocation -> {
            SysUser insertedUser = invocation.getArgument(0);
            insertedUser.setUserId(303L);
            return 1;
        });
        when(userMapper.insertUser(argThat(user -> user != null && "phase4-broken".equals(user.getUserName()))))
                .thenThrow(new IllegalStateException("db boom"));
        when(userOrgService.count(any(Wrapper.class))).thenReturn(0L);
        doAnswer(invocation -> null).when(userOrgService).addSysUserOrg(any(SysUserOrg.class));
        when(userRankService.save(any(SysUserRank.class))).thenReturn(true);

        assertThatThrownBy(() -> userService.importUser(List.of(successUser, brokenUser, untouchedUser), false, "phase4"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db boom");

        assertThat(transactionManager.getBeginCount()).isEqualTo(2);
        assertThat(transactionManager.getCommitCount()).isEqualTo(1);
        assertThat(transactionManager.getRollbackCount()).isEqualTo(1);
        verify(userMapper, never()).insertUser(argThat(user -> user != null && "phase4-untouched".equals(user.getUserName())));
    }

    /**
     * 创建为写服务织入事务通知的代理对象。
     *
     * @param target 被代理写服务
     * @param transactionManager 事务管理器探针
     * @return 代理后的写服务
     */
    private SysUserWriteService createWriteServiceProxy(SysUserWriteService target,
                                                        ProbeTransactionManager transactionManager) {
        NameMatchTransactionAttributeSource attributeSource = new NameMatchTransactionAttributeSource();
        RuleBasedTransactionAttribute transactionAttribute = new RuleBasedTransactionAttribute();
        transactionAttribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionAttribute.setRollbackRules(List.of(new RollbackRuleAttribute(Exception.class)));
        attributeSource.addTransactionalMethod("insertUser", transactionAttribute);
        attributeSource.addTransactionalMethod("updateUser", transactionAttribute);

        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(new TransactionInterceptor(transactionManager, attributeSource));
        return (SysUserWriteService) proxyFactory.getProxy();
    }

    /**
     * 构造最小依赖的用户服务实例，复用导入链路测试装配。
     *
     * @param userMapper 用户 Mapper
     * @param writeProxy 带事务代理的写服务
     * @param importService 导入服务
     * @return 用户服务实例
     */
    private SysUserServiceImpl buildUserService(SysUserMapper userMapper,
                                                SysUserWriteService writeProxy,
                                                SysUserImportService importService) {
        return new SysUserServiceImpl(
                userMapper,
                mock(SysRoleMapper.class),
                mock(SysPostMapper.class),
                mock(SysUserRoleMapper.class),
                mock(SysUserPostMapper.class),
                mock(ISysUserDeptService.class),
                mock(ISysUserRankService.class),
                mock(ISysOrgService.class),
                mock(ISysUserDutyService.class),
                writeProxy,
                importService,
                mock(SysUserQueryService.class)
        );
    }

    /**
     * 构造导入测试用户。
     *
     * @param userName 用户名
     * @param rankId 职级 ID
     * @return 用户对象
     */
    private SysUser buildUser(String userName, Long rankId) {
        SysUser user = new SysUser();
        user.setUserName(userName);
        user.setRankId(rankId);
        return user;
    }

    /**
     * 为测试设置最小登录上下文，供 SecurityUtils.getOrgId() 使用。
     *
     * @param orgId 当前组织 ID
     */
    private void setAuthenticatedOrg(Long orgId) {
        SysUser currentUser = new SysUser();
        currentUser.setUserId(999L);
        currentUser.setUserName("phase1-operator");
        currentUser.setOrgId(orgId);
        LoginUser loginUser = new LoginUser(currentUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(loginUser, null));
    }

    /**
     * 最小事务管理器探针，用于记录事务开启、提交与回滚次数。
     */
    private static final class ProbeTransactionManager extends AbstractPlatformTransactionManager {

        /** 记录事务开启次数。 */
        private int beginCount;

        /** 记录事务提交次数。 */
        private int commitCount;

        /** 记录事务回滚次数。 */
        private int rollbackCount;

        /**
         * 初始化事务同步策略，保证测试中能读取到事务上下文。
         */
        private ProbeTransactionManager() {
            setTransactionSynchronization(SYNCHRONIZATION_ALWAYS);
        }

        /**
         * @return 虚拟事务对象
         */
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        /**
         * 记录事务开启次数。
         *
         * @param transaction 虚拟事务对象
         * @param definition 事务定义
         */
        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            beginCount++;
        }

        /**
         * 记录事务提交次数。
         *
         * @param status 事务状态
         */
        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commitCount++;
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
         * @return 已开启事务次数
         */
        int getBeginCount() {
            return beginCount;
        }

        /**
         * @return 已提交事务次数
         */
        int getCommitCount() {
            return commitCount;
        }

        /**
         * @return 已回滚事务次数
         */
        int getRollbackCount() {
            return rollbackCount;
        }
    }
}
