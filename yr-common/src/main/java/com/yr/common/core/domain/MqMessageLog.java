package com.yr.common.core.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;

/**
 * MQ消息履历实体
 */
@TableName("mq_message_log")
public class MqMessageLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** RocketMQ消息ID */
    private String msgId;

    /** 业务唯一键(表名+主键) */
    private String msgKey;

    /** 消息主题 */
    private String topic;

    /** 消息标签 */
    private String tag;

    /** 操作类型: I/U/D */
    private String actionType;

    /** 消息体JSON */
    private String body;

    /** 发送状态: 0-待发送 1-成功 2-失败 */
    private Integer sendStatus;

    /** 消费状态: 0-未消费 1-成功 2-失败 */
    private Integer consumeStatus;

    /** 重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetry;

    /** 下次重试时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextRetryTime;

    /** 错误信息 */
    private String errorMsg;

    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMsgId() { return msgId; }
    public void setMsgId(String msgId) { this.msgId = msgId; }
    public String getMsgKey() { return msgKey; }
    public void setMsgKey(String msgKey) { this.msgKey = msgKey; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Integer getSendStatus() { return sendStatus; }
    public void setSendStatus(Integer sendStatus) { this.sendStatus = sendStatus; }
    public Integer getConsumeStatus() { return consumeStatus; }
    public void setConsumeStatus(Integer consumeStatus) { this.consumeStatus = consumeStatus; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public Integer getMaxRetry() { return maxRetry; }
    public void setMaxRetry(Integer maxRetry) { this.maxRetry = maxRetry; }
    public Date getNextRetryTime() { return nextRetryTime; }
    public void setNextRetryTime(Date nextRetryTime) { this.nextRetryTime = nextRetryTime; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
