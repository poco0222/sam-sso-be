/**
 * @file INIT_IMPORT 本地演练烟雾测试
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.sso;

import com.yr.YrApplication;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.enums.DataSourceType;
import com.yr.support.ExternalDependencyTestSupport;
import com.yr.system.config.SsoInitImportProperties;
import com.yr.system.mapper.SsoSyncTaskItemMapper;
import com.yr.system.mapper.SsoSyncTaskMapper;
import com.yr.system.service.ISsoSyncTaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在本地 source datasource 准备好后，提供一条固定的 full import rehearsal 命令。
 */
@SpringBootTest(
        classes = YrApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.druid.statViewServlet.enabled=false",
                "spring.datasource.druid.webStatFilter.enabled=false",
                "spring.liquibase.enabled=false",
                "token.secret=test-only-local-jwt-secret-for-init-import"
        }
)
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "runInitImportRehearsal", matches = "true")
class SsoInitImportRehearsalSmokeTest {

    /** 本地连通性探测超时时间。 */
    private static final Duration SOCKET_TIMEOUT = Duration.ofSeconds(1);

    @Autowired
    private ISsoSyncTaskService ssoSyncTaskService;

    @Autowired
    private SsoInitImportProperties ssoInitImportProperties;

    @Autowired
    private Environment environment;

    @Autowired
    private Map<String, DataSource> dataSourceRegistry;

    @Autowired
    private SsoSyncTaskMapper ssoSyncTaskMapper;

    @Autowired
    private SsoSyncTaskItemMapper ssoSyncTaskItemMapper;

    /** 本轮演练创建的任务 ID。 */
    private Long lastTaskId;

    /**
     * 当 master 与 legacy source 都可达时，验证 full import rehearsal 能跑完并产出结构化明细。
     */
    @Test
    void shouldRunInitImportTaskAgainstConfiguredLegacySource() {
        Assumptions.assumeTrue(isMasterReachable(), "master 数据库未就绪，跳过 INIT_IMPORT 演练");
        Assumptions.assumeTrue(isConfiguredLegacySourceReady(), "legacy source datasource 未启用或不可达，跳过 INIT_IMPORT 演练");

        SsoSyncTask command = new SsoSyncTask();
        command.setTargetClientCode("sam-mgmt");
        command.setCreateBy("rehearsal");

        SsoSyncTask task = ssoSyncTaskService.initImportTask(command);
        lastTaskId = task.getTaskId();

        assertThat(task.getTaskId()).isNotNull();
        assertThat(task.getStatus()).isEqualTo(SsoSyncTask.STATUS_SUCCESS);
        assertThat(task.getIdStrategy()).isEqualTo(SsoSyncTask.ID_STRATEGY_INHERIT_SOURCE_ID);
        assertThat(task.getOwnershipTransferStatus()).isEqualTo(SsoSyncTask.OWNERSHIP_TRANSFERRED);
        // local_sam 已确认存在真实组织主数据，本地 rehearsal 必须导出非 0 条才能证明读到了 source。
        assertThat(task.getTotalItemCount()).isGreaterThan(0);
        assertThat(task.getFailedItemCount()).isZero();
        assertThat(task.getTotalItemCount()).isEqualTo(task.getSuccessItemCount() + task.getFailedItemCount());

        SsoSyncTask detail = ssoSyncTaskService.selectSsoSyncTaskById(task.getTaskId());
        assertThat(detail.getItemList()).hasSize(detail.getTotalItemCount().intValue());
        assertThat(detail.getFailedItemCount()).isEqualTo(
                detail.getItemList().stream().filter(item -> "FAILED".equals(item.getStatus())).count()
        );
    }

    /**
     * 清理本轮 smoke test 生成的任务与明细，避免污染后续联调。
     */
    @AfterEach
    void cleanupTestData() {
        if (lastTaskId == null) {
            return;
        }
        ssoSyncTaskItemMapper.deleteByTaskId(lastTaskId);
        ssoSyncTaskMapper.deleteById(lastTaskId);
        lastTaskId = null;
    }

    /**
     * 判断 master 是否可达。
     *
     * @return master 可达时返回 true
     */
    private boolean isMasterReachable() {
        ExternalDependencyTestSupport.HostPort master = ExternalDependencyTestSupport.parseMySqlJdbcUrl(
                environment.getProperty("spring.datasource.druid.master.url")
        );
        return ExternalDependencyTestSupport.isTcpReachable(master.getHost(), master.getPort(), SOCKET_TIMEOUT);
    }

    /**
     * 判断当前配置的 legacy source datasource 是否已启用且可达。
     *
     * @return legacy source datasource 就绪时返回 true
     */
    private boolean isConfiguredLegacySourceReady() {
        DataSourceType legacySourceDatasource = ssoInitImportProperties.getLegacySourceDatasource();
        String dataSourceBeanName = resolveDataSourceBeanName(legacySourceDatasource);
        if (!dataSourceRegistry.containsKey(dataSourceBeanName)) {
            return false;
        }
        return switch (legacySourceDatasource) {
            case SLAVE -> isMySqlDatasourceReachable("spring.datasource.druid.slave.url");
            case SLAVEEC -> isSqlServerDatasourceReachable("spring.datasource.druid.slaveec.url");
            case SLAVEXX -> isSqlServerDatasourceReachable("spring.datasource.druid.slavexx.url");
            default -> false;
        };
    }

    /**
     * 判断指定 MySQL 数据源是否可达。
     *
     * @param jdbcPropertyKey JDBC 属性键
     * @return 数据源可达时返回 true
     */
    private boolean isMySqlDatasourceReachable(String jdbcPropertyKey) {
        ExternalDependencyTestSupport.HostPort hostPort = ExternalDependencyTestSupport.parseMySqlJdbcUrl(
                environment.getProperty(jdbcPropertyKey)
        );
        return ExternalDependencyTestSupport.isTcpReachable(hostPort.getHost(), hostPort.getPort(), SOCKET_TIMEOUT);
    }

    /**
     * 判断指定 SQL Server 数据源是否可达。
     *
     * @param jdbcPropertyKey JDBC 属性键
     * @return 数据源可达时返回 true
     */
    private boolean isSqlServerDatasourceReachable(String jdbcPropertyKey) {
        ExternalDependencyTestSupport.HostPort hostPort = ExternalDependencyTestSupport.parseSqlServerJdbcUrl(
                environment.getProperty(jdbcPropertyKey)
        );
        return ExternalDependencyTestSupport.isTcpReachable(hostPort.getHost(), hostPort.getPort(), SOCKET_TIMEOUT);
    }

    /**
     * 解析 datasource bean 名称。
     *
     * @param legacySourceDatasource 当前 legacy source 数据源类型
     * @return datasource bean 名称
     */
    private String resolveDataSourceBeanName(DataSourceType legacySourceDatasource) {
        return switch (legacySourceDatasource) {
            case SLAVE -> "slaveDataSource";
            case SLAVEEC -> "slaveecDataSource";
            case SLAVEXX -> "slavexxDataSource";
            default -> "masterDataSource";
        };
    }

}
