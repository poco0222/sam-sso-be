/**
 * @file INIT_IMPORT 执行服务实现
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.core.domain.entity.SsoSyncTaskItem;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysOrg;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.exception.CustomException;
import com.yr.system.domain.dto.SsoIdentityImportExecutionResult;
import com.yr.system.domain.dto.SsoIdentityImportSnapshot;
import com.yr.system.domain.entity.SysUserDept;
import com.yr.system.domain.entity.SysUserOrg;
import com.yr.system.mapper.SysDeptMapper;
import com.yr.system.mapper.SysOrgMapper;
import com.yr.system.mapper.SysUserDeptMapper;
import com.yr.system.mapper.SysUserMapper;
import com.yr.system.mapper.SysUserOrgMapper;
import com.yr.system.service.ISsoIdentityImportService;
import com.yr.system.service.ISsoLegacyIdentitySourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 执行 legacy snapshot -> identity center master 的导入流程。
 */
@Service
public class SsoIdentityImportServiceImpl implements ISsoIdentityImportService {

    /** 日志器。 */
    private static final Logger log = LoggerFactory.getLogger(SsoIdentityImportServiceImpl.class);

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

    /** task item 错误信息列长度上限，与 `sso_sync_task_item.error_message` 保持一致。 */
    private static final int MAX_ITEM_ERROR_MESSAGE_LENGTH = 500;

    /** 兼容 legacy 导入审计字段的默认操作人 ID。 */
    private static final String DEFAULT_OPERATOR_USER_ID = "1";

    /** legacy source 读取服务。 */
    private final ISsoLegacyIdentitySourceService ssoLegacyIdentitySourceService;

    /** 组织写入 Mapper。 */
    private final SysOrgMapper sysOrgMapper;

    /** 部门写入 Mapper。 */
    private final SysDeptMapper sysDeptMapper;

    /** 用户写入 Mapper。 */
    private final SysUserMapper sysUserMapper;

    /** 用户组织关系写入 Mapper。 */
    private final SysUserOrgMapper sysUserOrgMapper;

