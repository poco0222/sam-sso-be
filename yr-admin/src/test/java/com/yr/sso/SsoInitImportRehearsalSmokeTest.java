/**
 * @file INIT_IMPORT 本地演练烟雾测试
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.sso;

import com.yr.YrApplication;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.enums.DataSourceType;
import com.yr.system.config.SsoInitImportProperties;
import com.yr.system.mapper.SsoSyncTaskItemMapper;
import com.yr.system.mapper.SsoSyncTaskMapper;
import com.yr.system.service.ISsoSyncTaskService;
import com.yr.quartz.service.impl.SysJobServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在本地 source datasource 准备好后，提供一条固定的 full import rehearsal 命令。
 */
@SpringBootTest(
        classes = YrApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.druid.initialSize=0",
                "spring.datasource.druid.minIdle=0",
                "spring.datasource.druid.connectionErrorRetryAttempts=0",
                "spring.datasource.druid.breakAfterAcquireFailure=true",
                "spring.datasource.druid.timeBetweenConnectErrorMillis=100",
                "spring.datasource.druid.initExceptionThrow=false",
                "spring.datasource.druid.statViewServlet.enabled=false",
                "spring.datasource.druid.webStatFilter.enabled=false",
                "spring.liquibase.enabled=false",
                "token.secret=test-only-local-jwt-secret-for-init-import"
        }
)
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "runInitImportRehearsal", matches = "true")
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_DRUID_MASTER_PASSWORD", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_DRUID_SLAVE_PASSWORD", matches = ".+")
class SsoInitImportRehearsalSmokeTest {

    /** Mock Quartz 初始化服务，避免启动期抢占数据库连接影响 smoke 语义。 */
    @MockBean
    private SysJobServiceImpl sysJobService;

    @Autowired
    private ISsoSyncTaskService ssoSyncTaskService;

    @Autowired
    private SsoInitImportProperties ssoInitImportProperties;

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
        Assumptions.assumeTrue(isDataSourceReady("masterDataSource"), "master 数据库未就绪，跳过 INIT_IMPORT 演练");
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
     * 判断指定数据源 bean 是否已启用且可成功建立连接。
     *
     * @param dataSourceBeanName 数据源 bean 名称
     * @return 数据源可用时返回 true
     */
    private boolean isDataSourceReady(String dataSourceBeanName) {
        DataSource dataSource = dataSourceRegistry.get(dataSourceBeanName);
        if (dataSource == null) {
            return false;
        }
        try (var connection = dataSource.getConnection()) {
            return connection.isValid(1);
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * 判断当前配置的 legacy source datasource 是否已启用且可达。
     *
     * @return legacy source datasource 就绪时返回 true
     */
    private boolean isConfiguredLegacySourceReady() {
        DataSourceType legacySourceDatasource = ssoInitImportProperties.getLegacySourceDatasource();
        String dataSourceBeanName = resolveDataSourceBeanName(legacySourceDatasource);
        return isDataSourceReady(dataSourceBeanName);
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
