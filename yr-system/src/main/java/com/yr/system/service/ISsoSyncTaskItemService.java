/**
 * @file 同步任务明细服务接口
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service;

import com.yr.common.core.domain.entity.SsoSyncTaskItem;
import com.yr.common.mybatisplus.service.ICustomService;

import java.util.List;

/**
 * 负责同步任务 item 明细的查询与替换写入。
 */
public interface ISsoSyncTaskItemService extends ICustomService<SsoSyncTaskItem> {

    /**
     * 查询任务下的全部明细。
     *
     * @param taskId 任务 ID
     * @return 任务明细列表
     */
    List<SsoSyncTaskItem> selectByTaskId(Long taskId);

    /**
     * 查询任务下的失败明细。
     *
     * @param taskId 任务 ID
     * @return 失败明细列表
     */
    List<SsoSyncTaskItem> selectFailedByTaskId(Long taskId);

    /**
     * 用新明细整体替换任务下的旧明细。
     *
     * @param taskId 任务 ID
     * @param itemList 新明细列表
     */
    void replaceTaskItems(Long taskId, List<SsoSyncTaskItem> itemList);

    /**
     * 按既有 `msgKey` 原位回写 DISTRIBUTION after-commit 的最终状态。
     *
     * @param taskId 任务 ID
     * @param itemList 最终明细列表
     */
    void updateDispatchResult(Long taskId, List<SsoSyncTaskItem> itemList);
}
