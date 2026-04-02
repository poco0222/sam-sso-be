/**
 * @file PageHelper 自动配置兼容性回归测试
 * @author PopoY
 * @date 2026-03-10
 */
package com.yr.config;

import com.github.pagehelper.autoconfigure.PageHelperAutoConfiguration;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * PageHelper 自动配置兼容性测试
 */
class PageHelperAutoConfigurationCompatibilityTest {

    /**
     * 仅加载 PageHelper 自动配置，验证其在当前 Spring Boot 版本下不会触发循环依赖。
     */
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PageHelperAutoConfiguration.class))
            .withUserConfiguration(SqlSessionFactoryTestConfiguration.class);

    /**
     * 验证 PageHelper 自动配置能够正常创建，不会因循环依赖导致启动失败。
     */
    @Test
    void shouldLoadPageHelperAutoConfigurationWithoutCircularReference() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(PageHelperAutoConfiguration.class);
        });
    }

    /**
     * 为自动配置提供最小化的 SqlSessionFactory 测试桩。
     */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class SqlSessionFactoryTestConfiguration {

        /**
         * 提供可被 PageHelper 自动配置识别的 SqlSessionFactory。
         *
         * @return 模拟的 SqlSessionFactory
         */
        @Bean
        SqlSessionFactory sqlSessionFactory() {
            SqlSessionFactory sqlSessionFactory = mock(SqlSessionFactory.class);
            when(sqlSessionFactory.getConfiguration()).thenReturn(new Configuration());
            return sqlSessionFactory;
        }
    }
}
