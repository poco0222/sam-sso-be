/**
 * @file 组织服务实现，一期阶段仅维护组织、根部门与默认组织关系
 * @author PopoY
 * @date 2026-03-26
 */
package com.yr.system.service.impl;

import com.yr.common.constant.Constants;
import com.yr.common.constant.UserConstants;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysOrg;
import com.yr.common.core.text.Convert;
import com.yr.common.exception.CustomException;
import com.yr.common.utils.DateUtils;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.domain.entity.SysUserOrg;
import com.yr.system.mapper.SysDeptMapper;
import com.yr.system.mapper.SysOrgMapper;
import com.yr.system.service.ISysOrgService;
import com.yr.system.service.ISysUserOrgService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 组织信息Service业务层处理
 *
 * @author Youngron
 * @date 2021-09-09
 */
@Service
public class SysOrgServiceImpl implements ISysOrgService {
    private final SysOrgMapper sysOrgMapper;
    private final SysDeptMapper deptMapper;
    private final ISysUserOrgService sysUserOrgService;

    public SysOrgServiceImpl(SysOrgMapper sysOrgMapper,
                             SysDeptMapper deptMapper,
                             ISysUserOrgService sysUserOrgService) {
        this.sysOrgMapper = sysOrgMapper;
        this.deptMapper = deptMapper;
        this.sysUserOrgService = sysUserOrgService;
    }

    /**
     * 查询组织信息
     *
     * @param orgId 组织信息ID
     * @return 组织信息
     */
    @Override
    public SysOrg selectSysOrgById(Long orgId) {
        return sysOrgMapper.selectSysOrgById(orgId);
    }

    /**
     * 查询组织信息列表
     *
     * @param sysOrg 组织信息
     * @return 组织信息
     */
    @Override
    public List<SysOrg> selectSysOrgList(SysOrg sysOrg) {
        return sysOrgMapper.selectSysOrgList(sysOrg);
    }

    /**
     * 新增组织信息
     *
     * @param sysOrg 组织信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertSysOrg(SysOrg sysOrg) {
        SysOrg parentOrg = sysOrgMapper.selectSysOrgById(sysOrg.getParentId());
        if (parentOrg == null) {
            throw new CustomException("父级组织不能为空");
        }
        if (!UserConstants.DEPT_NORMAL.equals(parentOrg.getStatus())) {
            throw new CustomException("组织停用，不允许新增");
        }
        sysOrg.setAncestors(parentOrg.getAncestors() + "," + parentOrg.getOrgId());
        sysOrg.setCreateBy(SecurityUtils.getUsername());
        sysOrg.setCreateAt(DateUtils.getNowDate());
        int count = sysOrgMapper.insertSysOrg(sysOrg);
        if (count == 1) {
            // 新增根部门
            SysDept sysDept = new SysDept();
            sysDept.setParentId(0L);
            sysDept.setAncestors("0");
            sysDept.setDeptCode("-");
            sysDept.setDeptName(sysOrg.getOrgName());
            sysDept.setOrderNum("0");
            sysDept.setStatus("0");
            sysDept.setDelFlag("0");
            sysDept.setOrgId(sysOrg.getOrgId());
            sysDept.setCreateBy(SecurityUtils.getUsername());
            deptMapper.insertDept(sysDept);

            // 新增关联超级管理员
            SysUserOrg sysUserOrg = new SysUserOrg();
            sysUserOrg.setUserId(1L);
            sysUserOrg.setOrgId(sysOrg.getOrgId());
            sysUserOrgService.addSysUserOrg(sysUserOrg);
        }
        return count;
    }

    /**
     * 修改组织信息
     *
     * @param sysOrg 组织信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateSysOrg(SysOrg sysOrg) {
        SysOrg parent = sysOrgMapper.selectSysOrgById(sysOrg.getParentId());
        SysOrg oldOrg = sysOrgMapper.selectSysOrgById(sysOrg.getOrgId());
        if (StringUtils.isNotNull(parent) && StringUtils.isNotNull(oldOrg)) {
            String newAncestors = parent.getAncestors() + "," + parent.getOrgId();
            String oldAncestors = oldOrg.getAncestors();
            sysOrg.setAncestors(newAncestors);
            updateOrgChildren(sysOrg.getOrgId(), newAncestors, oldAncestors);
        }
        sysOrg.setUpdateBy(SecurityUtils.getUsername());
        sysOrg.setUpdateAt(DateUtils.getNowDate());
        int result = sysOrgMapper.updateSysOrg(sysOrg);
        if (UserConstants.DEPT_NORMAL.equals(sysOrg.getStatus()) && UserConstants.DEPT_DISABLE.equals(oldOrg.getStatus())) {
            // 如果该岗位是启用状态，则启用该岗位的所有上级岗位
            updateParentOrgStatusNormal(sysOrg);
        }
        return result;
    }

    private void updateOrgChildren(Long orgId, String newAncestors, String oldAncestors) {
        List<SysOrg> children = sysOrgMapper.selectChildrenOrgById(orgId);
        for (SysOrg child : children) {
            child.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
        }
        if (children.size() > 0) {
            sysOrgMapper.updateOrgChildren(children);
        }
    }

    /**
     * 修改该组织的上级组织状态为启用
     *
     * @param sysOrg 当前组织
     */
    private void updateParentOrgStatusNormal(SysOrg sysOrg) {
        String ancestors = sysOrg.getAncestors();
        Long[] orgIds = Convert.toLongArray(ancestors);
        sysOrgMapper.updateOrgStatusNormal(orgIds);
    }

    /**
     * 批量删除组织信息
     *
     * @param orgIds 需要删除的组织信息ID
     * @return 结果
     */
    @Override
    public int deleteSysOrgByIds(Long[] orgIds) {
        return sysOrgMapper.deleteSysOrgByIds(orgIds);
    }

    /**
     * 删除组织信息信息
     *
     * @param orgId 组织信息ID
     * @return 结果
     */
    @Override
    public int deleteSysOrgById(Long orgId) {
        return sysOrgMapper.deleteSysOrgById(orgId);
    }

    /**
     * 校验组织编码是否唯一
     *
     * @param orgCode 组织编码
     * @return
     */
    @Override
    public String checkOrgCodeUnique(String orgCode) {
        int count = sysOrgMapper.checkOrgCodeUnique(orgCode);
        if (count > 0) {
            return Constants.FAIL;
        }
        return Constants.SUCCESS;
    }

    /**
     * 状态修改
     *
     * @param sysOrg
     * @return
     */
    @Override
    public int updateOrgStatus(SysOrg sysOrg) {
        return sysOrgMapper.updateSysOrg(sysOrg);
    }

    /**
     * 查询是否有子节点数据
     *
     * @param orgIds
     * @return
     */
    @Override
    public boolean hasChildByOrgId(Long orgIds) {
        int result = sysOrgMapper.hasChildByOrgId(orgIds);
        return result > 0 ? true : false;
    }

    @Override
    public int selectNormalChildrenOrgById(Long orgId) {
        return sysOrgMapper.selectNormalChildrenOrgById(orgId);
    }
}
