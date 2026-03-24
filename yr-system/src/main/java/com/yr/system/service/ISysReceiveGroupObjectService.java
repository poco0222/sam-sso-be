package com.yr.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.mybatisplus.service.ICustomService;
import com.yr.system.domain.dto.SysGroupObjectDTO;
import com.yr.system.domain.entity.SysReceiveGroupObject;
import com.yr.system.domain.vo.SysReceiveGroupObjectVo;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author carl
 * @since 2022-01-04
 */
public interface ISysReceiveGroupObjectService extends ICustomService<SysReceiveGroupObject> {
    /**
     * 保存
     *
     * @param groupObjectDTO
     * @return
     */
    Integer save(SysGroupObjectDTO groupObjectDTO);

    /**
     * 分页查询
     *
     * @param reGroupId
     * @param page
     * @param size
     * @return
     */
    IPage<SysReceiveGroupObject> findByReGroupIdList(Long reGroupId, long page, long size);

    /**
     * 删除
     *
     * @param groupObjectDTO
     * @return
     */
    Integer del(SysGroupObjectDTO groupObjectDTO);

    /**
     * 通过接收人分组编码查询
     *
     * @param reCode
     * @return
     */
    List<SysReceiveGroupObjectVo> findByReGroupCode(String reCode);
}
