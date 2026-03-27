/**
 * @file 系统基础信息控制器
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.web.controller.common;


import com.yr.common.core.domain.AjaxResult;
import com.yr.web.domain.DatabaseInfo;
import com.yr.web.domain.RedisInfo;
import com.yr.web.domain.SystemInfoView;
import com.yr.web.service.ServerTimeScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统基本信息
 *
 * @author PopoY
 */
@RestController
public class SystemInfoController {
    @Autowired
    private RedisInfo redisInfo;

    @Autowired
    private DatabaseInfo databaseInfo;

    @Autowired
    private ServerTimeScheduleService serverTimeScheduleService;

    /**
     * 获取系统连接信息的脱敏视图。
     *
     * @return 仅包含安全展示字段的系统信息
     */
    @GetMapping("/getSystemInfo")
    public AjaxResult getSystemInfo() {
        return AjaxResult.success(buildSystemInfoView());
    }

    /**
     * 获取服务器时间
     */
    @GetMapping("/getServerTime")
    public AjaxResult getServerTime() {
        return AjaxResult.success(serverTimeScheduleService.getServerTimeFromRedis());
    }

    /**
     * 构建显式脱敏后的系统信息响应体，避免原始配置 Bean 被直接序列化给前端。
     *
     * @return 系统信息安全视图
     */
    private Map<String, SystemInfoView> buildSystemInfoView() {
        Map<String, SystemInfoView> info = new LinkedHashMap<>();
        info.put("databaseInfo", SystemInfoView.fromDatabase(databaseInfo));
        info.put("redisInfo", SystemInfoView.fromRedis(redisInfo));
        return info;
    }
}
