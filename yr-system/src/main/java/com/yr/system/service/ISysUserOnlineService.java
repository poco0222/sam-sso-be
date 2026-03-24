/**
 * @file 在线用户服务接口，声明在线会话查询与转换契约
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service;

import com.yr.common.core.domain.model.LoginUser;
import com.yr.system.domain.SysUserOnline;
import org.springframework.lang.Nullable;

/**
 * 在线用户 服务层
 *
 * @author Youngron
 */
public interface ISysUserOnlineService {
    /**
     * 通过登录地址查询信息
     *
     * @param ipaddr 登录地址
     * @param user   用户信息
     * @return 在线用户信息；未命中时返回 null
     */
    @Nullable
    public SysUserOnline selectOnlineByIpaddr(String ipaddr, LoginUser user);

    /**
     * 通过用户名称查询信息
     *
     * @param userName 用户名称
     * @param user     用户信息
     * @return 在线用户信息；未命中时返回 null
     */
    @Nullable
    public SysUserOnline selectOnlineByUserName(String userName, LoginUser user);

    /**
     * 通过登录地址/用户名称查询信息
     *
     * @param ipaddr   登录地址
     * @param userName 用户名称
     * @param user     用户信息
     * @return 在线用户信息；未命中时返回 null
     */
    @Nullable
    public SysUserOnline selectOnlineByInfo(String ipaddr, String userName, LoginUser user);

    /**
     * 设置在线用户信息
     *
     * @param user 用户信息
     * @return 在线用户；用户为空或缺失用户实体时返回 null
     */
    @Nullable
    public SysUserOnline loginUserToUserOnline(LoginUser user);
}
