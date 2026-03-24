/**
 * @file 用户写入事务服务，负责用户主表及关联关系的一致性提交
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yr.common.constant.UserConstants;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.exception.CustomException;
import com.yr.common.utils.SecurityUtils;
import com.yr.system.domain.entity.SysRank;
import com.yr.system.domain.entity.SysUserOrg;
import com.yr.system.domain.entity.SysUserRank;
import com.yr.system.mapper.SysUserMapper;
import com.yr.system.service.ISysRankService;
import com.yr.system.service.ISysUserOrgService;
import com.yr.system.service.ISysUserRankService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户写入事务服务。
 *
 * <p>该服务显式承接新增/修改用户的事务边界，避免在导入流程中发生自调用导致事务失效。</p>
 */
@Service
public class SysUserWriteService {

    /** 用户主表持久层。 */
    private final SysUserMapper userMapper;

    /** 职级服务。 */
    private final ISysRankService sysRankService;

    /** 用户组织关联服务。 */
    private final ISysUserOrgService sysUserOrgService;

    /** 用户职级关联服务。 */
    private final ISysUserRankService sysUserRankService;

    /**
     * 构造用户写入事务服务。
     *
     * @param userMapper 用户主表持久层
     * @param sysRankService 职级服务
     * @param sysUserOrgService 用户组织关联服务
     * @param sysUserRankService 用户职级关联服务
     */
    public SysUserWriteService(
            SysUserMapper userMapper,
            ISysRankService sysRankService,
            ISysUserOrgService sysUserOrgService,
            ISysUserRankService sysUserRankService
    ) {
        this.userMapper = userMapper;
        this.sysRankService = sysRankService;
        this.sysUserOrgService = sysUserOrgService;
        this.sysUserRankService = sysUserRankService;
    }

    /**
     * 新增用户及其组织、职级关联。
     *
     * @param user 用户信息
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int insertUser(SysUser user) {
        validateRank(user.getRankId());

        // 用户主表、组织关联、职级关联必须在同一事务中提交。
        int rows = userMapper.insertUser(user);

        SysUserOrg sysUserOrg = new SysUserOrg();
        sysUserOrg.setUserId(user.getUserId());
        sysUserOrg.setOrgId(SecurityUtils.getOrgId());
        LambdaQueryWrapper<SysUserOrg> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUserOrg::getUserId, user.getUserId());
        queryWrapper.eq(SysUserOrg::getIsDefault, 1);
        long count = sysUserOrgService.count(queryWrapper);
        if (count <= 0) {
            sysUserOrg.setIsDefault(1);
        }
        sysUserOrgService.addSysUserOrg(sysUserOrg);

        SysUserRank sysUserRank = new SysUserRank();
        sysUserRank.setUserId(user.getUserId());
        sysUserRank.setRankId(user.getRankId());
        sysUserRankService.save(sysUserRank);
        return rows;
    }

    /**
     * 修改用户及其职级关联。
     *
     * @param user 用户信息
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateUser(SysUser user) {
        validateRank(user.getRankId());
        if (user.getUserId() == null) {
            throw new CustomException("用户ID不能为空");
        }

        List<SysUserRank> userRankList = sysUserRankService.getUserRankByUserId(user.getUserId(), true);
        SysUserRank sysUserRank;
        if (userRankList != null && !userRankList.isEmpty()) {
            sysUserRank = userRankList.get(0);
            sysUserRank.setRankId(user.getRankId());
            sysUserRankService.updateById(sysUserRank);
        } else {
            sysUserRank = new SysUserRank();
            sysUserRank.setUserId(user.getUserId());
            sysUserRank.setRankId(user.getRankId());
            sysUserRankService.save(sysUserRank);
        }

        return userMapper.updateUser(user);
    }

    /**
     * 校验职级信息是否可用于用户写入。
     *
     * @param rankId 职级 ID
     */
    private void validateRank(Long rankId) {
        if (rankId == null) {
            throw new CustomException("职级不能为空");
        }
        SysRank sysRank = sysRankService.getById(rankId);
        if (sysRank == null) {
            throw new CustomException("职级不存在");
        }
        if (UserConstants.RANK_TYPE_CATALOG.equals(sysRank.getRankType())) {
            throw new CustomException("不能添加类型为目录的职级");
        }
    }
}
