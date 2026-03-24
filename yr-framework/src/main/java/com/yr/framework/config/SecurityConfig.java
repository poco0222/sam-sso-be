/**
 * @file Spring Security 认证与接口放行配置
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.framework.config;

import com.yr.framework.security.filter.JwtAuthenticationTokenFilter;
import com.yr.framework.security.handle.AuthenticationEntryPointImpl;
import com.yr.framework.security.handle.LogoutSuccessHandlerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.filter.CorsFilter;

import javax.annotation.Resource;

/**
 * 一期 Spring Security 配置。
 */
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    /** 用户认证明细服务。 */
    @Resource(description = "userDetailsService")
    @Autowired
    private UserDetailsService userDetailsService;

    /** 未认证访问处理器。 */
    @Autowired
    private AuthenticationEntryPointImpl unauthorizedHandler;

    /** 退出登录处理器。 */
    @Autowired
    private LogoutSuccessHandlerImpl logoutSuccessHandler;

    /** JWT 认证过滤器。 */
    @Autowired
    private JwtAuthenticationTokenFilter authenticationTokenFilter;

    /** 跨域过滤器。 */
    @Autowired
    private CorsFilter corsFilter;

    /**
     * 暴露 AuthenticationManager，供登录服务执行用户名密码认证。
     *
     * @return Spring Security AuthenticationManager
     * @throws Exception Bean 创建失败时抛出
     */
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    /**
     * 配置一期认证入口与受保护资源。
     *
     * @param httpSecurity HttpSecurity 配置器
     * @throws Exception 安全链配置失败时抛出
     */
    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .csrf().disable()
                .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests()
                // 一期认证面只保留账号密码登录、验证码与企业微信登录相关匿名入口。
                .antMatchers("/login", "/captchaImage", "/auth/wxwork/pre-login", "/auth/wxwork/login").anonymous()
                .antMatchers(
                        HttpMethod.GET,
                        "/",
                        "/api/public/**",
                        "/*.html",
                        "/**/*.html",
                        "/websocket/message/**",
                        "/**/*.css",
                        "/**/*.js",
                        "/profile/**",
                        "/system/file/download"
                ).permitAll()
                .antMatchers("/common/download**").anonymous()
                .antMatchers("/common/tmplDownload**").anonymous()
                .antMatchers("/common/download/resource**").anonymous()
                .antMatchers("/swagger-ui.html").anonymous()
                .antMatchers("/swagger-ui/**").anonymous()
                .antMatchers("/v3/api-docs/**").anonymous()
                .antMatchers("/webjars/**").anonymous()
                .antMatchers("/druid/**").anonymous()
                .anyRequest().authenticated()
                .and()
                .headers().frameOptions().disable();

        httpSecurity.logout().logoutUrl("/logout").logoutSuccessHandler(logoutSuccessHandler);
        httpSecurity.addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
        httpSecurity.addFilterBefore(corsFilter, JwtAuthenticationTokenFilter.class);
        httpSecurity.addFilterBefore(corsFilter, LogoutFilter.class);
    }

    /**
     * 提供 BCrypt 密码编码器。
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置用户认证与密码校验策略。
     *
     * @param auth AuthenticationManagerBuilder
     * @throws Exception 认证器配置失败时抛出
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(bCryptPasswordEncoder());
    }
}
