/**
 * @file 用户服务实现，负责查询、导入与委托写入事务
 * @author PopoY
 * @date 2026-03-16
 */
/**
 * @file 用户服务实现，补齐角色分配查询的数据范围契约
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.annotation.DataScope;
import com.yr.common.constant.UserConstants;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysOrg;
import com.yr.common.core.domain.entity.SysRole;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.page.PageDomain;
import com.yr.common.exception.CustomException;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.domain.SysPost;
import com.yr.system.domain.SysUserRole;
import com.yr.system.domain.entity.*;
import com.yr.system.mapper.*;
import com.yr.system.service.*;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户 业务层处理
 *
 * @author Youngron
 */
@Service
public class SysUserServiceImpl implements ISysUserService {
    private static final Logger log = LoggerFactory.getLogger(SysUserServiceImpl.class);

    private final SysUserMapper userMapper;

    private final SysRoleMapper roleMapper;

    private final SysPostMapper postMapper;

    private final SysUserRoleMapper userRoleMapper;

    private final SysUserPostMapper userPostMapper;

    private final ISysUserDeptService sysUserDeptService;

    private final ISysUserRankService sysUserRankService;

    private final ISysOrgService sysOrgService;

    private final ISysUserDutyService sysUserDutyService;

    private final SysUserWriteService sysUserWriteService;

    private final SysUserImportService sysUserImportService;

    private final SysUserQueryService sysUserQueryService;

    /**
     * 使用单一构造器显式声明依赖，避免字段注入带来的隐藏耦合。
     *
     * @param userMapper 用户 Mapper
     * @param roleMapper 角色 Mapper
     * @param postMapper 岗位 Mapper
     * @param userRoleMapper 用户角色 Mapper
     * @param userPostMapper 用户岗位 Mapper
     * @param sysUserDeptService 用户部门服务
     * @param sysUserRankService 用户职级服务
     * @param sysOrgService 组织服务
     * @param sysUserDutyService 用户职务服务
     * @param sysUserWriteService 用户写入服务
     * @param sysUserImportService 用户导入服务
     * @param sysUserQueryService 用户查询服务
     */
    public SysUserServiceImpl(SysUserMapper userMapper,
                              SysRoleMapper roleMapper,
                              SysPostMapper postMapper,
                              SysUserRoleMapper userRoleMapper,
                              SysUserPostMapper userPostMapper,
                              ISysUserDeptService sysUserDeptService,
                              ISysUserRankService sysUserRankService,
                              ISysOrgService sysOrgService,
                              ISysUserDutyService sysUserDutyService,
                              SysUserWriteService sysUserWriteService,
                              SysUserImportService sysUserImportService,
                              SysUserQueryService sysUserQueryService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.postMapper = postMapper;
        this.userRoleMapper = userRoleMapper;
        this.userPostMapper = userPostMapper;
        this.sysUserDeptService = sysUserDeptService;
        this.sysUserRankService = sysUserRankService;
        this.sysOrgService = sysOrgService;
        this.sysUserDutyService = sysUserDutyService;
        this.sysUserWriteService = sysUserWriteService;
        this.sysUserImportService = sysUserImportService;
        this.sysUserQueryService = sysUserQueryService;
    }


    /**
     * 根据条件分页查询用户列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysUser> selectUserList(SysUser user) {
        return userMapper.selectUserList(user);
    }

    @Override
    public List<SysUser> selectUserList2(SysUser user) {
        return userMapper.selectUserList2(user);
    }

    @Override
    public List<SysUser> selectUserListV2(SysUser user) {
        return userMapper.selectUserListV2(user);
    }

    /**
     * 根据条件分页查询已分配用户角色列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysUser> selectAllocatedList(SysUser user) {
        return userMapper.selectAllocatedList(user);
    }

    /**
     * 根据条件分页查询未分配用户角色列表
     *
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysUser> selectUnallocatedList(SysUser user) {
        if (user.getOrgId() == null) {
            user.setOrgId(SecurityUtils.getOrgId());
        }
        return userMapper.selectUnallocatedList(user);
    }

    /**
     * 通过用户名查询用户
     *
     * @param userName 用户名
     * @param orgId    组织ID，不传获取默认组织下的信息
     * @return 用户对象信息；未查询到时返回 null
     */
    @Override
    @Nullable
    public SysUser selectUserByUserName(String userName, Long orgId) {
        return this.getSysUserByNameOrId(userName, null, orgId);
    }

