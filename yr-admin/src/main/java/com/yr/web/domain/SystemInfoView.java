/**
 * @file 系统信息安全视图对象
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.web.domain;

/**
 * 提供给前端展示的系统信息脱敏视图。
 */
public class SystemInfoView {

    /** 主机地址。 */
    private String host;

    /** 端口。 */
    private Integer port;

    /** 数据库名，仅对数据库连接视图有意义。 */
    private String basename;

    /** Redis 库索引，仅对 Redis 连接视图有意义。 */
    private String database;

    /** Redis 超时时间，仅对 Redis 连接视图有意义。 */
    private String timeout;

    /**
     * 根据数据库配置构建安全视图。
     *
     * @param databaseInfo 数据库配置
     * @return 不包含密码的数据库连接视图
     */
    public static SystemInfoView fromDatabase(DatabaseInfo databaseInfo) {
        SystemInfoView view = new SystemInfoView();
        view.setHost(databaseInfo.getHost());
        view.setPort(databaseInfo.getPort());
        view.setBasename(databaseInfo.getBasename());
        return view;
    }

    /**
     * 根据 Redis 配置构建安全视图。
     *
     * @param redisInfo Redis 配置
     * @return 不包含密码的 Redis 连接视图
     */
    public static SystemInfoView fromRedis(RedisInfo redisInfo) {
        SystemInfoView view = new SystemInfoView();
        view.setHost(redisInfo.getHost());
        view.setPort(redisInfo.getPort());
        view.setDatabase(redisInfo.getDatabase());
        view.setTimeout(redisInfo.getTimeout());
        return view;
    }

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

    public String getBasename() {
        return basename;
    }

    public void setBasename(String basename) {
        this.basename = basename;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getTimeout() {
        return timeout;
    }

    public void setTimeout(String timeout) {
        this.timeout = timeout;
    }
}
