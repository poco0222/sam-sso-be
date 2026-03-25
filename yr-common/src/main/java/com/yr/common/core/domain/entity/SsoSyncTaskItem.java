/**
 * @file 身份中心同步任务明细实体
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yr.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 身份中心同步任务明细实体。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sso_sync_task_item")
public class SsoSyncTaskItem extends BaseEntity {

    /** 明细主键。 */
    @TableId(value = "item_id", type = IdType.AUTO)
    private Long itemId;

    /** 所属任务ID。 */
    private Long taskId;

    /** 同步实体类型。 */
    private String entityType;

    /** 来源系统ID。 */
    private String sourceId;

    /** 目标系统ID。 */
    private String targetId;

    /** 明细状态。 */
    private String status;

    /** 明细载荷 JSON。 */
    private String detailJson;

    /** 错误信息。 */
    private String errorMessage;
}
