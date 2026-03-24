package com.yr.system.mapper;

import com.yr.common.mybatisplus.custommapper.CustomMapper;
import com.yr.system.domain.entity.SysReceiveGroupObject;
import com.yr.system.domain.vo.SysReceiveGroupObjectVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author carl
 * @since 2022-01-04
 */
public interface SysReceiveGroupObjectMapper extends CustomMapper<SysReceiveGroupObject> {
    /**
     * 通过编码查询接收分组用户组
     *
     * @param reCode
     * @return
     */
    List<SysReceiveGroupObjectVo> findByReGroupCode(@Param("reGroupId") String reGroupId);

}
