package com.yr.quartz.task;

import com.yr.common.core.domain.MqMessageLog;
import com.yr.common.mapper.MqMessageLogMapper;
import com.yr.common.service.MqProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * MQ消息重发定时任务
 * 在系统管理-定时任务中配置: mqRetryTask.execute()
 */
@Component("mqRetryTask")
@ConditionalOnProperty(name = "rocketmq.enabled", havingValue = "true", matchIfMissing = true)
public class MqRetryTask {
    private static final Logger log = LoggerFactory.getLogger(MqRetryTask.class);

    @Autowired
    private MqMessageLogMapper mqMessageLogMapper;

    @Autowired
    private MqProducerService mqProducerService;

    public void execute() {
        List<MqMessageLog> retryList = mqMessageLogMapper.selectRetryList();
        if (retryList.isEmpty()) {
            return;
        }
        log.info("MQ消息重发任务开始，待重发消息数: {}", retryList.size());
        for (MqMessageLog msg : retryList) {
            try {
                mqProducerService.resend(msg);
            } catch (Exception e) {
                log.error("MQ消息重发失败, id={}, msgKey={}", msg.getId(), msg.getMsgKey(), e);
            }
        }
    }
}
