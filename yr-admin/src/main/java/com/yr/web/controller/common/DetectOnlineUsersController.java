package com.yr.web.controller.common;

import com.yr.common.constant.Constants;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.core.redis.RedisCache;
import com.yr.framework.web.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class DetectOnlineUsersController {

    @Autowired(required = false)
    private TokenService tokenService;

    @Autowired
    private RedisCache redisCache;

    //@Scheduled(fixedRate = 60000)
    //public void detectOnlineUsers() {
    //    Collection<String> keys = redisCache.keys(Constants.LOGIN_TOKEN_KEY + "*");
    //    for (String key : keys) {
    //        LoginUser user = redisCache.getCacheObject(key);
    //        //LoginUser user = ((JSONObject) redisCache.getCacheObject(key)).toJavaObject(LoginUser.class);
    //        if (tokenService.detectOnlineUsers(user)) {
    //            System.out.println("移除用户");
    //            tokenService.delLoginUser(user.getToken());
    //        }
    //    }
    //}
}
