/**
 * @file 身份中心同步任务服务实现
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yr.common.core.domain.MqMessageLog;
import com.yr.common.core.domain.entity.SsoClient;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.core.domain.entity.SsoSyncTaskItem;
import com.yr.common.core.domain.model.SsoSyncTaskMessageLogView;
import com.yr.common.exception.CustomException;
import com.yr.common.mapper.MqMessageLogMapper;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.system.domain.dto.SsoDistributionMessagePayload;
import com.yr.system.domain.dto.SsoSyncTaskClientSummaryView;
import com.yr.system.domain.dto.SsoSyncTaskExecutionResult;
import com.yr.system.mapper.SsoSyncTaskMapper;
import com.yr.system.service.ISsoClientService;
import com.yr.system.service.ISsoIdentityDistributionService;
import com.yr.system.service.ISsoIdentityImportService;
import com.yr.system.service.ISsoSyncTaskService;
import com.yr.system.service.ISsoSyncTaskItemService;
import com.yr.system.service.support.SsoSyncTaskFailureRecorder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 身份中心同步任务服务实现。
 */
@Service
public class SsoSyncTaskServiceImpl extends CustomServiceImpl<SsoSyncTaskMapper, SsoSyncTask> implements ISsoSyncTaskService {
    /** JSON 序列化器。 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** INIT_IMPORT 中用户组织关系采用组合键继承语义。 */
    private static final String USER_ORG_RELATION_IDENTITY = "userId+orgId";

    /** INIT_IMPORT 中用户部门关系采用组合键继承语义。 */
    private static final String USER_DEPT_RELATION_IDENTITY = "userId+deptId";

    /** INIT_IMPORT 服务；在纯单元测试场景允许为空。 */
    @Autowired(required = false)
    private ISsoIdentityImportService ssoIdentityImportService;

    /** DISTRIBUTION 服务；在未启用 RocketMQ 时允许为空。 */
    @Autowired(required = false)
    private ISsoIdentityDistributionService ssoIdentityDistributionService;

    /** 同步任务明细服务；在纯单元测试场景允许为空。 */
    @Autowired(required = false)
    private ISsoSyncTaskItemService ssoSyncTaskItemService;

    /** MQ 履历查询 Mapper；在未启用 MQ 模块时允许为空。 */
    @Autowired(required = false)
    private MqMessageLogMapper mqMessageLogMapper;

    /** 客户端服务；用于分发前校验目标客户端是否合法。 */
    @Autowired
    private ISsoClientService ssoClientService;

    /** 失败状态独立事务记录器。 */
    @Autowired
    private SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder;

    /**
     * 查询同步任务列表。
     *
     * @param query 查询条件
     * @return 同步任务列表
     */
    @Override
    public List<SsoSyncTask> selectSsoSyncTaskList(SsoSyncTask query) {
        return this.list(buildTaskListQueryWrapper(query));
    }

    /**
     * 查询客户端维度的投递观测摘要。
     *
     * @param query 查询条件
     * @return 客户端观测摘要
     */
    @Override
    public List<SsoSyncTaskClientSummaryView> selectSsoSyncTaskClientSummaryList(SsoSyncTask query) {
        List<SsoSyncTask> taskSnapshots = this.list(buildClientSummaryQueryWrapper(query));
        Map<String, SsoSyncTaskClientSummaryView> summaryMap = new LinkedHashMap<>();

        for (SsoSyncTask taskSnapshot : taskSnapshots) {
            if (taskSnapshot.getTargetClientCode() == null || taskSnapshot.getTargetClientCode().isBlank()) {
                continue;
            }
            SsoSyncTaskClientSummaryView summaryView = summaryMap.computeIfAbsent(taskSnapshot.getTargetClientCode(), clientCode -> {
                SsoSyncTaskClientSummaryView createdView = new SsoSyncTaskClientSummaryView();
                createdView.setClientCode(clientCode);
                createdView.setFailedTaskCount(0L);
                return createdView;
            });

            if (summaryView.getLatestTaskId() == null) {
                summaryView.setLatestTaskId(taskSnapshot.getTaskId());
                summaryView.setLatestBatchNo(taskSnapshot.getBatchNo());
            }
            if (isProblemStatus(taskSnapshot.getStatus())) {
                summaryView.setFailedTaskCount(summaryView.getFailedTaskCount() + 1);
                if (summaryView.getLatestFailedTaskId() == null) {
                    summaryView.setLatestFailedTaskId(taskSnapshot.getTaskId());
                    summaryView.setLatestFailedBatchNo(taskSnapshot.getBatchNo());
                }
            }
            if (SsoSyncTask.STATUS_SUCCESS.equals(taskSnapshot.getStatus()) && isNewer(taskSnapshot.getExecuteAt(), summaryView.getLatestSuccessTime())) {
                summaryView.setLatestSuccessTime(taskSnapshot.getExecuteAt());
            }
        }
        return new ArrayList<>(summaryMap.values());
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
        ssoSyncTaskFailureRecorder.persistNewTask(newTask);
        return executeTask(newTask, null, newTask.getTaskType());
    }

