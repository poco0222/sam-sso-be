/**
 * @file WebSocket 相关 Spring Bean 与 endpoint 注入配置
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.component.message.config;

import com.yr.system.component.message.IMessageListener;
import com.yr.system.component.message.MessageJsonSerializer;
import com.yr.system.component.message.impl.DefaultMessageListenerImpl;
import com.yr.system.service.ISysMessageBodyReceiverService;
import com.yr.system.service.ISysMessageBodyService;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * description
 * </p>
 *
 * @author carl 2022-01-17 13:45
 * @version V1.0
 */
@Configuration
public class WebSocketConfig {

    /**
     * 创建一个消息监听
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(IMessageListener.class)
    public IMessageListener messageListener(ISysMessageBodyService messageBodyService,
                                            ISysMessageBodyReceiverService messageBodyReceiverService,
                                            MessageJsonSerializer messageJsonSerializer) {
        return new DefaultMessageListenerImpl(messageBodyService, messageBodyReceiverService, messageJsonSerializer);
    }

    /**
     * 注册共享的 websocket 会话注册表。
     *
     * @return 会话注册表
     */
    @Bean
    public WebSocketSessionRegistry webSocketSessionRegistry() {
        return new WebSocketSessionRegistry();
    }

    /**
     * 提前把 Spring BeanFactory 暴露给 websocket 容器的 endpoint configurator。
     *
     * @param beanFactory Spring 自动注入工厂
     * @return endpoint configurator
     */
    @Bean
    public SpringEndpointConfigurator springEndpointConfigurator(AutowireCapableBeanFactory beanFactory) {
        return new SpringEndpointConfigurator(beanFactory);
    }

    /**
     * 实例化一个WebSocket服务
     *
     * @return
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    /**
     * 让 javax.websocket 容器创建 endpoint 时也能走 Spring 依赖注入。
     */
    public static class SpringEndpointConfigurator extends ServerEndpointConfig.Configurator {

        /** 保存可复用的 Spring BeanFactory，供容器创建 endpoint 时注入依赖。 */
        private static volatile AutowireCapableBeanFactory beanFactory;

        /**
         * 默认构造器供 websocket 容器反射创建 configurator。
         */
        public SpringEndpointConfigurator() {
        }

        /**
         * 由 Spring 在启动期注入 BeanFactory，初始化静态上下文。
         *
         * @param beanFactory Spring 自动注入工厂
         */
        public SpringEndpointConfigurator(AutowireCapableBeanFactory beanFactory) {
            SpringEndpointConfigurator.beanFactory = beanFactory;
        }

        /**
         * 创建带有 Spring 依赖注入能力的 endpoint 实例。
         *
         * @param endpointClass endpoint 类型
         * @param <T>          endpoint 泛型
         * @return 注入完成的 endpoint
         * @throws InstantiationException endpoint 创建失败
         */
        @Override
        public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
            if (beanFactory == null) {
                throw new InstantiationException("Spring BeanFactory 尚未初始化，无法创建 WebSocket endpoint");
            }
            try {
                return beanFactory.createBean(endpointClass);
            } catch (RuntimeException ex) {
                InstantiationException instantiationException = new InstantiationException(ex.getMessage());
                instantiationException.initCause(ex);
                throw instantiationException;
            }
        }
    }

    /**
     * 统一维护用户连接与在线人数，避免把共享状态塞在 endpoint 实例里。
     */
    public static class WebSocketSessionRegistry {

        /** 用户到 websocket 会话列表的线程安全映射。 */
        private final ConcurrentHashMap<Long, CopyOnWriteArrayList<Session>> sessionsByUser = new ConcurrentHashMap<>();

        /** 当前在线连接数，按 session 粒度统计。 */
        private final AtomicInteger onlineCount = new AtomicInteger(0);

        /**
         * 注册一个新的用户连接。
         *
         * @param userId  用户 ID
         * @param session websocket 会话
         * @return 注册后的在线连接数
         */
        public int register(Long userId, Session session) {
            if (userId == null || session == null) {
                return onlineCount.get();
            }
            sessionsByUser.compute(userId, (key, sessions) -> {
                CopyOnWriteArrayList<Session> safeSessions = sessions == null ? new CopyOnWriteArrayList<>() : sessions;
                safeSessions.add(session);
                return safeSessions;
            });
            return onlineCount.incrementAndGet();
        }

        /**
         * 注销一个用户连接，只有真正移除成功时才递减在线计数。
         *
         * @param userId  用户 ID
         * @param session websocket 会话
         * @return 是否实际移除了会话
         */
        public boolean unregister(Long userId, Session session) {
            if (userId == null || session == null) {
                return false;
            }
            AtomicBoolean removed = new AtomicBoolean(false);
            sessionsByUser.computeIfPresent(userId, (key, sessions) -> {
                removed.set(sessions.remove(session));
                return sessions.isEmpty() ? null : sessions;
            });
            if (removed.get()) {
                onlineCount.decrementAndGet();
            }
            return removed.get();
        }

        /**
         * 获取某个用户当前连接快照，防止外部直接修改内部容器。
         *
         * @param userId 用户 ID
         * @return 当前连接快照
         */
        public List<Session> getSessions(Long userId) {
            if (userId == null) {
                return Collections.emptyList();
            }
            List<Session> sessions = sessionsByUser.get(userId);
            if (sessions == null) {
                return Collections.emptyList();
            }
            return List.copyOf(sessions);
        }

        /**
         * 返回当前在线连接数。
         *
         * @return 在线连接数
         */
        public int getOnlineCount() {
            return onlineCount.get();
        }
    }
}
