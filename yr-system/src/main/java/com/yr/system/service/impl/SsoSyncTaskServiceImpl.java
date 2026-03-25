/**
 * @file 身份中心同步任务服务实现
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.core.domain.entity.SsoSyncTaskItem;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.system.domain.dto.SsoIdentityImportExecutionResult;
import com.yr.system.mapper.SsoSyncTaskMapper;
import com.yr.system.service.ISsoIdentityImportService;
import com.yr.system.service.ISsoSyncTaskService;
import com.yr.system.service.ISsoSyncTaskItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 身份中心同步任务服务实现。
 */
@Service
public class SsoSyncTaskServiceImpl extends CustomServiceImpl<SsoSyncTaskMapper, SsoSyncTask> implements ISsoSyncTaskService {

    /** INIT_IMPORT 中用户组织关系采用组合键继承语义。 */
    private static final String USER_ORG_RELATION_IDENTITY = "userId+orgId";

    /** INIT_IMPORT 中用户部门关系采用组合键继承语义。 */
    private static final String USER_DEPT_RELATION_IDENTITY = "userId+deptId";

    /** INIT_IMPORT 服务；在纯单元测试场景允许为空。 */
    @Autowired(required = false)
    private ISsoIdentityImportService ssoIdentityImportService;

    /** 同步任务明细服务；在纯单元测试场景允许为空。 */
    @Autowired(required = false)
    private ISsoSyncTaskItemService ssoSyncTaskItemService;

    /**
     * 查询同步任务列表。
     *
     * @param query 查询条件
     * @return 同步任务列表
     */
    @Override
    public List<SsoSyncTask> selectSsoSyncTaskList(SsoSyncTask query) {
        LambdaQueryWrapper<SsoSyncTask> queryWrapper = new LambdaQueryWrapper<SsoSyncTask>()
                .eq(query != null && query.getTaskId() != null, SsoSyncTask::getTaskId, query.getTaskId())
                .eq(query != null && query.getTaskType() != null && !query.getTaskType().isBlank(), SsoSyncTask::getTaskType, query.getTaskType())
                .eq(query != null && query.getStatus() != null && !query.getStatus().isBlank(), SsoSyncTask::getStatus, query.getStatus())
                .eq(query != null && query.getTargetClientCode() != null && !query.getTargetClientCode().isBlank(), SsoSyncTask::getTargetClientCode, query.getTargetClientCode())
                .orderByDesc(SsoSyncTask::getTaskId);
        return this.list(queryWrapper);
    }

    /**
     * 创建初始化导入任务。
     *
     * @param task 任务请求
     * @return 创建后的任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SsoSyncTask initImportTask(SsoSyncTask task) {
        SsoSyncTask newTask = task == null ? new SsoSyncTask() : task;
        String batchSeed = UUID.randomUUID().toString().replace("-", "");
        newTask.setTaskType(SsoSyncTask.TASK_TYPE_INIT_IMPORT);
        if (newTask.getTriggerType() == null || newTask.getTriggerType().isBlank()) {
            newTask.setTriggerType("MANUAL");
        }
        newTask.setStatus(SsoSyncTask.STATUS_RUNNING);
        newTask.setRetryCount(0);
        newTask.setBatchNo("INIT-" + batchSeed);
        newTask.setIdStrategy(SsoSyncTask.ID_STRATEGY_INHERIT_SOURCE_ID);
        newTask.setOwnershipTransferStatus(SsoSyncTask.OWNERSHIP_TRANSFERRED);
        if (newTask.getSourceBatchNo() == null || newTask.getSourceBatchNo().isBlank()) {
            newTask.setSourceBatchNo("SRC-" + batchSeed);
        }
        newTask.setImportSnapshotAt(new Date());
        newTask.setPayloadJson(buildInitImportPayload());
        this.save(newTask);
        return executeTask(newTask, null, false);
    }

    /**
     * 重试已有同步任务。
     *
     * @param taskId 任务ID
     * @return 更新后的任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SsoSyncTask retryTask(Long taskId) {
        SsoSyncTask task = requireTask(taskId);
        task.setRetryCount(task.getRetryCount() == null ? 1 : task.getRetryCount() + 1);
        task.setStatus(SsoSyncTask.STATUS_RUNNING);
        task.setExecuteAt(new Date());
        this.updateById(task);
        return executeTask(task, null, false);
    }

    /**
     * 触发补偿任务。
     *
     * @param taskId 任务ID
     * @return 更新后的任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SsoSyncTask compensateTask(Long taskId) {
        SsoSyncTask sourceTask = requireTask(taskId);
        List<SsoSyncTaskItem> failedItems = ssoSyncTaskItemService == null
                ? new ArrayList<>()
                : ssoSyncTaskItemService.selectFailedByTaskId(taskId);
        if (failedItems.isEmpty()) {
            throw new CustomException("当前任务没有失败明细，无法触发补偿");
        }

        SsoSyncTask compensationTask = new SsoSyncTask();
        compensationTask.setTaskType(SsoSyncTask.TASK_TYPE_COMPENSATION);
        compensationTask.setTriggerType("MANUAL");
        compensationTask.setTargetClientCode(sourceTask.getTargetClientCode());
        compensationTask.setBatchNo("COMP-" + UUID.randomUUID().toString().replace("-", ""));
        compensationTask.setStatus(SsoSyncTask.STATUS_RUNNING);
        compensationTask.setRetryCount(0);
        compensationTask.setExecuteAt(new Date());
        compensationTask.setIdStrategy(sourceTask.getIdStrategy());
        compensationTask.setOwnershipTransferStatus(sourceTask.getOwnershipTransferStatus());
        compensationTask.setSourceBatchNo(sourceTask.getSourceBatchNo());
        compensationTask.setImportSnapshotAt(new Date());
        compensationTask.setCreateBy(sourceTask.getCreateBy());
        compensationTask.setPayloadJson(buildCompensationPayload(sourceTask, failedItems));
        this.save(compensationTask);
        return executeTask(compensationTask, failedItems, true);
    }

    /**
     * 查询任务详情。
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    @Override
    public SsoSyncTask selectSsoSyncTaskById(Long taskId) {
        SsoSyncTask task = requireTask(taskId);
        attachTaskItems(task);
        return task;
    }

    /**
     * 查询并校验任务存在性。
     *
     * @param taskId 任务ID
     * @return 已存在任务
     */
    private SsoSyncTask requireTask(Long taskId) {
        if (taskId == null) {
            throw new CustomException("任务ID不能为空");
        }
        SsoSyncTask task = this.getById(taskId);
        if (task == null) {
            throw new CustomException("同步任务不存在");
        }
        return task;
    }

