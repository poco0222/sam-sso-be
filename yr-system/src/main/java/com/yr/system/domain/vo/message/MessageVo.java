package com.yr.system.domain.vo.message;

import com.yr.system.domain.vo.PageVo;

/**
 * <p>
 * 消息查询请求实体
 * </p>
 *
 * @author carl 2022-01-20 9:37
 * @version V1.0
 */
public class MessageVo extends PageVo<MessageVo> {
    /**
     * 标题
     */
    private String title;

    /**
     * 名称
     */
    private String name;

    /**
     * 状态
     */
    private String status;

    /**
     * 发送状态
     */
    private String sendType;

    /**
     * 阅读状态
     */
    private String receiveStatus;

    /**
     * 接收人
     */
    private Long toUserId;

    /**
     * 是否删除全部 read
     */
    private boolean allDel;

    /**
     * 全部设置为已读
     */
    private boolean updateAllRead;

    private Long id;

    public boolean isAllDel() {
        return allDel;
    }

    public void setAllDel(boolean allDel) {
        this.allDel = allDel;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSendType() {
        return sendType;
    }

    public void setSendType(String sendType) {
        this.sendType = sendType;
    }

    public String getReceiveStatus() {
        return receiveStatus;
    }

    public void setReceiveStatus(String receiveStatus) {
        this.receiveStatus = receiveStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isUpdateAllRead() {
        return updateAllRead;
    }

    public void setUpdateAllRead(boolean updateAllRead) {
        this.updateAllRead = updateAllRead;
    }
}
