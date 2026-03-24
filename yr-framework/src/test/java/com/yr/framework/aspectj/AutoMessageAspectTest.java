/**
 * @file 验证 AutoMessageAspect 的默认值与自定义值行为
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.framework.aspectj;

import com.yr.common.annotation.AutoMessage;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.enums.MessageType;
import com.yr.system.component.message.IMessageClient;
import com.yr.system.component.message.IMessageEntity;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AutoMessageAspect 行为测试。
 */
class AutoMessageAspectTest {

    /**
     * 每个用例后清理安全上下文，避免污染其它测试。
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 验证注解未提供正文和标题时会使用默认消息内容。
     *
     * @throws Throwable 切面执行异常
     */
    @Test
    void shouldUseDefaultMessageValuesWhenAnnotationStringsAreBlank() throws Throwable {
        IMessageClient client = mock(IMessageClient.class);
        AutoMessageAspect aspect = new AutoMessageAspect(client);
        setAuthenticatedUser(77L);
        ProceedingJoinPoint joinPoint = mockJoinPoint(new AutoMessageProbeTarget(), "sendDefaultNotice");

        Object result = aspect.message(joinPoint);

        ArgumentCaptor<IMessageEntity> entityCaptor = ArgumentCaptor.forClass(IMessageEntity.class);
        ArgumentCaptor<List> usersCaptor = ArgumentCaptor.forClass(List.class);
        verify(client).sendWebMessage(entityCaptor.capture(), usersCaptor.capture());
        assertThat(result).isEqualTo("done");
        assertThat(entityCaptor.getValue().getBody()).isEqualTo("刷新页面");
        assertThat(entityCaptor.getValue().getTitle()).isEqualTo("站内消息通知");
        assertThat(entityCaptor.getValue().getName()).isNull();
        assertThat(entityCaptor.getValue().getMessageType()).isEqualTo(MessageType.WEB_MESSAGE_NOTIFY);
        assertThat(usersCaptor.getValue()).containsExactly(77L);
    }

    /**
     * 验证注解显式提供的标题、名称、正文和类型会被原样发送。
     *
     * @throws Throwable 切面执行异常
     */
    @Test
    void shouldUseExplicitAnnotationValuesWhenProvided() throws Throwable {
        IMessageClient client = mock(IMessageClient.class);
        AutoMessageAspect aspect = new AutoMessageAspect(client);
        setAuthenticatedUser(88L);
        ProceedingJoinPoint joinPoint = mockJoinPoint(new AutoMessageProbeTarget(), "sendCustomNotice");

        aspect.message(joinPoint);

        ArgumentCaptor<IMessageEntity> entityCaptor = ArgumentCaptor.forClass(IMessageEntity.class);
        verify(client).sendWebMessage(entityCaptor.capture(), org.mockito.ArgumentMatchers.anyList());
        assertThat(entityCaptor.getValue().getBody()).isEqualTo("自定义正文");
        assertThat(entityCaptor.getValue().getTitle()).isEqualTo("自定义标题");
        assertThat(entityCaptor.getValue().getName()).isEqualTo("自定义名称");
        assertThat(entityCaptor.getValue().getMessageType()).isEqualTo(MessageType.WEB_MESSAGE);
    }

    /**
     * 构造最小可执行的切点桩。
     *
     * @param target 目标对象
     * @param methodName 方法名
     * @return ProceedingJoinPoint mock
     * @throws Throwable mock 配置异常
     */
    private ProceedingJoinPoint mockJoinPoint(Object target, String methodName) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn(methodName);
        when(joinPoint.proceed()).thenReturn("done");
        return joinPoint;
    }

    /**
     * 设置最小登录上下文，供 SecurityUtils.getUserId() 使用。
     *
     * @param userId 用户 ID
     */
    private void setAuthenticatedUser(Long userId) {
        SysUser currentUser = new SysUser();
        currentUser.setUserId(userId);
        currentUser.setUserName("auto-message-tester");
        LoginUser loginUser = new LoginUser(currentUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(loginUser, null));
    }

    /**
     * 用于提供带注解的方法目标。
     */
    static class AutoMessageProbeTarget {

        /**
         * 默认通知方法。
         */
        @AutoMessage
        public void sendDefaultNotice() {
        }

        /**
         * 自定义通知方法。
         */
        @AutoMessage(title = "自定义标题", name = "自定义名称", body = "自定义正文", type = MessageType.WEB_MESSAGE)
        public void sendCustomNotice() {
        }
    }
}
