/**
 * @file DISTRIBUTION 本地 MQ 烟雾测试
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.sso;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yr.YrApplication;
import com.yr.common.core.domain.MqMessageLog;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysOrg;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.enums.MqSendStatus;
import com.yr.support.ExternalDependencyTestSupport;
import com.yr.system.domain.dto.SsoIdentityImportSnapshot;
import com.yr.system.domain.entity.SysUserDept;
import com.yr.system.domain.entity.SysUserOrg;
import com.yr.system.mapper.SsoSyncTaskItemMapper;
import com.yr.system.mapper.SsoSyncTaskMapper;
import com.yr.system.service.ISsoSyncTaskService;
import com.yr.system.service.support.SsoCurrentIdentitySnapshotLoader;
import com.yr.common.mapper.MqMessageLogMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

/**
 * 在 master 与 NameServer 就绪时，验证 DISTRIBUTION 能把 task/item/log 链路打通。
 */
@SpringBootTest(
        classes = YrApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=",
                "spring.datasource.druid.statViewServlet.enabled=false",
                "spring.datasource.druid.webStatFilter.enabled=false",
                "spring.liquibase.enabled=false",
                "token.secret=test-only-local-jwt-secret-for-distribution-smoke",
                "rocketmq.enabled=true"
        }
)
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "runDistributionSmoke", matches = "true")
class SsoDistributionMqSmokeTest {

    /** 本地连通性探测超时时间。 */
    private static final Duration SOCKET_TIMEOUT = Duration.ofSeconds(1);

    @Autowired
    private ISsoSyncTaskService ssoSyncTaskService;

    @Autowired
    private Environment environment;

    @Autowired
    private Map<String, DataSource> dataSourceRegistry;

    @Autowired
    private SsoSyncTaskMapper ssoSyncTaskMapper;

    @Autowired
    private SsoSyncTaskItemMapper ssoSyncTaskItemMapper;

    @Autowired
    private MqMessageLogMapper mqMessageLogMapper;

    /** 分发快照读取器测试桩；保留真实 datasource type，只覆盖 loadSnapshot 行为。 */
    @SpyBean
    private SsoCurrentIdentitySnapshotLoader ssoCurrentIdentitySnapshotLoader;

    /** 本轮演练创建的任务 ID。 */
    private Long lastTaskId;

    /**
     * 当 master 与 NameServer 可达时，验证 full-batch distribution 能写 task/item/mq log。
     */
    @Test
    void shouldRunDistributionTaskAgainstCurrentMasterSnapshot() {
        Assumptions.assumeTrue(isMasterReachable(), "master 数据库未就绪，跳过 DISTRIBUTION 演练");
        Assumptions.assumeTrue(isNameServerReachable(), "RocketMQ NameServer 未就绪，跳过 DISTRIBUTION 演练");
        Assumptions.assumeTrue(isMqMessageLogTableReady(), "mq_message_log 未就绪，跳过 DISTRIBUTION 演练");
        ensureTaskItemMsgKeyColumnReady();
        doReturn(buildMinimalSnapshot()).when(ssoCurrentIdentitySnapshotLoader).loadSnapshot();

        SsoSyncTask command = new SsoSyncTask();
        command.setTargetClientCode("sam-mgmt");
        command.setCreateBy("rehearsal");

        SsoSyncTask task = ssoSyncTaskService.distributionTask(command);
        lastTaskId = task.getTaskId();

        assertThat(task.getTaskId()).isNotNull();
        assertThat(task.getStatus()).isEqualTo(SsoSyncTask.STATUS_SUCCESS);
        assertThat(task.getTotalItemCount()).isEqualTo(5);
        assertThat(task.getFailedItemCount()).isZero();

        List<MqMessageLog> mqLogs = mqMessageLogMapper.selectList(new QueryWrapper<MqMessageLog>()
                .likeRight("msg_key", "DIST:" + task.getTaskId() + ":"));
        assertThat(mqLogs).hasSize(5);
        assertThat(mqLogs).allSatisfy(log -> assertThat(log.getSendStatus()).isEqualTo(MqSendStatus.SUCCESS.getCode()));
    }

