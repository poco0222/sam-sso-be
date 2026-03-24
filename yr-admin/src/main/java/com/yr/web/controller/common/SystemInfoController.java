package com.yr.web.controller.common;


import com.yr.common.core.domain.AjaxResult;
import com.yr.web.domain.DatabaseInfo;
import com.yr.web.domain.RedisInfo;
import com.yr.web.domain.SystemInfo;
import com.yr.web.service.ServerTimeScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统基本信息
 *
 * @author Youngron
 */
@RestController
public class SystemInfoController {
    @Autowired
    private RedisInfo redisInfo;

    @Autowired
    private DatabaseInfo databaseInfo;

    @Autowired
    private ServerTimeScheduleService serverTimeScheduleService;

    @GetMapping("/getSystemInfo")
    public AjaxResult getSystemInfo() {
        SystemInfo info = new SystemInfo();
        info.setDatabaseInfo(databaseInfo);
        info.setRedisInfo(redisInfo);
        return AjaxResult.success(info);
    }

    /**
     * 获取服务器时间
     */
    @GetMapping("/getServerTime")
    public AjaxResult getServerTime() {
        return AjaxResult.success(serverTimeScheduleService.getServerTimeFromRedis());
    }
}
