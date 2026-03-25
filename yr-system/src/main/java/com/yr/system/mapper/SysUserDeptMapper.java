/**
 * @file 用户部门关联 Mapper，负责部门分配查询与默认部门切换 SQL
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.mybatisplus.custommapper.CustomMapper;
import com.yr.system.domain.entity.SysUserDept;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-18 10:44
 * @description
 */

public interface SysUserDeptMapper extends CustomMapper<SysUserDept> {

    /**
     * 分页查询部门已分配的用户列表
     *
     * @param page
     * @param deptId
     * @param sysUser
     * @return
     */
    IPage<SysUser> pageDeptAssignUserByDeptId(Page<SysUserDept> page, @Param("deptId") Long deptId, @Param("param") SysUser sysUser);

    /**
     * 分页查询未分配部门的用户列表
     *
     * @param page
     * @param deptId
     * @param orgId
     * @param sysUser
     * @return
     */
    IPage<SysUser> pageDeptUnAssignUserByDeptId(Page<SysUserDept> page,
                                                @Param("deptId") Long deptId,
                                                @Param("orgId") Long orgId,
                                                @Param("param") SysUser sysUser);

    /**
     * 根据用户ID查询用户已分配的部门
     *
     * @param page
     * @param userId
     * @param orgId
     * @return
     */
    IPage<SysDept> pageDeptByUserId(Page<SysDept> page, @Param("userId") Long userId, @Param("orgId") Long orgId);

    /**
     * 查询组织下的默认部门
     *
     * @param userId
     * @param orgId
     * @return
     */
    Long selectDefaultUserDeptId(@Param("userId") Long userId, @Param("orgId") Long orgId);

    /**
     * 清空指定用户部门关联的默认标记。
     *
     * @param id 用户部门关联 ID
     * @return 受影响行数
     */
    int clearDefaultUserDept(@Param("id") Long id);

    /**
     * 根据用户ID和组织ID查询部门信息
     *
     * @param userId
     * @param orgId
     * @param defaultDept 仅查询组织下的默认部门
     * @return
     */
    List<SysDept> selectDeptByUserIdAndOrgId(@Param("userId") Long userId, @Param("orgId") Long orgId, @Param("defaultDept") String defaultDept);

    /*
    获取默认的部门id
     */
    SysUserDept selectByUserIdAndIsDefault(Long userId);

    /*
    根据用户id 获取所有的部门id
     */
    List<SysUserDept> selectByUserId(Long userId);

    List<SysUserDept> selectSysUserDeptByDeptId(Long deptId);

    /**
     * 通过部门(含子部门)获取用户
     */
    List<SysUserDept> selectAllUserByDeptId(Long deptId);

    /**
     * 通过username获取部门id
     */
    List<SysUserDept> getDeptByUserName(@Param("userName") String userName);

    /**
     * 通过部门(含子部门)获取用户_批量版
     */
    List<SysUserDept> batchSelectUserByDeptId(Long[] deptIds);

    /**
     * @CodingBy PopoY
     * @DateTime 2024/12/25 18:49
     * @Description 通过userid批量查询默认部门
     * @Param userIds userid数组
     * @Return java.util.List<com.yr.system.domain.entity.SysUserDept>
     */
    List<SysUserDept> selectByUserIdArrayAndIsDefault(Long[] userIds);

    /**
     * @CodingBy PopoY
     * @DateTime 2025/1/15 14:38
     * @Description 通过userName查询默认部门
     * @Param userNameArray 用户名数组
     * @Return java.util.List<com.yr.system.domain.entity.SysUserDept>
     */
    List<SysUserDept> getDeptByUserNameArray(String[] userNameArray);

    @Select(" select * from sys_user_dept where is_default = 1 ")
    List<SysUserDept> getDefaultDeptList();

    /**
     * 供 INIT_IMPORT 使用的显式插入，绕开无登录上下文时的 auto-fill 依赖。
     *
     * @param relation 用户部门关系
     * @return 受影响行数
     */
    int insertInitImport(@Param("userId") Long userId,
                         @Param("deptId") Long deptId,
                         @Param("isDefault") Integer isDefault,
                         @Param("enabled") Integer enabled,
                         @Param("operatorUserId") Long operatorUserId,
                         @Param("operateAt") Date operateAt);

    /**
     * 供 INIT_IMPORT 使用的显式更新，绕开无登录上下文时的 auto-fill 依赖。
     *
     * @param relation 用户部门关系
     * @return 受影响行数
     */
    int updateInitImport(@Param("id") Long id,
                         @Param("isDefault") Integer isDefault,
                         @Param("enabled") Integer enabled,
                         @Param("operatorUserId") Long operatorUserId,
                         @Param("operateAt") Date operateAt);
}
