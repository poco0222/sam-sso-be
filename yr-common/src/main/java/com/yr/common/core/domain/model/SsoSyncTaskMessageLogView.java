/**
 * @file 同步任务条目 MQ 履历视图
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.common.core.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 供 sync-task console 直接展示的 MQ 履历视图。
 */
@Data
public class SsoSyncTaskMessageLogView {

    /** RocketMQ 消息 ID。 */
    private String msgId;

    /** 业务消息键。 */
    private String msgKey;

    /** 消息主题。 */
    private String topic;

    /** 消息标签。 */
    private String tag;

    /** 业务操作类型。 */
    private String actionType;

    /** 发送状态。 */
    private Integer sendStatus;

    /** 消费状态。 */
    private Integer consumeStatus;

    /** 已重试次数。 */
    private Integer retryCount;

    /** 最大重试次数。 */
    private Integer maxRetry;

    /** 下一次重试时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextRetryTime;

    /** 失败错误信息。 */
    private String errorMsg;

    /** 履历里的消息体。 */
    private String body;

    /** 履历创建时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 履历更新时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
