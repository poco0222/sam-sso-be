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
