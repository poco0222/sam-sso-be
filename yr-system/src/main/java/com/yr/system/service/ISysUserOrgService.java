package com.yr.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.mybatisplus.service.ICustomService;
import com.yr.system.domain.entity.SysUserOrg;

import java.util.List;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-14 14:25
 * @description
 */
public interface ISysUserOrgService extends ICustomService<SysUserOrg> {

    /**
     * 根据用户ID分页查询用户组织关联数据
     *
     * @param pageNum
     * @param pageSize
     * @param userId
     * @return
     */
    IPage<SysUserOrg> pageByUserId(int pageNum, int pageSize, Long userId);

    /**
     * 新增用户组织关联信息
     *
     * @param sysUserOrg
     */
    void addSysUserOrg(SysUserOrg sysUserOrg);

    /**
     * 启用/禁用
     *
     * @param id     主键ID
     * @param enable 是否启用
     * @return
     */
    int changeEnabledById(Long id, Integer enable);

    /**
     * 设置用户的默认组织
     *
     * @param userId 用户ID
     * @param orgId  组织ID
     * @return 结果
     */
    int setDefaultUserOrg(Long userId, Long orgId);

    /**
     * 校验用户是否仍然拥有启用中的组织归属关系。
     *
     * @param userId 用户 ID
     * @param orgId 组织 ID
     * @return true 表示归属关系存在且启用
     */
    boolean hasEnabledOrgMembership(Long userId, Long orgId);

    /**
     * 校验用户是否仍然拥有“关系启用 + 组织启用”的可切换组织归属。
     *
     * @param userId 用户 ID
     * @param orgId 组织 ID
     * @return true 表示归属关系存在且目标组织处于启用态
     */
    boolean hasActiveOrgMembership(Long userId, Long orgId);

    /**
     * 获取当前登录用户所有组织信息
     *
     * @return
     */
    List<SysUserOrg> getCurrUserAllOrg();
}
