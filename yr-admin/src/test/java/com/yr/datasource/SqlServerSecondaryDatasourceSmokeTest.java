/**
 * @file SQL Server 从库装配烟雾测试
 * @author Codex
 * @date 2026-03-11
 */
package com.yr.datasource;

import com.yr.common.utils.spring.SpringUtils;
import com.yr.framework.config.DruidConfig;
import com.yr.framework.config.properties.DruidProperties;
import com.yr.support.ExternalDependencyTestSupport;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用于在外部 SQL Server 就绪后验证 `slaveecDataSource` 的最小可用性。
 */
@SpringBootTest(
        classes = SqlServerSecondaryDatasourceSmokeTest.SqlServerSecondaryDatasourceSmokeTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.druid.initialSize=0",
                "spring.datasource.druid.minIdle=0",
                "spring.datasource.druid.connectionErrorRetryAttempts=0",
                "spring.datasource.druid.breakAfterAcquireFailure=true",
                "spring.datasource.druid.timeBetweenConnectErrorMillis=100",
                "spring.datasource.druid.initExceptionThrow=false",
                "spring.datasource.druid.statViewServlet.enabled=false",
                "spring.datasource.druid.webStatFilter.enabled=false",
                "token.secret=test-only-local-jwt-secret-for-sqlserver-smoke",
                "spring.datasource.druid.slaveec.enabled=true"
        }
)
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "runSqlServerIntegration", matches = "true")
class SqlServerSecondaryDatasourceSmokeTest {

    /** 本地连通性探测超时时间。 */
    private static final Duration SOCKET_TIMEOUT = Duration.ofSeconds(1);

    @Autowired
    @Qualifier("slaveecDataSource")
    private DataSource slaveecDataSource;

    @Autowired
    private Environment environment;

    /**
     * 当 SQL Server 从库可达时，执行最小 `SELECT 1` 探测。
     *
     * @throws Exception 数据源访问异常
     */
    @Test
    void shouldExecuteSelectOneOnSlaveEc() throws Exception {
        ExternalDependencyTestSupport.HostPort sqlServer = ExternalDependencyTestSupport.parseSqlServerJdbcUrl(
                environment.getProperty("spring.datasource.druid.slaveec.url"));

        Assumptions.assumeTrue(
                ExternalDependencyTestSupport.isTcpReachable(sqlServer.getHost(), sqlServer.getPort(), SOCKET_TIMEOUT),
                "SQL Server slaveec 未就绪，跳过从库烟雾测试"
        );

        try (Connection connection = slaveecDataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1");
             ResultSet resultSet = statement.executeQuery()) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt(1)).isEqualTo(1);
        }
    }

    /**
     * SQL Server 从库烟雾测试最小化应用上下文。
     */
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({DruidConfig.class, DruidProperties.class, SpringUtils.class})
    static class SqlServerSecondaryDatasourceSmokeTestApplication {
    }
}
