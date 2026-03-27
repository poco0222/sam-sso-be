/**
 * @file Redis 连接配置承载对象
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.web.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 仅用于承载 Redis 连接配置，不直接作为前端响应 DTO（数据传输对象）。
 */
@Component
@ConfigurationProperties(prefix = "spring.redis")
public class RedisInfo {
    private String host;
    private Integer port;
    private String database;

    /** Redis 密码仅供服务端内部使用。 */
    @JsonIgnore
    private String password;
    private String timeout;

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

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }
}
