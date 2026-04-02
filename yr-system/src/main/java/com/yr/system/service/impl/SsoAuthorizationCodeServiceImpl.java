/**
 * @file 认证接入协议授权码服务实现
 * @author PopoY
 * @date 2026-04-02
 */
package com.yr.system.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yr.common.core.domain.entity.SsoClient;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.exception.CustomException;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.domain.dto.SsoAuthorizationCodePayload;
import com.yr.system.service.ISsoAuthorizationCodeService;
import com.yr.system.service.ISsoClientService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 一次性授权码的认证接入协议实现。
 */
@Service
public class SsoAuthorizationCodeServiceImpl implements ISsoAuthorizationCodeService {

    /** 授权码缓存键前缀。 */
    private static final String AUTHORIZATION_CODE_CACHE_KEY_PREFIX = "sso:auth:code:";

    /** 授权码 TTL，保持短窗口降低重放风险。 */
    private static final long AUTHORIZATION_CODE_TTL_SECONDS = 300L;

    /** 客户端服务。 */
    private final ISsoClientService ssoClientService;

    /** Redis 字符串模板，用于原子消费一次性授权码。 */
    private final StringRedisTemplate stringRedisTemplate;

    /** JSON 序列化器。 */
    private final ObjectMapper objectMapper;

    /**
     * @param ssoClientService 客户端服务
     * @param stringRedisTemplate Redis 字符串模板
     * @param objectMapper JSON 序列化器
     */
    public SsoAuthorizationCodeServiceImpl(ISsoClientService ssoClientService,
                                           StringRedisTemplate stringRedisTemplate,
                                           ObjectMapper objectMapper) {
        this.ssoClientService = ssoClientService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String issueAuthorizeRedirectUrl(String clientCode, String redirectUri, String state, LoginUser loginUser) {
        SsoClient ssoClient = requireAuthorizeClient(clientCode);
        validateRedirectUri(redirectUri, state, ssoClient.getRedirectUris());
        String authorizationCode = UUID.randomUUID().toString().replace("-", "");
        SsoAuthorizationCodePayload payload = buildPayload(ssoClient, redirectUri, state, loginUser);

        persistAuthorizationCode(authorizationCode, payload);
        return buildDownstreamRedirectUrl(redirectUri, authorizationCode, state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SsoAuthorizationCodePayload exchangeIdentity(String clientCode, String clientSecret, String code) {
        if (StringUtils.isBlank(code)) {
            throw new CustomException("授权码无效或已过期");
        }
        String cacheKey = buildAuthorizationCodeCacheKey(code);
        String payloadJson = stringRedisTemplate.opsForValue().getAndDelete(cacheKey);
        if (StringUtils.isBlank(payloadJson)) {
            throw new CustomException("授权码无效或已过期");
        }

        SsoAuthorizationCodePayload payload = readPayload(payloadJson);
        if (!Objects.equals(payload.getClientCode(), clientCode)) {
            throw new CustomException("clientCode与授权码不匹配");
        }

        SsoClient persistedClient = requirePersistedClient(payload.getClientId());
        if (!SecurityUtils.matchesPassword(clientSecret, persistedClient.getClientSecret())) {
            throw new CustomException("clientSecret无效");
        }
        return payload;
    }

    /**
     * 校验 authorize 阶段的客户端必须存在且处于启用态。
     *
     * @param clientCode 客户端编码
     * @return 客户端配置
     */
    private SsoClient requireAuthorizeClient(String clientCode) {
        SsoClient ssoClient = ssoClientService.selectSsoClientByCode(clientCode);
        if (ssoClient == null) {
            throw new CustomException("clientCode无效或客户端不存在");
        }
        if (!"0".equals(ssoClient.getStatus())) {
            throw new CustomException("clientCode无效或客户端不存在");
        }
        return ssoClient;
    }

    /**
     * 校验 exchange 阶段的客户端仍然存在且处于启用态。
     *
     * @param clientId 客户端主键
     * @return 持久化客户端
     */
    private SsoClient requirePersistedClient(Long clientId) {
        SsoClient ssoClient = clientId == null ? null : ssoClientService.getById(clientId);
        if (ssoClient == null || !"0".equals(ssoClient.getStatus())) {
            throw new CustomException("clientCode无效或客户端不存在");
        }
        return ssoClient;
    }

    /**
     * 校验回调地址必须命中白名单，并拒绝 state 冲突。
     *
     * @param redirectUri 请求中的回调地址
     * @param state 请求中的 state
     * @param redirectUriWhitelist 客户端白名单
     */
    private void validateRedirectUri(String redirectUri, String state, String redirectUriWhitelist) {
        if (!isRedirectUriAllowed(redirectUri, redirectUriWhitelist)) {
            throw new CustomException("redirectUri不在白名单内");
        }
        String embeddedState = extractQueryParam(redirectUri, "state");
        if (StringUtils.isNotBlank(embeddedState) && !embeddedState.equals(state)) {
            throw new CustomException("state不匹配");
        }
    }

    /**
     * 判断回调地址是否命中客户端白名单。
     *
     * @param redirectUri 请求中的回调地址
     * @param redirectUriWhitelist 客户端白名单
     * @return 是否命中白名单
     */
    private boolean isRedirectUriAllowed(String redirectUri, String redirectUriWhitelist) {
        if (StringUtils.isBlank(redirectUri) || StringUtils.isBlank(redirectUriWhitelist)) {
            return false;
        }
        String normalizedRedirectUri = normalizeUri(redirectUri);
        for (String candidate : redirectUriWhitelist.split("[,\\n]")) {
            if (StringUtils.isBlank(candidate)) {
                continue;
            }
            if (normalizedRedirectUri.equals(normalizeUri(candidate.trim()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 规范化 URI 文本，避免格式差异影响白名单精确匹配。
     *
     * @param rawUri 原始 URI
     * @return 规范化后的 URI
     */
    private String normalizeUri(String rawUri) {
        return URI.create(rawUri).normalize().toString();
    }

    /**
     * 构造授权码缓存载荷。
     *
     * @param ssoClient 客户端配置
     * @param redirectUri 回调地址
     * @param state state
     * @param loginUser 当前登录用户
     * @return 缓存载荷
     */
    private SsoAuthorizationCodePayload buildPayload(SsoClient ssoClient,
                                                     String redirectUri,
                                                     String state,
                                                     LoginUser loginUser) {
        SysUser user = loginUser.getUser();
        String exchangeId = UUID.randomUUID().toString().replace("-", "");
        SsoAuthorizationCodePayload payload = new SsoAuthorizationCodePayload();
        payload.setExchangeId(exchangeId);
        payload.setTraceId("trace-" + exchangeId);
        payload.setClientId(ssoClient.getClientId());
        payload.setClientCode(ssoClient.getClientCode());
        payload.setRedirectUri(redirectUri);
        payload.setState(state);
        payload.setUserId(user.getUserId());
        payload.setUsername(user.getUserName());
        payload.setNickName(user.getNickName());
        payload.setOrgId(user.getOrgId());
        payload.setOrgName(user.getOrgName());
        payload.setDeptId(user.getDeptId());
        payload.setDeptName(user.getDeptName());
        return payload;
    }

    /**
     * 把授权码载荷写入 Redis。
     *
     * @param authorizationCode 授权码
     * @param payload 缓存载荷
     */
    private void persistAuthorizationCode(String authorizationCode, SsoAuthorizationCodePayload payload) {
        try {
            stringRedisTemplate.opsForValue().set(
                    buildAuthorizationCodeCacheKey(authorizationCode),
                    objectMapper.writeValueAsString(payload),
                    AUTHORIZATION_CODE_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
        } catch (JsonProcessingException exception) {
            throw new CustomException("授权码生成失败");
        }
    }

    /**
     * 从缓存 JSON 反序列化授权码载荷。
     *
     * @param payloadJson 缓存 JSON
     * @return 载荷
     */
    private SsoAuthorizationCodePayload readPayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, SsoAuthorizationCodePayload.class);
        } catch (JsonProcessingException exception) {
            throw new CustomException("授权码无效或已过期");
        }
    }

    /**
     * 构造授权码缓存键。
     *
     * @param authorizationCode 授权码
     * @return Redis 键
     */
    private String buildAuthorizationCodeCacheKey(String authorizationCode) {
        return AUTHORIZATION_CODE_CACHE_KEY_PREFIX + authorizationCode;
    }

    /**
     * 构造最终回跳到下游系统的地址。
     *
     * @param redirectUri 回调地址
     * @param authorizationCode 授权码
     * @param state state
     * @return 回调地址
     */
    private String buildDownstreamRedirectUrl(String redirectUri, String authorizationCode, String state) {
        String redirectWithCode = appendQueryParam(redirectUri, "code", authorizationCode);
        if (StringUtils.isNotBlank(extractQueryParam(redirectUri, "state"))) {
            return redirectWithCode;
        }
        return appendQueryParam(redirectWithCode, "state", state);
    }

    /**
     * 向 URL 追加查询参数。
     *
     * @param url 原始 URL
     * @param key 参数名
     * @param value 参数值
     * @return 追加后的 URL
     */
    private String appendQueryParam(String url, String key, String value) {
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 从 URL 中提取指定查询参数。
     *
     * @param url 原始 URL
     * @param key 参数名
     * @return 参数值
     */
    private String extractQueryParam(String url, String key) {
        String query = URI.create(url).getRawQuery();
        if (StringUtils.isBlank(query)) {
            return null;
        }
        for (String segment : query.split("&")) {
            String[] pair = segment.split("=", 2);
            if (pair.length == 0 || !key.equals(pair[0])) {
                continue;
            }
            return pair.length == 2 ? java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
        }
        return null;
    }
}
