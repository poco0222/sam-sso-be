/**
 * @file 自动消息切面，负责在方法执行后向当前登录人发送站内通知
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.framework.aspectj;

import com.yr.common.annotation.AutoMessage;
import com.yr.common.enums.MessageType;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.component.message.IMessageClient;
import com.yr.system.component.message.IMessageEntity;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

/**
 * <p>
 * description
 * </p>
 *
 * @author carl 2022-01-21 13:49
 * @version V1.0
 */
@Aspect
@Component
public class AutoMessageAspect {

    /** 自动消息切面日志。 */
    private static final Logger logger = LoggerFactory.getLogger(AutoMessageAspect.class);

    /** 消息发送客户端。 */
    private final IMessageClient client;

    /**
     * 使用构造器显式声明依赖，避免字段注入。
     *
     * @param client 消息发送客户端
     */
    public AutoMessageAspect(IMessageClient client) {
        this.client = client;
    }

    /**
     * 拦截自定义发送消息注解
     *
     * @param joinPoint
     * @return
     * @throws Throwable
     */
    @Around(value = "@annotation(com.yr.common.annotation.AutoMessage)") //注解切点表达式
    public Object message(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();//获取目标方法的方法名称，或者
        Method method = currentMethod(joinPoint, methodName);
        AutoMessage log = method.getAnnotation(AutoMessage.class);
        //先執行后发送消息
        Object result = joinPoint.proceed();
        //发送站内消息通知默认发送给自己当前登录人
        client.sendWebMessage(new IMessageEntity() {
            @Override
            public String getBody() {
                return StringUtils.isBlank(log.body()) ? "刷新页面" : log.body();
            }

            @Override
            public String getTitle() {
                return StringUtils.isBlank(log.title()) ? "站内消息通知" : log.title();
            }

            @Override
            public String getName() {
                return StringUtils.isBlank(log.name()) ? null : log.name();
            }

            @Override
            public MessageType getMessageType() {
                return log.type() == null ? MessageType.WEB_MESSAGE_NOTIFY : log.type();
            }
        }, new ArrayList<>(Collections.singleton(SecurityUtils.getUserId())));
        return result;
    }

    /**
     * 获取当前执行的方法
     *
     * @param joinPoint  连接点
     * @param methodName 方法名称
     * @return 方法
     */
    private Method currentMethod(ProceedingJoinPoint joinPoint, String methodName) {
        /**
         * 获取目标类的所有方法，找到当前要执行的方法
         */
        Method[] methods = joinPoint.getTarget().getClass().getMethods();
        Method resultMethod = null;
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                resultMethod = method;
                break;
            }
        }
        return resultMethod;
    }

}
