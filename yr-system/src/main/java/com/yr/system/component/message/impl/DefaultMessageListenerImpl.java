/**
 * @file 默认消息监听器实现，负责落库存储消息主体与接收关系
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.component.message.impl;

import com.yr.common.enums.MessageType;
import com.yr.common.utils.StringUtils;
import com.yr.system.component.message.AbstractMessageListener;
import com.yr.system.component.message.IMessageEntity;
import com.yr.system.component.message.MessageJsonSerializer;
import com.yr.system.domain.entity.SysMessageBody;
import com.yr.system.domain.entity.SysMessageBodyReceiver;
import com.yr.system.service.ISysMessageBodyReceiverService;
import com.yr.system.service.ISysMessageBodyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;

/**
 * <p>
 * 实现消息接收默认处理
 * </p>
 *
 * @author carl 2022-01-17 13:41
 * @version V1.0
 */
public class DefaultMessageListenerImpl extends AbstractMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(DefaultMessageListenerImpl.class);
    /**
     * 消息主体服务
     */
    private final ISysMessageBodyService messageBodyService;
    /**
     * 消息接收人服务
     */
    private final ISysMessageBodyReceiverService messageBodyReceiverService;

    /**
     * 通过构造器显式注入监听器依赖，避免 websocket bean 出现隐式字段注入。
     *
     * @param messageBodyService 消息主体服务
     * @param messageBodyReceiverService 消息接收人服务
     * @param messageJsonSerializer 统一 JSON 序列化器
     */
    public DefaultMessageListenerImpl(ISysMessageBodyService messageBodyService,
                                      ISysMessageBodyReceiverService messageBodyReceiverService,
                                      MessageJsonSerializer messageJsonSerializer) {
        super(messageJsonSerializer);
        this.messageBodyService = messageBodyService;
        this.messageBodyReceiverService = messageBodyReceiverService;
    }

    @Override
    public void close(Session session, Long userId, int currentUserSize) {
        logger.info("消息关闭 用户={}", userId);
    }

    @Override
    public void message(Session session, Long userId, String message) {
        logger.info("收到消息{}", message);
    }

    @Override
    public void error(Throwable error) {
        logger.error("接收消息错误{}", error.getMessage());
    }

    @Override
    public void sendAfter(String messageId, String fromUserId, Long toUserId, String status) {
        if (hasValidUserId(toUserId)
                && hasTextValue(messageId)
                && hasTextValue(fromUserId)) {
            SysMessageBody sysMessageBody = messageBodyService.getById(messageId);
            if (sysMessageBody != null) {
                SysMessageBodyReceiver sysMessageBodyReceiver = new SysMessageBodyReceiver();
                sysMessageBodyReceiver.setMsgId(sysMessageBody.getId());
                sysMessageBodyReceiver.setMsgTo(toUserId);
                sysMessageBodyReceiver.setSendStatus(status);
                messageBodyReceiverService.saveOrUpdate(sysMessageBodyReceiver);
            }
        }
    }

    @Override
    public String sendBefore(IMessageEntity entity, String formUserId) {
        String id = "";
        if (entity != null) {
            SysMessageBody sysMessageBody = new SysMessageBody();
            sysMessageBody.setMsgBody(entity.getBody());
            sysMessageBody.setMsgName(entity.getName());
            sysMessageBody.setMsgTitle(entity.getTitle());
            sysMessageBody.setSendType(MessageType.WEB_MESSAGE.name());
            sysMessageBody.setMsgFrom(formUserId);
            messageBodyService.saveOrUpdate(sysMessageBody);
            id = sysMessageBody.getId();
        }
        return id;
    }

    /**
     * 判断用户 ID 是否为合法非空值。
     *
     * @param userId 用户 ID
     * @return true 表示可继续处理
     */
    private boolean hasValidUserId(Long userId) {
        return userId != null && userId >= 0;
    }

    /**
     * 判断字符串参数是否具备真实文本值。
     *
     * @param value 字符串值
     * @return true 表示非空白且不是字面量 "null"
     */
    private boolean hasTextValue(String value) {
        return StringUtils.isNotBlank(value) && !"null".equalsIgnoreCase(value.trim());
    }

}
