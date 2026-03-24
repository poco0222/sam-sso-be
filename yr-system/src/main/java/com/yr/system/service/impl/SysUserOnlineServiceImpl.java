/**
 * @file 在线用户服务实现，负责把 LoginUser 映射为在线会话视图
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.utils.StringUtils;
import com.yr.system.domain.SysUserOnline;
import com.yr.system.service.ISysUserOnlineService;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * 在线用户 服务层处理
 *
 * @author Youngron
 */
@Service
public class SysUserOnlineServiceImpl implements ISysUserOnlineService {
    /**
     * 通过登录地址查询信息
     *
     * @param ipaddr 登录地址
     * @param user   用户信息
     * @return 在线用户信息；未命中时返回 null
     */
    @Override
    @Nullable
    public SysUserOnline selectOnlineByIpaddr(String ipaddr, LoginUser user) {
        if (StringUtils.isNull(user)) {
            return null;
        }
        if (StringUtils.equals(ipaddr, user.getIpaddr())) {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 通过用户名称查询信息
     *
     * @param userName 用户名称
     * @param user     用户信息
     * @return 在线用户信息；未命中时返回 null
     */
    @Override
    @Nullable
    public SysUserOnline selectOnlineByUserName(String userName, LoginUser user) {
        if (StringUtils.isNull(user)) {
            return null;
        }
        if (StringUtils.equals(userName, user.getUsername())) {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 通过登录地址/用户名称查询信息
     *
     * @param ipaddr   登录地址
     * @param userName 用户名称
     * @param user     用户信息
     * @return 在线用户信息；未命中时返回 null
     */
    @Override
    @Nullable
    public SysUserOnline selectOnlineByInfo(String ipaddr, String userName, LoginUser user) {
        if (StringUtils.isNull(user)) {
            return null;
        }
        if (StringUtils.equals(ipaddr, user.getIpaddr()) && StringUtils.equals(userName, user.getUsername())) {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 设置在线用户信息
     *
     * @param user 用户信息
     * @return 在线用户；用户为空或缺失用户实体时返回 null
     */
    @Override
    @Nullable
    public SysUserOnline loginUserToUserOnline(LoginUser user) {
        if (StringUtils.isNull(user) || StringUtils.isNull(user.getUser())) {
            return null;
        }
        SysUserOnline sysUserOnline = new SysUserOnline();
        sysUserOnline.setTokenId(user.getToken());
        sysUserOnline.setUserName(user.getUsername());
        sysUserOnline.setIpaddr(user.getIpaddr());
        sysUserOnline.setLoginLocation(user.getLoginLocation());
        sysUserOnline.setBrowser(user.getBrowser());
        sysUserOnline.setOs(user.getOs());
        sysUserOnline.setLoginTime(user.getLoginTime());
        if (StringUtils.isNotNull(user.getUser().getDept())) {
            sysUserOnline.setDeptName(user.getUser().getDept().getDeptName());
        }
        return sysUserOnline;
    }
}
