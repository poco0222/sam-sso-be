package com.yr.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.mybatisplus.service.ICustomService;
import com.yr.system.domain.entity.SysDuty;
import com.yr.system.domain.entity.SysUserDuty;

import java.util.List;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-23 18:32
 * @description
 */
public interface ISysUserDutyService extends ICustomService<SysUserDuty> {

    /**
     * 分页查询职务已关联的用户
     *
     * @param pageNum
     * @param pageSize
     * @param dutyId   职务ID
     * @param sysUser  用户查询条件
     * @return
     */
    IPage<SysUser> pageAssignUser(int pageNum, int pageSize, Long dutyId, SysUser sysUser);

    /**
     * 删除职务与用户的关联关系
     *
     * @param ids 关系表ID {@link SysUserDuty#id}
     * @return
     */
    void removeAssignUserDuty(Long[] ids);

    /**
     * 查询职务未关联的用户
     *
     * @param pageNum
     * @param pageSize
     * @param dutyId   职务ID
     * @param sysUser  用户查询条件
     * @return
     */
    IPage<SysUser> pageUnAssignUser(int pageNum, int pageSize, Long dutyId, SysUser sysUser);

    /**
     * 新增职务与用户的关联关系
     *
     * @param dutyId  职务ID
     * @param userIds 用户ID集合
     * @return
     */
    void addAssignUserDuty(Long dutyId, Long[] userIds);

    /**
     * 分页查询用户已分配的职务列表
     *
     * @param pageNum
     * @param pageSize
     * @param userId   用户ID
     * @return
     */
    IPage<SysDuty> pageAssignDuty(int pageNum, int pageSize, Long userId);

    /**
     * 根据用户ID和租户ID查询用户职务
     *
     * @param userId
     * @param orgId
     * @return
     */
    List<SysDuty> selectUserDutyListByUserIdAndOrgId(Long userId, Long orgId);
}
