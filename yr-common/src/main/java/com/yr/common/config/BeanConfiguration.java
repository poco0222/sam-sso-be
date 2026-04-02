package com.yr.common.config;

import com.yr.common.component.DefaultFileClientServiceImpl;
import com.yr.common.component.FtpFileClientServiceImpl;
import com.yr.common.component.IFileClientService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * <p>
 * description
 * </p>
 *
 * @author PopoY 2022-1-5 11:18
 * @version V1.0
 */

@Configuration
public class BeanConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "file", name = "clientType", havingValue = "ftp")
    public IFileClientService ftpFileClient(Environment environment) {
        FtpFileClientServiceImpl ftpFileClient = new FtpFileClientServiceImpl();
        ftpFileClient.setHost(environment.getProperty("file.ftp.host"));
        ftpFileClient.setPort(Integer.valueOf(environment.getProperty("file.ftp.port")));
        ftpFileClient.setUsername(environment.getProperty("file.ftp.username"));
        ftpFileClient.setPassword(environment.getProperty("file.ftp.password"));
        return ftpFileClient;
    }

    @Bean
    @ConditionalOnMissingBean(IFileClientService.class)
    public IFileClientService defaultFileClient(Environment environment) {
        return new DefaultFileClientServiceImpl();
    }

}
