/**
 * @file MQ 消息履历 Mapper
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.common.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yr.common.core.domain.MqMessageLog;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * MQ消息履历Mapper
 */
public interface MqMessageLogMapper extends BaseMapper<MqMessageLog> {

    /**
     * 按 msgKey 批量查询最新一条履历，供 sync-task console 详情回显。
     *
     * @param msgKeys 消息键列表
     * @return 最新履历列表
     */
    List<MqMessageLog> selectLatestByMsgKeys(@Param("msgKeys") List<String> msgKeys);

    /**
     * 查询待重试的失败消息
     */
    List<MqMessageLog> selectRetryList();

    /**
     * 更新发送状态
     */
    int updateSendStatus(@Param("id") Long id, @Param("sendStatus") Integer sendStatus,
                         @Param("msgId") String msgId, @Param("errorMsg") String errorMsg);

    /**
     * 更新消费状态
     */
    int updateConsumeStatus(@Param("msgKey") String msgKey, @Param("consumeStatus") Integer consumeStatus,
                            @Param("errorMsg") String errorMsg);

    /**
     * 增加重试次数
     */
    int incrementRetryCount(@Param("id") Long id);
}
