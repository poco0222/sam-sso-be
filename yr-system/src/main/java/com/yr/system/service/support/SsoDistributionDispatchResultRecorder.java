/**
 * @file DISTRIBUTION after-commit 结果回写器
 * @author PopoY
 * @date 2026-03-30
 */
package com.yr.system.service.support;

import com.yr.common.exception.CustomException;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.system.domain.dto.SsoSyncTaskExecutionResult;
import com.yr.system.mapper.SsoSyncTaskMapper;
import com.yr.system.service.ISsoSyncTaskItemService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 使用独立事务回写 DISTRIBUTION after-commit 的最终任务/明细状态。
 */
@Component
public class SsoDistributionDispatchResultRecorder {

    /** 同步任务 Mapper。 */
    private final SsoSyncTaskMapper ssoSyncTaskMapper;

    /** 同步任务明细服务。 */
    private final ISsoSyncTaskItemService ssoSyncTaskItemService;

    /**
     * @param ssoSyncTaskMapper 同步任务 Mapper
     * @param ssoSyncTaskItemService 同步任务明细服务
     */
    public SsoDistributionDispatchResultRecorder(SsoSyncTaskMapper ssoSyncTaskMapper,
                                                 ISsoSyncTaskItemService ssoSyncTaskItemService) {
        this.ssoSyncTaskMapper = ssoSyncTaskMapper;
        this.ssoSyncTaskItemService = ssoSyncTaskItemService;
    }

    /**
     * 在新事务中把 after-commit 发送后的 task/item 最终态落回数据库。
     *
     * @param task 当前同步任务
     * @param executionResult after-commit 最终执行结果
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordDispatchResult(SsoSyncTask task, SsoSyncTaskExecutionResult executionResult) {
        SsoSyncTask taskSnapshot = new SsoSyncTask();
        int updatedRows;

        taskSnapshot.setTaskId(task.getTaskId());
        taskSnapshot.setStatus(executionResult.getStatus());
        taskSnapshot.setResultSummary(executionResult.getResultSummary());
        taskSnapshot.setExecuteAt(new Date());
        updatedRows = ssoSyncTaskMapper.updateById(taskSnapshot);
        if (updatedRows != 1) {
            throw new CustomException("DISTRIBUTION 任务最终状态回写失败，taskId=" + task.getTaskId());
        }
        ssoSyncTaskItemService.updateDispatchResult(task.getTaskId(), executionResult.getItemList());
    }
}
