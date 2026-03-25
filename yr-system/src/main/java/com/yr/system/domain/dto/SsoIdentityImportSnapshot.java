/**
 * @file INIT_IMPORT 身份快照 DTO
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.domain.dto;

import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysOrg;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.system.domain.entity.SysUserDept;
import com.yr.system.domain.entity.SysUserOrg;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 承载从 legacy source 读取出来的身份主数据快照。
 */
@Data
public class SsoIdentityImportSnapshot {

    /** 组织快照。 */
    private List<SysOrg> orgList = new ArrayList<>();

    /** 部门快照。 */
    private List<SysDept> deptList = new ArrayList<>();

    /** 用户快照。 */
    private List<SysUser> userList = new ArrayList<>();

    /** 用户组织关联快照。 */
    private List<SysUserOrg> userOrgRelationList = new ArrayList<>();

    /** 用户部门关联快照。 */
    private List<SysUserDept> userDeptRelationList = new ArrayList<>();
}