    /**
     * 通过用户ID查询用户
     *
     * @param userId 用户ID
     * @param orgId  组织ID，不传获取默认组织下的信息
     * @return 用户对象信息；未查询到时返回 null
     */
    @Override
    @Nullable
    public SysUser selectUserById(Long userId, Long orgId) {
        return this.getSysUserByNameOrId(null, userId, orgId);
    }

    /**
     * 统一封装按用户名或用户 ID 查询用户的可空契约。
     *
     * @param userName 用户名
     * @param userId 用户 ID
     * @param orgId 组织 ID
     * @return 用户对象；未命中时返回 null
     */
    @Nullable
    private SysUser getSysUserByNameOrId(String userName, Long userId, Long orgId) {
        SysUser sysUser;
        if (StringUtils.isNotBlank(userName)) {
            sysUser = userMapper.selectUserByUserName(userName);
        } else {
            sysUser = userMapper.selectUserByUserId(userId);
        }
        if (sysUser == null) {
            return null;
        }
        // 如果传了组织ID，那么重新查询组织信息
        if (orgId != null) {
            SysOrg sysOrg = sysOrgService.selectSysOrgById(orgId);
            if (sysOrg == null) {
                throw new CustomException("未查询到组织信息");
            }
            sysUser.setOrgId(sysOrg.getOrgId());
            sysUser.setOrgName(sysOrg.getOrgName());
        }
        // 查询部门信息
        List<SysDept> sysDeptList = sysUserDeptService.selectDeptByUserIdAndOrgId(sysUser.getUserId(), sysUser.getOrgId(), true);
        if (CollectionUtils.isNotEmpty(sysDeptList)) {
            SysDept sysDept = sysDeptList.get(0);
            sysUser.setDeptId(sysDept.getDeptId());
            sysUser.setDept(sysDept);
            sysUser.setUserDeptId(sysDept.getUserDeptId());
            sysUser.setIsUserDefaultDept(1);
        }
        // 查询角色信息
        List<SysRole> roleList = roleMapper.selectRoleByUserId(sysUser.getUserId(), sysUser.getOrgId());
        if (CollectionUtils.isNotEmpty(roleList)) {
            sysUser.setRoles(roleList);
        }
        return sysUser;
    }

    /**
     * 查询用户所属角色组
     *
     * @param userName 用户名
     * @return 结果
     */
    @Override
    public String selectUserRoleGroup(String userName) {
        List<SysRole> list = roleMapper.selectRolesByUserName(userName);
        StringJoiner roleNameJoiner = new StringJoiner(",");
        for (SysRole role : list) {
            roleNameJoiner.add(role.getRoleName() == null ? "" : role.getRoleName());
        }
        return roleNameJoiner.toString();
    }

    /**
     * 查询用户所属岗位组
     *
     * @param userId 用户名
     * @return 结果
     */
    @Override
    public List<SysPost> selectUserPostGroup(Long userId, Long orgId) {
        return postMapper.selectPostsByUserId(userId, orgId);
    }

    @Override
    public List<SysDept> selectUserDeptListByUserIdAndOrgId(Long userId, Long orgId) {
        return sysUserDeptService.selectDeptByUserIdAndOrgId(userId, orgId, false);
    }

    @Override
    public List<SysDuty> selectUserDutyListByUserIdAndOrgId(Long userId, Long orgId) {
        return sysUserDutyService.selectUserDutyListByUserIdAndOrgId(userId, orgId);
    }

    @Override
    public SysRank selectUserRankByUserIdAndOrgId(Long userId, Long orgId) {
        return sysUserRankService.selectUserRankByUserIdAndOrgId(userId, orgId);
    }


