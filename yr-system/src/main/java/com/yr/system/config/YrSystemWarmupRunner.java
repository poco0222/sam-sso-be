/**
 * @file yr-system 启动预热执行器，统一调度缓存预热并输出观测日志
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.config;

import com.yr.system.service.ISysCodeRuleService;
import com.yr.system.service.ISysConfigService;
import com.yr.system.service.ISysDictTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * yr-system 启动完成后的缓存预热执行器。
 */
@Component
public class YrSystemWarmupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(YrSystemWarmupRunner.class);

    private final ISysCodeRuleService codeRuleService;
    private final ISysConfigService configService;
    private final ISysDictTypeService dictTypeService;

    /**
     * 显式声明 warmup 所需依赖，集中管理系统级预热流程。
     *
     * @param codeRuleService 编码规则服务
     * @param configService 参数服务
     * @param dictTypeService 字典服务
     */
    public YrSystemWarmupRunner(ISysCodeRuleService codeRuleService,
                                ISysConfigService configService,
                                ISysDictTypeService dictTypeService) {
        this.codeRuleService = codeRuleService;
        this.configService = configService;
        this.dictTypeService = dictTypeService;
    }

    /**
     * 在应用启动完成后统一执行缓存预热。
     * 预热失败时仅记录告警并继续启动，避免缓存预热阻塞整个系统可用性。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        long startedAt = System.nanoTime();
        int successCount = 0;
        int failureCount = 0;

        if (runWarmup("code-rule-cache", codeRuleService::warmUpCodeRuleCache)) {
            successCount++;
        } else {
            failureCount++;
        }

        if (runWarmup("config-cache", configService::warmUpConfigCache)) {
            successCount++;
        } else {
            failureCount++;
        }

        if (runWarmup("dict-cache", dictTypeService::warmUpDictCache)) {
            successCount++;
        } else {
            failureCount++;
        }

        log.info("yr-system warmup finished success={} failure={} costMs={}",
                successCount,
                failureCount,
                elapsedMillis(startedAt));
    }

    /**
     * 执行单个预热步骤，并输出耗时、数量与异常摘要。
     *
     * @param warmupName 预热名称
     * @param action 预热动作
     * @return 是否执行成功
     */
    private boolean runWarmup(String warmupName, Supplier<Integer> action) {
        long startedAt = System.nanoTime();
        try {
            int warmedCount = action.get();
            log.info("warmup {} success count={} costMs={}", warmupName, warmedCount, elapsedMillis(startedAt));
            return true;
        } catch (RuntimeException exception) {
            log.warn("warmup {} failed costMs={} summary={}; application will continue without preload",
                    warmupName,
                    elapsedMillis(startedAt),
                    exception.getMessage(),
                    exception);
            return false;
        }
    }

    /**
     * 计算从给定起点到当前的毫秒耗时。
     *
     * @param startedAt 纳秒起点
     * @return 毫秒耗时
     */
    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
