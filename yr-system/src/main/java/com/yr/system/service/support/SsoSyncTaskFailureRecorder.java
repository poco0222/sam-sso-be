/**
 * @file 同步任务失败状态记录器
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.system.service.support;

import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.system.mapper.SsoSyncTaskMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 使用独立事务记录同步任务失败状态，避免被外层业务回滚吞掉。
 */
@Component
public class SsoSyncTaskFailureRecorder {

    /** 同步任务持久化 Mapper。 */
    private final SsoSyncTaskMapper ssoSyncTaskMapper;

    /**
     * @param ssoSyncTaskMapper 同步任务 Mapper
     */
    public SsoSyncTaskFailureRecorder(SsoSyncTaskMapper ssoSyncTaskMapper) {
        this.ssoSyncTaskMapper = ssoSyncTaskMapper;
    }

    /**
     * 在独立事务中创建任务骨架，确保后续失败更新时数据库中已有可见记录。
     *
     * @param task 待持久化的新任务
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void persistNewTask(SsoSyncTask task) {
        ssoSyncTaskMapper.insert(task);
    }

    /**
     * 在独立事务中回写 FAILED 状态与失败摘要。
     *
     * @param task 失败任务
     * @param exception 执行期间抛出的运行时异常
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordFailure(SsoSyncTask task, RuntimeException exception) {
        task.setStatus(SsoSyncTask.STATUS_FAILED);
        task.setResultSummary(resolveFailureSummary(exception));
        task.setExecuteAt(new Date());
        ssoSyncTaskMapper.updateById(task);
    }

    /**
     * 将运行时异常整理为可持久化的失败摘要。
     *
     * @param exception 失败异常
     * @return 失败摘要
     */
    private String resolveFailureSummary(RuntimeException exception) {
        if (exception == null) {
            return "同步任务执行失败";
        }
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
    }
}
