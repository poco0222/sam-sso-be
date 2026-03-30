/**
 * @file 一期认证服务，负责密码登录、企业微信登录与组织切换令牌刷新
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.framework.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yr.common.constant.Constants;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.core.redis.RedisCache;
import com.yr.common.enums.PlatformType;
import com.yr.common.exception.CustomException;
import com.yr.common.exception.user.CaptchaException;
import com.yr.common.exception.user.CaptchaExpireException;
import com.yr.common.exception.user.UserPasswordNotMatchException;
import com.yr.common.exception.user.UserPasswordRetryLimitCountException;
import com.yr.common.exception.user.UserPasswordRetryLimitExceedException;
import com.yr.common.utils.DateUtils;
import com.yr.common.utils.MessageUtils;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.ServletUtils;
import com.yr.common.utils.StringUtils;
import com.yr.common.utils.ip.IpUtils;
import com.yr.common.utils.sign.RsaUtils;
import com.yr.framework.manager.AsyncManager;
import com.yr.framework.manager.factory.AsyncFactory;
import com.yr.system.service.ISysUserOrgService;
import com.yr.system.service.ISysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 一期登录校验与 token 签发服务。
 */
@Component
public class SysLoginService {

    /** 企业微信 access_token 的缓存键，避免每次登录都直连上游。 */
    private static final String WXWORK_ACCESS_TOKEN_CACHE_KEY = "wxwork:access_token";

    /** access_token 提前 5 分钟刷新，避免边界时刻拿到即将过期的凭证。 */
    private static final int WXWORK_ACCESS_TOKEN_BUFFER_SECONDS = 300;

    /** 企业微信用户不存在时返回的业务码。 */
    private static final int WXWORK_USER_NOT_FOUND_CODE = 40001;

    /** 企业微信授权码无效时返回的业务码。 */
    private static final int WXWORK_INVALID_CODE_CODE = 40002;

    /** 获取企业微信 access_token 的官方接口。 */
    private static final String WXWORK_ACCESS_TOKEN_ENDPOINT = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";

    /** 使用授权码换取企业微信用户身份的官方接口。 */
    private static final String WXWORK_USER_INFO_ENDPOINT = "https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo";

    /** 生成企业微信网页授权地址的官方入口。 */
    private static final String WXWORK_AUTHORIZE_ENDPOINT = "https://open.weixin.qq.com/connect/oauth2/authorize";

    /** 登录凭据非法时的受控提示。 */
    private static final String INVALID_LOGIN_CREDENTIAL_MESSAGE = "登录凭据无效，请重新登录";

    /** 登录认证服务异常时的受控提示。 */
    private static final String LOGIN_SERVICE_UNAVAILABLE_MESSAGE = "登录服务暂不可用，请稍后再试";

    /** 服务日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(SysLoginService.class);

    /** Token 服务，负责 JWT 签发。 */
    @Autowired
    private TokenService tokenService;

    /** Spring Security 认证管理器。 */
    @Resource
    private AuthenticationManager authenticationManager;

    /** Redis 缓存，用于登录错误次数与企业微信 access_token 缓存。 */
    @Autowired
    private RedisCache redisCache;

    /** 用户服务，用于查询系统用户。 */
    @Autowired
    private ISysUserService userService;

    /** 用户组织关联服务，用于校验切组织归属关系。 */
    @Autowired
    private ISysUserOrgService sysUserOrgService;

    /** 一期验证码开关，改为直接读取 application 配置。 */
    @Value("${yr.captcha.enabled:true}")
    private boolean captchaEnabled;

    /** 权限服务，用于构造切组织后的 LoginUser。 */
    @Autowired
    private SysPermissionService permissionService;

    /** 用户明细服务，用于复用现有 LoginUser 装配逻辑。 */
    @Autowired
    private UserDetailsService userDetailsService;

    /** Spring Boot 提供的 RestTemplateBuilder，用于访问企业微信 HTTP API。 */
    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    /** Spring 注入的 Jackson ObjectMapper，用于解析企业微信响应。 */
    @Autowired
    private ObjectMapper objectMapper;

