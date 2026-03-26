/**
 * @file 一期权限服务，提供固定角色与固定权限集合
 * @author PopoY
 * @date 2026-03-26
 */
package com.yr.framework.web.service;

import com.yr.common.core.domain.entity.SysUser;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 用户权限处理。
 */
@Component
public class SysPermissionService {

    /** 一期固定角色集合。 */
    private static final Set<String> PHASE_ONE_DEFAULT_ROLES = Set.of("ROLE_DEFAULT");

    /** 一期固定权限集合。 */
    private static final Set<String> PHASE_ONE_MENU_PERMISSIONS = Set.of(
            "monitor:logininfor:list",
            "monitor:logininfor:export",
            "monitor:logininfor:remove",
            "sso:client:list",
            "sso:client:add",
            "sso:client:edit",
            "sso:sync-task:list",
            "sso:sync-task:add",
            "sso:sync-task:edit",
            "sso:sync-task:query",
            "system:dept:list",
            "system:dept:query",
            "system:dept:add",
            "system:dept:edit",
            "system:dept:remove",
            "system:org:list",
            "system:org:export",
            "system:org:query",
            "system:org:add",
            "system:org:edit",
            "system:org:remove",
            "system:user:list",
            "system:user:query",
            "system:user:add",
            "system:user:edit",
            "system:user:remove",
            "system:user:export",
            "system:user:import",
            "system:user:resetPwd",
            "system:user:unlock",
            "system:userDept:addDept",
            "system:userDept:addUser",
            "system:userOrg:addOrg"
    );

    /**
     * 获取角色数据权限。
     *
     * @param user 用户信息
     * @return 角色权限信息
     */
    public Set<String> getRolePermission(SysUser user) {
        Set<String> roles = new LinkedHashSet<>();
        if (user.isAdmin()) {
            roles.add("admin");
        } else {
            roles.addAll(PHASE_ONE_DEFAULT_ROLES);
        }
        return roles;
    }

    /**
     * 获取菜单数据权限。
     *
     * @param user 用户信息
     * @return 菜单权限信息
     */
    public Set<String> getMenuPermission(SysUser user) {
        Set<String> perms = new LinkedHashSet<>();
        if (user.isAdmin()) {
            perms.add("*:*:*");
        } else {
            perms.addAll(PHASE_ONE_MENU_PERMISSIONS);
        }
        return perms;
    }
}
