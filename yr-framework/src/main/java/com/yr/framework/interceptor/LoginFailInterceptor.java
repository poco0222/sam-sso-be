package com.yr.framework.interceptor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.yr.common.constant.Constants;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.redis.RedisCache;
import com.yr.common.filter.RepeatedlyRequestWrapper;
import com.yr.common.utils.ServletUtils;
import com.yr.common.utils.http.HttpHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * 登录失败次数限制
 * </p>
 *
 * @author Youngron 2022-1-10 11:57
 * @version V1.0
 */

@Component
public class LoginFailInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisCache redisCache;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 账号密码输入允许错误的次数，如果没有配置该参数或参数值小于等于0，则不作限制
        Integer loginErrorTimesLimit = redisCache.getCacheInteger(Constants.SYS_CONFIG_KEY + "sys.loginErrorTimesLimit");
        if (loginErrorTimesLimit == null || loginErrorTimesLimit <= 0) {
            return true;
        }
        RepeatedlyRequestWrapper repeatedlyRequest = (RepeatedlyRequestWrapper) request;
        String bodyString = HttpHelper.getBodyString(repeatedlyRequest);
        JSONObject jsonObject = JSON.parseObject(bodyString);
        Integer errorTimes = redisCache.getCacheInteger("login_error:" + jsonObject.getString("username"));
        if (errorTimes != null && errorTimes >= loginErrorTimesLimit) {
            // 锁定时长（分钟）
            Integer lockTime = redisCache.getCacheInteger(Constants.SYS_CONFIG_KEY + "sys.loginErrorLockTime");
            if (lockTime == null) {
                lockTime = 30;
            }
            ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.error("账户已被锁定，请" + lockTime + "分钟后再试")));
            return false;
        }
        return true;
    }

}
