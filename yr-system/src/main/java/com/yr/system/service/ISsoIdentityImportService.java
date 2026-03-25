/**
 * @file INIT_IMPORT 执行服务接口
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service;

import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.core.domain.entity.SsoSyncTaskItem;
import com.yr.system.domain.dto.SsoIdentityImportExecutionResult;

import java.util.List;

/**
 * 负责把 legacy 身份快照导入到身份中心主库。
 */
public interface ISsoIdentityImportService {

    /**
     * 执行一次初始化导入或补偿导入。
     *
     * @param task 同步任务
     * @param scopedItems 指定执行范围；为空时表示导入完整快照
     * @return 执行结果
     */
    SsoIdentityImportExecutionResult execute(SsoSyncTask task, List<SsoSyncTaskItem> scopedItems);
}
