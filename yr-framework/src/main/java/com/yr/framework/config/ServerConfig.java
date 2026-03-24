/**
 * @file 服务地址配置工具，负责从当前请求推导系统访问根路径
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.framework.config;

import com.yr.common.utils.ServletUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 服务相关配置
 *
 * @author Youngron
 */
@Component
public class ServerConfig {

    /**
     * 根据请求对象组装服务根路径。
     *
     * @param request 当前请求
     * @return 服务根路径
     */
    public static String getDomain(HttpServletRequest request) {
        StringBuilder url = new StringBuilder(request.getRequestURL().toString());
        String contextPath = request.getServletContext().getContextPath();
        return url.delete(url.length() - request.getRequestURI().length(), url.length()).append(contextPath).toString();
    }

    /**
     * 获取完整的请求路径，包括：域名，端口，上下文访问路径
     *
     * @return 服务地址
     */
    public String getUrl() {
        HttpServletRequest request = ServletUtils.getRequest();
        return getDomain(request);
    }
}
