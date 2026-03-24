package com.yr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动程序
 *
 * @author Youngron
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
//@ComponentScan(basePackages = {"com.yr",})
//开启定时任务配置
@EnableScheduling
public class YrApplication {
    public static void main(String[] args) {
        //System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(YrApplication.class, args);
        System.out.println("------>>>>>> Youngron Application Started... <<<<<<------\n");
    }
}

