/**
 * @file 用户服务接口，声明用户查询与维护契约
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service;

import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysUser;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 用户 业务层
 *
 * @author Youngron
 */
public interface ISysUserService {
    /**
     * 根据条件分页查询用户列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectUserList(SysUser user);

    List<SysUser> selectUserList2(SysUser user);

    /**
     * 根据条件分页查询用户列表，多组织查询接口
     *
     * @param user
     * @return
     */
    List<SysUser> selectUserListV2(SysUser user);

    /**
     * 通过用户名查询用户
     *
     * @param userName 用户名
     * @param orgId    组织ID，不传获取默认组织下的信息
     * @return 用户对象信息；未查询到时返回 null
     */
    @Nullable
    public SysUser selectUserByUserName(String userName, Long orgId);

    /**
     * 通过用户ID查询用户
     *
     * @param userId 用户ID
     * @param orgId  组织ID，不传获取默认组织下的信息
     * @return 用户对象信息；未查询到时返回 null
     */
    @Nullable
    public SysUser selectUserById(Long userId, Long orgId);

    /**
     * 查询用户所属部门
     *
     * @param userId
     * @param orgId
     * @return
     */
    List<SysDept> selectUserDeptListByUserIdAndOrgId(Long userId, Long orgId);

    /**
     * 校验用户名称是否唯一
     *
     * @param userName 用户名称
     * @return 结果
     */
    public String checkUserNameUnique(String userName);

    /**
     * 校验手机号码是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    public String checkPhoneUnique(SysUser user);

    /**
     * 校验email是否唯一
     *
     * @param user 用户信息
     * @return 结果
     */
    public String checkEmailUnique(SysUser user);

    /**
     * 校验用户是否允许操作
     *
     * @param user 用户信息
     */
    public void checkUserAllowed(SysUser user);

    /**
     * 新增用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    public int insertUser(SysUser user);

    /**
     * 修改用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    public int updateUser(SysUser user);

    /**
     * 用户授权角色
     *
     * @param userId  用户ID
     * @param roleIds 角色组
     */
    public void insertUserAuth(Long userId, Long[] roleIds);

    /**
     * 修改用户状态
     *
     * @param user 用户信息
     * @return 结果
     */
    public int updateUserStatus(SysUser user);

    /**
     * 修改用户基本信息
     *
     * @param user 用户信息
     * @return 结果
     */
    public int updateUserProfile(SysUser user);

    /**
     * 修改用户头像
     *
     * @param userName 用户名
     * @param avatar   头像地址
     * @return 结果
     */
    public boolean updateUserAvatar(String userName, String avatar);

    /**
     * 重置用户密码
     *
     * @param user 用户信息
     * @return 结果
     */
    public int resetPwd(SysUser user);

    /**
     * 重置用户密码
     *
     * @param userName 用户名
     * @param password 密码
     * @return 结果
     */
    public int resetUserPwd(String userName, String password);

    /**
     * 批量删除用户信息
     *
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    public int deleteUserByIds(Long[] userIds);

    /**
     * 导入用户数据
     *
     * @param userList        用户数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    public String importUser(List<SysUser> userList, Boolean isUpdateSupport, String operName);

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId
     * @return
     */
    SysUser getUserById(Long userId);

    List<SysUser> selectSysUserById(String deptCode);

    List<SysUser> selectUserByDeptCode(String[] deptId);

    /**
     * 通过部门(含子部门)获取用户
     */
    List<SysUser> selectUserByDeptId(Long deptId);

    /**
     * 通过部门(含子部门)获取用户_批量版
     */
    Map<Long, List<SysUser>> batchSelectUserByDeptId(Long[] deptIds);
}
