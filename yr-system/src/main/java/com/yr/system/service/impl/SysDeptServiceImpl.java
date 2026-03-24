/**
 * @file 部门服务实现，收口父节点缺失时的 fail-fast 契约
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yr.common.constant.UserConstants;
import com.yr.common.core.domain.TreeSelect;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysDeptRoleVo;
import com.yr.common.core.domain.entity.SysRole;
import com.yr.common.core.text.Convert;
import com.yr.common.exception.CustomException;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.domain.entity.SysUserDept;
import com.yr.system.mapper.SysDeptMapper;
import com.yr.system.mapper.SysRoleMapper;
import com.yr.system.mapper.SysUserDeptMapper;
import com.yr.system.service.ISysDeptService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门管理 服务实现
 *
 * @author Youngron
 */
@Service
public class SysDeptServiceImpl implements ISysDeptService {
    private final SysDeptMapper deptMapper;

    private final SysRoleMapper roleMapper;

    private final SysUserDeptMapper sysUserDeptMapper;

    /** 通过接口代理保留可能存在的数据权限切面。 */
    private final ISysDeptService selfProxy;

    public SysDeptServiceImpl(SysDeptMapper deptMapper,
                              SysRoleMapper roleMapper,
                              SysUserDeptMapper sysUserDeptMapper,
                              @Lazy ISysDeptService selfProxy) {
        this.deptMapper = deptMapper;
        this.roleMapper = roleMapper;
        this.sysUserDeptMapper = sysUserDeptMapper;
        this.selfProxy = selfProxy;
    }

    /**
     * 查询部门管理数据
     *
     * @param dept 部门信息
     * @return 部门信息集合
     */
    @Override
    // @DataScope(deptAlias = "d")
    public List<SysDept> selectDeptList(SysDept dept) {
        if (dept.getOrgId() == null) {
            dept.setOrgId(SecurityUtils.getOrgId());
        }
        return deptMapper.selectDeptList(dept);
    }

    /**
     * 查询核算部门数据
     *
     * @return 部门信息集合
     */
    @Override
    public List<SysDept> getCheckDeptList() {
        return deptMapper.getCheckDeptList();
    }

    /**
     * 查询部门树结构信息
     *
     * @param dept 部门信息
     * @return 部门树信息集合
     */
    @Override
    public List<TreeSelect> selectDeptTreeList(SysDept dept) {
        List<SysDept> depts = selfProxy.selectDeptList(dept);
        return buildDeptTreeSelect(depts);
    }

    @Override
    public List<SysDept> selectSysDept() {
        List<SysDept> listDept = deptMapper.selectSysDept();
        return listDept;
    }

    /**
     * 构建前端所需要树结构
     *
     * @param depts 部门列表
     * @return 树结构列表
     */
    @Override
    public List<SysDept> buildDeptTree(List<SysDept> depts) {
        List<SysDept> returnList = new ArrayList<SysDept>();
        List<Long> tempList = new ArrayList<Long>();
        for (SysDept dept : depts) {
            tempList.add(dept.getDeptId());
        }
        for (Iterator<SysDept> iterator = depts.iterator(); iterator.hasNext(); ) {
            SysDept dept = (SysDept) iterator.next();
            // 如果是顶级节点, 遍历该父节点的所有子节点
            if (!tempList.contains(dept.getParentId())) {
                recursionFn(depts, dept);
                returnList.add(dept);
            }
        }
        if (returnList.isEmpty()) {
            returnList = depts;
        }
        return returnList;
    }

    @Override
    public List<SysDeptRoleVo> buildDeptRoleTree(List<SysDeptRoleVo> depts) {
        List<SysDeptRoleVo> returnList = new ArrayList<SysDeptRoleVo>();
        List<String> tempList = new ArrayList<String>();
        for (SysDeptRoleVo dept : depts) {
            tempList.add(dept.getId());
        }
        for (Iterator<SysDeptRoleVo> iterator = depts.iterator(); iterator.hasNext(); ) {
            SysDeptRoleVo dept = (SysDeptRoleVo) iterator.next();
            // 如果是顶级节点, 遍历该父节点的所有子节点
            if (!tempList.contains(dept.getParentId())) {
                recursionFnRole(depts, dept);
                returnList.add(dept);
            }
        }
        if (returnList.isEmpty()) {
            returnList = depts;
        }
        return returnList;
    }

