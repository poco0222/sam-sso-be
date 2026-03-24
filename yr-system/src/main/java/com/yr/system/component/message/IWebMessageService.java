package com.yr.system.component.message;


import java.util.List;
import java.util.Map;

/**
 * <p>
 * 消息类
 * </p>
 *
 * @author carl 2022-01-13 15:49
 * @version V1.0
 */
public interface IWebMessageService {
    /**
     * 发送模板站内消息
     *
     * @param templateCode      模板编码
     * @param receiverGroupCode 接收人组编码
     * @param args              需要替换的参数
     */
    void sendMessage(String templateCode, String receiverGroupCode, Map<String, String> args);

    /**
     * 根据模板发送给自定义人
     *
     * @param templateCoe
     * @param users
     * @param args
     */
    void sendMessage(String templateCoe, List<Long> users, Map<String, String> args);

    /**
     * 自定义消息 ，自定义人
     *
     * @param message
     * @param users
     */
    void sendMessage(IMessageEntity message, List<Long> users);
}
