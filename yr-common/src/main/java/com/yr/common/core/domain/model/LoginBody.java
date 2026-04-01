/**
 * @file 登录请求体，统一承载标准登录与企业微信授权登录入参
 * @author PopoY
 * @date 2026-03-31
 */
package com.yr.common.core.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yr.common.enums.PlatformType;
import com.yr.common.utils.StringUtils;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;

/**
 * 登录请求体模型。
 */
public class LoginBody {
    /**
     * 标准账号密码登录校验分组。
     *
     * @author PopoY
     */
    public interface PasswordLoginValidation {
    }

    /**
     * 企业微信授权登录校验分组。
     *
     * @author PopoY
     */
    public interface WxworkLoginValidation {
    }

    /**
     * 用户名
     */
    @NotBlank(message = "username不能为空", groups = PasswordLoginValidation.class)
    private String username;

    /**
     * 用户密码
     */
    @NotBlank(message = "password不能为空", groups = PasswordLoginValidation.class)
    private String password;

    /**
     * 验证码
     */
    @NotBlank(message = "code不能为空", groups = WxworkLoginValidation.class)
    private String code;

    /**
     * 唯一标识
     */
    private String uuid = "";

    /**
     * 登录平台
     * mgmt 管理后台；desktop 桌面端
     */
    private String platform;

    /**
     * 企业微信 OAuth（授权）state，用于防止重放与 CSRF。
     */
    @NotBlank(message = "state不能为空", groups = WxworkLoginValidation.class)
    private String state;

    /**
     * 登录后前端期望回跳的 URL（地址）。
     */
    private String url;

    /**
     * @return 登录后回跳地址
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url 登录后回跳地址
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return 企业微信 OAuth state
     */
    public String getState() {
        return state;
    }

    /**
     * @param state 企业微信 OAuth state
     */
    public void setState(String state) {
        this.state = state;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * 标准登录只有在非 desktop 平台时才强制要求验证码，避免管理端缺失 code 继续下沉到服务层。
     *
     * @return true 表示当前平台已满足验证码契约
     */
    @JsonIgnore
    @AssertTrue(message = "code不能为空", groups = PasswordLoginValidation.class)
    public boolean isPasswordLoginCaptchaSatisfied() {
        if (PlatformType.DESKTOP.getName().equalsIgnoreCase(platform)) {
            return true;
        }
        return StringUtils.isNotBlank(code);
    }
}
