/**
 * @file 同步任务明细服务实现
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SsoSyncTaskItem;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.system.mapper.SsoSyncTaskItemMapper;
import com.yr.system.service.ISsoSyncTaskItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 同步任务明细服务实现。
 */
@Service
public class SsoSyncTaskItemServiceImpl extends CustomServiceImpl<SsoSyncTaskItemMapper, SsoSyncTaskItem> implements ISsoSyncTaskItemService {

    /** 明细 Mapper。 */
    private final SsoSyncTaskItemMapper ssoSyncTaskItemMapper;

    /**
     * @param ssoSyncTaskItemMapper 明细 Mapper
     */
    public SsoSyncTaskItemServiceImpl(SsoSyncTaskItemMapper ssoSyncTaskItemMapper) {
        this.ssoSyncTaskItemMapper = ssoSyncTaskItemMapper;
    }

    /**
     * 查询任务下全部明细。
     *
     * @param taskId 任务 ID
     * @return 明细列表
     */
    @Override
    public List<SsoSyncTaskItem> selectByTaskId(Long taskId) {
        return taskId == null ? Collections.emptyList() : ssoSyncTaskItemMapper.selectByTaskId(taskId);
    }

    /**
     * 查询任务下失败明细。
     *
     * @param taskId 任务 ID
     * @return 失败明细列表
     */
    @Override
    public List<SsoSyncTaskItem> selectFailedByTaskId(Long taskId) {
        return taskId == null ? Collections.emptyList() : ssoSyncTaskItemMapper.selectFailedByTaskId(taskId);
    }

    /**
     * 用新明细整体替换旧明细。
     *
     * @param taskId 任务 ID
     * @param itemList 新明细列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceTaskItems(Long taskId, List<SsoSyncTaskItem> itemList) {
        ssoSyncTaskItemMapper.deleteByTaskId(taskId);
        if (itemList == null || itemList.isEmpty()) {
            return;
        }
        for (SsoSyncTaskItem item : itemList) {
            item.setTaskId(taskId);
        }
        this.saveBatch(itemList);
    }
}
