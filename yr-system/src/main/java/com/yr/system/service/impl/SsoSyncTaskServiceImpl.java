/**
 * @file 身份中心同步任务服务实现
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.system.mapper.SsoSyncTaskMapper;
import com.yr.system.service.ISsoSyncTaskService;
import org.springframework.stereotype.Service;

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

    /**
     * 查询同步任务列表。
     *
     * @param query 查询条件
     * @return 同步任务列表
     */
    @Override
    public List<SsoSyncTask> selectSsoSyncTaskList(SsoSyncTask query) {
        LambdaQueryWrapper<SsoSyncTask> queryWrapper = new LambdaQueryWrapper<SsoSyncTask>()
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
    public SsoSyncTask initImportTask(SsoSyncTask task) {
        SsoSyncTask newTask = task == null ? new SsoSyncTask() : task;
        String batchSeed = UUID.randomUUID().toString().replace("-", "");
        newTask.setTaskType(SsoSyncTask.TASK_TYPE_INIT_IMPORT);
        if (newTask.getTriggerType() == null || newTask.getTriggerType().isBlank()) {
            newTask.setTriggerType("MANUAL");
        }
        if (newTask.getStatus() == null || newTask.getStatus().isBlank()) {
            newTask.setStatus(SsoSyncTask.STATUS_PENDING);
        }
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
        return newTask;
    }

    /**
     * 重试已有同步任务。
     *
     * @param taskId 任务ID
     * @return 更新后的任务
     */
    @Override
    public SsoSyncTask retryTask(Long taskId) {
        SsoSyncTask task = requireTask(taskId);
        task.setStatus(SsoSyncTask.STATUS_PENDING);
        task.setRetryCount(task.getRetryCount() == null ? 1 : task.getRetryCount() + 1);
        task.setExecuteAt(new Date());
        this.updateById(task);
        return task;
    }

    /**
     * 触发补偿任务。
     *
     * @param taskId 任务ID
     * @return 更新后的任务
     */
    @Override
    public SsoSyncTask compensateTask(Long taskId) {
        SsoSyncTask task = requireTask(taskId);
        task.setTaskType(SsoSyncTask.TASK_TYPE_COMPENSATION);
        task.setStatus(SsoSyncTask.STATUS_PENDING);
        task.setExecuteAt(new Date());
        this.updateById(task);
        return task;
    }

    /**
     * 查询任务详情。
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    @Override
    public SsoSyncTask selectSsoSyncTaskById(Long taskId) {
        return requireTask(taskId);
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
}
