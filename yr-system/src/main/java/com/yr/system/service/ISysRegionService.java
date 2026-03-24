package com.yr.system.service;

import com.yr.common.mybatisplus.service.ICustomService;
import com.yr.system.domain.dto.SysRegionDTO;
import com.yr.system.domain.entity.SysRegion;

import java.util.List;

/**
 * 系统区域表(SysRegion)表服务接口
 *
 * @author Youngron
 * @since 2021-10-20 18:48:34
 */
public interface ISysRegionService extends ICustomService<SysRegion> {

    /**
     * 初始化区域数据
     *
     * @param list
     */
    void initData(List<SysRegionDTO> list);

    /**
     * 校验编码是否存在
     *
     * @param sysRegion
     * @return
     */
    boolean checkCodeUnique(SysRegion sysRegion);
}
