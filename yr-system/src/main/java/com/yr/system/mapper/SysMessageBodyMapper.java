package com.yr.system.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.mybatisplus.custommapper.CustomMapper;
import com.yr.system.domain.entity.SysMessageBody;
import com.yr.system.domain.vo.message.MessageVo;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author carl
 * @since 2022-01-18
 */
public interface SysMessageBodyMapper extends CustomMapper<SysMessageBody> {

    /**
     * 分页查询
     *
     * @param request
     * @return
     */
    IPage<SysMessageBody> pageList(@Param("request") MessageVo request);

    /**
     * 通过Id查询
     *
     * @param id
     * @return
     */
    SysMessageBody findById(@Param("id") Long id);

    /**
     * @CodingBy PopoY
     * @DateTime 2024/8/19 09:44
     * @Description 插入消息通知主表
     * @Param sysMessageBody 消息主表对象
     * @Return int sql执行结果
     */
    int insertInToSysMessageBody(SysMessageBody sysMessageBody);
}
