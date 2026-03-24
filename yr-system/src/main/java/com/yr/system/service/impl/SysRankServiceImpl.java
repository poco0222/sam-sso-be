/**
 * @file 职级服务实现
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yr.common.constant.UserConstants;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.domain.entity.SysRank;
import com.yr.system.domain.entity.SysUserRank;
import com.yr.system.mapper.SysRankMapper;
import com.yr.system.service.ISysRankService;
import com.yr.system.service.ISysUserRankService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-26 18:21
 * @description
 */

@Service
public class SysRankServiceImpl extends CustomServiceImpl<SysRankMapper, SysRank> implements ISysRankService {

    private final ISysUserRankService iSysUserRankService;

    public SysRankServiceImpl(ISysUserRankService iSysUserRankService) {
        this.iSysUserRankService = iSysUserRankService;
    }

    @Override
    public List<SysRank> listRank(SysRank sysRank) {
        LambdaQueryWrapper<SysRank> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(sysRank.getRankCode()), SysRank::getRankCode, sysRank.getRankCode());
        queryWrapper.like(StringUtils.isNotBlank(sysRank.getRankName()), SysRank::getRankName, sysRank.getRankName());
        queryWrapper.eq(SysRank::getOrgId, SecurityUtils.getOrgId());
        queryWrapper.orderByAsc(SysRank::getParentId)
                .orderByAsc(SysRank::getOrderNum);
        return this.list(queryWrapper);
    }

    @Override
    public SysRank addRank(SysRank sysRank) {
        if (sysRank.getOrgId() == null) {
            sysRank.setOrgId(SecurityUtils.getOrgId());
        }

        if (UserConstants.NOT_UNIQUE.equals(this.checkRankCodeUnique(sysRank))) {
            throw new CustomException("新增失败，编码已存在");
        }

        SysRank parent = this.getById(sysRank.getParentId());
        // 父节点不存在时直接返回业务异常，避免后续树路径计算抛出 NPE。
        if (parent == null) {
            throw new CustomException("上级职级不存在");
        }
        if (UserConstants.RANK_TYPE_RANK.equals(parent.getRankType())) {
            throw new CustomException("只有分类为目录的才能新增下级节点");
        }
        sysRank.setAncestors(parent.getAncestors() + "," + sysRank.getParentId());

        this.save(sysRank);
        return sysRank;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRank updateRank(SysRank sysRank) {
        if (UserConstants.NOT_UNIQUE.equals(this.checkRankCodeUnique(sysRank))) {
            throw new CustomException("更新失败，编码已存在");
        }
        if (sysRank.getId().equals(sysRank.getParentId())) {
            throw new CustomException("更新失败，上级节点不能是自己");
        }
        if (UserConstants.RANK_TYPE_CATALOG.equals(sysRank.getRankType())) {
            LambdaQueryWrapper<SysUserRank> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysUserRank::getRankId, sysRank.getId());
            long count = iSysUserRankService.count(queryWrapper);
            if (count > 0) {
                throw new CustomException("职级存在用户,不允许改为目录");
            }
        }

        SysRank parent = this.getById(sysRank.getParentId());
        if (parent == null) {
            throw new CustomException("上级职级不存在");
        }
        SysRank oldRank = this.getById(sysRank.getId());
        if (oldRank == null) {
            throw new CustomException("职级不存在");
        }
        String newAncestors = parent.getAncestors() + "," + parent.getId();
        String oldAncestors = oldRank.getAncestors();
        sysRank.setAncestors(newAncestors);
        this.updateRankChildrenAncestors(sysRank.getId(), newAncestors, oldAncestors);

        SysRank mergedRank = mergeRankForUpdate(oldRank, sysRank);
        if (!this.updateById(mergedRank)) {
            throw new CustomException("更新失败，数据可能已经被修改");
        }
        return mergedRank;
    }

    @Override
    public void deleteRankById(Long id) {
        if (id == null) {
            throw new CustomException("id can't be null");
        }

        SysRank sysRank = this.getById(id);
        if (sysRank == null) {
            throw new CustomException("数据不存在");
        }
        if (sysRank.getParentId().compareTo(0L) == 0) {
            throw new CustomException("根节点不允许删除");
        }

        // 检查是否存在下级职级
        LambdaQueryWrapper<SysRank> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRank::getParentId, id);
        long childrenCount = this.count(queryWrapper);
        if (childrenCount > 0) {
            throw new CustomException("存在下级节点，不能删除");
        }

        // 检查是否关联了用户
        LambdaQueryWrapper<SysUserRank> queryWrapper2 = new LambdaQueryWrapper<>();
        queryWrapper2.eq(SysUserRank::getRankId, id);
        long count = iSysUserRankService.count(queryWrapper2);
        if (count > 0) {
            throw new CustomException("职级存在关联的用户，不能删除");
        }

        // 删除职级
        this.removeById(id);
    }

    /**
     * 检查职级编码是否唯一
     *
     * @param sysRank
     * @return
     */
    private String checkRankCodeUnique(SysRank sysRank) {
        Long rankId = StringUtils.isNull(sysRank.getId()) ? -1L : sysRank.getId();

        LambdaQueryWrapper<SysRank> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRank::getOrgId, sysRank.getOrgId());
        queryWrapper.eq(SysRank::getRankCode, sysRank.getRankCode());
        SysRank info = this.getOne(queryWrapper);
        if (StringUtils.isNotNull(info) && info.getId().compareTo(rankId) != 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 更新子节点的祖级列表
     *
     * @param rankId       职级ID
     * @param newAncestors 新的祖级列表
     * @param oldAncestors 需要替换的祖级列表
     */
    private void updateRankChildrenAncestors(Long rankId, String newAncestors, String oldAncestors) {
        List<SysRank> childrenList = getBaseMapper().selectDescendants(rankId);
        if (CollectionUtils.isNotEmpty(childrenList)) {
            for (SysRank child : childrenList) {
                child.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
            }
            this.updateBatchById(childrenList);
        }
    }

    /**
     * 合并旧实体与更新命令，避免 partial payload 覆盖数据库已有字段。
     *
     * @param oldRank 已存在的职级实体
     * @param updateCommand 本次更新命令
     * @return 可安全持久化的合并结果
     */
    private SysRank mergeRankForUpdate(SysRank oldRank, SysRank updateCommand) {
        SysRank mergedRank = new SysRank();
        mergedRank.setId(oldRank.getId());
        mergedRank.setParentId(updateCommand.getParentId() != null ? updateCommand.getParentId() : oldRank.getParentId());
        mergedRank.setAncestors(updateCommand.getAncestors() != null ? updateCommand.getAncestors() : oldRank.getAncestors());
        mergedRank.setRankCode(updateCommand.getRankCode() != null ? updateCommand.getRankCode() : oldRank.getRankCode());
        mergedRank.setRankName(updateCommand.getRankName() != null ? updateCommand.getRankName() : oldRank.getRankName());
        mergedRank.setRankType(updateCommand.getRankType() != null ? updateCommand.getRankType() : oldRank.getRankType());
        mergedRank.setOrderNum(updateCommand.getOrderNum() != null ? updateCommand.getOrderNum() : oldRank.getOrderNum());
        mergedRank.setOrgId(updateCommand.getOrgId() != null ? updateCommand.getOrgId() : oldRank.getOrgId());
        return mergedRank;
    }
}
