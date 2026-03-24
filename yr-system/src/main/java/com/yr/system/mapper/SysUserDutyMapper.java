package com.yr.system.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.mybatisplus.custommapper.CustomMapper;
import com.yr.system.domain.entity.SysDuty;
import com.yr.system.domain.entity.SysUserDuty;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-23 18:32
 * @description
 */
public interface SysUserDutyMapper extends CustomMapper<SysUserDuty> {

    /**
     * 分页查询职务已关联的用户
     *
     * @param page
     * @param dutyId
     * @param sysUser
     * @return
     */
    IPage<SysUser> pageAssignUser(Page<SysUserDuty> page, @Param("dutyId") Long dutyId, @Param("param") SysUser sysUser);

    /**
     * 查询职务未关联的用户
     *
     * @param page
     * @param dutyId
     * @param orgId
     * @param sysUser
     * @return
     */
    IPage<SysUser> pageUnAssignUser(Page<SysUserDuty> page, @Param("dutyId") Long dutyId, @Param("orgId") Long orgId, @Param("param") SysUser sysUser);

    /**
     * 分页查询用户已分配的职务列表
     *
     * @param page
     * @param userId 用户ID
     * @param orgId  租户ID
     * @return
     */
    IPage<SysDuty> pageAssignDuty(Page<SysDuty> page, @Param("userId") Long userId, @Param("orgId") Long orgId);

    /**
     * 根据用户ID和租户ID查询用户职务
     *
     * @param userId
     * @param orgId
     * @return
     */
    List<SysDuty> selectUserDutyListByUserIdAndOrgId(@Param("userId") Long userId, @Param("orgId") Long orgId);
}
