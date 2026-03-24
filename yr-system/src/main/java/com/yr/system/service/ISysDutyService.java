package com.yr.system.service;

import com.yr.common.mybatisplus.service.ICustomService;
import com.yr.system.domain.entity.SysDuty;

import java.util.List;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-23 18:28
 * @description
 */
public interface ISysDutyService extends ICustomService<SysDuty> {

    /**
     * 查询职务数据
     *
     * @param sysDuty 查询条件
     * @return
     */
    List<SysDuty> listDuty(SysDuty sysDuty);

    /**
     * 新增职务
     *
     * @param sysDuty
     * @return
     */
    SysDuty addDuty(SysDuty sysDuty);

    /**
     * 更新职务
     *
     * @param sysDuty
     * @return
     */
    SysDuty updateDuty(SysDuty sysDuty);

    /**
     * 删除职务
     *
     * @param id 职务ID
     * @return
     */
    int deleteDuty(Long id);
}
