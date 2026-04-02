/**
 * @file Spring Security 认证与接口放行配置
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.framework.config;

import com.yr.framework.security.filter.JwtAuthenticationTokenFilter;
import com.yr.framework.security.handle.AuthenticationEntryPointImpl;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.filter.CorsFilter;

/**
 * 一期 Spring Security 配置。
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfig {

    /** 用户认证明细服务。 */
    private final UserDetailsService userDetailsService;

    /** 未认证访问处理器。 */
    private final AuthenticationEntryPointImpl unauthorizedHandler;

    /** 退出登录处理器。 */
    private final LogoutSuccessHandler logoutSuccessHandler;

    /** JWT 认证过滤器。 */
    private final JwtAuthenticationTokenFilter authenticationTokenFilter;

    /** 跨域过滤器。 */
    private final CorsFilter corsFilter;

    /**
     * @param userDetailsService 用户认证明细服务
     * @param unauthorizedHandler 未认证访问处理器
     * @param logoutSuccessHandler 退出登录处理器
     * @param authenticationTokenFilter JWT 认证过滤器
     * @param corsFilter 自定义跨域过滤器
     */
    public SecurityConfig(UserDetailsService userDetailsService,
                          AuthenticationEntryPointImpl unauthorizedHandler,
                          LogoutSuccessHandler logoutSuccessHandler,
                          JwtAuthenticationTokenFilter authenticationTokenFilter,
                          @Qualifier("yrCorsFilter") CorsFilter corsFilter) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
        this.authenticationTokenFilter = authenticationTokenFilter;
        this.corsFilter = corsFilter;
    }

    /**
     * 暴露 AuthenticationManager，供登录服务执行用户名密码认证。
     *
     * @param httpSecurity HttpSecurity 配置器
     * @return Spring Security AuthenticationManager
     * @throws Exception Bean 创建失败时抛出
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity httpSecurity) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(userDetailsService).passwordEncoder(bCryptPasswordEncoder());
        return authenticationManagerBuilder.build();
    }

    /**
     * 配置一期认证入口与受保护资源。
     *
     * @param httpSecurity HttpSecurity 配置器
     * @param authenticationManager 认证管理器
     * @return SecurityFilterChain 安全过滤链
     * @throws Exception 安全链配置失败时抛出
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
                                                   AuthenticationManager authenticationManager) throws Exception {
        httpSecurity
                .authenticationManager(authenticationManager)
                .csrf().disable()
                .exceptionHandling().authenticationEntryPoint(unauthorizedHandler).and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .authorizeRequests()
                // 一期认证面只保留账号密码登录、验证码与企业微信登录相关匿名入口。
                .antMatchers("/login", "/captchaImage", "/auth/wxwork/pre-login", "/auth/wxwork/login").anonymous()
                // 二期认证接入协议需要允许匿名发起 authorize/exchange，再由控制器自行执行登录跳转或客户端密钥校验。
                .antMatchers("/auth/authorize", "/auth/exchange").permitAll()
                // Swagger / springdoc / Druid 不再默认匿名开放，必须经过认证后才能访问。
                .antMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/druid/**"
                ).authenticated()
                .antMatchers(
                        HttpMethod.GET,
                        "/",
                        "/api/public/**",
                        "/*.html",
                        "/**/*.html",
                        "/**/*.css",
                        "/**/*.js",
                        "/profile/avatar/**",
                        "/system/file/download"
                ).permitAll()
                // 头像目录仍需支持前端通过原生 `<img>` 直连展示，其余 `/profile/**` 资源必须鉴权。
                .antMatchers("/profile/**").authenticated()
                // 通用文件下载仅允许已登录用户访问，避免匿名绕过控制层边界。
                .antMatchers("/common/download**").authenticated()
                .antMatchers("/common/tmplDownload**").authenticated()
                .antMatchers("/common/download/resource**").authenticated()
                .anyRequest().authenticated()
                .and()
                .headers().frameOptions().disable();

        httpSecurity.logout().logoutUrl("/logout").logoutSuccessHandler(logoutSuccessHandler);
        httpSecurity.addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);
        httpSecurity.addFilterBefore(corsFilter, JwtAuthenticationTokenFilter.class);
        httpSecurity.addFilterBefore(corsFilter, LogoutFilter.class);
        return httpSecurity.build();
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
}
