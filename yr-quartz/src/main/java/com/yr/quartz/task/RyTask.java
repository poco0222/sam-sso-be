package com.yr.quartz.task;

import com.yr.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 定时任务调度测试
 *
 * @author Youngron
 */
@Component("ryTask")
public class RyTask {
    /** 定时任务调度日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(RyTask.class);

    public void ryMultipleParams(String s, Boolean b, Long l, Double d, Integer i) {
        LOGGER.info("{}", StringUtils.format("执行多参方法： 字符串类型{}，布尔类型{}，长整型{}，浮点型{}，整形{}", s, b, l, d, i));
    }

    public void ryParams(String params) {
        LOGGER.info("执行有参方法：{}", params);
    }

    public void ryNoParams() {
        LOGGER.info("执行无参方法");
    }
}
