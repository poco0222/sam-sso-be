/**
 * @file DISTRIBUTION 执行服务接口
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service;

import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.core.domain.entity.SsoSyncTaskItem;
import com.yr.system.domain.dto.SsoSyncTaskExecutionResult;

import java.util.List;

/**
 * 负责把当前身份中心主数据按 full-batch snapshot upsert 发往下游客户端。
 */
public interface ISsoIdentityDistributionService {

    /**
     * 执行一次全量分发或 scoped 补偿分发。
     *
     * @param task 同步任务
     * @param scopedItems 指定执行范围；为空时表示分发当前全量快照
     * @return 执行结果
     */
    SsoSyncTaskExecutionResult execute(SsoSyncTask task, List<SsoSyncTaskItem> scopedItems);
}
