/**
 * @file 用户组织关联服务实现
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.common.utils.SecurityUtils;
import com.yr.system.domain.entity.SysUserOrg;
import com.yr.system.mapper.SysUserOrgMapper;
import com.yr.system.service.ISysUserOrgService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Youngron
 * @version V1.0
 * @date 2021-9-14 14:26
 */

@Service
public class SysUserOrgServiceImpl extends CustomServiceImpl<SysUserOrgMapper, SysUserOrg> implements ISysUserOrgService {

    private final SysUserOrgMapper sysUserOrgMapper;

    public SysUserOrgServiceImpl(SysUserOrgMapper sysUserOrgMapper) {
        this.sysUserOrgMapper = sysUserOrgMapper;
    }

    @Override
    public IPage<SysUserOrg> pageByUserId(int pageNum, int pageSize, Long userId) {
        if (userId == null) {
            userId = SecurityUtils.getUserId();
            if (userId == null) {
                throw new CustomException("用户ID为空");
            }
        }
        SysUserOrg sysUserOrg = new SysUserOrg();
        sysUserOrg.setUserId(userId);
        return sysUserOrgMapper.pageSysUserOrg(new Page<>(pageNum, pageSize), sysUserOrg);
    }

    @Override
    public void addSysUserOrg(SysUserOrg sysUserOrg) {
        if (sysUserOrg.getUserId() == null) {
            throw new CustomException("用户ID不能为空");
        }
        if (sysUserOrg.getOrgId() == null) {
            throw new CustomException("组织ID不能为空");
        }
        LambdaQueryWrapper<SysUserOrg> queryWrapper = new LambdaQueryWrapper<SysUserOrg>()
                .eq(SysUserOrg::getUserId, sysUserOrg.getUserId())
                .eq(SysUserOrg::getOrgId, sysUserOrg.getOrgId());
        SysUserOrg one = this.getOne(queryWrapper);
        if (one != null) {
            throw new CustomException("该用户已经在组织下");
        }
        this.save(sysUserOrg);
    }

    @Override
    public int changeEnabledById(Long id, Integer enable) {
        if (id == null) {
            throw new CustomException("主键ID不能为空");
        }
        if (enable == null) {
            throw new CustomException("需要改变的状态不能为空");
        }
        SysUserOrg sysUserOrg = new SysUserOrg();
        sysUserOrg.setId(id);
        sysUserOrg.setEnabled(enable);
        return sysUserOrgMapper.updateById(sysUserOrg);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int setDefaultUserOrg(Long userId, Long orgId) {
        // 先清理旧默认组织，再设置新的默认组织，保证整个切换过程原子完成。
        sysUserOrgMapper.clearDefaultUserOrg(userId);
        // 第二次更新必须命中唯一目标组织，否则整体回滚，避免用户丢失默认组织。
        int count = sysUserOrgMapper.setDefaultUserOrg(userId, orgId);
        if (count != 1) {
            throw new CustomException("设置失败");
        }
        return 1;
    }

    @Override
    public List<SysUserOrg> getCurrUserAllOrg() {
        SysUserOrg sysUserOrg = new SysUserOrg();
        sysUserOrg.setUserId(SecurityUtils.getUserId());
        IPage<SysUserOrg> page = sysUserOrgMapper.pageSysUserOrg(new Page<>(1, 100), sysUserOrg);
        return page.getRecords();
    }
}
