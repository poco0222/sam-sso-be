/**
 * @file 数据库连接配置承载对象
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.web.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 仅用于承载数据库连接配置，不直接作为前端响应 DTO（数据传输对象）。
 */
@Component
@ConfigurationProperties(prefix = "spring.datasource.druid.master")
public class DatabaseInfo {
    private String host;
    private Integer port;
    private String username;

    /** 数据库密码仅供服务端内部使用。 */
    @JsonIgnore
    private String password;
    private String basename;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBasename() {
        return basename;
    }

    public void setBasename(String basename) {
        this.basename = basename;
    }
}
