package com.yr.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.mybatisplus.service.ICustomService;
import com.yr.system.domain.entity.SysMessageBodyReceiver;
import com.yr.system.domain.vo.message.MessageVo;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author carl
 * @since 2022-01-18
 */
public interface ISysMessageBodyReceiverService extends ICustomService<SysMessageBodyReceiver> {
    /**
     * 分页查询
     *
     * @param vo
     * @return
     */
    IPage<SysMessageBodyReceiver> pageList(MessageVo vo);

    /**
     * 通过接收列表ID获取消息详情
     *
     * @param sMBRId
     * @return
     */
    SysMessageBodyReceiver getReceiverMessage(Long sMBRId);

    /**
     * 修改阅读状态
     *
     * @param vo
     */
    void updateReadingStatus(MessageVo vo);

    /**
     * 删除消息
     *
     * @param vo
     */
    void del(MessageVo vo);
}
