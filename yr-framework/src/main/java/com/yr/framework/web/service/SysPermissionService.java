package com.yr.framework.web.service;

import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.utils.SecurityUtils;
import com.yr.system.service.ISysMenuService;
import com.yr.system.service.ISysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 用户权限处理
 *
 * @author Youngron
 */
@Component
public class SysPermissionService {
    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysMenuService menuService;

    /**
     * 获取角色数据权限
     *
     * @param user 用户信息
     * @return 角色权限信息
     */
    public Set<String> getRolePermission(SysUser user) {
        Set<String> roles = new HashSet<String>();
        // 管理员拥有所有权限
        if (user.isAdmin()) {
            roles.add("admin");
        } else {
            if (user.getOrgId() == null) {
                user.setOrgId(SecurityUtils.getOrgId());
            }
            roles.addAll(roleService.selectRolePermissionByUserId(user.getUserId(), user.getOrgId()));
        }
        return roles;
    }

    /**
     * 获取菜单数据权限
     *
     * @param user 用户信息
     * @return 菜单权限信息
     */
    public Set<String> getMenuPermission(SysUser user) {
        Set<String> perms = new HashSet<String>();
        // 管理员拥有所有权限
        if (user.isAdmin()) {
            perms.add("*:*:*");
        } else {
            if (user.getOrgId() == null) {
                user.setOrgId(SecurityUtils.getOrgId());
            }
            perms.addAll(menuService.selectMenuPermsByUserId(user.getUserId(), user.getOrgId()));
        }
        return perms;
    }
}
