package com.yr.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.mybatisplus.service.ICustomService;
import com.yr.system.domain.entity.SysMessageBody;
import com.yr.system.domain.vo.message.MessageVo;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author carl
 * @since 2022-01-18
 */
public interface ISysMessageBodyService extends ICustomService<SysMessageBody> {
    /**
     * 分页查询
     *
     * @param request
     * @return
     */
    IPage<SysMessageBody> pageList(MessageVo request);

    /**
     * @CodingBy PopoY
     * @DateTime 2024/8/19 11:42
     * @Description 组装并插入消息推送主表
     * @Param msgTitle 消息标题
     * @Param msgName 消息名称
     * @Param msgBody 消息主体
     * @Param sendType 消息类型
     * @Param status 消息状态
     * @Param remark 备注
     * @Param url 消息链接
     * @Param urlParam 链接参数
     * @Return int sql执行结果
     */
    int insertSysMessageBody(String msgTitle, String msgName, String msgBody, String sendType, String status, String remark, String url, String urlParam);
}
