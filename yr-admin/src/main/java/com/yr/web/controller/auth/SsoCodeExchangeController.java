/**
 * @file 认证接入协议换票控制器
 * @author PopoY
 * @date 2026-04-02
 */
package com.yr.web.controller.auth;

import com.yr.common.core.domain.AjaxResult;
import com.yr.system.domain.dto.SsoAuthorizationCodePayload;
import com.yr.system.service.ISsoAuthorizationCodeService;
import com.yr.web.controller.auth.dto.SsoCodeExchangeRequest;
import com.yr.web.controller.auth.dto.SsoExchangeIdentityView;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接入协议 exchange（换票）入口。
 */
@RestController
@RequestMapping("/auth")
public class SsoCodeExchangeController {

    /** 认证接入协议服务。 */
    private final ISsoAuthorizationCodeService ssoAuthorizationCodeService;

    /**
     * @param ssoAuthorizationCodeService 认证接入协议服务
     */
    public SsoCodeExchangeController(ISsoAuthorizationCodeService ssoAuthorizationCodeService) {
        this.ssoAuthorizationCodeService = ssoAuthorizationCodeService;
    }

    /**
     * 使用 clientCode + clientSecret + code 交换标准化身份载荷。
     *
     * @param request 换票请求
     * @return 标准化身份载荷
     */
    @PostMapping("/exchange")
    public AjaxResult exchange(@Validated @RequestBody SsoCodeExchangeRequest request) {
        SsoAuthorizationCodePayload payload = ssoAuthorizationCodeService.exchangeIdentity(
                request.getClientCode(),
                request.getClientSecret(),
                request.getCode()
        );
        return AjaxResult.success(toView(payload));
    }

    /**
     * 把领域载荷映射为对外响应视图，避免把内部缓存字段直接透传给下游系统。
     *
     * @param payload 领域载荷
     * @return 对外视图
     */
    private SsoExchangeIdentityView toView(SsoAuthorizationCodePayload payload) {
        SsoExchangeIdentityView view = new SsoExchangeIdentityView();
        view.setExchangeId(payload.getExchangeId());
        view.setTraceId(payload.getTraceId());
        view.setClientCode(payload.getClientCode());
        view.setUserId(payload.getUserId());
        view.setUsername(payload.getUsername());
        view.setNickName(payload.getNickName());
        view.setOrgId(payload.getOrgId());
        view.setOrgName(payload.getOrgName());
        view.setDeptId(payload.getDeptId());
        view.setDeptName(payload.getDeptName());
        return view;
    }
}
