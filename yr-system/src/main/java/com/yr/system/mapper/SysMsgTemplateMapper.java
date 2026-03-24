package com.yr.system.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.mybatisplus.custommapper.CustomMapper;
import com.yr.system.domain.entity.SysMsgTemplate;
import com.yr.system.domain.vo.SysMsgTemplateVo;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author carl
 * @since 2022-01-04
 */
public interface SysMsgTemplateMapper extends CustomMapper<SysMsgTemplate> {
    /**
     * 分页查询list
     *
     * @param template 查询参数
     * @return
     */
    IPage<SysMsgTemplate> pageList(@Param("request") SysMsgTemplateVo template);
}
