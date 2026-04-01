/**
 * @file 企业微信认证控制器，承载一期预登录与授权码登录入口
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.web.controller.auth;

import com.yr.common.constant.Constants;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.domain.model.LoginBody;
import com.yr.framework.web.service.SysLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 一期企业微信认证入口。
 */
@RestController
@RequestMapping("/auth/wxwork")
public class WxworkAuthController {

    /** 登录服务，负责企业微信授权码换 token 以及预登录地址生成。 */
    @Autowired
    private SysLoginService loginService;

    /**
     * 返回企业微信网页授权地址，供前端点击登录按钮后跳转。
     *
     * @return 包含授权地址的响应体
     */
    @GetMapping("/pre-login")
    public AjaxResult preLogin() {
        AjaxResult ajaxResult = AjaxResult.success("获取企业微信预登录地址成功");
        ajaxResult.put("authorizeUrl", loginService.buildWxworkPreLoginUrl());
        return ajaxResult;
    }

    /**
     * 使用企业微信授权码与 state 登录，并返回一期统一 token 字段。
     *
     * @param loginBody 企业微信登录请求体
     * @return 登录结果
     */
    @PostMapping("/login")
    public AjaxResult login(@Validated(LoginBody.WxworkLoginValidation.class) @RequestBody LoginBody loginBody) {
        AjaxResult ajaxResult = AjaxResult.success("登录成功");
        ajaxResult.put(Constants.TOKEN, loginService.loginByWxworkCode(loginBody.getCode(), loginBody.getState()));
        return ajaxResult;
    }
}
