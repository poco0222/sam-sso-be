/**
 * @file 用户组织关联 Mapper，负责分页查询与默认组织切换 SQL
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yr.common.mybatisplus.custommapper.CustomMapper;
import com.yr.system.domain.entity.SysUserOrg;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-14 14:17
 * @description
 */

public interface SysUserOrgMapper extends CustomMapper<SysUserOrg> {

    /**
     * 分页查询用户组织关联数据
     *
     * @param pageParam
     * @param param
     * @return
     */
    IPage<SysUserOrg> pageSysUserOrg(Page<SysUserOrg> pageParam, @Param("param") SysUserOrg param);

    /**
     * 清空用户当前默认组织标记。
     *
     * @param userId 用户 ID
     * @return 受影响行数
     */
    int clearDefaultUserOrg(@Param("userId") Long userId);

    /**
     * 为指定用户和组织设置默认组织标记。
     *
     * @param userId 用户 ID
     * @param orgId 组织 ID
     * @return 受影响行数
     */
    int setDefaultUserOrg(@Param("userId") Long userId, @Param("orgId") Long orgId);

    /**
     * 供 INIT_IMPORT 使用的显式插入，绕开无登录上下文时的 auto-fill 依赖。
     *
     * @param relation 用户组织关系
     * @return 受影响行数
     */
    int insertInitImport(@Param("userId") Long userId,
                         @Param("orgId") Long orgId,
                         @Param("isDefault") Integer isDefault,
                         @Param("enabled") Integer enabled,
                         @Param("operatorUserId") Long operatorUserId,
                         @Param("operateAt") Date operateAt);

    /**
     * 供 INIT_IMPORT 使用的显式更新，绕开无登录上下文时的 auto-fill 依赖。
     *
     * @param relation 用户组织关系
     * @return 受影响行数
     */
    int updateInitImport(@Param("id") Long id,
                         @Param("isDefault") Integer isDefault,
                         @Param("enabled") Integer enabled,
                         @Param("operatorUserId") Long operatorUserId,
                         @Param("operateAt") Date operateAt);
}