    /** 环境配置访问入口，用于读取企业微信相关参数。 */
    @Autowired
    private Environment environment;

    /**
     * 执行账号密码登录。
     *
     * @param username 用户名
     * @param password 密码密文
     * @param code 验证码
     * @param uuid 验证码唯一标识
     * @param platform 登录平台
     * @return 登录成功后的 token
     */
    public String login(String username, String password, String code, String uuid, String platform) {
        boolean captchaOnOff = captchaEnabled;
        if (PlatformType.DESKTOP.getName().equals(platform)) {
            captchaOnOff = false;
        }
        if (captchaOnOff) {
            validateCaptcha(username, code, uuid);
        }

        SysUser sysUser = userService.selectUserByUserName(username, null);
        if (sysUser == null) {
            throw new CustomException("账号密码错误，请重新登录");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, decryptPassword(password))
            );
        } catch (Exception ex) {
            if (ex instanceof BadCredentialsException) {
                AsyncManager.me().execute(
                        AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match"))
                );
                Integer loginErrorTimesLimit = redisCache.getCacheInteger(Constants.SYS_CONFIG_KEY + "sys.loginErrorTimesLimit");
                if (loginErrorTimesLimit != null && loginErrorTimesLimit > 0) {
                    Integer errorTimes = redisCache.getCacheInteger("login_error:" + username);
                    if (errorTimes == null) {
                        errorTimes = 1;
                    } else {
                        errorTimes++;
                    }
                    redisCache.setCacheObject("login_error:" + username, errorTimes);
                    Integer lockTime = redisCache.getCacheInteger(Constants.SYS_CONFIG_KEY + "sys.loginErrorLockTime");
                    if (lockTime == null) {
                        lockTime = 30;
                    }
                    redisCache.expire("login_error:" + username, lockTime, TimeUnit.MINUTES);
                    int remainTimes = loginErrorTimesLimit - errorTimes;
                    if (remainTimes < 1) {
                        throw new UserPasswordRetryLimitExceedException(loginErrorTimesLimit, lockTime);
                    }
                    throw new UserPasswordRetryLimitCountException(remainTimes);
                }
                throw new UserPasswordNotMatchException();
            }
            if (ex instanceof CustomException customException) {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, customException.getMessage()));
                throw customException;
            }
            LOGGER.error("账号密码登录失败，username={}", username, ex);
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, LOGIN_SERVICE_UNAVAILABLE_MESSAGE));
            throw new CustomException(LOGIN_SERVICE_UNAVAILABLE_MESSAGE, ex);
        }

        redisCache.deleteObject("login_error:" + username);
        AsyncManager.me().execute(
                AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"))
        );
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        recordLoginInfo(loginUser.getUser());
        return tokenService.createToken(loginUser);
    }

    /**
     * 解密前端传入的密码密文，并把底层解密异常转换成受控登录语义。
     *
     * @param encryptedPassword 前端提交的密码密文
     * @return 解密后的密码明文
     */
    private String decryptPassword(String encryptedPassword) {
        try {
            return RsaUtils.decryptByPrivateKey(encryptedPassword);
        } catch (Exception exception) {
            LOGGER.warn("登录密码解密失败", exception);
            throw new CustomException(INVALID_LOGIN_CREDENTIAL_MESSAGE, exception);
        }
    }

    /**
     * 切换组织后重新签发 token。
     *
     * @param orgId 目标组织 ID
     * @return 新 token
     */
    public String changeOrg(Long orgId) {
        if (orgId == null) {
            throw new CustomException("组织ID不能为空");
        }
        LoginUser currentLoginUser = SecurityUtils.getLoginUser();
        if (!sysUserOrgService.hasEnabledOrgMembership(currentLoginUser.getUserId(), orgId)) {
            throw new CustomException("目标组织不属于当前用户或已停用");
        }
        SysUser user = userService.selectUserByUserName(currentLoginUser.getUsername(), orgId);
        if (user == null) {
            throw new CustomException("目标组织用户上下文不存在或已停用");
        }
        LoginUser loginUser = new LoginUser(
                user,
                permissionService.getMenuPermission(user),
                permissionService.getRolePermission(user),
                UserDetailsServiceImpl.getActivitiAuthority(user)
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, loginUser.getPassword(), loginUser.getAuthorities())
        );
        if (StringUtils.isNotBlank(currentLoginUser.getToken())) {
            tokenService.delLoginUser(currentLoginUser.getToken());
        }
        return tokenService.createToken(loginUser);
    }

    /**
     * 基于企业微信授权码登录，并复用现有 token 签发与登录记录链路。
     *
     * @param code 企业微信授权码
     * @return 登录成功后的 token
     */
    public String loginByWxworkCode(String code) {
        if (StringUtils.isBlank(code)) {
            throw new CustomException("授权码无效或已过期", WXWORK_INVALID_CODE_CODE);
        }

        String accessToken = getWxworkAccessToken();
        String userId = requestWxworkUserId(accessToken, code);
        if (StringUtils.isBlank(userId)) {
            throw new CustomException("授权码无效或已过期", WXWORK_INVALID_CODE_CODE);
        }

        SysUser sysUser = userService.selectUserByUserName(userId, null);
        if (sysUser == null) {
            throw new CustomException("未找到对应的系统用户，请联系管理员", WXWORK_USER_NOT_FOUND_CODE);
        }

        return issueTokenForUser(sysUser);
    }

    /**
     * 生成企业微信网页授权地址，供前端点击登录按钮时跳转。
     *
     * @return 企业微信预登录授权地址
     */
    public String buildWxworkPreLoginUrl() {
        String corpId = requireWxworkProperty("wxwork.corp-id", "wxwork.corpId", "企业微信 corpId");
        String agentId = requireWxworkProperty("wxwork.agent-id", "wxwork.agentId", "企业微信 agentId");
        String redirectUri = requireWxworkProperty("wxwork.redirect-uri", "wxwork.redirectUri", "企业微信 redirectUri");

        // 预登录地址由后端统一生成，避免前端散落企业微信授权参数拼装细节。
        String authorizeUrl = UriComponentsBuilder.fromHttpUrl(WXWORK_AUTHORIZE_ENDPOINT)
                .queryParam("appid", corpId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "snsapi_privateinfo")
                .queryParam("agentid", agentId)
                .queryParam("state", UUID.randomUUID().toString().replace("-", ""))
                .build()
                .encode()
                .toUriString();
        return authorizeUrl + "#wechat_redirect";
    }

    /**
     * 校验验证码。
     *
     * @param username 用户名
     * @param code 验证码
     * @param uuid 验证码唯一标识
     */
    public void validateCaptcha(String username, String code, String uuid) {
        String verifyKey = Constants.CAPTCHA_CODE_KEY + uuid;
        String captcha = redisCache.getCacheObject(verifyKey);
        redisCache.deleteObject(verifyKey);
        if (captcha == null) {
            AsyncManager.me().execute(
                    AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"))
            );
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha)) {
            AsyncManager.me().execute(
                    AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error"))
            );
            throw new CaptchaException();
        }
    }

    /**
     * 记录登录成功后的登录 IP 与登录时间。
     *
     * @param user 当前登录用户
     */
    public void recordLoginInfo(SysUser user) {
        user.setLoginIp(IpUtils.getIpAddr(ServletUtils.getRequest()));
        user.setLoginDate(DateUtils.getNowDate());
        userService.updateUserProfile(user);
    }

    /**
     * 使用当前工作区配置获取企业微信 access_token。
     *
     * @return access_token
     */
    private String getWxworkAccessToken() {
        String cachedAccessToken = redisCache.getCacheObject(WXWORK_ACCESS_TOKEN_CACHE_KEY);
        if (StringUtils.isNotBlank(cachedAccessToken)) {
            return cachedAccessToken;
        }

        String corpId = requireWxworkProperty("wxwork.corp-id", "wxwork.corpId", "企业微信 corpId");
        String corpSecret = requireWxworkProperty("wxwork.corp-secret", "wxwork.corpSecret", "企业微信 corpSecret");
        String requestUrl = UriComponentsBuilder.fromHttpUrl(WXWORK_ACCESS_TOKEN_ENDPOINT)
                .queryParam("corpid", corpId)
                .queryParam("corpsecret", corpSecret)
                .build()
                .encode()
                .toUriString();

        JsonNode response = requestWxworkJson(requestUrl, "获取企业微信 access_token");
        int errCode = response.path("errcode").asInt(-1);
        if (errCode != 0) {
            LOGGER.warn("获取企业微信 access_token 失败，errcode={}, errmsg={}", errCode, response.path("errmsg").asText());
            throw new CustomException("企业微信 access_token 获取失败");
        }

        String accessToken = response.path("access_token").asText(null);
        if (StringUtils.isBlank(accessToken)) {
            throw new CustomException("企业微信 access_token 获取失败");
        }

        int expiresIn = response.path("expires_in").asInt(7200);
        int cacheSeconds = Math.max(60, expiresIn - WXWORK_ACCESS_TOKEN_BUFFER_SECONDS);
        redisCache.setCacheObject(WXWORK_ACCESS_TOKEN_CACHE_KEY, accessToken, cacheSeconds, TimeUnit.SECONDS);
        return accessToken;
    }

    /**
     * 使用授权码向企业微信换取用户身份。
     *
     * @param accessToken 企业微信 access_token
     * @param code 企业微信授权码
     * @return 企业微信 userid；如果授权码无效则返回 null
     */
    private String requestWxworkUserId(String accessToken, String code) {
        String requestUrl = UriComponentsBuilder.fromHttpUrl(WXWORK_USER_INFO_ENDPOINT)
                .queryParam("access_token", accessToken)
                .queryParam("code", code)
                .build()
                .encode()
                .toUriString();

        JsonNode response = requestWxworkJson(requestUrl, "获取企业微信用户身份");
        int errCode = response.path("errcode").asInt(-1);
        if (errCode != 0) {
            LOGGER.warn("获取企业微信用户身份失败，errcode={}, errmsg={}", errCode, response.path("errmsg").asText());
            return null;
        }
        return response.path("userid").asText(null);
    }

    /**
     * 发起企业微信 GET 请求并解析 JSON。
     *
     * @param requestUrl 请求地址
     * @param scene 日志场景
     * @return 解析后的 JSON 节点
     */
    private JsonNode requestWxworkJson(String requestUrl, String scene) {
        try {
            String responseBody = restTemplateBuilder.build().getForObject(requestUrl, String.class);
            if (StringUtils.isBlank(responseBody)) {
                throw new CustomException(scene + "失败");
            }
            return objectMapper.readTree(responseBody);
        } catch (RestClientException | IOException ex) {
            LOGGER.error("{}失败，请求地址：{}", scene, requestUrl, ex);
            throw new CustomException(scene + "失败", ex);
        }
    }

    /**
     * 复用现有登录成功链路为指定系统用户签发 token。
     *
     * @param sysUser 系统用户
     * @return 登录 token
     */
    private String issueTokenForUser(SysUser sysUser) {
        redisCache.deleteObject("login_error:" + sysUser.getUserName());
        AsyncManager.me().execute(
                AsyncFactory.recordLogininfor(sysUser.getUserName(), Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"))
        );
        LoginUser loginUser = (LoginUser) userDetailsService.loadUserByUsername(sysUser.getUserName());
        recordLoginInfo(loginUser.getUser());
        return tokenService.createToken(loginUser);
    }

    /**
     * 读取企业微信配置；同时兼容 kebab-case 与 camelCase 两种属性命名。
     *
     * @param primaryKey 主属性名
     * @param secondaryKey 兼容属性名
     * @param label 配置说明
     * @return 配置值
     */
    private String requireWxworkProperty(String primaryKey, String secondaryKey, String label) {
        String propertyValue = environment.getProperty(primaryKey);
        if (StringUtils.isBlank(propertyValue)) {
            propertyValue = environment.getProperty(secondaryKey);
        }
        if (StringUtils.isBlank(propertyValue)) {
            throw new CustomException(label + " 配置缺失");
        }
        return propertyValue;
    }
}
