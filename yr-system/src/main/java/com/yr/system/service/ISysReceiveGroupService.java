package com.yr.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yr.common.mybatisplus.service.ICustomService;
import com.yr.system.domain.entity.SysReceiveGroup;
import com.yr.system.domain.vo.SysObjectTreeVo;
import com.yr.system.domain.vo.SysReceiveGroupVo;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author carl
 * @since 2022-01-04
 */
public interface ISysReceiveGroupService extends ICustomService<SysReceiveGroup> {
    /**
     * 分页查询集合
     *
     * @param sysReceiveGroupVo
     * @return
     */
    Page<SysReceiveGroup> pageList(SysReceiveGroupVo sysReceiveGroupVo);

    /**
     * 保存分组
     *
     * @param sysReceiveGroup
     * @return
     */
    SysReceiveGroup saveReceiveGroup(SysReceiveGroup sysReceiveGroup);

    /**
     * 删除
     *
     * @param id
     * @return
     */
    Integer del(Long id);

    /**
     * 通过code查询
     *
     * @param code
     * @return
     */
    SysReceiveGroup get(String code);


    /**
     * 通过reGroupId获取具体对象
     *
     * @param reGroupId
     * @return
     */
    SysObjectTreeVo getSpecificObjects(Long reGroupId);

    /**
     * 通过code查询接收分组集合
     *
     * @param code
     * @return
     */
    SysReceiveGroup getReceiveGroupList(String code);
}
