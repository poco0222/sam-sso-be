package com.yr.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yr.common.enums.MessageType;
import com.yr.common.mybatisplus.entity.PkModelEntity;
import com.yr.system.component.message.IMessageEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Map;

/**
 * <p>
 *
 * </p>
 *
 * @author carl
 * @since 2022-01-04
 */
@TableName("sys_msg_template")
public class SysMsgTemplate extends PkModelEntity implements IMessageEntity {
    public static final String MSG_CODE = "msg_code";
    public static final String MSG_NAME = "msg_name";
    public static final String MSG_CONTENT = "msg_content";
    public static final String MSG_PARAMS = "msg_params";
    /**
     * 消息模板编码
     */
    @NotBlank(message = "消息编码不能为空")
    @Size(min = 0, max = 50, message = "参数名称不能超过50个字符")
    private String msgCode;
    /**
     * 消息模板名称
     */
    @NotBlank(message = "消息名称不能为空")
    @Size(min = 0, max = 50, message = "参数名称不能超过50个字符")
    private String msgName;
    /**
     * 消息模板内容
     */
    @NotBlank(message = "消息内容不能为空")
    private String msgContent;
    @NotBlank(message = "消息标题不能为空")
    private String title;
    /***
     * 消息模板参数
     */
//    @NotBlank(message = "消息转义参数不能为空")
    private String msgParams;
    /**
     * 消息类型
     */
    @TableField(exist = false)
    private MessageType messageType;

    public SysMsgTemplate() {
    }

    @JsonIgnore
    @Override
    public Map<String, Object> getParams() {
        return super.getParams();
    }

    @Override
    public MessageType getMessageType() {
        if (null == this.messageType) {
            this.messageType = MessageType.WEB_MESSAGE;
        }
        return this.messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    @Override
    public String getBody() {
        return this.msgContent;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getName() {
        return this.msgName;
    }

    public String getMsgCode() {
        return msgCode;
    }

    public void setMsgCode(String msgCode) {
        this.msgCode = msgCode;
    }

    public String getMsgName() {
        return msgName;
    }

    public void setMsgName(String msgName) {
        this.msgName = msgName;
    }

    public String getMsgContent() {
        return msgContent;
    }

    public void setMsgContent(String msgContent) {
        this.msgContent = msgContent;
    }

    public String getMsgParams() {
        return msgParams;
    }

    public void setMsgParams(String msgParams) {
        this.msgParams = msgParams;
    }
}
