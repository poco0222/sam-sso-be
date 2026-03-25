/**
 * @file DISTRIBUTION 测试夹具
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysOrg;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.system.domain.dto.SsoIdentityImportSnapshot;
import com.yr.system.domain.entity.SysUserDept;
import com.yr.system.domain.entity.SysUserOrg;

import java.util.List;

/**
 * 提供 distribution 相关测试共享的最小快照样例。
 */
public final class SsoDistributionTestFixtures {

    private SsoDistributionTestFixtures() {
    }

    /**
     * 构造最小五类主数据快照。
     *
     * @return 快照
     */
    public static SsoIdentityImportSnapshot minimalSnapshot() {
        SysOrg org = new SysOrg();
        org.setOrgId(101L);
        org.setOrgCode("ORG-101");
        org.setOrgName("总部");

        SysDept dept = new SysDept();
        dept.setDeptId(201L);
        dept.setDeptCode("DEPT-201");
        dept.setDeptName("技术中心");
        dept.setOrgId(101L);
        dept.setDelFlag("0");

        SysUser user = new SysUser();
        user.setUserId(301L);
        user.setUserName("zhangsan");
        user.setNickName("张三");
        user.setDeptId(201L);
        user.setStatus("0");

        SysUserOrg userOrg = new SysUserOrg();
        userOrg.setId(9001L);
        userOrg.setUserId(301L);
        userOrg.setOrgId(101L);
        userOrg.setIsDefault(1);
        userOrg.setEnabled(1);

        SysUserDept userDept = new SysUserDept();
        userDept.setId(9002L);
        userDept.setUserId(301L);
        userDept.setDeptId(201L);
        userDept.setIsDefault(1);
        userDept.setEnabled(1);

        SsoIdentityImportSnapshot snapshot = new SsoIdentityImportSnapshot();
        snapshot.setOrgList(List.of(org));
        snapshot.setDeptList(List.of(dept));
        snapshot.setUserList(List.of(user));
        snapshot.setUserOrgRelationList(List.of(userOrg));
        snapshot.setUserDeptRelationList(List.of(userDept));
        return snapshot;
    }
}
