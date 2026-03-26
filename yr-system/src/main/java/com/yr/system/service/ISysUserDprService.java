/**
 * @file 用户 DPR 查询接口，一期仅保留部门维度的批量查询能力
 * @author PopoY
 * @date 2026-03-26
 */
package com.yr.system.service;

import com.yr.common.core.domain.entity.SysUser;

import java.util.List;

/**
 * 用户 业务层
 *
 * @author Youngron
 */
public interface ISysUserDprService {

    List<SysUser> listUserByDeptId(Long deptId);

    List<SysUser> listUserByDeptCode(String deptCode);

    List<SysUser> listUserByDeptIdWithAncestors(Long deptId);

    List<SysUser> listUserByDeptIdWithDescenders(Long deptId);

    List<SysUser> listUserByDeptCodeWithAncestors(String deptCode);

    List<SysUser> listUserByDeptCodeWithDescenders(String deptCode);
}
