/**
 * @file yr-system 启动预热执行器，统一调度缓存预热并输出观测日志
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * yr-system 启动完成后的缓存预热执行器。
 */
@Component
public class YrSystemWarmupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(YrSystemWarmupRunner.class);

    /**
     * 一期阶段不再承接 code-rule/config/dict 预热，保留最小 runner 作为后续扩展点。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("yr-system phase1 warmup skipped: no legacy cache preload configured");
    }
}