    /**
     * 执行任务并刷新 item 明细；没有注入执行器时退化为 skeleton 行为，便于纯单元测试继续复用。
     *
     * @param task 当前任务
     * @param scopedItems 指定执行范围；为空时执行完整快照
     * @param preserveTaskType 是否保留当前 taskType（补偿任务为 true）
     * @return 执行后的任务
     */
    private SsoSyncTask executeTask(SsoSyncTask task, List<SsoSyncTaskItem> scopedItems, boolean preserveTaskType) {
        if (ssoIdentityImportService == null || ssoSyncTaskItemService == null) {
            if (!preserveTaskType) {
                task.setStatus(SsoSyncTask.STATUS_PENDING);
            }
            attachTaskStatistics(task, new ArrayList<>());
            return task;
        }
        try {
            SsoIdentityImportExecutionResult executionResult = ssoIdentityImportService.execute(task, scopedItems);
            task.setStatus(executionResult.getStatus());
            task.setResultSummary(executionResult.getResultSummary());
            task.setExecuteAt(new Date());
            this.updateById(task);
            ssoSyncTaskItemService.replaceTaskItems(task.getTaskId(), executionResult.getItemList());
            task.setItemList(executionResult.getItemList());
            task.setTotalItemCount(executionResult.getTotalItemCount());
            task.setSuccessItemCount(executionResult.getSuccessItemCount());
            task.setFailedItemCount(executionResult.getFailedItemCount());
            return task;
        } catch (RuntimeException exception) {
            task.setStatus(SsoSyncTask.STATUS_FAILED);
            task.setResultSummary(exception.getMessage());
            task.setExecuteAt(new Date());
            this.updateById(task);
            throw exception;
        }
    }

    /**
     * 给详情对象挂接 item 明细与统计信息。
     *
     * @param task 任务详情
     */
    private void attachTaskItems(SsoSyncTask task) {
        List<SsoSyncTaskItem> itemList = ssoSyncTaskItemService == null
                ? new ArrayList<>()
                : ssoSyncTaskItemService.selectByTaskId(task.getTaskId());
        task.setItemList(itemList);
        attachTaskStatistics(task, itemList);
    }

    /**
     * 回填明细统计字段。
     *
     * @param task 任务
     * @param itemList 明细列表
     */
    private void attachTaskStatistics(SsoSyncTask task, List<SsoSyncTaskItem> itemList) {
        long totalCount = itemList.size();
        long successCount = itemList.stream().filter(item -> "SUCCESS".equals(item.getStatus())).count();
        long failedCount = itemList.stream().filter(item -> "FAILED".equals(item.getStatus())).count();
        task.setTotalItemCount(totalCount);
        task.setSuccessItemCount(successCount);
        task.setFailedItemCount(failedCount);
    }

    /**
     * 构造 INIT_IMPORT 的最小结构化契约，显式记录导入范围与关系继承策略。
     *
     * @return INIT_IMPORT 任务载荷 JSON
     */
    private String buildInitImportPayload() {
        return String.format(
                "{\"entityScopes\":[\"org\",\"dept\",\"user\",\"user_org_relation\",\"user_dept_relation\"],"
                        + "\"identityRules\":{\"org\":\"%1$s\",\"dept\":\"%1$s\",\"user\":\"%1$s\","
                        + "\"user_org_relation\":\"%2$s\",\"user_dept_relation\":\"%3$s\"}}",
                SsoSyncTask.ID_STRATEGY_INHERIT_SOURCE_ID,
                USER_ORG_RELATION_IDENTITY,
                USER_DEPT_RELATION_IDENTITY
        );
    }

    /**
     * 构造补偿任务载荷，显式记录来源任务与失败明细范围。
     *
     * @param sourceTask 来源任务
     * @param failedItems 失败明细
     * @return 补偿任务 payload
     */
    private String buildCompensationPayload(SsoSyncTask sourceTask, List<SsoSyncTaskItem> failedItems) {
        String scopedItems = failedItems.stream()
                .map(item -> String.format("{\"entityType\":\"%s\",\"sourceId\":\"%s\"}", item.getEntityType(), item.getSourceId()))
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        return String.format(
                "{\"sourceTaskId\":%d,\"sourceBatchNo\":\"%s\",\"failedItems\":[%s]}",
                sourceTask.getTaskId(),
                sourceTask.getSourceBatchNo(),
                scopedItems
        );
    }
}