    /**
     * 创建手工全量分发任务。
     *
     * @param task 任务请求
     * @return 创建后的任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SsoSyncTask distributionTask(SsoSyncTask task) {
        SsoSyncTask newTask = task == null ? new SsoSyncTask() : task;
        if (newTask.getTargetClientCode() == null || newTask.getTargetClientCode().isBlank()) {
            throw new CustomException("DISTRIBUTION 目标客户端编码不能为空");
        }
        validateDistributionTargetClient(newTask.getTargetClientCode());
        String batchSeed = UUID.randomUUID().toString().replace("-", "");
        newTask.setTaskType(SsoSyncTask.TASK_TYPE_DISTRIBUTION);
        if (newTask.getTriggerType() == null || newTask.getTriggerType().isBlank()) {
            newTask.setTriggerType("MANUAL");
        }
        newTask.setStatus(SsoSyncTask.STATUS_RUNNING);
        newTask.setRetryCount(0);
        newTask.setBatchNo("DIST-" + batchSeed);
        newTask.setIdStrategy(SsoSyncTask.ID_STRATEGY_INHERIT_SOURCE_ID);
        newTask.setOwnershipTransferStatus(SsoSyncTask.OWNERSHIP_TRANSFERRED);
        if (newTask.getSourceBatchNo() == null || newTask.getSourceBatchNo().isBlank()) {
            newTask.setSourceBatchNo("LOCAL-" + batchSeed);
        }
        newTask.setImportSnapshotAt(new Date());
        newTask.setPayloadJson(buildDistributionPayload());
        ssoSyncTaskFailureRecorder.persistNewTask(newTask);
        return executeTask(newTask, null, newTask.getTaskType());
    }

    /**
     * 校验分发目标客户端是否存在、启用且开启同步。
     *
     * @param targetClientCode 目标客户端编码
     */
    private void validateDistributionTargetClient(String targetClientCode) {
        SsoClient targetClient = ssoClientService.selectSsoClientByCode(targetClientCode);
        if (targetClient == null) {
            throw new CustomException("DISTRIBUTION 目标客户端不存在");
        }
        if (!"0".equals(targetClient.getStatus())) {
            throw new CustomException("DISTRIBUTION 目标客户端已停用");
        }
        if (!"Y".equals(targetClient.getSyncEnabled())) {
            throw new CustomException("DISTRIBUTION 目标客户端未启用同步");
        }
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
        int nextRetryCount;
        Date retryExecuteAt;
        UpdateWrapper<SsoSyncTask> retryUpdateWrapper;
        int affectedRows;

        if (!SsoSyncTask.STATUS_FAILED.equals(task.getStatus())) {
            throw new CustomException("只有失败态任务允许重试");
        }
        nextRetryCount = task.getRetryCount() == null ? 1 : task.getRetryCount() + 1;
        retryExecuteAt = new Date();
        retryUpdateWrapper = new UpdateWrapper<SsoSyncTask>()
                .eq("task_id", taskId)
                .eq("status", SsoSyncTask.STATUS_FAILED)
                .set("retry_count", nextRetryCount)
                .set("status", SsoSyncTask.STATUS_RUNNING)
                .set("execute_at", retryExecuteAt)
                .set("update_time", retryExecuteAt);
        affectedRows = this.getBaseMapper().update(null, retryUpdateWrapper);
        if (affectedRows != 1) {
            throw new CustomException("任务状态已变化，请刷新后重试");
        }
        task.setRetryCount(nextRetryCount);
        task.setStatus(SsoSyncTask.STATUS_RUNNING);
        task.setExecuteAt(retryExecuteAt);
        List<SsoSyncTaskItem> scopedItems = SsoSyncTask.TASK_TYPE_COMPENSATION.equals(task.getTaskType()) && ssoSyncTaskItemService != null
                ? ssoSyncTaskItemService.selectByTaskId(taskId)
                : null;
        return executeTask(task, scopedItems, resolveExecutionTaskType(task));
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
        ssoSyncTaskFailureRecorder.persistNewTask(compensationTask);
        return executeTask(compensationTask, failedItems, sourceTask.getTaskType());
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
     * @param executionTaskType 本轮真正要调用的执行器类型
     * @return 执行后的任务
     */
    private SsoSyncTask executeTask(SsoSyncTask task, List<SsoSyncTaskItem> scopedItems, String executionTaskType) {
        try {
            ensureExecutorReady(executionTaskType);
            SsoSyncTaskExecutionResult executionResult = executeByTaskType(executionTaskType, task, scopedItems);
            task.setStatus(executionResult.getStatus());
            task.setResultSummary(executionResult.getResultSummary());
            task.setExecuteAt(new Date());
            this.updateById(task);
            ssoSyncTaskItemService.replaceTaskItems(task.getTaskId(), executionResult.getItemList());
            hydrateTaskItems(executionResult.getItemList());
            attachMessageLogs(executionResult.getItemList());
            task.setItemList(executionResult.getItemList());
            task.setTotalItemCount(executionResult.getTotalItemCount());
            task.setSuccessItemCount(executionResult.getSuccessItemCount());
            task.setFailedItemCount(executionResult.getFailedItemCount());
            return task;
        } catch (RuntimeException exception) {
            ssoSyncTaskFailureRecorder.recordFailure(task, exception);
            throw exception;
        }
    }

    /**
     * 在真正执行前校验执行器与关键依赖是否已启用，缺失时直接 fail-fast。
     *
     * @param executionTaskType 本轮执行类型
     */
    private void ensureExecutorReady(String executionTaskType) {
        if (!hasExecutor(executionTaskType)) {
            throw new CustomException(executionTaskType + "执行器未启用，任务无法执行");
        }
        if (ssoSyncTaskItemService == null) {
            throw new CustomException("同步任务明细服务未启用，任务无法执行");
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
        hydrateTaskItems(itemList);
        attachMessageLogs(itemList);
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
     * 构造列表查询 wrapper，统一收口 task list 与 log 页过滤字段。
     *
     * @param query 查询条件
     * @return 查询 wrapper
     */
    private QueryWrapper<SsoSyncTask> buildTaskListQueryWrapper(SsoSyncTask query) {
        // MyBatis-Plus 3.4.x 的 lambda wrapper 在 JDK 17 下会反射 SerializedLambda，
        // 这里改用显式列名保持列表接口可在当前运行时稳定工作。
        return new QueryWrapper<SsoSyncTask>()
                .eq(query != null && query.getTaskId() != null, "task_id", query == null ? null : query.getTaskId())
                .eq(query != null && query.getTaskType() != null && !query.getTaskType().isBlank(), "task_type", query == null ? null : query.getTaskType())
                .eq(query != null && query.getStatus() != null && !query.getStatus().isBlank(), "status", query == null ? null : query.getStatus())
                .eq(query != null && query.getTargetClientCode() != null && !query.getTargetClientCode().isBlank(), "target_client_code", query == null ? null : query.getTargetClientCode())
                .eq(query != null && query.getBatchNo() != null && !query.getBatchNo().isBlank(), "batch_no", query == null ? null : query.getBatchNo())
                .orderByDesc("task_id");
    }

    /**
     * 构造客户端摘要查询 wrapper，只挑选摘要所需字段，避免把全量 payload 拉回内存。
     *
     * @param query 查询条件
     * @return 摘要查询 wrapper
     */
    private QueryWrapper<SsoSyncTask> buildClientSummaryQueryWrapper(SsoSyncTask query) {
        return new QueryWrapper<SsoSyncTask>()
                .select("task_id", "task_type", "target_client_code", "batch_no", "status", "execute_at")
                .eq(query != null && query.getTaskType() != null && !query.getTaskType().isBlank(), "task_type", query == null ? null : query.getTaskType())
                .eq(query != null && query.getTargetClientCode() != null && !query.getTargetClientCode().isBlank(), "target_client_code", query == null ? null : query.getTargetClientCode())
                .orderByDesc("task_id");
    }

    /**
     * 判断任务状态是否属于需要运营关注的问题态。
     *
     * @param status 任务状态
     * @return 问题态返回 true
     */
    private boolean isProblemStatus(String status) {
        return SsoSyncTask.STATUS_FAILED.equals(status) || SsoSyncTask.STATUS_PARTIAL_SUCCESS.equals(status);
    }

    /**
     * 判断当前时间是否比基准时间更新。
     *
     * @param candidate 候选时间
     * @param baseline 基准时间
     * @return 候选时间更新时返回 true
     */
    private boolean isNewer(Date candidate, Date baseline) {
        if (candidate == null) {
            return false;
        }
        if (baseline == null) {
            return true;
        }
        return candidate.after(baseline);
    }

    /**
     * 构造 INIT_IMPORT 的最小结构化契约，显式记录导入范围与关系继承策略。
     *
     * @return INIT_IMPORT 任务载荷 JSON
     */
    private String buildInitImportPayload() {
        List<String> entityScopes = List.of("org", "dept", "user", "user_org_relation", "user_dept_relation");
        Map<String, Object> identityRules = new LinkedHashMap<>();
        Map<String, Object> payload = new LinkedHashMap<>();

        identityRules.put("org", SsoSyncTask.ID_STRATEGY_INHERIT_SOURCE_ID);
        identityRules.put("dept", SsoSyncTask.ID_STRATEGY_INHERIT_SOURCE_ID);
        identityRules.put("user", SsoSyncTask.ID_STRATEGY_INHERIT_SOURCE_ID);
        identityRules.put("user_org_relation", USER_ORG_RELATION_IDENTITY);
        identityRules.put("user_dept_relation", USER_DEPT_RELATION_IDENTITY);
        payload.put("entityScopes", entityScopes);
        payload.put("identityRules", identityRules);
        return writeJson(payload);
    }

    /**
     * 构造 DISTRIBUTION 的最小结构化契约，显式记录 full-batch snapshot upsert 语义。
     *
     * @return DISTRIBUTION 任务载荷 JSON
     */
    private String buildDistributionPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("entityScopes", List.of("org", "dept", "user", "user_org_relation", "user_dept_relation"));
        payload.put("deliveryMode", SsoDistributionMessagePayload.DELIVERY_MODE_FULL_BATCH_SNAPSHOT);
        payload.put("mqActionType", "UPSERT");
        payload.put("sourceSystem", SsoDistributionMessagePayload.SOURCE_SYSTEM_LOCAL_SAM_EMPTY);
        return writeJson(payload);
    }

    /**
     * 构造补偿任务载荷，显式记录来源任务与失败明细范围。
     *
     * @param sourceTask 来源任务
     * @param failedItems 失败明细
     * @return 补偿任务 payload
     */
    private String buildCompensationPayload(SsoSyncTask sourceTask, List<SsoSyncTaskItem> failedItems) {
        List<Map<String, Object>> scopedItems = failedItems.stream()
                .map(item -> {
                    Map<String, Object> scopedItem = new LinkedHashMap<>();
                    scopedItem.put("entityType", item.getEntityType());
                    scopedItem.put("sourceId", item.getSourceId());
                    return scopedItem;
                })
                .toList();
        Map<String, Object> payload = new LinkedHashMap<>();

        payload.put("sourceTaskId", sourceTask.getTaskId());
        payload.put("sourceTaskType", sourceTask.getTaskType());
        payload.put("sourceBatchNo", sourceTask.getSourceBatchNo());
        payload.put("failedItems", scopedItems);
        return writeJson(payload);
    }

    /**
     * 根据任务类型选择真正的执行器。
     *
     * @param executionTaskType 执行器类型
     * @param task 当前任务
     * @param scopedItems 指定执行范围
     * @return 执行结果
     */
    private SsoSyncTaskExecutionResult executeByTaskType(String executionTaskType,
                                                         SsoSyncTask task,
                                                         List<SsoSyncTaskItem> scopedItems) {
        return switch (executionTaskType) {
            case SsoSyncTask.TASK_TYPE_DISTRIBUTION -> ssoIdentityDistributionService.execute(task, scopedItems);
            case SsoSyncTask.TASK_TYPE_INIT_IMPORT -> ssoIdentityImportService.execute(task, scopedItems);
            default -> throw new CustomException("不支持的同步任务执行类型: " + executionTaskType);
        };
    }

    /**
     * 判断当前任务类型是否已有可用执行器。
     *
     * @param executionTaskType 执行器类型
     * @return 可执行时返回 true
     */
    private boolean hasExecutor(String executionTaskType) {
        return switch (executionTaskType) {
            case SsoSyncTask.TASK_TYPE_DISTRIBUTION -> ssoIdentityDistributionService != null;
            case SsoSyncTask.TASK_TYPE_INIT_IMPORT -> ssoIdentityImportService != null;
            default -> false;
        };
    }

    /**
     * 解析补偿任务真正要走的执行器类型。
     *
     * @param task 当前任务
     * @return 执行器类型
     */
    private String resolveExecutionTaskType(SsoSyncTask task) {
        if (!SsoSyncTask.TASK_TYPE_COMPENSATION.equals(task.getTaskType())) {
            return task.getTaskType();
        }
        if (task.getPayloadJson() == null || task.getPayloadJson().isBlank()) {
            return SsoSyncTask.TASK_TYPE_INIT_IMPORT;
        }
        JsonNode payload = readJson(task.getPayloadJson());
        String sourceTaskType = payload.path("sourceTaskType").asText();
        return sourceTaskType == null || sourceTaskType.isBlank() ? SsoSyncTask.TASK_TYPE_INIT_IMPORT : sourceTaskType;
    }

    /**
     * 从 detailJson 回填运行态字段，便于 sync-task console 直接展示 msgKey。
     *
     * @param itemList 明细列表
     */
    private void hydrateTaskItems(List<SsoSyncTaskItem> itemList) {
        for (SsoSyncTaskItem item : itemList) {
            if (item.getMsgKey() != null && !item.getMsgKey().isBlank()) {
                continue;
            }
            if (item.getDetailJson() == null || item.getDetailJson().isBlank()) {
                continue;
            }
            try {
                JsonNode detailJson = readJson(item.getDetailJson());
                JsonNode msgKeyNode = detailJson.get("msgKey");
                item.setMsgKey(msgKeyNode == null || msgKeyNode.isNull() ? null : msgKeyNode.asText());
            } catch (Exception exception) {
                item.setMsgKey(null);
            }
        }
    }

    /**
     * 把对象序列化为 JSON（JavaScript 对象表示法）字符串。
     *
     * @param payload 待序列化对象
     * @return JSON 字符串
     */
    private String writeJson(Object payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("同步任务 payload 序列化失败", ex);
        }
    }

    /**
     * 读取 JSON（JavaScript 对象表示法）字符串。
     *
     * @param json JSON 字符串
     * @return JSON 树节点
     */
    private JsonNode readJson(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception ex) {
            throw new IllegalStateException("同步任务 payload 解析失败", ex);
        }
    }

