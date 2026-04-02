/**
 * @file SSO 应用启动烟雾测试
 * @author PopoY
 * @date 2026-04-02
 */
package com.yr.sso;

import com.yr.YrApplication;
import com.yr.framework.config.SecurityConfig;
import com.yr.quartz.service.impl.SysJobServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用于单独验证当前 Web/Security 上下文可以正常装配。
 */
@SpringBootTest(
        classes = YrApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.druid.initialSize=0",
                "spring.datasource.druid.minIdle=0",
                "spring.datasource.druid.connectionErrorRetryAttempts=0",
                "spring.datasource.druid.breakAfterAcquireFailure=true",
                "spring.datasource.druid.timeBetweenConnectErrorMillis=100",
                "spring.datasource.druid.initExceptionThrow=false",
                "spring.datasource.druid.statViewServlet.enabled=false",
                "spring.datasource.druid.webStatFilter.enabled=false",
                "spring.liquibase.enabled=false",
                "token.secret=test-only-local-jwt-secret-for-startup-smoke"
        }
)
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "runStartupSmoke", matches = "true")
@EnabledIfEnvironmentVariable(named = "SPRING_DATASOURCE_DRUID_MASTER_PASSWORD", matches = ".+")
class SsoApplicationStartupSmokeTest {

    /** Mock Quartz 初始化服务，避免启动烟雾测试被数据库调度初始化抢占。 */
    @MockBean
    private SysJobServiceImpl sysJobService;

    /** 启动后的应用上下文。 */
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 验证当前 Web/Security 上下文可以完成最小装配。
     */
    @Test
    void shouldLoadWebSecurityApplicationContext() {
        assertThat(applicationContext.getBean(SecurityConfig.class)).isNotNull();
        assertThat(applicationContext.getBean(AuthenticationManager.class)).isNotNull();
        assertThat(applicationContext.getBean(SecurityFilterChain.class)).isNotNull();
        assertThat(applicationContext.getBean(UserDetailsService.class)).isNotNull();
    }
}
