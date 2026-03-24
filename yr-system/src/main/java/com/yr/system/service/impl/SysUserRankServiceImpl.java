package com.yr.system.service.impl;

import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.common.utils.SecurityUtils;
import com.yr.system.domain.entity.SysRank;
import com.yr.system.domain.entity.SysUserRank;
import com.yr.system.mapper.SysUserRankMapper;
import com.yr.system.service.ISysUserRankService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-26 18:29
 * @description
 */

@Service
public class SysUserRankServiceImpl extends CustomServiceImpl<SysUserRankMapper, SysUserRank> implements ISysUserRankService {

    private final SysUserRankMapper sysUserRankMapper;

    public SysUserRankServiceImpl(SysUserRankMapper sysUserRankMapper) {
        this.sysUserRankMapper = sysUserRankMapper;
    }

    @Override
    public List<SysUserRank> getUserRankByUserId(Long userId, Boolean limitCurrOrg) {
        if (limitCurrOrg) {
            return sysUserRankMapper.getUserRankByUserId(userId, SecurityUtils.getOrgId());
        }
        return sysUserRankMapper.getUserRankByUserId(userId, null);
    }

    @Override
    public SysRank selectUserRankByUserIdAndOrgId(Long userId, Long orgId) {
        return sysUserRankMapper.selectUserRankByUserIdAndOrgId(userId, orgId);
    }
}