    /**
     * 按 msgKey 批量挂接最新 MQ 履历视图，避免详情查询逐条打库。
     *
     * @param itemList 明细列表
     */
    private void attachMessageLogs(List<SsoSyncTaskItem> itemList) {
        if (mqMessageLogMapper == null || itemList == null || itemList.isEmpty()) {
            return;
        }
        List<String> msgKeys = itemList.stream()
                .map(SsoSyncTaskItem::getMsgKey)
                .filter(msgKey -> msgKey != null && !msgKey.isBlank())
                .distinct()
                .toList();
        if (msgKeys.isEmpty()) {
            return;
        }
        List<MqMessageLog> latestLogs = mqMessageLogMapper.selectLatestByMsgKeys(msgKeys);
        Map<String, SsoSyncTaskMessageLogView> messageLogViewMap = latestLogs.stream()
                .collect(Collectors.toMap(
                        MqMessageLog::getMsgKey,
                        this::buildMessageLogView,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        for (SsoSyncTaskItem item : itemList) {
            if (item.getMsgKey() == null || item.getMsgKey().isBlank()) {
                continue;
            }
            item.setMessageLog(messageLogViewMap.get(item.getMsgKey()));
        }
    }

    /**
     * 将持久化履历实体裁剪成 console 可直接消费的视图对象。
     *
     * @param mqMessageLog MQ 履历实体
     * @return MQ 履历视图
     */
    private SsoSyncTaskMessageLogView buildMessageLogView(MqMessageLog mqMessageLog) {
        SsoSyncTaskMessageLogView messageLogView = new SsoSyncTaskMessageLogView();
        messageLogView.setMsgId(mqMessageLog.getMsgId());
        messageLogView.setMsgKey(mqMessageLog.getMsgKey());
        messageLogView.setTopic(mqMessageLog.getTopic());
        messageLogView.setTag(mqMessageLog.getTag());
        messageLogView.setActionType(mqMessageLog.getActionType());
        messageLogView.setSendStatus(mqMessageLog.getSendStatus());
        messageLogView.setConsumeStatus(mqMessageLog.getConsumeStatus());
        messageLogView.setRetryCount(mqMessageLog.getRetryCount());
        messageLogView.setMaxRetry(mqMessageLog.getMaxRetry());
        messageLogView.setNextRetryTime(mqMessageLog.getNextRetryTime());
        messageLogView.setErrorMsg(mqMessageLog.getErrorMsg());
        messageLogView.setBody(mqMessageLog.getBody());
        messageLogView.setCreateTime(mqMessageLog.getCreateTime());
        messageLogView.setUpdateTime(mqMessageLog.getUpdateTime());
        return messageLogView;
    }
}
