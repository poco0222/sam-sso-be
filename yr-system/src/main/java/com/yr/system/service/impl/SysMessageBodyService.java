/**
 * @file 站内消息主体服务实现，负责消息主体分页与新增落库
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.uuid.UUID;
import com.yr.system.domain.entity.SysMessageBody;
import com.yr.system.domain.vo.message.MessageVo;
import com.yr.system.mapper.SysMessageBodyMapper;
import com.yr.system.service.ISysMessageBodyService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author carl
 * @since 2022-01-18
 */
@Service
public class SysMessageBodyService extends CustomServiceImpl<SysMessageBodyMapper, SysMessageBody> implements ISysMessageBodyService {

    private final SysMessageBodyMapper sysMessageBodyMapper;

    public SysMessageBodyService(SysMessageBodyMapper sysMessageBodyMapper) {
        super();
        this.sysMessageBodyMapper = sysMessageBodyMapper;
    }

    @Override
    public IPage<SysMessageBody> pageList(MessageVo request) {
        return sysMessageBodyMapper.pageList(request);
    }

    /**
     * @CodingBy PopoY
     * @DateTime 2024/8/19 11:43
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
    @Override
    public int insertSysMessageBody(String msgTitle, String msgName, String msgBody, String sendType, String status, String remark, String url, String urlParam) {
        SysMessageBody sysMessageBody = new SysMessageBody();
        sysMessageBody.setId(UUID.randomUUID().toString());
        sysMessageBody.setMsgTitle(msgTitle);
        sysMessageBody.setMsgName(msgName);
        sysMessageBody.setMsgBody(msgBody);
        sysMessageBody.setSendType(sendType);
        sysMessageBody.setStatus(status);
        sysMessageBody.setRemark(remark);
        sysMessageBody.setMsgFrom(SecurityUtils.getUsername());
        sysMessageBody.setUrl(url);
        sysMessageBody.setUrlParam(urlParam);
        sysMessageBody.setCreateBy(SecurityUtils.getUsername());
        sysMessageBody.setCreateAt(LocalDateTime.now());
        return sysMessageBodyMapper.insertInToSysMessageBody(sysMessageBody);
    }
}
