/**
 * @file 认证接入协议授权码服务接口
 * @author PopoY
 * @date 2026-04-02
 */
package com.yr.system.service;

import com.yr.common.core.domain.model.LoginUser;
import com.yr.system.domain.dto.SsoAuthorizationCodePayload;

/**
 * 认证接入协议授权码服务接口。
 */
public interface ISsoAuthorizationCodeService {

    /**
     * 为已登录用户生成一次性授权码，并构造回跳到下游系统的浏览器地址。
     *
     * @param clientCode 客户端编码
     * @param redirectUri 回调地址
     * @param state 下游系统防重放 state
     * @param loginUser 当前登录用户
     * @return 下游系统回调地址
     */
    String issueAuthorizeRedirectUrl(String clientCode, String redirectUri, String state, LoginUser loginUser);

    /**
     * 使用 clientCode + clientSecret + code 交换标准化身份载荷。
     *
     * @param clientCode 客户端编码
     * @param clientSecret 客户端密钥
     * @param code 一次性授权码
     * @return 标准化身份载荷
     */
    SsoAuthorizationCodePayload exchangeIdentity(String clientCode, String clientSecret, String code);
}
