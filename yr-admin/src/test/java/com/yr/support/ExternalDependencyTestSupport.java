/**
 * @file 外部依赖连通性测试支持工具
 * @author PopoY
 * @date 2026-03-11
 */
package com.yr.support;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 为集成测试提供外部依赖地址解析与端口探测能力。
 */
public final class ExternalDependencyTestSupport {

    /** MySQL JDBC 地址解析正则。 */
    private static final Pattern MYSQL_URL_PATTERN = Pattern.compile("jdbc:mysql://([^:/?;]+):(\\d+).*");

    /** SQL Server JDBC 地址解析正则。 */
    private static final Pattern SQL_SERVER_URL_PATTERN = Pattern.compile("jdbc:sqlserver://([^:;]+):(\\d+).*");

    /** 私有构造方法，禁止实例化。 */
    private ExternalDependencyTestSupport() {
    }

    /**
     * 解析 RocketMQ NameServer 地址，默认取第一个地址。
     *
     * @param nameServer RocketMQ NameServer 配置
     * @return 主机与端口
     */
    public static HostPort parseRocketMqNameServer(String nameServer) {
        if (nameServer == null || nameServer.trim().isEmpty()) {
            return new HostPort("127.0.0.1", 9876);
        }
        String firstServer = nameServer.split(",")[0].trim();
        int delimiterIndex = firstServer.lastIndexOf(':');
        if (delimiterIndex < 0) {
            return new HostPort(firstServer, 9876);
        }
        String host = firstServer.substring(0, delimiterIndex).trim();
        int port = Integer.parseInt(firstServer.substring(delimiterIndex + 1).trim());
        return new HostPort(host, port);
    }

    /**
     * 从 MySQL JDBC URL 中解析主机与端口。
     *
     * @param jdbcUrl MySQL JDBC URL
     * @return 主机与端口
     */
    public static HostPort parseMySqlJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return new HostPort("127.0.0.1", 3306);
        }
        Matcher matcher = MYSQL_URL_PATTERN.matcher(jdbcUrl);
        if (!matcher.matches()) {
            return new HostPort("127.0.0.1", 3306);
        }
        return new HostPort(matcher.group(1), Integer.parseInt(matcher.group(2)));
    }

    /**
     * 从 SQL Server JDBC URL 中解析主机与端口。
     *
     * @param jdbcUrl SQL Server JDBC URL
     * @return 主机与端口
     */
    public static HostPort parseSqlServerJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return new HostPort("127.0.0.1", 1433);
        }
        Matcher matcher = SQL_SERVER_URL_PATTERN.matcher(jdbcUrl);
        if (!matcher.matches()) {
            return new HostPort("127.0.0.1", 1433);
        }
        return new HostPort(matcher.group(1), Integer.parseInt(matcher.group(2)));
    }

    /**
     * 检查目标 TCP 端口是否可连通。
     *
     * @param host 主机地址
     * @param port 端口
     * @param timeout 连接超时时间
     * @return 可连通时返回 true，否则返回 false
     */
    public static boolean isTcpReachable(String host, int port, Duration timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), Math.toIntExact(timeout.toMillis()));
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    /**
     * 主机端口值对象。
     */
    public static final class HostPort {
        /** 主机。 */
        private final String host;

        /** 端口。 */
        private final int port;

        /**
         * 构造主机端口对象。
         *
         * @param host 主机
         * @param port 端口
         */
        public HostPort(String host, int port) {
            this.host = host;
            this.port = port;
        }

        /**
         * 获取主机。
         *
         * @return 主机
         */
        public String getHost() {
            return host;
        }

        /**
         * 获取端口。
         *
         * @return 端口
         */
        public int getPort() {
            return port;
        }
    }
}
