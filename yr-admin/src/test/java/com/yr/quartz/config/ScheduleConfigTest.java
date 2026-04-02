/**
 * @file Quartz 调度配置回归测试
 * @author PopoY
 * @date 2026-03-10
 */
package com.yr.quartz.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Quartz 调度配置测试
 */
class ScheduleConfigTest {

    /**
     * 验证 Quartz 应使用 Spring 管理的数据源存储实现，避免要求额外的命名数据源。
     */
    @Test
    void shouldUseSpringManagedQuartzJobStore() {
        ScheduleConfig scheduleConfig = new ScheduleConfig();
        SchedulerFactoryBean schedulerFactoryBean = scheduleConfig.schedulerFactoryBean(new NoOpDataSource());

        Properties quartzProperties = (Properties) ReflectionTestUtils.getField(schedulerFactoryBean, "quartzProperties");

        Assertions.assertNotNull(quartzProperties, "Quartz 属性不应为空");
        Assertions.assertEquals(
                "org.springframework.scheduling.quartz.LocalDataSourceJobStore",
                quartzProperties.getProperty("org.quartz.jobStore.class"),
                "Quartz 应通过 Spring 托管的数据源访问数据库"
        );
    }

    /**
     * 仅用于构造 SchedulerFactoryBean 的空实现数据源。
     */
    static class NoOpDataSource implements DataSource {
        @Override
        public Connection getConnection() throws SQLException {
            throw new SQLException("测试数据源不提供真实连接");
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            throw new SQLException("测试数据源不提供真实连接");
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("不支持 unwrap");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException("不支持父级日志");
        }
    }
}
