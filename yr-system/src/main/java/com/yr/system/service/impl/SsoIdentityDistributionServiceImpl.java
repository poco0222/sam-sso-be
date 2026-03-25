/**
 * @file DISTRIBUTION 执行服务实现
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.core.domain.entity.SsoSyncTaskItem;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysOrg;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.enums.MqActionType;
import com.yr.common.exception.CustomException;
import com.yr.common.service.MqProducerService;
import com.yr.system.domain.dto.SsoDistributionMessagePayload;
import com.yr.system.domain.dto.SsoIdentityImportSnapshot;
import com.yr.system.domain.dto.SsoSyncTaskExecutionResult;
import com.yr.system.domain.entity.SysUserDept;
import com.yr.system.domain.entity.SysUserOrg;
import com.yr.system.service.ISsoIdentityDistributionService;
import com.yr.system.service.support.SsoCurrentIdentitySnapshotLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行 current master -> RocketMQ 的 full-batch snapshot upsert 分发流程。
 */
@Service
@ConditionalOnBean(MqProducerService.class)
public class SsoIdentityDistributionServiceImpl implements ISsoIdentityDistributionService {

    /** RocketMQ distribution topic。 */
    private static final String DISTRIBUTION_TOPIC = "sso-identity-distribution";

    /** 组织实体类型。 */
    private static final String ENTITY_TYPE_ORG = "org";

    /** 部门实体类型。 */
    private static final String ENTITY_TYPE_DEPT = "dept";

    /** 用户实体类型。 */
    private static final String ENTITY_TYPE_USER = "user";

    /** 用户组织关系实体类型。 */
    private static final String ENTITY_TYPE_USER_ORG = "user_org_relation";

    /** 用户部门关系实体类型。 */
    private static final String ENTITY_TYPE_USER_DEPT = "user_dept_relation";

    /** 成功状态。 */
    private static final String ITEM_STATUS_SUCCESS = "SUCCESS";

    /** 失败状态。 */
    private static final String ITEM_STATUS_FAILED = "FAILED";

    /** item 错误信息长度上限。 */
    private static final int MAX_ITEM_ERROR_MESSAGE_LENGTH = 500;

    /** 当前主库快照读取器。 */
    private final SsoCurrentIdentitySnapshotLoader ssoCurrentIdentitySnapshotLoader;