    /**
     * 清理本轮 smoke test 生成的任务、明细与 MQ 履历。
     */
    @AfterEach
    void cleanupTestData() {
        if (lastTaskId == null) {
            return;
        }
        mqMessageLogMapper.delete(new QueryWrapper<MqMessageLog>()
                .likeRight("msg_key", "DIST:" + lastTaskId + ":"));
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
     * 判断 NameServer 是否可达。
     *
     * @return NameServer 可达时返回 true
     */
    private boolean isNameServerReachable() {
        ExternalDependencyTestSupport.HostPort nameServer = ExternalDependencyTestSupport.parseRocketMqNameServer(
                environment.getProperty("rocketmq.name-server")
        );
        return ExternalDependencyTestSupport.isTcpReachable(nameServer.getHost(), nameServer.getPort(), SOCKET_TIMEOUT);
    }

    /**
     * 判断当前 master 是否已经具备 MQ 发送履历表。
     *
     * @return `mq_message_log` 表可用时返回 true
     */
    private boolean isMqMessageLogTableReady() {
        DataSource masterDataSource = dataSourceRegistry.get("masterDataSource");
        if (masterDataSource == null) {
            return false;
        }
        try (Connection connection = masterDataSource.getConnection();
             ResultSet tables = connection.getMetaData().getTables(connection.getCatalog(), null, "mq_message_log", null)) {
            return tables.next();
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * 在关闭 Liquibase 的 smoke 场景下，显式补齐本轮新增的 `msg_key` 列，避免演练被旧 schema 干扰。
     */
    private void ensureTaskItemMsgKeyColumnReady() {
        DataSource masterDataSource = dataSourceRegistry.get("masterDataSource");
        assertThat(masterDataSource)
                .withFailMessage("masterDataSource 未就绪，无法补齐 sso_sync_task_item.msg_key")
                .isNotNull();
        try (Connection connection = masterDataSource.getConnection()) {
            if (hasColumn(connection, "sso_sync_task_item", "msg_key")) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                // smoke 显式关闭了 Liquibase，这里只补本轮 DISTRIBUTION 演练必需的最小列与索引。
                statement.execute("ALTER TABLE sso_sync_task_item ADD COLUMN msg_key varchar(128) DEFAULT NULL COMMENT '对应MQ消息键'");
                statement.execute("CREATE INDEX sso_sync_task_item_n2 ON sso_sync_task_item (msg_key)");
            }
            assertThat(hasColumn(connection, "sso_sync_task_item", "msg_key"))
                    .withFailMessage("未能补齐 sso_sync_task_item.msg_key，无法继续 DISTRIBUTION 演练")
                    .isTrue();
        } catch (Exception exception) {
            throw new IllegalStateException("补齐 sso_sync_task_item.msg_key 失败", exception);
        }
    }

    /**
     * 判断表上是否已经存在目标列。
     *
     * @param connection JDBC 连接
     * @param tableName 表名
     * @param columnName 列名
     * @return 列存在时返回 true
     */
    private boolean hasColumn(Connection connection, String tableName, String columnName) {
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return columns.next();
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * 构造最小五类主数据快照，避免 smoke test 依赖本地主库现状。
     *
     * @return 最小快照
     */
    private SsoIdentityImportSnapshot buildMinimalSnapshot() {
        SysOrg org = new SysOrg();
        org.setOrgId(101L);
        org.setOrgCode("ORG-101");
        org.setOrgName("总部");

        SysDept dept = new SysDept();
        dept.setDeptId(201L);
        dept.setDeptCode("DEPT-201");
        dept.setDeptName("技术中心");
        dept.setOrgId(101L);
        dept.setDelFlag("0");

        SysUser user = new SysUser();
        user.setUserId(301L);
        user.setUserName("zhangsan");
        user.setNickName("张三");
        user.setDeptId(201L);
        user.setStatus("0");

        SysUserOrg userOrg = new SysUserOrg();
        userOrg.setId(9001L);
        userOrg.setUserId(301L);
        userOrg.setOrgId(101L);
        userOrg.setIsDefault(1);
        userOrg.setEnabled(1);

        SysUserDept userDept = new SysUserDept();
        userDept.setId(9002L);
        userDept.setUserId(301L);
        userDept.setDeptId(201L);
        userDept.setIsDefault(1);
        userDept.setEnabled(1);

        SsoIdentityImportSnapshot snapshot = new SsoIdentityImportSnapshot();
        snapshot.setOrgList(List.of(org));
        snapshot.setDeptList(List.of(dept));
        snapshot.setUserList(List.of(user));
        snapshot.setUserOrgRelationList(List.of(userOrg));
        snapshot.setUserDeptRelationList(List.of(userDept));
        return snapshot;
    }
}