    /** 用户部门关系写入 Mapper。 */
    private final SysUserDeptMapper sysUserDeptMapper;

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * @param ssoLegacyIdentitySourceService legacy source 读取服务
     * @param sysOrgMapper 组织写入 Mapper
     * @param sysDeptMapper 部门写入 Mapper
     * @param sysUserMapper 用户写入 Mapper
     * @param sysUserOrgMapper 用户组织关系写入 Mapper
     * @param sysUserDeptMapper 用户部门关系写入 Mapper
     * @param objectMapper JSON 序列化器
     */
    public SsoIdentityImportServiceImpl(ISsoLegacyIdentitySourceService ssoLegacyIdentitySourceService,
                                        SysOrgMapper sysOrgMapper,
                                        SysDeptMapper sysDeptMapper,
                                        SysUserMapper sysUserMapper,
                                        SysUserOrgMapper sysUserOrgMapper,
                                        SysUserDeptMapper sysUserDeptMapper,
                                        ObjectMapper objectMapper) {
        this.ssoLegacyIdentitySourceService = ssoLegacyIdentitySourceService;
        this.sysOrgMapper = sysOrgMapper;
        this.sysDeptMapper = sysDeptMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysUserOrgMapper = sysUserOrgMapper;
        this.sysUserDeptMapper = sysUserDeptMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行导入。
     *
     * @param task 同步任务
     * @param scopedItems 指定执行范围；为空时表示完整快照
     * @return 执行结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SsoIdentityImportExecutionResult execute(SsoSyncTask task, List<SsoSyncTaskItem> scopedItems) {
        SsoIdentityImportSnapshot snapshot = ssoLegacyIdentitySourceService.loadSnapshot();
        List<SsoSyncTaskItem> itemList = scopedItems == null || scopedItems.isEmpty()
                ? buildSnapshotItems(snapshot)
                : cloneScopedItems(scopedItems);

        SnapshotIndex snapshotIndex = buildSnapshotIndex(snapshot);
        int successCount = 0;
        int failedCount = 0;

        for (SsoSyncTaskItem item : itemList) {
            try {
                processItem(item, snapshotIndex, task);
                item.setStatus(ITEM_STATUS_SUCCESS);
                successCount++;
            } catch (RuntimeException exception) {
                // 每条失败都落 warning，便于 rehearsal 时直接定位 entityType/sourceId。
                log.warn(
                        "INIT_IMPORT item failed, taskId={}, entityType={}, sourceId={}, error={}",
                        task.getTaskId(),
                        item.getEntityType(),
                        item.getSourceId(),
                        exception.getMessage(),
                        exception
                );
                item.setStatus(ITEM_STATUS_FAILED);
                item.setErrorMessage(normalizeErrorMessage(exception.getMessage()));
                failedCount++;
            }
        }

        SsoIdentityImportExecutionResult result = new SsoIdentityImportExecutionResult();
        result.setItemList(itemList);
        result.setTotalItemCount(itemList.size());
        result.setSuccessItemCount(successCount);
        result.setFailedItemCount(failedCount);
        result.setStatus(resolveTaskStatus(itemList.size(), successCount, failedCount));
        result.setResultSummary(buildResultSummary(itemList.size(), successCount, failedCount));
        log.info(
                "INIT_IMPORT executed, taskId={}, scopedItemCount={}, totalItemCount={}, successItemCount={}, failedItemCount={}, status={}",
                task.getTaskId(),
                scopedItems == null ? 0 : scopedItems.size(),
                itemList.size(),
                successCount,
                failedCount,
                result.getStatus()
        );
        return result;
    }

    /**
     * 构建快照索引，便于 scoped retry/compensation 按 sourceId 定位实体。
     *
     * @param snapshot 来源快照
     * @return 快照索引
     */
    private SnapshotIndex buildSnapshotIndex(SsoIdentityImportSnapshot snapshot) {
        SnapshotIndex snapshotIndex = new SnapshotIndex();
        snapshotIndex.orgMap = snapshot.getOrgList().stream()
                .filter(org -> org.getOrgId() != null)
                .collect(Collectors.toMap(SysOrg::getOrgId, org -> org, (left, right) -> right, LinkedHashMap::new));
        snapshotIndex.deptMap = snapshot.getDeptList().stream()
                .filter(dept -> dept.getDeptId() != null)
                .collect(Collectors.toMap(SysDept::getDeptId, dept -> dept, (left, right) -> right, LinkedHashMap::new));
        snapshotIndex.userMap = snapshot.getUserList().stream()
                .filter(user -> user.getUserId() != null)
                .collect(Collectors.toMap(SysUser::getUserId, user -> user, (left, right) -> right, LinkedHashMap::new));
        snapshotIndex.userOrgMap = snapshot.getUserOrgRelationList().stream()
                .collect(Collectors.toMap(this::buildUserOrgSourceId, relation -> relation, (left, right) -> right, LinkedHashMap::new));
        snapshotIndex.userDeptMap = snapshot.getUserDeptRelationList().stream()
                .collect(Collectors.toMap(this::buildUserDeptSourceId, relation -> relation, (left, right) -> right, LinkedHashMap::new));
        return snapshotIndex;
    }

    /**
     * 把完整快照展开为 task item 列表，顺序固定为主表优先、关系表靠后。
     *
     * @param snapshot 来源快照
     * @return task item 列表
     */
    private List<SsoSyncTaskItem> buildSnapshotItems(SsoIdentityImportSnapshot snapshot) {
        List<SsoSyncTaskItem> itemList = new ArrayList<>();
        snapshot.getOrgList().stream()
                .sorted(Comparator.comparing(SysOrg::getOrgId))
                .forEach(org -> itemList.add(buildItem(ENTITY_TYPE_ORG, String.valueOf(org.getOrgId()), serializeDetail(detailMap(
                        "orgId", org.getOrgId(),
                        "orgCode", org.getOrgCode(),
                        "orgName", org.getOrgName()
                )))));
        snapshot.getDeptList().stream()
                .sorted(Comparator.comparing(SysDept::getDeptId))
                .forEach(dept -> itemList.add(buildItem(ENTITY_TYPE_DEPT, String.valueOf(dept.getDeptId()), serializeDetail(detailMap(
                        "deptId", dept.getDeptId(),
                        "deptCode", dept.getDeptCode(),
                        "deptName", dept.getDeptName()
                )))));
        snapshot.getUserList().stream()
                .sorted(Comparator.comparing(SysUser::getUserId))
                .forEach(user -> itemList.add(buildItem(ENTITY_TYPE_USER, String.valueOf(user.getUserId()), serializeDetail(detailMap(
                        "userId", user.getUserId(),
                        "userName", user.getUserName(),
                        "nickName", user.getNickName()
                )))));
        snapshot.getUserOrgRelationList().forEach(relation -> itemList.add(buildItem(
                ENTITY_TYPE_USER_ORG,
                buildUserOrgSourceId(relation),
                serializeDetail(detailMap(
                        "userId", relation.getUserId(),
                        "orgId", relation.getOrgId(),
                        "isDefault", relation.getIsDefault(),
                        "enabled", relation.getEnabled()
                ))
        )));
        snapshot.getUserDeptRelationList().forEach(relation -> itemList.add(buildItem(
                ENTITY_TYPE_USER_DEPT,
                buildUserDeptSourceId(relation),
                serializeDetail(detailMap(
                        "userId", relation.getUserId(),
                        "deptId", relation.getDeptId(),
                        "isDefault", relation.getIsDefault(),
                        "enabled", relation.getEnabled()
                ))
        )));
        return itemList;
    }

    /**
     * 复制 scoped item，避免直接污染历史任务上的明细对象。
     *
     * @param scopedItems 原 scoped item 列表
     * @return 可重新执行的新明细列表
     */
    private List<SsoSyncTaskItem> cloneScopedItems(List<SsoSyncTaskItem> scopedItems) {
        List<SsoSyncTaskItem> clonedItems = new ArrayList<>(scopedItems.size());
        for (SsoSyncTaskItem scopedItem : scopedItems) {
            SsoSyncTaskItem clonedItem = new SsoSyncTaskItem();
            clonedItem.setEntityType(scopedItem.getEntityType());
            clonedItem.setSourceId(scopedItem.getSourceId());
            clonedItem.setDetailJson(scopedItem.getDetailJson());
            clonedItems.add(clonedItem);
        }
        return clonedItems;
    }

    /**
     * 执行单条明细。
     *
     * @param item 当前明细
     * @param snapshotIndex 快照索引
     * @param task 所属任务
     */
    private void processItem(SsoSyncTaskItem item, SnapshotIndex snapshotIndex, SsoSyncTask task) {
        switch (item.getEntityType()) {
            case ENTITY_TYPE_ORG -> upsertOrg(item, snapshotIndex.orgMap.get(parseLong(item.getSourceId())), task);
            case ENTITY_TYPE_DEPT -> upsertDept(item, snapshotIndex.deptMap.get(parseLong(item.getSourceId())), task);
            case ENTITY_TYPE_USER -> upsertUser(item, snapshotIndex.userMap.get(parseLong(item.getSourceId())), task);
            case ENTITY_TYPE_USER_ORG -> upsertUserOrgRelation(item, snapshotIndex.userOrgMap.get(item.getSourceId()), task);
            case ENTITY_TYPE_USER_DEPT -> upsertUserDeptRelation(item, snapshotIndex.userDeptMap.get(item.getSourceId()), task);
            default -> throw new CustomException("不支持的明细类型: " + item.getEntityType());
        }
    }

    /**
     * 组织 upsert。
     */
    private void upsertOrg(SsoSyncTaskItem item, SysOrg source, SsoSyncTask task) {
        if (source == null) {
            throw new CustomException("未找到来源组织: " + item.getSourceId());
        }
        source.setUpdateBy(resolveOperator(task));
        source.setUpdateAt(new Date());
        SysOrg existed = sysOrgMapper.selectSysOrgById(source.getOrgId());
        if (existed == null) {
            source.setCreateBy(resolveOperator(task));
            source.setCreateAt(new Date());
            sysOrgMapper.insertSysOrg(source);
        } else {
            sysOrgMapper.updateSysOrg(source);
        }
        item.setTargetId(String.valueOf(source.getOrgId()));
    }

    /**
     * 部门 upsert。
     */
    private void upsertDept(SsoSyncTaskItem item, SysDept source, SsoSyncTask task) {
        if (source == null) {
            throw new CustomException("未找到来源部门: " + item.getSourceId());
        }
        source.setUpdateBy(resolveOperator(task));
        SysDept existed = sysDeptMapper.selectSysDeptByDeptId(source.getDeptId());
        if (existed == null) {
            source.setCreateBy(resolveOperator(task));
            sysDeptMapper.insertDept(source);
        } else {
            sysDeptMapper.updateDept(source);
        }
        item.setTargetId(String.valueOf(source.getDeptId()));
    }

    /**
     * 用户 upsert。
     */
    private void upsertUser(SsoSyncTaskItem item, SysUser source, SsoSyncTask task) {
        if (source == null) {
            throw new CustomException("未找到来源用户: " + item.getSourceId());
        }
        source.setUpdateBy(resolveOperator(task));
        SysUser existed = sysUserMapper.selectSysUserByUserId(source.getUserId());
        if (existed == null) {
            source.setCreateBy(resolveOperator(task));
            sysUserMapper.insertUser(source);
        } else {
            sysUserMapper.updateUser(source);
        }
        item.setTargetId(String.valueOf(source.getUserId()));
    }

    /**
     * 用户组织关系 upsert。
     */
    private void upsertUserOrgRelation(SsoSyncTaskItem item, SysUserOrg source, SsoSyncTask task) {
        if (source == null) {
            throw new CustomException("未找到来源用户组织关系: " + item.getSourceId());
        }
        SysUserOrg existed = sysUserOrgMapper.selectOne(new QueryWrapper<SysUserOrg>()
                .eq("user_id", source.getUserId())
                .eq("org_id", source.getOrgId()));
        if (existed == null) {
            Long operatorUserId = resolveOperatorUserIdValue(task);
            Date operateAt = new Date();
            sysUserOrgMapper.insertInitImport(
                    source.getUserId(),
                    source.getOrgId(),
                    source.getIsDefault(),
                    resolveEnabledFlag(source.getEnabled()),
                    operatorUserId,
                    operateAt
            );
            SysUserOrg inserted = sysUserOrgMapper.selectOne(new QueryWrapper<SysUserOrg>()
                    .eq("user_id", source.getUserId())
                    .eq("org_id", source.getOrgId()));
            item.setTargetId(inserted == null ? item.getSourceId() : String.valueOf(inserted.getId()));
            return;
        }
        sysUserOrgMapper.updateInitImport(
                existed.getId(),
                source.getIsDefault(),
                resolveEnabledFlag(source.getEnabled()),
                resolveOperatorUserIdValue(task),
                new Date()
        );
        item.setTargetId(String.valueOf(existed.getId()));
    }

    /**
     * 用户部门关系 upsert。
     */
    private void upsertUserDeptRelation(SsoSyncTaskItem item, SysUserDept source, SsoSyncTask task) {
        if (source == null) {
            throw new CustomException("未找到来源用户部门关系: " + item.getSourceId());
        }
        SysUserDept existed = sysUserDeptMapper.selectOne(new QueryWrapper<SysUserDept>()
                .eq("user_id", source.getUserId())
                .eq("dept_id", source.getDeptId()));
        if (existed == null) {
            Long operatorUserId = resolveOperatorUserIdValue(task);
            Date operateAt = new Date();
            sysUserDeptMapper.insertInitImport(
                    source.getUserId(),
                    source.getDeptId(),
                    source.getIsDefault(),
                    resolveEnabledFlag(source.getEnabled()),
                    operatorUserId,
                    operateAt
            );
            SysUserDept inserted = sysUserDeptMapper.selectOne(new QueryWrapper<SysUserDept>()
                    .eq("user_id", source.getUserId())
                    .eq("dept_id", source.getDeptId()));
            item.setTargetId(inserted == null ? item.getSourceId() : String.valueOf(inserted.getId()));
            return;
        }
        sysUserDeptMapper.updateInitImport(
                existed.getId(),
                source.getIsDefault(),
                resolveEnabledFlag(source.getEnabled()),
                resolveOperatorUserIdValue(task),
                new Date()
        );
        item.setTargetId(String.valueOf(existed.getId()));
    }

    /**
     * 构造基础 item。
     */
    private SsoSyncTaskItem buildItem(String entityType, String sourceId, String detailJson) {
        SsoSyncTaskItem item = new SsoSyncTaskItem();
        item.setEntityType(entityType);
        item.setSourceId(sourceId);
        item.setDetailJson(detailJson);
        return item;
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
     * 解析长整型。
     */
    private Long parseLong(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new CustomException("无法解析 ID: " + value);
        }
    }

    /**
     * 解析操作人账号。
     */
    private String resolveOperator(SsoSyncTask task) {
        return task.getCreateBy() == null || task.getCreateBy().isBlank() ? "system" : task.getCreateBy();
    }

    /**
     * 当前导入流程没有强依赖用户 ID；这里只在关联表审计字段上做兼容兜底。
     */
    private String resolveOperatorUserId(SsoSyncTask task) {
        String createBy = task == null ? null : task.getCreateBy();

        if (createBy == null || createBy.isBlank()) {
            return DEFAULT_OPERATOR_USER_ID;
        }
        try {
            Long.parseLong(createBy);
            return createBy;
        } catch (NumberFormatException exception) {
            return DEFAULT_OPERATOR_USER_ID;
        }
    }

    /**
     * 解析关联表审计字段要用的操作人 ID。
     *
     * @param task 所属任务
     * @return 操作人 ID
     */
    private Long resolveOperatorUserIdValue(SsoSyncTask task) {
        return parseLong(resolveOperatorUserId(task));
    }

    /**
     * 归一化 enabled 标记，避免 legacy source 空值把 NOT NULL 列打爆。
     *
     * @param enabled legacy source 的 enabled
     * @return 可安全落库的 enabled
     */
    private Integer resolveEnabledFlag(Integer enabled) {
        return enabled == null ? 1 : enabled;
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
        return String.format("INIT_IMPORT 完成，总计 %d 条，成功 %d 条，失败 %d 条", totalCount, successCount, failedCount);
    }

    /**
     * 把错误信息裁剪到任务明细表字段上限，避免真正错误被明细落库再次放大。
     *
     * @param errorMessage 原始错误信息
     * @return 可安全落库的错误信息
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
     * 构造允许空值的 detail map，避免 Map.of 因 null 抛出异常。
     *
     * @param pairs 键值对
     * @return detail map
     */
    private Map<String, Object> detailMap(Object... pairs) {
        Map<String, Object> detailMap = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            detailMap.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return detailMap;
    }

    /**
     * 序列化 detail json。
     */
    private String serializeDetail(Map<String, Object> detailMap) {
        try {
            return objectMapper.writeValueAsString(detailMap);
        } catch (JsonProcessingException exception) {
            throw new CustomException("序列化任务明细失败");
        }
    }

    /**
     * 快照索引。
     */
    private static final class SnapshotIndex {
        private Map<Long, SysOrg> orgMap = new HashMap<>();
        private Map<Long, SysDept> deptMap = new HashMap<>();
        private Map<Long, SysUser> userMap = new HashMap<>();
        private Map<String, SysUserOrg> userOrgMap = new HashMap<>();
        private Map<String, SysUserDept> userDeptMap = new HashMap<>();
    }
}
