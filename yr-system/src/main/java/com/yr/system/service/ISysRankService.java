package com.yr.system.service;

import com.yr.common.mybatisplus.service.ICustomService;
import com.yr.system.domain.entity.SysRank;

import java.util.List;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-26 18:21
 * @description
 */
public interface ISysRankService extends ICustomService<SysRank> {

    /**
     * 查询职级列表
     *
     * @param sysRank
     * @return
     */
    List<SysRank> listRank(SysRank sysRank);

    /**
     * 新增职级
     *
     * @param sysRank
     * @return
     */
    SysRank addRank(SysRank sysRank);

    /**
     * 更新职级
     *
     * @param sysRank
     * @return
     */
    SysRank updateRank(SysRank sysRank);

    /**
     * 删除职级
     *
     * @param id 职级ID
     */
    void deleteRankById(Long id);
}
