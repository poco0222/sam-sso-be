/**
 * @file 身份中心同步任务实体
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.common.core.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.yr.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 身份中心同步任务实体，承载一期 INIT_IMPORT / DISTRIBUTION / COMPENSATION 任务骨架。
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SsoSyncTask extends BaseEntity {

    /** 初始化导入任务。 */
    public static final String TASK_TYPE_INIT_IMPORT = "INIT_IMPORT";

    /** 分发任务。 */
    public static final String TASK_TYPE_DISTRIBUTION = "DISTRIBUTION";

    /** 补偿任务。 */
    public static final String TASK_TYPE_COMPENSATION = "COMPENSATION";

    /** 待执行状态。 */
    public static final String STATUS_PENDING = "PENDING";

    /** 执行中状态。 */
    public static final String STATUS_RUNNING = "RUNNING";

    /** 执行成功状态。 */
    public static final String STATUS_SUCCESS = "SUCCESS";

    /** 执行失败状态。 */
    public static final String STATUS_FAILED = "FAILED";

    /** 部分成功状态。 */
    public static final String STATUS_PARTIAL_SUCCESS = "PARTIAL_SUCCESS";

    /** 继承来源系统 ID 的策略标识。 */
    public static final String ID_STRATEGY_INHERIT_SOURCE_ID = "INHERIT_SOURCE_ID";

    /** 主权已转移到身份中心的标识。 */
    public static final String OWNERSHIP_TRANSFERRED = "TRANSFERRED";

    /** 任务主键。 */
    private Long taskId;

    /** 任务类型。 */
    private String taskType;

    /** 触发方式。 */
    private String triggerType;

    /** 目标客户端编码。 */
    private String targetClientCode;

    /** 批次号。 */
    private String batchNo;

    /** 任务状态。 */
    private String status;

    /** 任务载荷 JSON。 */
    private String payloadJson;

    /** 结果摘要。 */
    private String resultSummary;

    /** 重试次数。 */
    private Integer retryCount;

    /** 实际执行时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date executeAt;

    /** ID 继承策略。 */
    private String idStrategy;

    /** 主权转移状态。 */
    private String ownershipTransferStatus;

    /** 来源批次号。 */
    private String sourceBatchNo;

    /** 导入快照时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date importSnapshotAt;
}
