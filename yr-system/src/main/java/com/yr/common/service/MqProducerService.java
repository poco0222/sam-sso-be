/**
 * @file MQ 消息发送服务，迁移到 system 模块承载 RocketMQ 基础设施
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.common.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yr.common.core.domain.MqMessageLog;
import com.yr.common.core.domain.MqSyncMessage;
import com.yr.common.enums.MqActionType;
import com.yr.common.enums.MqConsumeStatus;
import com.yr.common.enums.MqSendStatus;
import com.yr.common.mapper.MqMessageLogMapper;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * MQ（消息队列）消息发送服务。
 */
@Service
@ConditionalOnProperty(name = "rocketmq.enabled", havingValue = "true", matchIfMissing = true)
public class MqProducerService {
    /** 日志记录器。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(MqProducerService.class);

    /** 默认最大重试次数。 */
    private static final int DEFAULT_MAX_RETRY = 3;

    /** JSON 序列化器。 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** RocketMQ 模板。 */
    private final RocketMQTemplate rocketMQTemplate;

    /** MQ 消息履历 Mapper（映射器）。 */
    private final MqMessageLogMapper mqMessageLogMapper;

    /**
     * 构造 MQ 消息发送服务。
     *
     * @param rocketMQTemplate RocketMQ 模板
     * @param mqMessageLogMapper MQ 消息履历 Mapper（映射器）
     */
    public MqProducerService(RocketMQTemplate rocketMQTemplate, MqMessageLogMapper mqMessageLogMapper) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.mqMessageLogMapper = mqMessageLogMapper;
    }

    /**
     * 发送同步消息并记录履历。
     *
     * @param topic 主题
     * @param tag 标签
     * @param actionType 操作类型
     * @param msgKey 业务唯一键
     * @param data 业务数据对象
     * @return 是否发送成功
     */
    public boolean send(String topic, String tag, MqActionType actionType, String msgKey, Object data) {
        String dataJson = data instanceof String ? (String) data : toJson(data);
        MqSyncMessage syncMessage = new MqSyncMessage(msgKey, actionType.getCode(), dataJson);
        String body = toJson(syncMessage);

        MqMessageLog logEntity = buildLog(topic, tag, actionType, msgKey, body);
        mqMessageLogMapper.insert(logEntity);
        return doSend(logEntity, topic, tag, msgKey, body);
    }

    /**
     * 重发消息。
     *
     * @param logEntity 已存在的消息履历
     * @return 是否发送成功
     */
    public boolean resend(MqMessageLog logEntity) {
        mqMessageLogMapper.incrementRetryCount(logEntity.getId());
        return doSend(logEntity, logEntity.getTopic(), logEntity.getTag(), logEntity.getMsgKey(), logEntity.getBody());
    }

    /**
     * 执行底层 MQ 发送。
     *
     * @param logEntity 消息履历
     * @param topic 主题
     * @param tag 标签
     * @param msgKey 业务唯一键
     * @param body 消息体
     * @return 是否发送成功
     */
    private boolean doSend(MqMessageLog logEntity, String topic, String tag, String msgKey, String body) {
        String destination = topic + ":" + tag;
        try {
            SendResult result = rocketMQTemplate.syncSend(
                    destination,
                    MessageBuilder.withPayload(body).setHeader("KEYS", msgKey).build());

            if (result.getSendStatus() == SendStatus.SEND_OK) {
                mqMessageLogMapper.updateSendStatus(logEntity.getId(), MqSendStatus.SUCCESS.getCode(), result.getMsgId(), null);
                return true;
            }

            mqMessageLogMapper.updateSendStatus(
                    logEntity.getId(),
                    MqSendStatus.FAILED.getCode(),
                    result.getMsgId(),
                    result.getSendStatus().name());
            return false;
        } catch (Exception ex) {
            LOGGER.error("MQ 消息发送失败, topic={}, msgKey={}", topic, msgKey, ex);
            String errorMessage = ex.getMessage();
            if (errorMessage != null && errorMessage.length() > 500) {
                errorMessage = errorMessage.substring(0, 500);
            }
            mqMessageLogMapper.updateSendStatus(logEntity.getId(), MqSendStatus.FAILED.getCode(), null, errorMessage);
            return false;
        }
    }

    /**
     * 构建消息履历对象。
     *
     * @param topic 主题
     * @param tag 标签
     * @param actionType 操作类型
     * @param msgKey 业务唯一键
     * @param body 消息体
     * @return 消息履历
     */
    private MqMessageLog buildLog(String topic, String tag, MqActionType actionType, String msgKey, String body) {
        MqMessageLog log = new MqMessageLog();
        log.setTopic(topic);
        log.setTag(tag);
        log.setActionType(actionType.getCode());
        log.setMsgKey(msgKey);
        log.setBody(body);
        log.setSendStatus(MqSendStatus.PENDING.getCode());
        log.setConsumeStatus(MqConsumeStatus.PENDING.getCode());
        log.setRetryCount(0);
        log.setMaxRetry(DEFAULT_MAX_RETRY);
        log.setCreateTime(new Date());
        return log;
    }

    /**
     * 把对象序列化为 JSON（JavaScript 对象表示法）字符串。
     *
     * @param payload 待序列化对象
     * @return JSON 字符串
     */
    private String toJson(Object payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("MQ 消息序列化失败", ex);
        }
    }
}
