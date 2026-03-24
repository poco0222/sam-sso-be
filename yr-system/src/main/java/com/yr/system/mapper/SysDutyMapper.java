package com.yr.system.mapper;

import com.yr.common.mybatisplus.custommapper.CustomMapper;
import com.yr.system.domain.entity.SysDuty;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-23 18:28
 * @description
 */
public interface SysDutyMapper extends CustomMapper<SysDuty> {

    /**
     * 查询包含指定祖级节点的全部后代职务。
     *
     * @param dutyId 祖级职务 ID
     * @return 后代职务列表
     */
    List<SysDuty> selectDescendants(@Param("dutyId") Long dutyId);
}
