/**
 * @file 用户服务实现，负责一期用户查询、导入与写入委托
 * @author PopoY
 * @date 2026-03-26
 */
package com.yr.system.service.impl;

import com.yr.common.constant.UserConstants;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysOrg;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.exception.CustomException;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.domain.entity.SysUserDept;
import com.yr.system.mapper.SysUserMapper;
import com.yr.system.service.ISysOrgService;
import com.yr.system.service.ISysUserDeptService;
import com.yr.system.service.ISysUserService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 用户 业务层处理
 *
 * @author Youngron
 */
@Service
public class SysUserServiceImpl implements ISysUserService {
    private static final Logger log = LoggerFactory.getLogger(SysUserServiceImpl.class);

    private final SysUserMapper userMapper;

    private final ISysUserDeptService sysUserDeptService;

    private final ISysOrgService sysOrgService;

    private final SysUserWriteService sysUserWriteService;

    private final SysUserImportService sysUserImportService;

    private final SysUserQueryService sysUserQueryService;

    /**
     * 使用单一构造器显式声明依赖，避免字段注入带来的隐藏耦合。
     *
     * @param userMapper 用户 Mapper
     * @param sysUserDeptService 用户部门服务
     * @param sysOrgService 组织服务
     * @param sysUserWriteService 用户写入服务
     * @param sysUserImportService 用户导入服务
     * @param sysUserQueryService 用户查询服务
     */
    public SysUserServiceImpl(SysUserMapper userMapper,
                              ISysUserDeptService sysUserDeptService,
                              ISysOrgService sysOrgService,
                              SysUserWriteService sysUserWriteService,
                              SysUserImportService sysUserImportService,
                              SysUserQueryService sysUserQueryService) {
        this.userMapper = userMapper;
        this.sysUserDeptService = sysUserDeptService;
        this.sysOrgService = sysOrgService;
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
        return sysUser;
    }

    @Override
    public List<SysDept> selectUserDeptListByUserIdAndOrgId(Long userId, Long orgId) {
        return sysUserDeptService.selectDeptByUserIdAndOrgId(userId, orgId, false);
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
     * 修改用户状态
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserStatus(SysUser user) {
        if (user == null || user.getUserId() == null) {
            throw new CustomException("用户ID不能为空");
        }
        if (StringUtils.isBlank(user.getStatus())) {
            throw new CustomException("用户状态不能为空");
        }
        return userMapper.updateUserStatus(user);
    }

    /**
     * 修改当前登录用户的自助资料字段。
     *
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserProfile(SysUser user) {
        return sysUserWriteService.updateUserProfile(user);
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
        return resetUserPwd(user);
    }

    /**
     * 重置用户密码
     *
     * @param userName 用户名
     * @param password 密码
     * @return 结果
     */
    @Override
    public int resetUserPwd(SysUser user) {
        if (user == null || user.getUserId() == null) {
            throw new CustomException("用户ID不能为空");
        }
        if (StringUtils.isBlank(user.getPassword())) {
            throw new CustomException("密码不能为空");
        }
        if (StringUtils.isBlank(user.getFirstLogin())) {
            user.setFirstLogin("0");
        }
        return userMapper.resetUserPwd(user);
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
    public SysUser getUserById(Long userId) {
        return sysUserQueryService.getUserById(userId);
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

}
