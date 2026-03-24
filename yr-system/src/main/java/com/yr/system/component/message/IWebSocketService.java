/**
 * @file 站内 WebSocket 服务接口
 * @author Codex
 * @date 2026-03-16
 */
package com.yr.system.component.message;


import javax.websocket.Session;
import java.io.IOException;
import java.util.List;

/**
 * <p>
 * 站内websocket消息
 * </p>
 *
 * @author carl 2022-01-13 16:09
 * @version V1.0
 */
public interface IWebSocketService {


    //    /**
    //     * 发送异步消息
    //     * * @param receive
    //     * @param result
    //     * @throws IOException
    //     */
    //    void send(Long receive, Object result) throws IOException;

    /**
     * 群发消息
     *
     * @param receive
     * @param result
     * @param fromUserId
     * @throws IOException
     */
    void send(List<Long> receive, IMessageEntity result, String fromUserId) throws IOException;

    /**
     * 当前登录人数
     *
     * @return
     */
    int getOnlineCount();

    /**
     * 获取用户当前连接快照
     *
     * @param userId
     * @return
     */
    List<Session> getSession(Long userId);

    /**
     * @CodingBy PopoY
     * @DateTime 2024/12/4 18:15
     * @Description 全局消息推送
     * @Param userId 用户id
     * @Param text 推送内容
     * @Return void
     */
    void sendGlobalMsgToUser(Long userId, String text);


    //
    //    /**
    //     * 添加登录人数
    //     */
    //    void addOnlineCount();
    //
    //    /**
    //     * 减去一个人数
    //     */
    //    void subOnlineCount()
}
