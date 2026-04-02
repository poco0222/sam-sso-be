/** @file OpenAPI 文档配置 */
package com.yr.web.core.config;

import com.yr.common.config.YrConfig;
import com.yr.common.utils.StringUtils;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAPI 接口文档配置
 *
 * @author PopoY
 */
@Configuration
@ConditionalOnProperty(prefix = "swagger", name = "enabled", havingValue = "true")
public class SwaggerConfig {
    /**
     * 系统基础配置
     */
    @Autowired
    private YrConfig yrConfig;

    /**
     * 设置请求的统一前缀
     */
    @Value("${swagger.pathMapping:/}")
    private String pathMapping;

    /**
     * 构建 OpenAPI 文档对象
     *
     * @return OpenAPI 文档配置
     */
    @Bean
    public OpenAPI openAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(buildInfo())
                .components(new Components().addSecuritySchemes("Authorization", buildSecurityScheme()))
                .addSecurityItem(new SecurityRequirement().addList("Authorization"));

        // 保留原有 pathMapping 语义，便于网关或统一前缀场景下展示正确的服务地址
        if (StringUtils.isNotEmpty(pathMapping) && !"/".equals(pathMapping)) {
            List<Server> servers = new ArrayList<>();
            servers.add(new Server().url(pathMapping));
            openAPI.setServers(servers);
        }
        return openAPI;
    }

    /**
     * 构建接口文档基础信息
     *
     * @return 接口文档基础信息
     */
    private Info buildInfo() {
        return new Info()
                .title("标题：Youngron管理系统_接口文档")
                .description("描述：用于管理集团旗下公司的人员信息,具体包括XXX,XXX模块...")
                .contact(new Contact().name(yrConfig.getName()))
                .version("版本号:" + yrConfig.getVersion());
    }

    /**
     * 构建鉴权方案
     *
     * @return 鉴权方案
     */
    private SecurityScheme buildSecurityScheme() {
        return new SecurityScheme()
                .name("Authorization")
                .in(SecurityScheme.In.HEADER)
                .type(SecurityScheme.Type.APIKEY);
    }
}
