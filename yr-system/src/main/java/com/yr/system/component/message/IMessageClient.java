package com.yr.system.component.message;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * description
 * </p>
 *
 * @author carl 2022-01-14 17:16
 * @version V1.0
 */
public interface IMessageClient {
    /**
     * 发送模板站内消息
     *
     * @param templateCode      模板编码
     * @param receiverGroupCode 接收人组编码
     * @param args              需要替换的参数
     */
    void sendWebMessage(String templateCode, String receiverGroupCode, Map<String, String> args);

    /**
     * 根据模板发送给自定义人
     *
     * @param templateCoe
     * @param users
     * @param args
     */
    void sendWebMessage(String templateCoe, List<Long> users, Map<String, String> args);

    /**
     * 自定义消息 ，自定义人
     *
     * @param message
     * @param users
     */
    void sendWebMessage(IMessageEntity message, List<Long> users);
}
