/**
 * @file legacy 身份快照加载器接口
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service.support;

import com.yr.common.enums.DataSourceType;
import com.yr.system.domain.dto.SsoIdentityImportSnapshot;

/**
 * 按 datasource type（数据源类型）加载 legacy 身份快照。
 */
public interface ISsoLegacyIdentitySnapshotLoader {

    /**
     * @return 当前 loader 对应的数据源类型
     */
    DataSourceType getDataSourceType();

    /**
     * @return 当前数据源上的身份快照
     */
    SsoIdentityImportSnapshot loadSnapshot();
}
