/**
 * @file INIT_IMPORT legacy 身份来源服务接口
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service;

import com.yr.system.domain.dto.SsoIdentityImportSnapshot;

/**
 * 负责从 legacy source 读取初始化导入所需的身份快照。
 */
public interface ISsoLegacyIdentitySourceService {

    /**
     * 读取完整身份快照。
     *
     * @return 当前 legacy source 上的组织、部门、用户与关系快照
     */
    SsoIdentityImportSnapshot loadSnapshot();
}
