package com.yr.system.service;

import com.yr.common.mybatisplus.service.ICustomService;
import com.yr.system.domain.entity.SysRank;
import com.yr.system.domain.entity.SysUserRank;

import java.util.List;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-26 18:29
 * @description
 */
public interface ISysUserRankService extends ICustomService<SysUserRank> {

    /**
     * 根据用户ID获取用户职级关联信息
     *
     * @param userId       用户ID
     * @param limitCurrOrg 限制仅查询当前组织
     * @return
     */
    List<SysUserRank> getUserRankByUserId(Long userId, Boolean limitCurrOrg);

    /**
     * 根据用户ID和租户ID查询用户职级
     *
     * @param userId
     * @param orgId
     * @return
     */
    SysRank selectUserRankByUserIdAndOrgId(Long userId, Long orgId);
}
