/**
 * @file 通用静态资源与跨域配置
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.framework.config;

import com.yr.common.config.YrConfig;
import com.yr.common.constant.Constants;
import com.yr.common.utils.StringUtils;
import com.yr.common.utils.spring.SpringUtils;
import com.yr.framework.interceptor.LoginFailInterceptor;
import com.yr.framework.interceptor.RepeatSubmitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 通用配置
 *
 * @author Youngron
 */
@Configuration
public class ResourcesConfig implements WebMvcConfigurer {
    @Autowired
    private RepeatSubmitInterceptor repeatSubmitInterceptor;
    @Autowired
    private LoginFailInterceptor loginFailInterceptor;

    /** 默认仅允许本机开发来源，Spring 未注入属性时也不会退回到通配星号。 */
    private java.util.List<String> corsAllowedOriginPatterns =
            new java.util.ArrayList<>(java.util.Arrays.asList("http://localhost:*", "http://127.0.0.1:*"));

    /**
     * 从应用配置读取允许携带凭证的跨域来源模式。
     *
     * @param allowedOriginPatterns 逗号分隔的来源模式列表
     */
    @Value("${yr.security.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    public void setCorsAllowedOriginPatterns(String allowedOriginPatterns) {
        this.corsAllowedOriginPatterns = StringUtils.str2List(allowedOriginPatterns, ",", true, true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        /** 本地文件上传路径 */
        ResourceHandlerRegistration resourceHandlerRegistration = registry.addResourceHandler(Constants.RESOURCE_PREFIX + "/**");
        String constantPath = SpringUtils.getEnvironment().getProperty("file.constantPath");
        if (StringUtils.isBlank(constantPath) || constantPath.equals(YrConfig.getProfile())) {
            resourceHandlerRegistration.addResourceLocations("file:" + YrConfig.getProfile() + "/");
        } else {
            resourceHandlerRegistration.addResourceLocations("file:" + YrConfig.getProfile() + "/", "file:" + constantPath + "/");
        }
        // springdoc 已内置 Swagger UI 静态资源映射，这里无需额外手工注册
    }

    /**
     * 自定义拦截规则
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginFailInterceptor).addPathPatterns("/login");
        registry.addInterceptor(repeatSubmitInterceptor).addPathPatterns("/**");
    }

    /**
     * 跨域配置
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // 允许凭证时必须使用明确来源白名单，避免 `*` 与 cookie（凭证）组合暴露过宽。
        for (String allowedOriginPattern : corsAllowedOriginPatterns) {
            config.addAllowedOriginPattern(allowedOriginPattern);
        }
        // 设置访问源请求头
        config.addAllowedHeader("*");
        // 设置访问源请求方法
        config.addAllowedMethod("*");
        // 对接口配置跨域设置
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
