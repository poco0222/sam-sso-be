package com.yr.web.controller.monitor;

import com.yr.common.annotation.Log;
import com.yr.common.constant.Constants;
import com.yr.common.core.controller.BaseController;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.core.page.TableDataInfo;
import com.yr.common.core.redis.RedisCache;
import com.yr.common.enums.BusinessType;
import com.yr.common.utils.StringUtils;
import com.yr.system.domain.SysUserOnline;
import com.yr.system.service.ISysUserOnlineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 在线用户监控
 *
 * @author Youngron
 */
@RestController
@RequestMapping("/monitor/online")
public class SysUserOnlineController extends BaseController {
    @Autowired
    private ISysUserOnlineService userOnlineService;

    @Autowired
    private RedisCache redisCache;

    @PreAuthorize("@ss.hasPermi('monitor:online:list')")
    @GetMapping("/list")
    public TableDataInfo list(String ipaddr, String userName) {
        Collection<String> keys = redisCache.keys(Constants.LOGIN_TOKEN_KEY + "*");
        List<SysUserOnline> userOnlineList = keys.stream()
                .map(redisCache::getCacheObject)
                .filter(Objects::nonNull)
                .map(obj -> {
                    //类型转换为 LoginUser
                    LoginUser user = (LoginUser) obj;
                    if (StringUtils.isNotEmpty(ipaddr) && StringUtils.isNotEmpty(userName)) {
                        if (StringUtils.equals(ipaddr, user.getIpaddr()) && StringUtils.equals(userName, user.getUsername())) {
                            return userOnlineService.selectOnlineByInfo(ipaddr, userName, user);
                        }
                    } else if (StringUtils.isNotEmpty(ipaddr)) {
                        if (StringUtils.equals(ipaddr, user.getIpaddr())) {
                            return userOnlineService.selectOnlineByIpaddr(ipaddr, user);
                        }
                    } else if (StringUtils.isNotEmpty(userName) && StringUtils.isNotNull(user.getUser())) {
                        if (StringUtils.equals(userName, user.getUsername())) {
                            return userOnlineService.selectOnlineByUserName(userName, user);
                        }
                    } else {
                        return userOnlineService.loginUserToUserOnline(user);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                //根据 loginTime 倒序排序
                .sorted(Comparator.comparing(SysUserOnline::getLoginTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        return getDataTable(userOnlineList);
    }

    /**
     * 强退用户
     */
    @PreAuthorize("@ss.hasPermi('monitor:online:forceLogout')")
    @Log(title = "在线用户", businessType = BusinessType.FORCE)
    @DeleteMapping("/{tokenId}")
    public AjaxResult forceLogout(@PathVariable String tokenId) {
        redisCache.deleteObject(Constants.LOGIN_TOKEN_KEY + tokenId);
        return AjaxResult.success();
    }

}
