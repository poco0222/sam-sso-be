/**
 * @file 同步任务明细 Mapper
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.mapper;

import com.yr.common.core.domain.entity.SsoSyncTaskItem;
import com.yr.common.mybatisplus.custommapper.CustomMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 同步任务明细持久层。
 */
public interface SsoSyncTaskItemMapper extends CustomMapper<SsoSyncTaskItem> {

    /**
     * 查询任务下全部明细。
     *
     * @param taskId 任务 ID
     * @return 明细列表
     */
    List<SsoSyncTaskItem> selectByTaskId(@Param("taskId") Long taskId);

    /**
     * 查询任务下失败明细。
     *
     * @param taskId 任务 ID
     * @return 失败明细列表
     */
    List<SsoSyncTaskItem> selectFailedByTaskId(@Param("taskId") Long taskId);

    /**
     * 删除任务下全部明细。
     *
     * @param taskId 任务 ID
     * @return 受影响行数
     */
    int deleteByTaskId(@Param("taskId") Long taskId);

    /**
     * 按 taskId + msgKey 原位回写 after-commit 的最终状态。
     *
     * @param taskId 任务 ID
     * @param msgKey 消息键
     * @param targetId 目标 ID
     * @param status 最终状态
     * @param detailJson 明细载荷
     * @param errorMessage 错误信息
     * @return 受影响行数
     */
    int updateDispatchResultByTaskIdAndMsgKey(@Param("taskId") Long taskId,
                                              @Param("msgKey") String msgKey,
                                              @Param("targetId") String targetId,
                                              @Param("status") String status,
                                              @Param("detailJson") String detailJson,
                                              @Param("errorMessage") String errorMessage);
}
