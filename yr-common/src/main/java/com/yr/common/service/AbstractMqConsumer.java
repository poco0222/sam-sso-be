package com.yr.common.service;

import com.alibaba.fastjson.JSON;
import com.yr.common.core.domain.MqSyncMessage;
import com.yr.common.enums.MqConsumeStatus;
import com.yr.common.mapper.MqMessageLogMapper;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * MQ消费者基类，子类实现 handleMessage 处理业务逻辑
 */
public abstract class AbstractMqConsumer implements RocketMQListener<String> {
    private static final Logger log = LoggerFactory.getLogger(AbstractMqConsumer.class);

    @Autowired
    private MqMessageLogMapper mqMessageLogMapper;

    @Override
    public void onMessage(String message) {
        MqSyncMessage syncMsg = null;
        try {
            syncMsg = JSON.parseObject(message, MqSyncMessage.class);
            handleMessage(syncMsg);
            // 更新消费状态为成功
            mqMessageLogMapper.updateConsumeStatus(
                    syncMsg.getMsgKey(), MqConsumeStatus.SUCCESS.getCode(), null);
        } catch (Exception e) {
            log.error("MQ消息消费失败, message={}", message, e);
            if (syncMsg != null) {
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.length() > 500) {
                    errorMsg = errorMsg.substring(0, 500);
                }
                mqMessageLogMapper.updateConsumeStatus(
                        syncMsg.getMsgKey(), MqConsumeStatus.FAILED.getCode(), errorMsg);
            }
            throw new RuntimeException("消费失败，触发重试", e);
        }
    }

    /**
     * 子类实现具体业务处理逻辑
     */
    protected abstract void handleMessage(MqSyncMessage message);
}
