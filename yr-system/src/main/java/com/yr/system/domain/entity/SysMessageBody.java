/**
 * @file 站内消息主体实体，收口消息时间类型与基础字段契约
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

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
@TableName("sys_message_body")
public class SysMessageBody implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private String id;
    /**
     * 消息标题
     * not null
     */
    private String msgTitle;
    /**
     * 消息名称
     * not null
     */
    private String msgName;
    /**
     * 消息主体
     * not null
     */
    private String msgBody;
    /**
     * 消息类型,ordinary普通通知 documents单据通知等
     * not null
     * default 'ordinary'
     */
    private String sendType;
    /**
     * 创建时间
     * not null
     * default now()
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createAt;
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateAt;
    /**
     * 消息状态,normal正常 deprecated废弃
     * not null
     * default 'normal'
     */
    private String status;
    /**
     * 创建者
     * not null
     */
    private String createBy;
    /**
     * 更新者
     */
    private String updateBy;
    /**
     * 备注
     */
    private String remark;
    /**
     * 行版本号，用来处理锁
     */
    private Integer objectVersionNumber;
    /**
     * 发送人
     * not null
     */
    private String msgFrom;
    /**
     * 超链接
     */
    private String url;
    /**
     * 跳转单号
     */
    private String urlParam;
}
