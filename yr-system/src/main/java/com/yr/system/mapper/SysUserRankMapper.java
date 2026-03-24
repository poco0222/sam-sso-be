package com.yr.system.mapper;

import com.yr.common.mybatisplus.custommapper.CustomMapper;
import com.yr.system.domain.entity.SysRank;
import com.yr.system.domain.entity.SysUserRank;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-26 18:27
 * @description
 */
public interface SysUserRankMapper extends CustomMapper<SysUserRank> {

    /**
     * 根据用户ID获取用户职级关联信息
     *
     * @param userId 用户ID
     * @param orgId  组织ID，如果为空，则查询所有
     * @return
     */
    List<SysUserRank> getUserRankByUserId(@Param("userId") Long userId, @Param("orgId") Long orgId);

    /**
     * 根据用户ID和租户ID查询用户职级
     *
     * @param userId
     * @param orgId
     * @return
     */
    SysRank selectUserRankByUserIdAndOrgId(@Param("userId") Long userId, @Param("orgId") Long orgId);
}
