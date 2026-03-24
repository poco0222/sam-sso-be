package com.yr.system.mapper;

import com.yr.common.core.domain.entity.SysRole;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色表 数据层
 *
 * @author Youngron
 */
public interface SysRoleMapper {
    /**
     * 根据条件分页查询角色数据
     *
     * @param role 角色信息
     * @return 角色数据集合信息
     */
    public List<SysRole> selectRoleList(SysRole role);

    /**
     * 根据条件分页查询角色数据
     *
     * @param role 角色信息
     * @return
     */
    List<SysRole> selectRoleListV2(SysRole role);

    /**
     * 根据用户ID查询角色
     *
     * @param userId 用户ID
     * @param orgId  组织ID
     * @return 角色列表
     */
    public List<SysRole> selectRolePermissionByUserId(@Param("userId") Long userId, @Param("orgId") Long orgId);

    /**
     * 查询所有角色
     *
     * @return 角色列表
     */
    public List<SysRole> selectRoleAll();

    /**
     * 根据用户ID获取角色选择框列表
     *
     * @param userId 用户ID
     * @return 选中角色ID列表
     */
    public List<Integer> selectRoleListByUserId(Long userId);

    /**
     * 通过角色ID查询角色
     *
     * @param roleId 角色ID
     * @return 角色对象信息
     */
    public SysRole selectRoleById(Long roleId);

    /**
     * 通过角色ID查询角色
     *
     * @param roleId 角色ID
     * @return 角色对象信息
     */
    SysRole selectRoleByIdV2(Long roleId);

    /**
     * 根据用户ID查询角色
     *
     * @param userName 用户名
     * @return 角色列表
     */
    public List<SysRole> selectRolesByUserName(String userName);

    /**
     * 校验角色名称是否唯一
     *
     * @param roleName 角色名称
     * @return 角色信息
     */
    public SysRole checkRoleNameUnique(String roleName);

    /**
     * 检查角色名称是否唯一
     *
     * @param roleName
     * @param orgId
     * @return
     */
    SysRole checkRoleNameUniqueV2(@Param("roleName") String roleName, @Param("orgId") Long orgId);

    /**
     * 校验角色权限是否唯一
     *
     * @param roleKey 角色权限
     * @return 角色信息
     */
    public SysRole checkRoleKeyUnique(String roleKey);

    /**
     * 校验角色权限是否唯一
     *
     * @param roleKey
     * @param orgId
     * @return
     */
    SysRole checkRoleKeyUniqueV2(@Param("roleKey") String roleKey, @Param("orgId") Long orgId);

    /**
     * 修改角色信息
     *
     * @param role 角色信息
     * @return 结果
     */
    public int updateRole(SysRole role);

    /**
     * 新增角色信息
     *
     * @param role 角色信息
     * @return 结果
     */
    public int insertRole(SysRole role);

    /**
     * 通过角色ID删除角色
     *
     * @param roleId 角色ID
     * @return 结果
     */
    public int deleteRoleById(Long roleId);

    /**
     * 批量删除角色信息
     *
     * @param roleIds 需要删除的角色ID
     * @return 结果
     */
    public int deleteRoleByIds(Long[] roleIds);

    /**
     * 根据用户ID查询角色信息
     *
     * @param userId 用户ID
     * @param orgId  组织ID
     * @return
     */
    List<SysRole> selectRoleByUserId(@Param("userId") Long userId, @Param("orgId") Long orgId);

    @Select(" select * from sys_role where role_key = #{roleKey} ")
    SysRole selectRoleByRoleKey(String roleKey);
}
