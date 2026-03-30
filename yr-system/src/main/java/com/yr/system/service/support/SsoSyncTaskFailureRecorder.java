/**
 * @file 同步任务失败状态记录器
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.system.service.support;

import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.system.domain.dto.SsoSyncTaskExecutionResult;
import com.yr.system.mapper.SsoSyncTaskMapper;
import com.yr.system.service.ISsoSyncTaskItemService;
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

    /** 同步任务明细服务。 */
    private final ISsoSyncTaskItemService ssoSyncTaskItemService;

    /**
     * @param ssoSyncTaskMapper 同步任务 Mapper
     * @param ssoSyncTaskItemService 同步任务明细服务
     */
    public SsoSyncTaskFailureRecorder(SsoSyncTaskMapper ssoSyncTaskMapper,
                                      ISsoSyncTaskItemService ssoSyncTaskItemService) {
        this.ssoSyncTaskMapper = ssoSyncTaskMapper;
        this.ssoSyncTaskItemService = ssoSyncTaskItemService;
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
     * 在独立事务中记录 after-commit 状态回写失败后的任务级对账失败标记。
     *
     * @param task 当前同步任务
     * @param executionResult after-commit 执行结果
     * @param exception 状态回写阶段抛出的异常
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordDispatchReconciliationFailure(SsoSyncTask task,
                                                    SsoSyncTaskExecutionResult executionResult,
                                                    RuntimeException exception) {
        String fallbackStatus = resolveDispatchReconciliationFailureStatus(executionResult);
        String fallbackSummary = buildDispatchReconciliationFailureSummary(executionResult, exception);

        if (executionResult != null) {
            executionResult.setStatus(fallbackStatus);
            executionResult.setResultSummary(fallbackSummary);
        }
        task.setStatus(fallbackStatus);
        task.setResultSummary(fallbackSummary);
        task.setExecuteAt(new Date());
        ssoSyncTaskMapper.updateById(task);
        if (executionResult == null || executionResult.getItemList() == null || executionResult.getItemList().isEmpty()) {
            return;
        }
        try {
            ssoSyncTaskItemService.updateDispatchResult(task.getTaskId(), executionResult.getItemList());
        } catch (RuntimeException itemException) {
            task.setResultSummary(fallbackSummary + "; item state fallback failed: " + resolveFailureSummary(itemException));
            ssoSyncTaskMapper.updateById(task);
        }
    }

    /**
     * 计算 recorder failure（结果回写失败）时应落回的任务状态。
     *
     * @param executionResult after-commit 执行结果
     * @return 兜底后的任务状态
     * @author PopoY
     */
    private String resolveDispatchReconciliationFailureStatus(SsoSyncTaskExecutionResult executionResult) {
        String originalStatus = executionResult == null ? null : executionResult.getStatus();

        if (SsoSyncTask.STATUS_PARTIAL_SUCCESS.equals(originalStatus) || SsoSyncTask.STATUS_FAILED.equals(originalStatus)) {
            return originalStatus;
        }
        if (SsoSyncTask.STATUS_SUCCESS.equals(originalStatus)) {
            return SsoSyncTask.STATUS_PARTIAL_SUCCESS;
        }
        return SsoSyncTask.STATUS_FAILED;
    }

    /**
     * 拼装 recorder failure（结果回写失败）时的任务摘要，显式暴露 reconciliation failure（对账失败）。
     *
     * @param executionResult after-commit 执行结果
     * @param exception 状态回写阶段异常
     * @return 兜底后的任务摘要
     * @author PopoY
     */
    private String buildDispatchReconciliationFailureSummary(SsoSyncTaskExecutionResult executionResult,
                                                             RuntimeException exception) {
        String originalSummary = executionResult == null ? null : executionResult.getResultSummary();
        String exceptionSummary = resolveFailureSummary(exception);
        String reconciliationSummary = "dispatch state reconciliation failed";

        if (originalSummary == null || originalSummary.isBlank()) {
            return reconciliationSummary + ": " + exceptionSummary;
        }
        return originalSummary + "; " + reconciliationSummary + ": " + exceptionSummary;
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
