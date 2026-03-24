/**
 * @file 站内 websocket 消息服务实现
 * @author PopoY
 * @date 2026-03-11
 */
package com.yr.system.component.message.impl;

import com.yr.common.enums.ModeType;
import com.yr.common.exception.CustomException;
import com.yr.system.component.message.IMessageEntity;
import com.yr.system.component.message.IWebMessageService;
import com.yr.system.component.message.MessageJsonSerializer;
import com.yr.system.domain.entity.SysMsgTemplate;
import com.yr.system.domain.entity.SysReceiveGroup;
import com.yr.system.domain.entity.SysReceiveGroupObject;
import com.yr.system.service.ISysMsgTemplateService;
import com.yr.system.service.ISysReceiveGroupService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>
 * websocket消息
 * </p>
 *
 * @author carl 2022-01-14 10:36
 * @version V1.0
 */
@Component
public class WebMessageServiceImpl implements IWebMessageService {

    /** 站内消息服务日志。 */
    private static final Logger logger = LoggerFactory.getLogger(WebMessageServiceImpl.class);

    /** 仅匹配模板占位符 `${...}`。 */
    private static final Pattern TEMPLATE_VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]*)\\}");

    private final ISysMsgTemplateService templateService;

    private final ISysReceiveGroupService receiveGroupService;

    /** 异步消息分发服务。 */
    private final AsyncWebMessageDispatchService asyncDispatchService;

    /** 消息链路统一 JSON serializer（JSON 序列化器）。 */
    private final MessageJsonSerializer messageJsonSerializer;

    /**
     * 构造站内消息服务实现。
     *
     * @param templateService 模板服务
     * @param receiveGroupService 接收组服务
     * @param asyncDispatchService 异步消息分发服务
     * @param messageJsonSerializer 消息链路统一 JSON serializer
     */
    public WebMessageServiceImpl(ISysMsgTemplateService templateService,
                                 ISysReceiveGroupService receiveGroupService,
                                 AsyncWebMessageDispatchService asyncDispatchService,
                                 MessageJsonSerializer messageJsonSerializer) {
        this.templateService = templateService;
        this.receiveGroupService = receiveGroupService;
        this.asyncDispatchService = asyncDispatchService;
        this.messageJsonSerializer = messageJsonSerializer;
    }

    @Override
    public void sendMessage(String templateCode, String receiverGroupCode, Map<String, String> args) {
        //模板
        SysMsgTemplate msgTemplate = getSysMsgTemplate(templateCode);
        //接收人模组
        SysReceiveGroup sysReceiveGroup = getSysReceiveGroup(receiverGroupCode);
        //如果模板有参数就替换 验证参数 解析
        resolveTemplate(args, msgTemplate);

        List<Long> userList = null;

        //转换用户组
        if (CollectionUtils.isNotEmpty(sysReceiveGroup.getGroupObjectList())) {
            userList = sysReceiveGroup.getGroupObjectList().stream().map(SysReceiveGroupObject::getReObjectId).collect(Collectors.toList());
        }
        //执行
        extracted(msgTemplate, sysReceiveGroup.getReCode(), userList, sysReceiveGroup.getReMode());
    }

    private SysReceiveGroup getSysReceiveGroup(String receiverGroupCode) {
        SysReceiveGroup sysReceiveGroup = Optional
                .ofNullable(receiveGroupService.getReceiveGroupList(receiverGroupCode))
                .orElseThrow(() -> new CustomException("模组没有用户，请先添加模组或者用户[+" + receiverGroupCode + "+]"));
        return sysReceiveGroup;
    }

    /**
     * 执行方法
     *
     * @param msgTemplate
     * @param reModeCode  备用字段如果以后有其他的模组使用
     * @param modeType
     * @param userList
     */
    private void extracted(IMessageEntity msgTemplate, String reModeCode, List<Long> userList, String modeType) {
        //判断是什么用户组
        switch (ModeType.valueOf(modeType)) {
            case USER_GROUP -> {
                // 当前仅支持用户组消息，保留原有发送与日志副作用。
                sendUserMessage(msgTemplate, userList);
                logger.info("消息发送完毕");
            }
            default -> {
            }
        }
    }

    /**
     * 发送用户组消息
     *
     * @param msgTemplate
     * @param userList
     */
    private void sendUserMessage(IMessageEntity msgTemplate, List<Long> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            throw new CustomException("模组没有用户，请先添加模组或者用户");
        }
        logger.info("消息内容:{}", messageJsonSerializer.toJsonQuietly(msgTemplate));
        // 默认 admin 用户发送，并交给独立异步分发 Bean 执行。
        asyncDispatchService.dispatch(userList, msgTemplate, "admin");
    }

    /**
     * 解析替换模板参数
     *
     * @param args
     * @param msgTemplate
     */
    private void resolveTemplate(Map<String, String> args, SysMsgTemplate msgTemplate) {
        if (args == null || args.isEmpty() || msgTemplate == null) {
            return;
        }
        // 只解析真正展示给用户的文本字段，避免误伤编码、参数定义和枚举属性。
        msgTemplate.setMsgName(resolveTemplateField("msgName", msgTemplate.getMsgName(), args));
        msgTemplate.setTitle(resolveTemplateField("title", msgTemplate.getTitle(), args));
        msgTemplate.setMsgContent(resolveTemplateField("msgContent", msgTemplate.getMsgContent(), args));
    }

    /**
     * 解析单个模板字段。
     *
     * <p>字段值为空时保持为空；占位符缺失时保留原始占位符；
     * 真正的解析异常升级为业务异常，避免静默吞错。</p>
     *
     * @param fieldName 字段名
     * @param value     原始字段值
     * @param args      模板参数
     * @return 解析后的字段值
     */
    private String resolveTemplateField(String fieldName, String value, Map<String, String> args) {
        if (value == null) {
            return null;
        }
        try {
            Matcher matcher = TEMPLATE_VARIABLE_PATTERN.matcher(value);
            StringBuilder resolved = new StringBuilder();
            while (matcher.find()) {
                String placeholder = matcher.group();
                String replacement = args.get(placeholder);
                if (replacement == null) {
                    replacement = placeholder;
                }
                matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(resolved);
            return resolved.toString();
        } catch (RuntimeException ex) {
            logger.error("解析消息模板字段失败 field={}", fieldName, ex);
            throw new CustomException("解析消息模板字段失败[" + fieldName + "]");
        }
    }

    /**
     * 获取消息模板
     *
     * @param templateCode
     * @return
     */
    private SysMsgTemplate getSysMsgTemplate(String templateCode) {
        SysMsgTemplate msgTemplate = Optional.ofNullable(templateService.get(templateCode))
                .orElseThrow(() -> new CustomException("模板编码无效[+" + templateCode + "+]"));
        return msgTemplate;
    }

    @Override
    public void sendMessage(String templateCode, List<Long> users, Map<String, String> args) {
        //模板
        SysMsgTemplate msgTemplate = getSysMsgTemplate(templateCode);
        //接收人模组
        if (CollectionUtils.isEmpty(users)) {
            throw new CustomException("接收人用户集合为");
        }
        //解析替换模板
        resolveTemplate(args, msgTemplate);
        //执行
        extracted(msgTemplate, null, users, ModeType.USER_GROUP.name());
    }

    @Override
    public void sendMessage(IMessageEntity message, List<Long> users) {
        //消息体
        if (message == null) {
            throw new CustomException("消息内容不能为空");
        }
        //接收人
        if (CollectionUtils.isEmpty(users)) {
            throw new CustomException("接收人用户集合为");
        }
        //执行
        extracted(message, null, users, ModeType.USER_GROUP.name());
    }
}
