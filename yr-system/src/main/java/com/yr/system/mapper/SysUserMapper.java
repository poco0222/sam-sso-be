package com.yr.system.mapper;

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
     * 修改用户状态。
     *
     * @param user 用户状态信息
     * @return 结果
     */
    public int updateUserStatus(SysUser user);

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
    public int resetUserPwd(SysUser user);

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

    @Select(" select * from sys_user ")
    List<SysUser> selectUserListForExcel();

    List<SysUser> getChildDeptUser(String deptId);
}
