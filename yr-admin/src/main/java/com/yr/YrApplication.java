package com.yr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动程序
 *
 * @author PopoY
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
//@ComponentScan(basePackages = {"com.yr",})
//开启定时任务配置
@EnableScheduling
public class YrApplication {
    /** 启动日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(YrApplication.class);

    public static void main(String[] args) {
        //System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(YrApplication.class, args);
        LOGGER.info("Youngron Application Started");
    }
}
