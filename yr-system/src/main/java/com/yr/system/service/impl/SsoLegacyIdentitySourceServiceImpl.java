/**
 * @file INIT_IMPORT legacy 身份来源服务实现
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service.impl;

import com.yr.common.enums.DataSourceType;
import com.yr.common.exception.CustomException;
import com.yr.system.config.SsoInitImportProperties;
import com.yr.system.domain.dto.SsoIdentityImportSnapshot;
import com.yr.system.service.ISsoLegacyIdentitySourceService;
import com.yr.system.service.support.ISsoLegacyIdentitySnapshotLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 根据显式配置选择真实 legacy source adapter，并阻止 secondary datasource 静默回退 master。
 */
@Service
public class SsoLegacyIdentitySourceServiceImpl implements ISsoLegacyIdentitySourceService {

    /** 日志器。 */
    private static final Logger log = LoggerFactory.getLogger(SsoLegacyIdentitySourceServiceImpl.class);

    /** secondary datasource 适配器允许的类型。 */
    private static final List<DataSourceType> SUPPORTED_SECONDARY_DATASOURCES = List.of(
            DataSourceType.SLAVE,
            DataSourceType.SLAVEEC,
            DataSourceType.SLAVEXX
    );

    /** 初始化导入配置。 */
    private final SsoInitImportProperties ssoInitImportProperties;

    /** 按数据源类型索引的快照加载器。 */
    private final Map<DataSourceType, ISsoLegacyIdentitySnapshotLoader> snapshotLoaderMap;

    /** 已装配的数据源 Bean 注册表。 */
    private final Map<String, DataSource> dataSourceRegistry;

    /**
     * @param ssoInitImportProperties 初始化导入配置
     * @param snapshotLoaders 已装配的快照加载器集合
     * @param dataSourceRegistry 已装配的数据源 Bean 注册表
     */
    public SsoLegacyIdentitySourceServiceImpl(SsoInitImportProperties ssoInitImportProperties,
                                              List<ISsoLegacyIdentitySnapshotLoader> snapshotLoaders,
                                              Map<String, DataSource> dataSourceRegistry) {
        this.ssoInitImportProperties = ssoInitImportProperties;
        this.snapshotLoaderMap = new EnumMap<>(DataSourceType.class);
        for (ISsoLegacyIdentitySnapshotLoader snapshotLoader : snapshotLoaders) {
            this.snapshotLoaderMap.put(snapshotLoader.getDataSourceType(), snapshotLoader);
        }
        this.dataSourceRegistry = dataSourceRegistry;
    }

    /**
     * 读取完整身份快照。
     *
     * @return legacy source 当前快照
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public SsoIdentityImportSnapshot loadSnapshot() {
        DataSourceType legacySourceDatasource = resolveLegacySourceDatasource();
        String dataSourceBeanName = resolveDataSourceBeanName(legacySourceDatasource);
        ISsoLegacyIdentitySnapshotLoader snapshotLoader = snapshotLoaderMap.get(legacySourceDatasource);

        // 显式校验目标 datasource bean 是否存在，避免 DynamicDataSource 在 secondary 未启用时静默回退 master。
        if (!dataSourceRegistry.containsKey(dataSourceBeanName)) {
            throw new CustomException("INIT_IMPORT 来源数据源未启用: " + legacySourceDatasource + " (" + dataSourceBeanName + ")");
        }
        if (snapshotLoader == null) {
            throw new CustomException("INIT_IMPORT 缺少来源数据源适配器: " + legacySourceDatasource);
        }

        SsoIdentityImportSnapshot snapshot = snapshotLoader.loadSnapshot();
        log.info(
                "INIT_IMPORT legacy snapshot loaded from datasource={}, orgCount={}, deptCount={}, userCount={}, userOrgRelationCount={}, userDeptRelationCount={}",
                legacySourceDatasource,
                snapshot.getOrgList().size(),
                snapshot.getDeptList().size(),
                snapshot.getUserList().size(),
                snapshot.getUserOrgRelationList().size(),
                snapshot.getUserDeptRelationList().size()
        );
        return snapshot;
    }

    /**
     * 解析本轮初始化导入真正要走的来源数据源。
     *
     * @return 来源数据源类型
     */
    private DataSourceType resolveLegacySourceDatasource() {
        DataSourceType legacySourceDatasource = ssoInitImportProperties.getLegacySourceDatasource();
        if (!SUPPORTED_SECONDARY_DATASOURCES.contains(legacySourceDatasource)) {
            throw new CustomException("INIT_IMPORT 只允许 secondary datasource: " + SUPPORTED_SECONDARY_DATASOURCES);
        }
        return legacySourceDatasource;
    }

    /**
     * 解析 datasource bean 名称。
     *
     * @param legacySourceDatasource 来源数据源类型
     * @return datasource bean 名称
     */
    private String resolveDataSourceBeanName(DataSourceType legacySourceDatasource) {
        return switch (legacySourceDatasource) {
            case SLAVE -> "slaveDataSource";
            case SLAVEEC -> "slaveecDataSource";
            case SLAVEXX -> "slavexxDataSource";
            default -> throw new CustomException("不支持的 INIT_IMPORT 来源数据源: " + legacySourceDatasource);
        };
    }
}
