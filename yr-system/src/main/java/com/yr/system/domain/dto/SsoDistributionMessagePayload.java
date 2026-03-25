/**
 * @file DISTRIBUTION 消息载荷 DTO
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.domain.dto;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 承载 full-batch snapshot upsert 的单条分发消息契约。
 */
@Data
public class SsoDistributionMessagePayload {

    /** full-batch snapshot 分发模式常量。 */
    public static final String DELIVERY_MODE_FULL_BATCH_SNAPSHOT = "FULL_BATCH_SNAPSHOT";

    /** 当前一期固定的主数据来源。 */
    public static final String SOURCE_SYSTEM_LOCAL_SAM_EMPTY = "local_sam_empty";

    /** 所属任务 ID。 */
    private Long taskId;

    /** 所属业务批次号。 */
    private String batchNo;

    /** 目标客户端编码。 */
    private String targetClientCode;

    /** 分发模式。 */
    private String deliveryMode;

    /** MQ 动作类型。 */
    private String mqActionType;

    /** 来源系统标识。 */
    private String sourceSystem;

    /** 快照时间毫秒值。 */
    private Long snapshotAt;

    /** 实体类型。 */
    private String entityType;

    /** 来源主键。 */
    private String sourceId;

    /** 对应 MQ 消息键。 */
    private String msgKey;

    /** 单条实体载荷。 */
    private Map<String, Object> payload = new LinkedHashMap<>();
}
