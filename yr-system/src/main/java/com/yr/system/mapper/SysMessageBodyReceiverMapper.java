package com.yr.system.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.mybatisplus.custommapper.CustomMapper;
import com.yr.system.domain.entity.SysMessageBodyReceiver;
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
public interface SysMessageBodyReceiverMapper extends CustomMapper<SysMessageBodyReceiver> {
    /**
     * 分页查询集合
     *
     * @param vo
     * @return
     */
    IPage<SysMessageBodyReceiver> pageList(@Param("request") MessageVo vo);

    /**
     * 通过ID查询
     *
     * @param sMBRId
     * @return
     */
    SysMessageBodyReceiver getReceiverMessage(@Param("id") Long sMBRId);
}
