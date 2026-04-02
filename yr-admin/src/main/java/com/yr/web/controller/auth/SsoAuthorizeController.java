/**
 * @file 认证接入协议授权跳转控制器
 * @author PopoY
 * @date 2026-04-02
 */
package com.yr.web.controller.auth;

import com.yr.common.core.domain.model.LoginUser;
import com.yr.system.service.ISsoAuthorizationCodeService;
import com.yr.web.controller.auth.dto.SsoAuthorizeQuery;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;

/**
 * 认证接入协议 authorize（授权跳转）入口。
 */
@RestController
@RequestMapping("/auth")
public class SsoAuthorizeController {

    /** 认证接入协议服务。 */
    private final ISsoAuthorizationCodeService ssoAuthorizationCodeService;

    /**
     * @param ssoAuthorizationCodeService 认证接入协议服务
     */
    public SsoAuthorizeController(ISsoAuthorizationCodeService ssoAuthorizationCodeService) {
        this.ssoAuthorizationCodeService = ssoAuthorizationCodeService;
    }

    /**
     * 执行浏览器授权跳转；未登录时先回到登录中心现有登录页，已登录时直接回跳下游系统。
     *
     * @param query 授权参数
     * @param request 当前 HTTP 请求
     * @return 302 跳转响应
     */
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(@Validated SsoAuthorizeQuery query, HttpServletRequest request) {
        LoginUser loginUser = resolveLoginUser();
        String redirectLocation;

        if (loginUser == null) {
            redirectLocation = buildLoginRedirectUrl(request);
        } else {
            redirectLocation = ssoAuthorizationCodeService.issueAuthorizeRedirectUrl(
                    query.getClientCode(),
                    query.getRedirectUri(),
                    query.getState(),
                    loginUser
            );
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectLocation))
                .build();
    }

    /**
     * 从安全上下文中解析当前登录用户；当前协议允许匿名访问，因此取不到登录态时返回 null。
     *
     * @return 当前登录用户；匿名请求返回 null
     */
    private LoginUser resolveLoginUser() {
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication() == null ? null : org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        if (principal instanceof LoginUser) {
            return (LoginUser) principal;
        }
        return null;
    }

    /**
     * 构造登录页回跳地址，登录成功后由前端继续浏览器级跳回当前 authorize 请求。
     *
     * @param request 当前 HTTP 请求
     * @return 登录页地址
     */
    private String buildLoginRedirectUrl(HttpServletRequest request) {
        UriComponentsBuilder authorizeUrlBuilder = UriComponentsBuilder.fromHttpUrl(request.getRequestURL().toString());
        request.getParameterMap().forEach((key, values) -> {
            if (values == null) {
                return;
            }
            for (String value : values) {
                authorizeUrlBuilder.queryParam(key, value);
            }
        });
        String currentAuthorizeUrl = authorizeUrlBuilder
                .build()
                .toUriString();
        return UriComponentsBuilder.fromPath("/login")
                .queryParam("redirect", currentAuthorizeUrl)
                .build()
                .encode()
                .toUriString();
    }
}
