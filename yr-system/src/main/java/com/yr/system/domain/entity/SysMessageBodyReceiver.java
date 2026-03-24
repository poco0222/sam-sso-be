/**
 * @file 站内消息接收人实体，保存消息发送结果与阅读状态
 * @author Codex
 * @date 2026-03-16
 */
package com.yr.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yr.common.mybatisplus.entity.PkModelEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * <p>
 *
 * </p>
 *
 * @author carl
 * @since 2022-01-18
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_message_body_receiver")
public class SysMessageBodyReceiver extends PkModelEntity {
    public static final String MSG_ID = "msg_id";
    public static final String SEND_STATUS = "send_status";
    public static final String RECEIVE_STATUS = "receive_status";
    public static final String MSG_TO = "msg_to";
    /**
     * 消息主题ID
     */
    private String msgId;
    /**
     * 发送状态 0 成功 1未成功
     */
    private String sendStatus;
    /**
     * 阅读状态 0 未读  1已读
     */
    private String receiveStatus;
    /**
     * 接收人用户ID
     */
    private Long msgTo;

    //接收时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date receiveTime;
    /**
     * 关联查询实体
     */
    @TableField(exist = false)
    private SysMessageBody sysMessageBody;
    /**
     * 标题
     */
    @TableField(exist = false)
    private String title;
    /**
     * 名称
     */
    @TableField(exist = false)
    private String name;
    /**
     * 发送人
     */
    @TableField(exist = false)
    private String formUser;
    /**
     * 账号
     */
    @TableField(exist = false)
    private String userName;
    /**
     * 账号名称
     */
    @TableField(exist = false)
    private String nickName;
    /**
     * 消息主题
     */
    @TableField(exist = false)
    private String msgBody;
}
