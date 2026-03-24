package com.yr.system.mapper;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yr.common.mybatisplus.custommapper.CustomMapper;
import com.yr.system.domain.entity.SysReceiveGroup;
import com.yr.system.domain.vo.SysReceiveGroupVo;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author carl
 * @since 2022-01-04
 */
public interface SysReceiveGroupMapper extends CustomMapper<SysReceiveGroup> {
    /**
     * 分页查询
     *
     * @param vo
     * @return
     */
    Page<SysReceiveGroup> pageList(@Param("request") SysReceiveGroupVo vo);

    /**
     * 获取最大值
     *
     * @return
     */
    Long getMaxId();

    /**
     * 通过Code获取分组对象列表
     *
     * @param code
     * @return
     */
    SysReceiveGroup getReceiveGroupList(@Param("code") String code);
}
