package com.yr.common.service;

import com.alibaba.fastjson.JSON;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import java.util.Date;

/**
 * MQ消息发送服务
 */
@Service
@ConditionalOnProperty(name = "rocketmq.enabled", havingValue = "true", matchIfMissing = true)
public class MqProducerService {
    private static final Logger log = LoggerFactory.getLogger(MqProducerService.class);

    private static final int DEFAULT_MAX_RETRY = 3;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private MqMessageLogMapper mqMessageLogMapper;

    /**
     * 发送同步消息并记录履历
     *
     * @param topic      主题
     * @param tag        标签
     * @param actionType 操作类型
     * @param msgKey     业务唯一键
     * @param data       业务数据对象
     */
    public boolean send(String topic, String tag, MqActionType actionType, String msgKey, Object data) {
        String dataJson = data instanceof String ? (String) data : JSON.toJSONString(data);
        MqSyncMessage syncMsg = new MqSyncMessage(msgKey, actionType.getCode(), dataJson);
        String body = JSON.toJSONString(syncMsg);

        // 记录履历
        MqMessageLog logEntity = buildLog(topic, tag, actionType, msgKey, body);
        mqMessageLogMapper.insert(logEntity);

        // 发送消息
        return doSend(logEntity, topic, tag, msgKey, body);
    }

    /**
     * 重发消息（由重试任务调用）
     */
    public boolean resend(MqMessageLog logEntity) {
        mqMessageLogMapper.incrementRetryCount(logEntity.getId());
        return doSend(logEntity, logEntity.getTopic(), logEntity.getTag(),
                logEntity.getMsgKey(), logEntity.getBody());
    }

    private boolean doSend(MqMessageLog logEntity, String topic, String tag, String msgKey, String body) {
        String destination = topic + ":" + tag;
        try {
            SendResult result = rocketMQTemplate.syncSend(destination,
                    MessageBuilder.withPayload(body).setHeader("KEYS", msgKey).build());

            if (result.getSendStatus() == SendStatus.SEND_OK) {
                mqMessageLogMapper.updateSendStatus(logEntity.getId(),
                        MqSendStatus.SUCCESS.getCode(), result.getMsgId(), null);
                return true;
            }
            mqMessageLogMapper.updateSendStatus(logEntity.getId(),
                    MqSendStatus.FAILED.getCode(), result.getMsgId(), result.getSendStatus().name());
            return false;
        } catch (Exception e) {
            log.error("MQ消息发送失败, topic={}, msgKey={}", topic, msgKey, e);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500);
            }
            mqMessageLogMapper.updateSendStatus(logEntity.getId(),
                    MqSendStatus.FAILED.getCode(), null, errorMsg);
            return false;
        }
    }

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
}