    /** MQ 发送服务。 */
    private final MqProducerService mqProducerService;

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * @param ssoCurrentIdentitySnapshotLoader 当前主库快照读取器
     * @param mqProducerService MQ 发送服务
     * @param objectMapper JSON 序列化器
     */
    public SsoIdentityDistributionServiceImpl(SsoCurrentIdentitySnapshotLoader ssoCurrentIdentitySnapshotLoader,
                                              MqProducerService mqProducerService,
                                              ObjectMapper objectMapper) {
        this.ssoCurrentIdentitySnapshotLoader = ssoCurrentIdentitySnapshotLoader;
        this.mqProducerService = mqProducerService;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行分发。
     *
     * @param task 同步任务
     * @param scopedItems 指定执行范围；为空时表示当前全量快照
     * @return 执行结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SsoSyncTaskExecutionResult execute(SsoSyncTask task, List<SsoSyncTaskItem> scopedItems) {
        SsoIdentityImportSnapshot snapshot = scopedItems == null || scopedItems.isEmpty()
                ? ssoCurrentIdentitySnapshotLoader.loadSnapshot()
                : null;
        List<SsoSyncTaskItem> itemList = scopedItems == null || scopedItems.isEmpty()
                ? buildSnapshotItems(snapshot)
                : cloneScopedItems(scopedItems);

        SnapshotIndex snapshotIndex = snapshot == null ? null : buildSnapshotIndex(snapshot);
        int successCount = 0;
        int failedCount = 0;

        for (SsoSyncTaskItem item : itemList) {
            try {
                sendItem(task, item, snapshotIndex);
                item.setStatus(ITEM_STATUS_SUCCESS);
                item.setErrorMessage(null);
                successCount++;
            } catch (RuntimeException exception) {
                item.setStatus(ITEM_STATUS_FAILED);
                item.setErrorMessage(normalizeErrorMessage(exception.getMessage()));
                failedCount++;
            }
        }

        SsoSyncTaskExecutionResult result = new SsoSyncTaskExecutionResult();
        result.setItemList(itemList);
        result.setTotalItemCount(itemList.size());
        result.setSuccessItemCount(successCount);
        result.setFailedItemCount(failedCount);
        result.setStatus(resolveTaskStatus(itemList.size(), successCount, failedCount));
        result.setResultSummary(buildResultSummary(itemList.size(), successCount, failedCount));
        return result;
    }

    /**
     * 构建快照索引，便于当前全量分发按 sourceId 定位实体。
     *
     * @param snapshot 当前快照
     * @return 快照索引
     */
    private SnapshotIndex buildSnapshotIndex(SsoIdentityImportSnapshot snapshot) {
        SnapshotIndex snapshotIndex = new SnapshotIndex();
        snapshot.getOrgList().forEach(org -> snapshotIndex.orgMap.put(String.valueOf(org.getOrgId()), org));
        snapshot.getDeptList().forEach(dept -> snapshotIndex.deptMap.put(String.valueOf(dept.getDeptId()), dept));
        snapshot.getUserList().forEach(user -> snapshotIndex.userMap.put(String.valueOf(user.getUserId()), user));
        snapshot.getUserOrgRelationList().forEach(relation -> snapshotIndex.userOrgMap.put(buildUserOrgSourceId(relation), relation));
        snapshot.getUserDeptRelationList().forEach(relation -> snapshotIndex.userDeptMap.put(buildUserDeptSourceId(relation), relation));
        return snapshotIndex;
    }

    /**
     * 把当前快照展开为固定顺序的 task item 列表。
     *
     * @param snapshot 当前快照
     * @return task item 列表
     */
    private List<SsoSyncTaskItem> buildSnapshotItems(SsoIdentityImportSnapshot snapshot) {
        List<SsoSyncTaskItem> itemList = new ArrayList<>();
        snapshot.getOrgList().stream()
                .sorted(Comparator.comparing(SysOrg::getOrgId))
                .forEach(org -> itemList.add(buildItem(ENTITY_TYPE_ORG, String.valueOf(org.getOrgId()))));
        snapshot.getDeptList().stream()
                .sorted(Comparator.comparing(SysDept::getDeptId))
                .forEach(dept -> itemList.add(buildItem(ENTITY_TYPE_DEPT, String.valueOf(dept.getDeptId()))));
        snapshot.getUserList().stream()
                .sorted(Comparator.comparing(SysUser::getUserId))
                .forEach(user -> itemList.add(buildItem(ENTITY_TYPE_USER, String.valueOf(user.getUserId()))));
        snapshot.getUserOrgRelationList().stream()
                .sorted(Comparator.comparing(this::buildUserOrgSourceId))
                .forEach(relation -> itemList.add(buildItem(ENTITY_TYPE_USER_ORG, buildUserOrgSourceId(relation))));
        snapshot.getUserDeptRelationList().stream()
                .sorted(Comparator.comparing(this::buildUserDeptSourceId))
                .forEach(relation -> itemList.add(buildItem(ENTITY_TYPE_USER_DEPT, buildUserDeptSourceId(relation))));
        return itemList;
    }

    /**
     * 复制 scoped item，避免直接污染历史任务上的明细对象。
     *
     * @param scopedItems 原 scoped item 列表
     * @return 可重新发送的新明细列表
     */
    private List<SsoSyncTaskItem> cloneScopedItems(List<SsoSyncTaskItem> scopedItems) {
        List<SsoSyncTaskItem> clonedItems = new ArrayList<>(scopedItems.size());
        for (SsoSyncTaskItem scopedItem : scopedItems) {
            SsoSyncTaskItem clonedItem = new SsoSyncTaskItem();
            clonedItem.setEntityType(scopedItem.getEntityType());
            clonedItem.setSourceId(scopedItem.getSourceId());
            clonedItem.setTargetId(scopedItem.getTargetId());
            clonedItem.setDetailJson(scopedItem.getDetailJson());
            clonedItems.add(clonedItem);
        }
        return clonedItems;
    }

    /**
     * 发送单条分发消息。
     *
     * @param task 当前任务
     * @param item 当前明细
     * @param snapshotIndex 当前快照索引；scoped compensation 时允许为空
     */
    private void sendItem(SsoSyncTask task, SsoSyncTaskItem item, SnapshotIndex snapshotIndex) {
        SsoDistributionMessagePayload payload = item.getDetailJson() == null || item.getDetailJson().isBlank()
                ? buildPayloadFromSnapshot(task, item, snapshotIndex)
                : rebuildPayloadFromStoredDetail(task, item);
        item.setMsgKey(payload.getMsgKey());
        item.setTargetId(item.getSourceId());
        item.setDetailJson(serializeDetail(payload));
        boolean sendResult = mqProducerService.send(
                DISTRIBUTION_TOPIC,
                resolveTargetTag(task),
                MqActionType.UPSERT,
                payload.getMsgKey(),
                payload
        );
        if (!sendResult) {
            throw new CustomException("MQ 发送失败，请检查 mq_message_log，msgKey=" + payload.getMsgKey());
        }
    }

    /**
     * 从当前快照构造本轮消息载荷。
     *
     * @param task 当前任务
     * @param item 当前明细
     * @param snapshotIndex 快照索引
     * @return 单条消息载荷
     */
    private SsoDistributionMessagePayload buildPayloadFromSnapshot(SsoSyncTask task,
                                                                   SsoSyncTaskItem item,
                                                                   SnapshotIndex snapshotIndex) {
        if (snapshotIndex == null) {
            throw new CustomException("当前分发缺少快照索引");
        }
        Map<String, Object> payload = switch (item.getEntityType()) {
            case ENTITY_TYPE_ORG -> buildOrgPayload(snapshotIndex.orgMap.get(item.getSourceId()), item.getSourceId());
            case ENTITY_TYPE_DEPT -> buildDeptPayload(snapshotIndex.deptMap.get(item.getSourceId()), item.getSourceId());
            case ENTITY_TYPE_USER -> buildUserPayload(snapshotIndex.userMap.get(item.getSourceId()), item.getSourceId());
            case ENTITY_TYPE_USER_ORG -> buildUserOrgPayload(snapshotIndex.userOrgMap.get(item.getSourceId()), item.getSourceId());
            case ENTITY_TYPE_USER_DEPT -> buildUserDeptPayload(snapshotIndex.userDeptMap.get(item.getSourceId()), item.getSourceId());
            default -> throw new CustomException("不支持的 DISTRIBUTION 明细类型: " + item.getEntityType());
        };
        return buildMessagePayload(task, item.getEntityType(), item.getSourceId(), payload);
    }

    /**
     * 从历史明细中重建补偿消息载荷，只替换本轮 task/batch/msgKey 上下文。
     *
     * @param task 当前任务
     * @param item 当前明细
     * @return 单条消息载荷
     */
    private SsoDistributionMessagePayload rebuildPayloadFromStoredDetail(SsoSyncTask task, SsoSyncTaskItem item) {
        SsoDistributionMessagePayload storedPayload = deserializeDetail(item.getDetailJson());
        return buildMessagePayload(task, item.getEntityType(), item.getSourceId(), storedPayload.getPayload());
    }

    /**
     * 统一构造单条分发消息载荷。
     *
     * @param task 当前任务
     * @param entityType 实体类型
     * @param sourceId 来源主键
     * @param payload 单条实体载荷
     * @return 消息载荷
     */
    private SsoDistributionMessagePayload buildMessagePayload(SsoSyncTask task,
                                                              String entityType,
                                                              String sourceId,
                                                              Map<String, Object> payload) {
        SsoDistributionMessagePayload messagePayload = new SsoDistributionMessagePayload();
        messagePayload.setTaskId(task.getTaskId());
        messagePayload.setBatchNo(task.getBatchNo());
        messagePayload.setTargetClientCode(task.getTargetClientCode());
        messagePayload.setDeliveryMode(SsoDistributionMessagePayload.DELIVERY_MODE_FULL_BATCH_SNAPSHOT);
        // transport 层复用 `U` 写 MQ 履历，业务语义仍显式标记为 UPSERT。
        messagePayload.setMqActionType("UPSERT");
        messagePayload.setSourceSystem(SsoDistributionMessagePayload.SOURCE_SYSTEM_LOCAL_SAM_EMPTY);
        messagePayload.setSnapshotAt(resolveSnapshotAt(task));
        messagePayload.setEntityType(entityType);
        messagePayload.setSourceId(sourceId);
        messagePayload.setMsgKey(buildMsgKey(task.getTaskId(), entityType, sourceId));
        messagePayload.setPayload(payload);
        return messagePayload;
    }

    /**
     * 构造组织 payload。
     */
    private Map<String, Object> buildOrgPayload(SysOrg org, String sourceId) {
        if (org == null) {
            throw new CustomException("未找到当前组织快照: " + sourceId);
        }
        return detailMap(
                "orgId", org.getOrgId(),
                "orgCode", org.getOrgCode(),
                "orgName", org.getOrgName(),
                "status", org.getStatus(),
                "parentId", org.getParentId()
        );
    }

    /**
     * 构造部门 payload。
     */
    private Map<String, Object> buildDeptPayload(SysDept dept, String sourceId) {
        if (dept == null) {
            throw new CustomException("未找到当前部门快照: " + sourceId);
        }
        return detailMap(
                "deptId", dept.getDeptId(),
                "deptCode", dept.getDeptCode(),
                "deptName", dept.getDeptName(),
                "orgId", dept.getOrgId(),
                "parentId", dept.getParentId(),
                "status", dept.getStatus()
        );
    }

    /**
     * 构造用户 payload。
     */
    private Map<String, Object> buildUserPayload(SysUser user, String sourceId) {
        if (user == null) {
            throw new CustomException("未找到当前用户快照: " + sourceId);
        }
        return detailMap(
                "userId", user.getUserId(),
                "userName", user.getUserName(),
                "nickName", user.getNickName(),
                "deptId", user.getDeptId(),
                "status", user.getStatus(),
                "email", user.getEmail(),
                "phonenumber", user.getPhonenumber()
        );
    }

    /**
     * 构造用户组织关系 payload。
     */
    private Map<String, Object> buildUserOrgPayload(SysUserOrg relation, String sourceId) {
        if (relation == null) {
            throw new CustomException("未找到当前用户组织关系快照: " + sourceId);
        }
        return detailMap(
                "userId", relation.getUserId(),
                "orgId", relation.getOrgId(),
                "isDefault", relation.getIsDefault(),
                "enabled", relation.getEnabled()
        );
    }

    /**
     * 构造用户部门关系 payload。
     */
    private Map<String, Object> buildUserDeptPayload(SysUserDept relation, String sourceId) {
        if (relation == null) {
            throw new CustomException("未找到当前用户部门关系快照: " + sourceId);
        }
        return detailMap(
                "userId", relation.getUserId(),
                "deptId", relation.getDeptId(),
                "isDefault", relation.getIsDefault(),
                "enabled", relation.getEnabled()
        );
    }

    /**
     * 生成用户组织关系来源 ID。
     */
    private String buildUserOrgSourceId(SysUserOrg relation) {
        return relation.getUserId() + ":" + relation.getOrgId();
    }

    /**
     * 生成用户部门关系来源 ID。
     */
    private String buildUserDeptSourceId(SysUserDept relation) {
        return relation.getUserId() + ":" + relation.getDeptId();
    }

    /**
     * 构造基础 item。
     */
    private SsoSyncTaskItem buildItem(String entityType, String sourceId) {
        SsoSyncTaskItem item = new SsoSyncTaskItem();
        item.setEntityType(entityType);
        item.setSourceId(sourceId);
        return item;
    }

    /**
     * 构造消息键，显式把 task/entity/source 三层语义串起来。
     */
    private String buildMsgKey(Long taskId, String entityType, String sourceId) {
        return "DIST:" + taskId + ":" + entityType + ":" + sourceId;
    }

    /**
     * 解析目标 tag。
     */
    private String resolveTargetTag(SsoSyncTask task) {
        if (task.getTargetClientCode() == null || task.getTargetClientCode().isBlank()) {
            throw new CustomException("DISTRIBUTION 目标客户端编码不能为空");
        }
        return task.getTargetClientCode();
    }

    /**
     * 解析快照时间毫秒值。
     */
    private long resolveSnapshotAt(SsoSyncTask task) {
        return task.getImportSnapshotAt() == null ? new Date().getTime() : task.getImportSnapshotAt().getTime();
    }

    /**
     * 根据成功/失败数解析任务状态。
     */
    private String resolveTaskStatus(int totalCount, int successCount, int failedCount) {
        if (totalCount == 0) {
            return SsoSyncTask.STATUS_SUCCESS;
        }
        if (failedCount == 0) {
            return SsoSyncTask.STATUS_SUCCESS;
        }
        if (successCount == 0) {
            return SsoSyncTask.STATUS_FAILED;
        }
        return SsoSyncTask.STATUS_PARTIAL_SUCCESS;
    }

    /**
     * 构造结果摘要。
     */
    private String buildResultSummary(int totalCount, int successCount, int failedCount) {
        return String.format("DISTRIBUTION 完成，总计 %d 条，成功 %d 条，失败 %d 条", totalCount, successCount, failedCount);
    }

    /**
     * 构造允许空值的 detail map。
     */
    private Map<String, Object> detailMap(Object... pairs) {
        Map<String, Object> detailMap = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            detailMap.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return detailMap;
    }

    /**
     * 序列化消息载荷。
     */
    private String serializeDetail(SsoDistributionMessagePayload detailPayload) {
        try {
            return objectMapper.writeValueAsString(detailPayload);
        } catch (JsonProcessingException exception) {
            throw new CustomException("序列化 DISTRIBUTION 明细失败");
        }
    }

    /**
     * 反序列化历史明细。
     */
    private SsoDistributionMessagePayload deserializeDetail(String detailJson) {
        try {
            return objectMapper.readValue(detailJson, SsoDistributionMessagePayload.class);
        } catch (IOException exception) {
            throw new CustomException("反序列化 DISTRIBUTION 明细失败");
        }
    }

    /**
     * 裁剪错误信息，避免失败明细再次触发落库异常。
     */
    private String normalizeErrorMessage(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return errorMessage;
        }
        if (errorMessage.length() <= MAX_ITEM_ERROR_MESSAGE_LENGTH) {
            return errorMessage;
        }
        return errorMessage.substring(0, MAX_ITEM_ERROR_MESSAGE_LENGTH - 3) + "...";
    }

    /**
     * 当前快照索引。
     */
    private static final class SnapshotIndex {
        private final Map<String, SysOrg> orgMap = new LinkedHashMap<>();
        private final Map<String, SysDept> deptMap = new LinkedHashMap<>();
        private final Map<String, SysUser> userMap = new LinkedHashMap<>();
        private final Map<String, SysUserOrg> userOrgMap = new LinkedHashMap<>();
        private final Map<String, SysUserDept> userDeptMap = new LinkedHashMap<>();
    }
}
