package com.yr.system.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yr.common.core.domain.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户表 数据层
 *
 * @author Youngron
 */
@Repository
public interface SysUserMapper {
    /**
     * 根据条件分页查询用户列表
     *
     * @param sysUser 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectUserList(SysUser sysUser);

    List<SysUser> selectUserList2(SysUser sysUser);

    /**
     * 未删除,状态正常,存在部门,存在号码的用户
     */
    List<SysUser> selectAllUserList(SysUser sysUser);

    /**
     * 根据条件分页查询用户列表，多组织查询接口
     *
     * @param sysUser
     * @return
     */
    List<SysUser> selectUserListV2(SysUser sysUser);

    /**
     * 根据条件分页查询未已配用户角色列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectAllocatedList(SysUser user);

    /**
     * 根据条件分页查询未分配用户角色列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    public List<SysUser> selectUnallocatedList(SysUser user);

    /**
     * 通过用户名查询用户
     *
     * @param userName 用户名
     * @return 用户对象信息
     */
    public SysUser selectUserByUserName(String userName);

    /**
     * 根据用户ID查询用户信息
     *
     * @param userId
     * @return
     */
    SysUser selectUserByUserId(@Param("userId") Long userId);

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
     * 修改用户头像
     *
     * @param userName 用户名
     * @param avatar   头像地址
     * @return 结果
     */
    public int updateUserAvatar(@Param("userName") String userName, @Param("avatar") String avatar);

    /**
     * 重置用户密码
     *
     * @param userName 用户名
     * @param password 密码
     * @return 结果
     */
    public int resetUserPwd(@Param("userName") String userName, @Param("password") String password);

    /**
     * 通过用户ID删除用户
     *
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteUserById(Long userId);

    /**
     * 批量删除用户信息
     *
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    public int deleteUserByIds(Long[] userIds);

    /**
     * 校验用户名称是否唯一
     *
     * @param userName 用户名称
     * @return 结果
     */
    public int checkUserNameUnique(String userName);

    /**
     * 校验手机号码是否唯一
     *
     * @param phonenumber 手机号码
     * @return 结果
     */
    public SysUser checkPhoneUnique(String phonenumber);

    /**
     * 校验email是否唯一
     *
     * @param email 用户邮箱
     * @return 结果
     */
    public SysUser checkEmailUnique(String email);

    /**
     * 查询岗位已分配的用户列表
     *
     * @param postId
     * @param sysUser
     * @return
     */
    List<SysUser> listPostAssignUserByPostId(@Param("postId") Long postId,
                                             @Param("param") SysUser sysUser);

    /**
     * 查询岗位未分配的用户列表
     *
     * @param postId
     * @param sysUser
     * @return
     */
    List<SysUser> listUnAssignUserByPostId(@Param("postId") Long postId,
                                           @Param("param") SysUser sysUser);

    /**
     * 查询岗位未分配的用户列表
     *
     * @param page
     * @param sysUser
     * @return
     */
    IPage<SysUser> queryModeUserGroupInformationCollection(Page page,
                                                           @Param("param") SysUser sysUser);


    public List<String> selectUserNameByPostCodeAndDeptId(String postCode, Long deptId);

    public SysUser selectSysUserById(String userName);

    List<SysUser> selectUserListByUserName(String[] userName);

    List<SysUser> selectUserListByAccount(String[] accountList);

    SysUser selectSysUserByUserId(Long userId);

    List<SysUser> selectUserByDeptCode(@Param("deptId") String[] deptId);

    List<SysUser> selectSysUserByUserIds(Long[] userIds);

    /**
     * 通过单个部门编码批量查询部门下的启用用户。
     *
     * @param deptCode 部门编码
     * @return 用户列表
     */
    List<SysUser> selectSysUsersByDeptCode(@Param("deptCode") String deptCode);

    SysUser selectSysUserByUserNameAndStatusAndDelFlag(String userName);

    @Select(" select user_name as dict_value, nick_name as dict_label from sys_user")
    List<SysUser> getAllUserForOptions();

    List<SysUser> selectUserListByDeptRole(SysUser sysUser);

    /**
     * @CodingBy PopoY
     * @DateTime 2024/12/27 16:09
     * @Description 通过部门和岗位查询用户_手动查询流程角色
     * @Param deptId 部门id
     * @Param postId 岗位id
     * @Return java.util.List<com.yr.common.core.domain.entity.SysUser>
     */
    List<SysUser> selectUserByDeptAndPost(@Param("deptId") Long deptId, @Param("postId") Long postId);

    @Select(" select * from sys_user ")
    List<SysUser> selectUserListForExcel();

    /**
     * @CodingBy PopoY
     * @DateTime 2025/2/23 19:08
     * @Description 通过角色编号批量查询用户
     * @Param roleKeys 角色编号数组
     * @Return java.util.List<com.yr.common.core.domain.entity.SysUser>
     */
    List<SysUser> selectUserRoleListByRoleKeysBatch(String[] roleKeys);

    List<SysUser> getChildDeptUser(String deptId);

    /**
     * 查询技术中心下所有在职用户
     */
    List<SysUser> selectTechCenterActiveUsers();
}