    /**
     * 校验用户名称是否唯一
     *
     * @param userName 用户名称
     * @return 结果
     */
    @Override
    public String checkUserNameUnique(String userName) {
        int count = userMapper.checkUserNameUnique(userName);
        if (count > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验用户名称是否唯一
     *
     * @param user 用户信息
     * @return
     */
    @Override
    public String checkPhoneUnique(SysUser user) {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkPhoneUnique(user.getPhonenumber());
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验email是否唯一
     *
     * @param user 用户信息
     * @return
     */
    @Override
    public String checkEmailUnique(SysUser user) {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkEmailUnique(user.getEmail());
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验用户是否允许操作
     *
     * @param user 用户信息
     */
    @Override
    public void checkUserAllowed(SysUser user) {
        if (StringUtils.isNotNull(user.getUserId()) && user.isAdmin()) {
            throw new CustomException("不允许操作超级管理员用户");
        }
    }

    /**
     * 新增保存用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int insertUser(SysUser user) {
        return sysUserWriteService.insertUser(user);
    }

    /**
     * 修改保存用户信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUser(SysUser user) {
        return sysUserWriteService.updateUser(user);
    }

    /**
     * 用户授权角色
     *
     * @param userId  用户ID
     * @param roleIds 角色组
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertUserAuth(Long userId, Long[] roleIds) {
        List<SysRole> roleList = roleMapper.selectRoleByUserId(userId, SecurityUtils.getOrgId());
        if (CollectionUtils.isNotEmpty(roleList)) {
            userRoleMapper.deleteUserRoleByUserId(userId, roleList.stream().map(SysRole::getRoleId).toArray(Long[]::new));
        }
        insertUserRole(userId, roleIds);
    }

    /**
     * 修改用户状态
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserStatus(SysUser user) {
        return userMapper.updateUser(user);
    }

    /**
     * 修改用户基本信息
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserProfile(SysUser user) {
        return userMapper.updateUser(user);
    }

    /**
     * 修改用户头像
     *
     * @param userName 用户名
     * @param avatar   头像地址
     * @return 结果
     */
    @Override
    public boolean updateUserAvatar(String userName, String avatar) {
        return userMapper.updateUserAvatar(userName, avatar) > 0;
    }

    /**
     * 重置用户密码
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int resetPwd(SysUser user) {
        return userMapper.updateUser(user);
    }

    /**
     * 重置用户密码
     *
     * @param userName 用户名
     * @param password 密码
     * @return 结果
     */
    @Override
    public int resetUserPwd(String userName, String password) {
        return userMapper.resetUserPwd(userName, password);
    }

    /**
     * 新增用户角色信息
     *
     * @param userId  用户ID
     * @param roleIds 角色组
     */
    public void insertUserRole(Long userId, Long[] roleIds) {
        if (StringUtils.isNotNull(roleIds)) {
            // 新增用户与角色管理
            List<SysUserRole> list = new ArrayList<SysUserRole>();
            for (Long roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                list.add(ur);
            }
            if (list.size() > 0) {
                userRoleMapper.batchUserRole(list);
            }
        }
    }

    /**
     * 批量删除用户信息
     *
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteUserByIds(Long[] userIds) {
        for (Long userId : userIds) {
            checkUserAllowed(new SysUser(userId));
        }
        // 删除用户与角色关联
        userRoleMapper.deleteUserRole(userIds);
        // 删除用户与岗位关联
        userPostMapper.deleteUserPost(userIds);
        return userMapper.deleteUserByIds(userIds);
    }

    /**
     * 导入用户数据
     *
     * @param userList        用户数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importUser(List<SysUser> userList, Boolean isUpdateSupport, String operName) {
        return sysUserImportService.importUser(userList, isUpdateSupport, operName);
    }

    @Override
    public List<SysUser> listPostAssignUserByPostId(Long postId, SysUser sysUser) {
        return userMapper.listPostAssignUserByPostId(postId, sysUser);
    }

    @Override
    public List<SysUser> listUnAssignUserByPostId(Long postId, SysUser sysUser) {
        return userMapper.listUnAssignUserByPostId(postId, sysUser);
    }

    @Override
    public SysUser getUserById(Long userId) {
        return sysUserQueryService.getUserById(userId);
    }

    @Override
    public IPage<SysUser> queryModeUserGroupInformationCollection(PageDomain pageDomain, SysUser sysUser) {
        return sysUserQueryService.queryModeUserGroupInformationCollection(pageDomain, sysUser);
    }


    /**
     * 通过部门查询用户
     *
     * @param deptId 部门对象
     * @return List<SysUser>
     */
    @Override
    public List<SysUser> selectUserByDeptCode(String[] deptId) {
        return sysUserQueryService.selectUserByDeptCode(deptId);
    }

    @Override
    public List<SysUser> selectSysUserById(String deptCode) {
        return sysUserQueryService.selectSysUserById(deptCode);
    }

    /**
     * 通过部门(含子部门)获取用户
     */
    @Override
    public List<SysUser> selectUserByDeptId(Long deptId) {
        return sysUserQueryService.selectUserByDeptId(deptId);
    }

    /**
     * 通过部门(含子部门)获取用户_批量版
     */
    @Override
    public Map<Long, List<SysUser>> batchSelectUserByDeptId(Long[] deptIds) {
        return sysUserQueryService.batchSelectUserByDeptId(deptIds);
    }

    @Override
    public List<SysUser> selectUserListByDeptRole(SysUser sysUser){
        return sysUserQueryService.selectUserListByDeptRole(sysUser);
    }

}
