/**
 * @file 接收组对象服务实现，负责组对象的保存、删除与分页查询
 * @author Codex
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.system.domain.dto.ModeObjectDTO;
import com.yr.system.domain.dto.SysGroupObjectDTO;
import com.yr.system.domain.entity.SysReceiveGroupObject;
import com.yr.system.domain.vo.SysReceiveGroupObjectVo;
import com.yr.system.mapper.SysReceiveGroupObjectMapper;
import com.yr.system.service.ISysReceiveGroupObjectService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author carl
 * @since 2022-01-04
 */
@Service
public class SysReceiveGroupObjectService extends CustomServiceImpl<SysReceiveGroupObjectMapper, SysReceiveGroupObject> implements ISysReceiveGroupObjectService {

    @Override
    public Integer save(SysGroupObjectDTO groupObjectDTO) {
        //重新插入
        List<SysReceiveGroupObject> list = transform(groupObjectDTO);
        if (null != list) {
            saveOrUpdateBatch(list);
        }
        return 1;
    }

    /**
     * 转换成DB对象
     *
     * @return
     */
    private List<SysReceiveGroupObject> transform(SysGroupObjectDTO groupObjectDTO) {
        List<SysReceiveGroupObject> list = null;
        if (null != groupObjectDTO.getObjectIds() && groupObjectDTO.getObjectIds().size() > 0) {
            list = new ArrayList<>();
            for (ModeObjectDTO object : groupObjectDTO.getObjectIds()) {
                //效验重复对象
                SysReceiveGroupObject sysReceiveGroupObject = getOne(new LambdaQueryWrapper<SysReceiveGroupObject>().eq(SysReceiveGroupObject::getReGroupId
                        , groupObjectDTO.getReGroupId()).eq(SysReceiveGroupObject::getReObjectId, object.getId()));
                if (null == sysReceiveGroupObject) {
                    sysReceiveGroupObject = new SysReceiveGroupObject();
                    sysReceiveGroupObject.setReObjectId(object.getId());
                    sysReceiveGroupObject.setReGroupId(groupObjectDTO.getReGroupId());
                    sysReceiveGroupObject.setName(object.getName());
                }
                list.add(sysReceiveGroupObject);
            }
        }
        return list;
    }

    @Override
    public IPage<SysReceiveGroupObject> findByReGroupIdList(Long reGroupId, long page, long size) {
        return page(new Page<>(page, size), new LambdaQueryWrapper<SysReceiveGroupObject>().eq(SysReceiveGroupObject::getReGroupId, reGroupId).orderByDesc(SysReceiveGroupObject::getCreateAt));
    }

    @Override
    public Integer del(SysGroupObjectDTO groupObjectDTO) {
        if (groupObjectDTO.isDelAll()) {
            //全部删除
            remove(new LambdaQueryWrapper<SysReceiveGroupObject>().
                    eq(SysReceiveGroupObject::getReGroupId, groupObjectDTO.getReGroupId()));
        } else {
            //根据ID删除
            if (null != groupObjectDTO.getIds() && groupObjectDTO.getIds().size() > 0) {
                removeByIds(groupObjectDTO.getIds());
            }
        }
        return 1;
    }

    @Override
    public List<SysReceiveGroupObjectVo> findByReGroupCode(String reCode) {
        return getBaseMapper().findByReGroupCode(reCode);
    }
}
