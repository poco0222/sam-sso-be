/**
 * @file 锁定 INIT_IMPORT 数据源接入方式契约
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service.impl;

import com.yr.common.exception.CustomException;
import com.yr.common.enums.DataSourceType;
import com.yr.system.config.SsoInitImportProperties;
import com.yr.system.domain.dto.SsoIdentityImportSnapshot;
import com.yr.system.service.support.ISsoLegacyIdentitySnapshotLoader;
import com.yr.system.service.support.SsoSlaveEcLegacyIdentitySnapshotLoader;
import com.yr.system.service.support.SsoSlaveLegacyIdentitySnapshotLoader;
import com.yr.system.service.support.SsoSlaveXxLegacyIdentitySnapshotLoader;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定 INIT_IMPORT 读取 legacy snapshot 的 datasource adapter（数据源适配器）契约。
 */
class SsoInitImportDatasourceContractTest {

    /**
     * 验证初始化导入默认显式指向 MySQL `SLAVE`，不再依赖隐式 skeleton 约定。
     */
    @Test
    void shouldDefaultInitImportLegacySourceDatasourceToSlave() {
        SsoInitImportProperties properties = new SsoInitImportProperties();

        assertThat(properties.getLegacySourceDatasource()).isEqualTo(DataSourceType.SLAVE);
    }

    /**
     * 验证 orchestration service（编排服务）会委派给配置的 secondary datasource adapter。
     */
    @Test
    void shouldDelegateToConfiguredSecondaryDatasourceAdapter() {
        SsoInitImportProperties properties = new SsoInitImportProperties();
        properties.setLegacySourceDatasource(DataSourceType.SLAVEXX);
        SsoIdentityImportSnapshot snapshot = new SsoIdentityImportSnapshot();
        ISsoLegacyIdentitySnapshotLoader slaveLoader = mock(ISsoLegacyIdentitySnapshotLoader.class);
        ISsoLegacyIdentitySnapshotLoader slaveEcLoader = mock(ISsoLegacyIdentitySnapshotLoader.class);
        ISsoLegacyIdentitySnapshotLoader slaveXxLoader = mock(ISsoLegacyIdentitySnapshotLoader.class);

        when(slaveLoader.getDataSourceType()).thenReturn(DataSourceType.SLAVE);
        when(slaveEcLoader.getDataSourceType()).thenReturn(DataSourceType.SLAVEEC);
        when(slaveXxLoader.getDataSourceType()).thenReturn(DataSourceType.SLAVEXX);
        when(slaveXxLoader.loadSnapshot()).thenReturn(snapshot);

        SsoLegacyIdentitySourceServiceImpl service = new SsoLegacyIdentitySourceServiceImpl(
                properties,
                List.of(slaveLoader, slaveEcLoader, slaveXxLoader),
                Map.of("slavexxDataSource", mock(javax.sql.DataSource.class))
        );

        assertThat(service.loadSnapshot()).isSameAs(snapshot);
        verify(slaveXxLoader).loadSnapshot();
    }

    /**
     * 验证 adapter 明确拒绝 `MASTER`，避免 secondary datasource 未启用时静默回退主库。
     */
    @Test
    void shouldRejectMasterDatasourceToAvoidSilentFallback() {
        SsoInitImportProperties properties = new SsoInitImportProperties();
        properties.setLegacySourceDatasource(DataSourceType.MASTER);

        SsoLegacyIdentitySourceServiceImpl service = new SsoLegacyIdentitySourceServiceImpl(
                properties,
                List.of(),
                Map.of()
        );

        assertThatThrownBy(service::loadSnapshot)
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("secondary datasource");
    }

    /**
     * 验证 secondary datasource bean 未启用时会直接失败，而不是静默回退到 master。
     */
    @Test
    void shouldRejectMissingSecondaryDatasourceBean() {
        SsoInitImportProperties properties = new SsoInitImportProperties();
        properties.setLegacySourceDatasource(DataSourceType.SLAVEEC);

        SsoLegacyIdentitySourceServiceImpl service = new SsoLegacyIdentitySourceServiceImpl(
                properties,
                List.of(),
                Map.of()
        );

        assertThatThrownBy(service::loadSnapshot)
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("未启用")
                .hasMessageContaining("SLAVEEC");
    }

    /**
     * 验证三个 concrete adapter（具体适配器）与 datasource annotation（数据源注解）一一对应。
     */
    @Test
    void shouldLockConcreteAdaptersToDeclaredDatasourceAnnotations() {
        assertDataSourceAnnotation(SsoSlaveLegacyIdentitySnapshotLoader.class, DataSourceType.SLAVE);
        assertDataSourceAnnotation(SsoSlaveEcLegacyIdentitySnapshotLoader.class, DataSourceType.SLAVEEC);
        assertDataSourceAnnotation(SsoSlaveXxLegacyIdentitySnapshotLoader.class, DataSourceType.SLAVEXX);
    }

    /**
     * 统一断言 concrete adapter 上的数据源注解。
     *
     * @param adapterClass adapter 类型
     * @param expectedDataSource 预期数据源
     */
    private void assertDataSourceAnnotation(Class<?> adapterClass, DataSourceType expectedDataSource) {
        com.yr.common.annotation.DataSource dataSource = adapterClass.getAnnotation(com.yr.common.annotation.DataSource.class);

        assertThat(dataSource)
                .withFailMessage("%s 缺少 @DataSource 注解", adapterClass.getSimpleName())
                .isNotNull();
        assertThat(dataSource.value()).isEqualTo(expectedDataSource);
    }
}