    /**
     * 构建前端所需要下拉树结构
     *
     * @param depts 部门列表
     * @return 下拉树结构列表
     */
    @Override
    public List<TreeSelect> buildDeptTreeSelect(List<SysDept> depts) {
        List<SysDept> deptTrees = buildDeptTree(depts);
        return deptTrees.stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    @Override
    public List<TreeSelect> buildDeptRoleTreeSelect(List<SysDeptRoleVo> sysDeptRoleVos) {
        List<SysDeptRoleVo> deptTrees = buildDeptRoleTree(sysDeptRoleVos);
        return deptTrees.stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    /**
     * 根据角色ID查询部门树信息
     *
     * @param roleId 角色ID
     * @return 选中部门列表
     */
    @Override
    public List<Integer> selectDeptListByRoleId(Long roleId) {
        SysRole role = roleMapper.selectRoleById(roleId);
        // fail-fast（快速失败）：角色缺失直接抛 CustomException，避免 NPE（NullPointerException，空指针异常）泄漏到 controller。
        if (role == null) {
            throw new CustomException("角色不存在或已删除: roleId=" + roleId);
        }
        return deptMapper.selectDeptListByRoleId(roleId, role.isDeptCheckStrictly());
    }

    /**
     * 根据部门ID查询信息
     *
     * @param deptId 部门ID
     * @return 部门信息
     */
    @Override
    public SysDept selectDeptById(Long deptId) {
        return deptMapper.selectDeptById(deptId);
    }

    /**
     * 根据ID查询所有子部门（正常状态）
     *
     * @param deptId 部门ID
     * @return 子部门数
     */
    @Override
    public int selectNormalChildrenDeptById(Long deptId) {
        return deptMapper.selectNormalChildrenDeptById(deptId);
    }

    /**
     * 是否存在子节点
     *
     * @param deptId 部门ID
     * @return 结果
     */
    @Override
    public boolean hasChildByDeptId(Long deptId) {
        int result = deptMapper.hasChildByDeptId(deptId);
        return result > 0 ? true : false;
    }

    /**
     * 查询部门是否存在用户
     *
     * @param deptId 部门ID
     * @return 结果 true 存在 false 不存在
     */
    @Override
    public boolean checkDeptExistUser(Long deptId) {
//        int result = deptMapper.checkDeptExistUser(deptId);
        LambdaQueryWrapper<SysUserDept> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUserDept::getDeptId, deptId);
        Long count = sysUserDeptMapper.selectCount(queryWrapper);
        return count != null && count.compareTo(0L) > 0;
    }

    /**
     * 校验部门编码是否唯一
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    public String checkDeptCodeUnique(SysDept dept) {
        Long deptId = StringUtils.isNull(dept.getDeptId()) ? -1L : dept.getDeptId();
        SysDept info = deptMapper.checkDeptCodeUnique(dept.getDeptCode(), dept.getParentId(), dept.getOrgId());
        if (StringUtils.isNotNull(info) && info.getDeptId().longValue() != deptId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 新增保存部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    public int insertDept(SysDept dept) {
        SysDept info = deptMapper.selectDeptById(dept.getParentId());
        if (info == null) {
            throw new CustomException("上级部门不存在: parentId=" + dept.getParentId());
        }
        // 如果父节点不为正常状态,则不允许新增子节点
        if (!UserConstants.DEPT_NORMAL.equals(info.getStatus())) {
            throw new CustomException("部门停用，不允许新增");
        }
        dept.setAncestors(info.getAncestors() + "," + dept.getParentId());

        // 如果组织ID为空，取父部门的组织ID
        if (dept.getOrgId() == null) {
            dept.setOrgId(info.getOrgId());
        }
        return deptMapper.insertDept(dept);
    }

    /**
     * 修改保存部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDept(SysDept dept) {
        SysDept newParentDept = deptMapper.selectDeptById(dept.getParentId());
        SysDept oldDept = deptMapper.selectDeptById(dept.getDeptId());
        if (StringUtils.isNotNull(newParentDept) && StringUtils.isNotNull(oldDept)) {
            String newAncestors = newParentDept.getAncestors() + "," + newParentDept.getDeptId();
            String oldAncestors = oldDept.getAncestors();
            dept.setAncestors(newAncestors);
            updateDeptChildren(dept.getDeptId(), newAncestors, oldAncestors);
        }
        int result = deptMapper.updateDept(dept);
        if (UserConstants.DEPT_NORMAL.equals(dept.getStatus())) {
            // 如果该部门是启用状态，则启用该部门的所有上级部门
            updateParentDeptStatusNormal(dept);
        }
        return result;
    }

    /**
     * 修改该部门的父级部门状态
     *
     * @param dept 当前部门
     */
    private void updateParentDeptStatusNormal(SysDept dept) {
        String ancestors = dept.getAncestors();
        Long[] deptIds = Convert.toLongArray(ancestors);
        deptMapper.updateDeptStatusNormal(deptIds);
    }

    /**
     * 修改子元素关系
     *
     * @param deptId       被修改的部门ID
     * @param newAncestors 新的父ID集合
     * @param oldAncestors 旧的父ID集合
     */
    public void updateDeptChildren(Long deptId, String newAncestors, String oldAncestors) {
        List<SysDept> children = deptMapper.selectChildrenDeptById(deptId);
        for (SysDept child : children) {
            child.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
        }
        if (children.size() > 0) {
            deptMapper.updateDeptChildren(children);
        }
    }

    /**
     * 删除部门管理信息
     *
     * @param deptId 部门ID
     * @return 结果
     */
    @Override
    public int deleteDeptById(Long deptId) {
        return deptMapper.deleteDeptById(deptId);
    }

    /*
    根据物料编码 查询deptId
     */
    @Override
    public SysDept selectByDeptCode(String deptCode) {
        return deptMapper.selectByDeptCode(deptCode);
    }

    /**
     * 递归列表
     */
    private void recursionFn(List<SysDept> list, SysDept t) {
        // 得到子节点列表
        List<SysDept> childList = getChildList(list, t);
        t.setChildren(childList);
        for (SysDept tChild : childList) {
            if (hasChild(list, tChild)) {
                recursionFn(list, tChild);
            }
        }
    }

    private void recursionFnRole(List<SysDeptRoleVo> list, SysDeptRoleVo t) {
        // 得到子节点列表
        List<SysDeptRoleVo> childList = getChildListRole(list, t);
        t.setChildren(childList);
        for (SysDeptRoleVo tChild : childList) {
            if (hasChildRole(list, tChild)) {
                recursionFnRole(list, tChild);
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<SysDept> getChildList(List<SysDept> list, SysDept t) {
        List<SysDept> tlist = new ArrayList<SysDept>();
        Iterator<SysDept> it = list.iterator();
        while (it.hasNext()) {
            SysDept n = (SysDept) it.next();
            if (StringUtils.isNotNull(n.getParentId()) && n.getParentId().longValue() == t.getDeptId().longValue()) {
                tlist.add(n);
            }
        }
        return tlist;
    }

    private List<SysDeptRoleVo> getChildListRole(List<SysDeptRoleVo> list, SysDeptRoleVo t) {
        List<SysDeptRoleVo> tlist = new ArrayList<SysDeptRoleVo>();
        Iterator<SysDeptRoleVo> it = list.iterator();
        while (it.hasNext()) {
            SysDeptRoleVo n = (SysDeptRoleVo) it.next();
            if (StringUtils.isNotNull(n.getParentId()) && n.getParentId().equals(t.getId())) {
                tlist.add(n);
            }
        }
        return tlist;
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<SysDept> list, SysDept t) {
        return getChildList(list, t).size() > 0 ? true : false;
    }

    private boolean hasChildRole(List<SysDeptRoleVo> list, SysDeptRoleVo t) {
        return getChildListRole(list, t).size() > 0 ? true : false;
    }

    @Override
    public List<SysDeptRoleVo> selectDeptRoleTreeList(SysDept sysDept) {
        if (sysDept.getOrgId() == null) {
            sysDept.setOrgId(SecurityUtils.getOrgId());
        }
        return deptMapper.selectDeptRoleTreeList(sysDept);
    }
}
