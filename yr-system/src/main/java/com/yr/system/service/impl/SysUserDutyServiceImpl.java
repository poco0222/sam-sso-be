package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.common.utils.SecurityUtils;
import com.yr.system.domain.entity.SysDuty;
import com.yr.system.domain.entity.SysUserDuty;
import com.yr.system.mapper.SysUserDutyMapper;
import com.yr.system.service.ISysUserDutyService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-23 18:32
 * @description
 */

@Service
public class SysUserDutyServiceImpl extends CustomServiceImpl<SysUserDutyMapper, SysUserDuty> implements ISysUserDutyService {

    private final SysUserDutyMapper sysUserDutyMapper;

    public SysUserDutyServiceImpl(SysUserDutyMapper sysUserDutyMapper) {
        this.sysUserDutyMapper = sysUserDutyMapper;
    }

    @Override
    public IPage<SysUser> pageAssignUser(int pageNum, int pageSize, Long dutyId, SysUser sysUser) {
        return sysUserDutyMapper.pageAssignUser(new Page<>(pageNum, pageSize), dutyId, sysUser);
    }

    @Override
    public void removeAssignUserDuty(Long[] ids) {
        this.removeByIds(Arrays.asList(ids));
    }

    @Override
    public IPage<SysUser> pageUnAssignUser(int pageNum, int pageSize, Long dutyId, SysUser sysUser) {
        return sysUserDutyMapper.pageUnAssignUser(new Page<>(pageNum, pageSize), dutyId, SecurityUtils.getOrgId(), sysUser);
    }

    @Override
    public void addAssignUserDuty(Long dutyId, Long[] userIds) {
        if (userIds.length <= 0) {
            return;
        }
        // 因为职务分配用户、用户分配职务这两个功能调用的都是这个方法
        // 所以当新增的用户ID只有一条时，检查一下是否已经存在了关联关系（用户分配职务进来的需要检查，职务分配用户进来的不需要检查）
        if (userIds.length == 1) {
            LambdaQueryWrapper<SysUserDuty> queryWrapper = new LambdaQueryWrapper<SysUserDuty>()
                    .eq(SysUserDuty::getDutyId, dutyId)
                    .eq(SysUserDuty::getUserId, userIds[0]);
            long count = this.count(queryWrapper);
            if (count >= 1) {
                throw new CustomException("用户已经关联了该职务");
            }
        }
        List<SysUserDuty> sysUserDutyList = new ArrayList<>(userIds.length);
        for (Long userId : userIds) {
            SysUserDuty sysUserDuty = new SysUserDuty();
            sysUserDuty.setDutyId(dutyId);
            sysUserDuty.setUserId(userId);

            sysUserDutyList.add(sysUserDuty);
        }
        this.saveBatch(sysUserDutyList);
    }

    @Override
    public IPage<SysDuty> pageAssignDuty(int pageNum, int pageSize, Long userId) {
        return sysUserDutyMapper.pageAssignDuty(new Page<>(pageNum, pageSize), userId, SecurityUtils.getOrgId());
    }

    @Override
    public List<SysDuty> selectUserDutyListByUserIdAndOrgId(Long userId, Long orgId) {
        return sysUserDutyMapper.selectUserDutyListByUserIdAndOrgId(userId, orgId);
    }
}
