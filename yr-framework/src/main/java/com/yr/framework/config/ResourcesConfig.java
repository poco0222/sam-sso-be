/** @file 通用静态资源与跨域配置 */
package com.yr.framework.config;

import com.yr.common.config.YrConfig;
import com.yr.common.constant.Constants;
import com.yr.common.utils.StringUtils;
import com.yr.common.utils.spring.SpringUtils;
import com.yr.framework.interceptor.LoginFailInterceptor;
import com.yr.framework.interceptor.RepeatSubmitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
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
        // Spring 5.3+ 在允许凭证时不再接受 "*"，这里改为模式匹配并回写具体来源地址
        config.addAllowedOriginPattern("*");
        // 设置访问源请求头
        config.addAllowedHeader("*");
        // 设置访问源请求方法
        config.addAllowedMethod("*");
        // 对接口配置跨域设置
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
