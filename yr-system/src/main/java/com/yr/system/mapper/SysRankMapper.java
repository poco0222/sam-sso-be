package com.yr.system.mapper;

import com.yr.common.mybatisplus.custommapper.CustomMapper;
import com.yr.system.domain.entity.SysRank;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-26 18:20
 * @description
 */
public interface SysRankMapper extends CustomMapper<SysRank> {

    /**
     * 查询包含指定祖级节点的全部后代职级。
     *
     * @param rankId 祖级职级 ID
     * @return 后代职级列表
     */
    List<SysRank> selectDescendants(@Param("rankId") Long rankId);
}
