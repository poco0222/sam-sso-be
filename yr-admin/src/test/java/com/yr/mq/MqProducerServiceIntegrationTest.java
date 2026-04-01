/**
 * @file RocketMQ 发送链路集成烟雾测试
 * @author Codex
 * @date 2026-03-11
 */
package com.yr.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yr.common.core.domain.MqMessageLog;
import com.yr.common.enums.MqActionType;
import com.yr.common.enums.MqSendStatus;
import com.yr.common.mapper.MqMessageLogMapper;
import com.yr.common.mybatisplus.config.MybatisPlusConfig;
import com.yr.common.service.MqProducerService;
import com.yr.common.utils.spring.SpringUtils;
import com.yr.framework.config.ApplicationConfig;
import com.yr.framework.config.DruidConfig;
import com.yr.framework.config.properties.DruidProperties;
import com.yr.support.ExternalDependencyTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用于在外部依赖恢复后快速确认 MQ 发送与日志落库链路。
 */
@SpringBootTest(
        classes = MqProducerServiceIntegrationTest.MqProducerServiceIntegrationTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=",
                "spring.datasource.druid.initialSize=0",
                "spring.datasource.druid.minIdle=0",
                "spring.datasource.druid.connectionErrorRetryAttempts=0",
                "spring.datasource.druid.breakAfterAcquireFailure=true",
                "spring.datasource.druid.timeBetweenConnectErrorMillis=100",
                "spring.datasource.druid.initExceptionThrow=false",
                "spring.datasource.druid.statViewServlet.enabled=false",
                "spring.datasource.druid.webStatFilter.enabled=false",
                "spring.liquibase.enabled=false",
                "token.secret=test-only-local-jwt-secret-for-mq-integration",
                "rocketmq.enabled=true"
        }
)
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "runMqIntegration", matches = "true")
class MqProducerServiceIntegrationTest {

    /** 本地连通性探测超时时间。 */
    private static final Duration SOCKET_TIMEOUT = Duration.ofSeconds(1);

    @Autowired
    private MqProducerService mqProducerService;

    @Autowired
    private MqMessageLogMapper mqMessageLogMapper;

    @Autowired
    private Environment environment;

    /** 本次测试插入的消息键，用于清理履历。 */
    private String lastMsgKey;

    /**
     * 当 NameServer 不可达时，验证发送结果会回落为失败，并写入失败状态日志。
     */
    @Test
    void shouldMarkMessageAsFailedWhenNameServerUnavailable() {
        ExternalDependencyTestSupport.HostPort mysql = ExternalDependencyTestSupport.parseMySqlJdbcUrl(
                environment.getProperty("spring.datasource.druid.master.url"));
        ExternalDependencyTestSupport.HostPort nameServer = ExternalDependencyTestSupport.parseRocketMqNameServer(
                environment.getProperty("rocketmq.name-server"));

        Assumptions.assumeTrue(
                ExternalDependencyTestSupport.isTcpReachable(mysql.getHost(), mysql.getPort(), SOCKET_TIMEOUT),
                "MySQL 未就绪，跳过 MQ 失败链路测试"
        );
        Assumptions.assumeTrue(
                !ExternalDependencyTestSupport.isTcpReachable(nameServer.getHost(), nameServer.getPort(), SOCKET_TIMEOUT),
                "当前 NameServer 已可达，跳过不可达场景测试"
        );

        lastMsgKey = "boot27-mq-fail-" + System.currentTimeMillis();
        boolean result = mqProducerService.send(
                "yr-test-topic",
                "boot27",
                MqActionType.I,
                lastMsgKey,
                Collections.singletonMap("message", "hello")
        );

        MqMessageLog logEntity = selectLatestLogByMsgKey(lastMsgKey);
        assertThat(result).isFalse();
        assertThat(logEntity).isNotNull();
        assertThat(logEntity.getSendStatus()).isEqualTo(MqSendStatus.FAILED.getCode());
        assertThat(logEntity.getErrorMsg()).isNotBlank();
    }

    /**
     * 当 NameServer 可达时，验证同步发送返回成功，并更新发送状态。
     */
    @Test
    void shouldSendMessageSuccessfullyWhenNameServerAvailable() {
        ExternalDependencyTestSupport.HostPort mysql = ExternalDependencyTestSupport.parseMySqlJdbcUrl(
                environment.getProperty("spring.datasource.druid.master.url"));
        ExternalDependencyTestSupport.HostPort nameServer = ExternalDependencyTestSupport.parseRocketMqNameServer(
                environment.getProperty("rocketmq.name-server"));

        Assumptions.assumeTrue(
                ExternalDependencyTestSupport.isTcpReachable(mysql.getHost(), mysql.getPort(), SOCKET_TIMEOUT),
                "MySQL 未就绪，跳过 MQ 成功链路测试"
        );
        Assumptions.assumeTrue(
                ExternalDependencyTestSupport.isTcpReachable(nameServer.getHost(), nameServer.getPort(), SOCKET_TIMEOUT),
                "RocketMQ NameServer 未就绪，跳过 MQ 成功链路测试"
        );

        lastMsgKey = "boot27-mq-success-" + System.currentTimeMillis();
        boolean result = mqProducerService.send(
                "yr-test-topic",
                "boot27",
                MqActionType.I,
                lastMsgKey,
                Collections.singletonMap("message", "hello")
        );

        MqMessageLog logEntity = selectLatestLogByMsgKey(lastMsgKey);
        assertThat(result).isTrue();
        assertThat(logEntity).isNotNull();
        assertThat(logEntity.getSendStatus()).isEqualTo(MqSendStatus.SUCCESS.getCode());
        assertThat(logEntity.getMsgId()).isNotBlank();
    }

    /**
     * 清理本轮测试写入的 MQ 履历，避免污染后续联调。
     */
    @AfterEach
    void cleanupTestData() {
        if (lastMsgKey == null) {
            return;
        }
        mqMessageLogMapper.delete(new LambdaQueryWrapper<MqMessageLog>()
                .eq(MqMessageLog::getMsgKey, lastMsgKey));
        lastMsgKey = null;
    }

    /**
     * 根据消息键查询最新一条履历。
     *
     * @param msgKey 消息键
     * @return 最新履历
     */
    private MqMessageLog selectLatestLogByMsgKey(String msgKey) {
        return mqMessageLogMapper.selectOne(new LambdaQueryWrapper<MqMessageLog>()
                .eq(MqMessageLog::getMsgKey, msgKey)
                .orderByDesc(MqMessageLog::getId)
                .last("limit 1"));
    }

    /**
     * MQ 集成测试最小化应用上下文。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({ApplicationConfig.class, DruidConfig.class, DruidProperties.class, SpringUtils.class,
            MybatisPlusConfig.class, MqProducerService.class})
    static class MqProducerServiceIntegrationTestApplication {
    }
}
