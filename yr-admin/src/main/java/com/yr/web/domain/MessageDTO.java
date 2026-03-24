package com.yr.web.domain;

import lombok.Data;

import java.io.Serializable;

@Data
public class MessageDTO implements Serializable {
    private static final long serialVersionUID = -5690444483968058442L;

    /**
     * 谁发的
     */
    protected String fromUser;

    /**
     * 谁接的
     */
    protected String toUser;

    /**
     * &#064;所有人
     */
    protected Boolean toAll;

    /**
     * 消息主题
     */
    protected String title;

    /**
     * 消息内容
     */
    protected String content;

    /**
     * 消息类型标识，由具体消息处理方自行解析。
     */
    protected String type;

    public MessageDTO(String fromUser, String toUser, String title, String content) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.title = title;
        this.content = content;
    }

    public MessageDTO(String fromUser, String toUser, String title, String content, String category) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.title = title;
        this.content = content;
    }
}
