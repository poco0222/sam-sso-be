/**
 * @file 用户 DPR Mapper，一期仅保留部门维度的派生查询 SQL 声明
 * @author PopoY
 * @date 2026-03-26
 */
package com.yr.system.mapper;

import com.yr.common.core.domain.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户表 数据层
 *
 * @author Youngron
 */
@Mapper
public interface SysUserDprMapper {
    List<SysUser> listUserByDeptId(@Param("deptId") Long deptId);

    List<SysUser> listUserByDeptCode(@Param("deptCode") String deptCode);

    List<SysUser> listUserByDeptIdWithAncestors(@Param("deptId") Long deptId);

    List<SysUser> listUserByDeptIdWithDescenders(@Param("deptId") Long deptId);

    List<SysUser> listUserByDeptCodeWithAncestors(@Param("deptCode") String deptCode);

    List<SysUser> listUserByDeptCodeWithDescenders(@Param("deptCode") String deptCode);
}
