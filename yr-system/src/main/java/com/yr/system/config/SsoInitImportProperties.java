/**
 * @file INIT_IMPORT 配置属性
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.config;

import com.yr.common.enums.DataSourceType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 管理 INIT_IMPORT 的 legacy source 读取配置。
 */
@Component
@ConfigurationProperties(prefix = "sso.init-import")
public class SsoInitImportProperties {

    /** 默认显式指向当前证据最充分的 MySQL `slave` 读库。 */
    private DataSourceType legacySourceDatasource = DataSourceType.SLAVE;

    /**
     * @return 初始化导入 legacy source 对应的数据源类型
     */
    public DataSourceType getLegacySourceDatasource() {
        return legacySourceDatasource;
    }

    /**
     * @param legacySourceDatasource 初始化导入 legacy source 对应的数据源类型
     */
    public void setLegacySourceDatasource(DataSourceType legacySourceDatasource) {
        this.legacySourceDatasource = legacySourceDatasource;
    }
}
