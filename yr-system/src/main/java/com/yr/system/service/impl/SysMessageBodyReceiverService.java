/**
 * @file 消息接收人服务实现，负责阅读状态与删除行为
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.annotation.AutoMessage;
import com.yr.common.enums.MessageStatus;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.common.utils.SecurityUtils;
import com.yr.system.domain.entity.SysMessageBodyReceiver;
import com.yr.system.domain.vo.message.MessageVo;
import com.yr.system.mapper.SysMessageBodyReceiverMapper;
import com.yr.system.service.ISysMessageBodyReceiverService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author carl
 * @since 2022-01-18
 */
@Service
public class SysMessageBodyReceiverService extends CustomServiceImpl<SysMessageBodyReceiverMapper, SysMessageBodyReceiver> implements ISysMessageBodyReceiverService {

    @Override
    public IPage<SysMessageBodyReceiver> pageList(MessageVo vo) {
        return getBaseMapper().pageList(vo);
    }

    @Override
    public SysMessageBodyReceiver getReceiverMessage(Long sMBRId) {
        return getBaseMapper().getReceiverMessage(sMBRId);
    }

    @Override
    @AutoMessage
    public void updateReadingStatus(MessageVo vo) {
        if (vo.isUpdateAllRead()) {
            //更新全部
            List<SysMessageBodyReceiver> messageList = list(new LambdaQueryWrapper<SysMessageBodyReceiver>()
                    .eq(SysMessageBodyReceiver::getMsgTo, SecurityUtils.getUserId()));
            if (CollectionUtils.isNotEmpty(messageList)) {
                for (SysMessageBodyReceiver sysMessageBodyReceiver : messageList) {
                    sysMessageBodyReceiver.setReceiveStatus(MessageStatus.MSG_HAVE_READ.getCode());
                }
            }
            saveOrUpdateBatch(messageList);
        } else if (hasValidReceiverId(vo.getId())) {
            SysMessageBodyReceiver sysMessageBodyReceiver = Optional.ofNullable(getById(vo.getId()))
                    .orElseThrow(() -> new CustomException("接收列表ID无效"));
            //设置状态已读
            sysMessageBodyReceiver.setReceiveStatus(MessageStatus.MSG_HAVE_READ.getCode());
            saveOrUpdate(sysMessageBodyReceiver);
        }
    }


    @Override
    @AutoMessage
    public void del(MessageVo vo) {
        if (vo.isAllDel()) {
            //删除全部
            remove(new LambdaQueryWrapper<SysMessageBodyReceiver>()
                    .eq(SysMessageBodyReceiver::getMsgTo, SecurityUtils.getUserId()));
        } else if (hasValidReceiverId(vo.getId())) {
            //根据ID删除
            removeById(vo.getId());
        } else {
            throw new CustomException("选至少选择一种删除方式");
        }
    }

    /**
     * 判断接收记录 ID 是否为可用值。
     *
     * @param id 接收记录 ID
     * @return true 表示可继续处理
     */
    private boolean hasValidReceiverId(Long id) {
        return id != null && id >= 0;
    }
}
