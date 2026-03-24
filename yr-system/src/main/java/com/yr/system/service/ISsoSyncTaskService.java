/**
 * @file 身份中心同步任务服务接口
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service;

import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.mybatisplus.service.ICustomService;

import java.util.List;

/**
 * 身份中心同步任务服务接口。
 */
public interface ISsoSyncTaskService extends ICustomService<SsoSyncTask> {

    /**
     * 查询同步任务列表。
     *
     * @param query 查询条件
     * @return 同步任务列表
     */
    List<SsoSyncTask> selectSsoSyncTaskList(SsoSyncTask query);

    /**
     * 创建初始化导入任务。
     *
     * @param task 任务请求
     * @return 创建后的任务
     */
    SsoSyncTask initImportTask(SsoSyncTask task);

    /**
     * 重试已有同步任务。
     *
     * @param taskId 任务ID
     * @return 更新后的任务
     */
    SsoSyncTask retryTask(Long taskId);

    /**
     * 触发补偿任务。
     *
     * @param taskId 任务ID
     * @return 更新后的任务
     */
    SsoSyncTask compensateTask(Long taskId);

    /**
     * 查询任务详情。
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    SsoSyncTask selectSsoSyncTaskById(Long taskId);
}
