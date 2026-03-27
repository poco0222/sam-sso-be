/**
 * @file Spring Security 用户明细加载服务
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.framework.web.service;

import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.enums.UserStatus;
import com.yr.common.exception.BaseException;
import com.yr.common.utils.StringUtils;
import com.yr.system.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户验证处理
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    /** 用户服务，用于读取当前登录用户。 */
    private final ISysUserService userService;

    /** 权限服务，用于构造登录态权限集合。 */
    private final SysPermissionService permissionService;

    /**
     * @param userService 用户服务
     * @param permissionService 权限服务
     */
    public UserDetailsServiceImpl(ISysUserService userService, SysPermissionService permissionService) {
        this.userService = userService;
        this.permissionService = permissionService;
    }

    public static List<SimpleGrantedAuthority> getActivitiAuthority(SysUser user) {
        Set<String> postCode = new HashSet<>(2);
        if (user.isAdmin()) {
            postCode.add("ROLE_ACTIVITI_ADMIN");
        }
        postCode.add("ROLE_ACTIVITI_USER");
        return postCode.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userService.selectUserByUserName(username, null);
        if (StringUtils.isNull(user)) {
            log.info("登录用户：{} 不存在.", username);
            throw new UsernameNotFoundException("账号或密码错误，请重新登录");
        } else if (UserStatus.DELETED.getCode().equals(user.getDelFlag())) {
            log.info("登录用户：{} 已被删除.", username);
            throw new BaseException("账号已被删除，请联系管理员");
        } else if (UserStatus.DISABLE.getCode().equals(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", username);
            throw new BaseException("账号已停用，请联系管理员");
        }

        return createLoginUser(user);
    }

    public UserDetails createLoginUser(SysUser user) {
        return new LoginUser(user, permissionService.getMenuPermission(user), permissionService.getRolePermission(user), getActivitiAuthority(user));
    }
}
