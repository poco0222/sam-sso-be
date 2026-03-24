package com.yr.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.mybatisplus.service.ICustomService;
import com.yr.system.domain.entity.SysUserDept;

import java.util.List;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-18 11:06
 * @description
 */
public interface ISysUserDeptService extends ICustomService<SysUserDept> {

    /**
     * 分页查询部门已分配的用户列表
     *
     * @param pageNum
     * @param pageSize
     * @param deptId
     * @param sysUser
     * @return
     */
    IPage<SysUser> pageDeptAssignUserByDeptId(int pageNum, int pageSize, Long deptId, SysUser sysUser);

    /**
     * 分页查询未分配部门的用户列表
     *
     * @param pageNum
     * @param pageSize
     * @param deptId
     * @param sysUser
     * @return
     */
    IPage<SysUser> pageDeptUnAssignUserByDeptId(int pageNum, int pageSize, Long deptId, SysUser sysUser);

    /**
     * 给部门分配用户
     *
     * @param deptId
     * @param userIds
     */
    void addAssignUserDept(Long deptId, Long[] userIds);

    /**
     * 从部门中移除用户
     *
     * @param ids
     */
    void removeAssignUserDeptByIds(Long[] ids);

    /**
     * 根据用户ID查询用户已分配的部门
     *
     * @param pageNum
     * @param pageSize
     * @param userId
     * @return
     */
    IPage<SysDept> pageDeptByUserId(int pageNum, int pageSize, Long userId);

    /**
     * 为用户添加部门
     *
     * @param userId
     * @param deptId
     */
    void addDeptForUser(Long userId, Long deptId);

    /**
     * 设置用户默认的部门
     *
     * @param userId
     * @param deptId
     */
    void setDefaultUserDept(Long userId, Long deptId);

    /**
     * 根据用户ID和组织ID查询部门信息
     *
     * @param userId
     * @param orgId
     * @param defaultDept 仅查询组织下的默认部门
     * @return
     */
    List<SysDept> selectDeptByUserIdAndOrgId(Long userId, Long orgId, Boolean defaultDept);





}
