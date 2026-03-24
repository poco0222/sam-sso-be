package com.yr.web.service;

import com.yr.common.core.redis.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 服务器时间定时任务服务
 *
 * @author Youngron
 */
@Service
public class ServerTimeScheduleService {

    @Autowired
    private RedisCache redisCache;

    /**
     * 定时获取并存储服务器时间到Redis
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000) //每60000毫秒(1分钟)执行一次
    public void updateServerTimeToRedis() {
        //格式化时间字符串
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedTime = sdf.format(new Date());

        //存储到Redis，设置过期时间为2分钟（避免数据过期）
        redisCache.setCacheObject("server:time", formattedTime, 120, TimeUnit.SECONDS);
    }

    /**
     * 从Redis获取服务器时间
     *
     * @return 服务器时间字符串
     */
    public String getServerTimeFromRedis() {
        String serverTime = redisCache.getCacheObject("server:time");
        if (serverTime == null) {
            //如果Redis中没有数据，返回当前时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            serverTime = sdf.format(new Date());
        }
        return serverTime;
    }
}