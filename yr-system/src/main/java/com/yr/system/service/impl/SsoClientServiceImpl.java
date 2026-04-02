/**
 * @file 身份中心客户端服务实现
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yr.common.core.domain.entity.SsoClient;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.domain.dto.SsoClientIntegrationGuideView;
import com.yr.system.domain.dto.SsoClientSecretIssueResult;
import com.yr.system.mapper.SsoClientMapper;
import com.yr.system.service.ISsoClientService;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 身份中心客户端服务实现。
 */
@Service
public class SsoClientServiceImpl extends CustomServiceImpl<SsoClientMapper, SsoClient> implements ISsoClientService {

    /** 允许的客户端状态。 */
    private static final Set<String> ALLOWED_CLIENT_STATUS = Set.of("0", "1");

    /** 允许的客户端开关值。 */
    private static final Set<String> ALLOWED_SWITCH_VALUE = Set.of("Y", "N");

    /** 允许的回调地址协议。 */
    private static final Set<String> ALLOWED_REDIRECT_URI_SCHEME = Set.of("http", "https");

    /** 授权跳转路径。 */
    private static final String AUTHORIZE_PATH = "/auth/authorize";

    /** 换票路径。 */
    private static final String EXCHANGE_PATH = "/auth/exchange";

    /** 当客户端尚未配置回调地址时的说明示例。 */
    private static final String REDIRECT_URI_PLACEHOLDER = "https://your-app.example.com/callback";

    /** 标准化身份载荷字段说明。 */
    private static final List<String> IDENTITY_PAYLOAD_FIELDS = List.of(
            "userId",
            "userName",
            "nickName",
            "orgInfo",
            "deptInfo",
            "orgRelations",
            "deptRelations",
            "traceId",
            "exchangeId"
    );

    /**
     * 查询客户端列表。
     *
     * @param query 查询条件
     * @return 客户端列表
     */
    @Override
    public List<SsoClient> selectSsoClientList(SsoClient query) {
        // 先提取查询值，避免在 condition（条件）为 false 时仍因参数求值访问空对象。
        String clientCode = query == null ? null : query.getClientCode();
        String clientName = query == null ? null : query.getClientName();
        String status = query == null ? null : query.getStatus();

        LambdaQueryWrapper<SsoClient> queryWrapper = new LambdaQueryWrapper<SsoClient>()
                .like(clientCode != null && !clientCode.isBlank(), SsoClient::getClientCode, clientCode)
                .like(clientName != null && !clientName.isBlank(), SsoClient::getClientName, clientName)
                .eq(status != null && !status.isBlank(), SsoClient::getStatus, status)
                .orderByAsc(SsoClient::getClientId);
        return this.list(queryWrapper);
    }

    /**
     * 查询分发任务可选客户端。
     *
     * @return 启用且开启同步的客户端列表
     */
    @Override
    public List<SsoClient> selectDistributionClientOptions() {
        QueryWrapper<SsoClient> queryWrapper = new QueryWrapper<SsoClient>()
                .select("client_code", "client_name")
                .eq("status", "0")
                .eq("sync_enabled", "Y")
                .orderByAsc("client_id");
        return this.list(queryWrapper);
    }

    /**
     * 按客户端编码查询客户端。
     *
     * @param clientCode 客户端编码
     * @return 客户端；不存在时返回 null
     */
    @Override
    public SsoClient selectSsoClientByCode(String clientCode) {
        if (StringUtils.isBlank(clientCode)) {
            return null;
        }
        QueryWrapper<SsoClient> queryWrapper = new QueryWrapper<SsoClient>()
                .select(
                        "client_id",
                        "client_code",
                        "client_name",
                        "redirect_uris",
                        "allow_password_login",
                        "allow_wxwork_login",
                        "status",
                        "sync_enabled"
                )
                .eq("client_code", clientCode.trim())
                .last("limit 1");
        return this.getOne(queryWrapper);
    }

    /**
     * 构建客户端接入治理说明，给控制台直接展示与复制。
     *
     * @param clientId 客户端ID
     * @return 接入说明视图
     */
    @Override
    public SsoClientIntegrationGuideView buildIntegrationGuide(Long clientId) {
        if (clientId == null) {
            throw new CustomException("客户端ID不能为空");
        }
        SsoClient ssoClient = this.getById(clientId);
        if (ssoClient == null) {
            throw new CustomException("客户端不存在");
        }
        return toIntegrationGuideView(ssoClient);
    }

    /**
     * 新增客户端。
     *
     * @param ssoClient 客户端信息
     * @return 影响行数
     */
    @Override
    public SsoClientSecretIssueResult insertSsoClient(SsoClient ssoClient) {
        validateForWrite(ssoClient, false);
        String clientSecret = generateClientSecret();
        ssoClient.setClientSecret(SecurityUtils.encryptPassword(clientSecret));
        if (!this.save(ssoClient)) {
            throw new CustomException("新增客户端失败");
        }
        SsoClientSecretIssueResult issueResult = new SsoClientSecretIssueResult();
        issueResult.setClientId(ssoClient.getClientId());
        issueResult.setClientCode(ssoClient.getClientCode());
        issueResult.setClientSecret(clientSecret);
        return issueResult;
    }

    /**
     * 更新客户端。
     *
     * @param ssoClient 客户端信息
     * @return 影响行数
     */
    @Override
    public int updateSsoClient(SsoClient ssoClient) {
        validateForWrite(ssoClient, true);
        // 普通编辑入口不允许覆盖已存储的哈希密钥。
        ssoClient.setClientSecret(null);
        return this.updateById(ssoClient) ? 1 : 0;
    }

    /**
     * 轮换客户端密钥。
     *
     * @param clientId 客户端ID
     * @return 新密钥
     */
    @Override
    public String rotateClientSecret(Long clientId) {
        if (clientId == null) {
            throw new CustomException("客户端ID不能为空");
        }
        SsoClient ssoClient = this.getById(clientId);
        if (ssoClient == null) {
            throw new CustomException("客户端不存在");
        }
        String clientSecret = generateClientSecret();
        ssoClient.setClientSecret(SecurityUtils.encryptPassword(clientSecret));
        if (!this.updateById(ssoClient)) {
            throw new CustomException("轮换客户端密钥失败");
        }
        return clientSecret;
    }

    /**
     * 修改客户端状态。
     *
     * @param clientId 客户端ID
     * @param status 新状态
     * @return 影响行数
     */
    @Override
    public int changeStatus(Long clientId, String status) {
        if (clientId == null) {
            throw new CustomException("客户端ID不能为空");
        }
        validateClientStatus(status);
        SsoClient ssoClient = new SsoClient();
        ssoClient.setClientId(clientId);
        ssoClient.setStatus(status);
        return this.updateById(ssoClient) ? 1 : 0;
    }

    /**
     * 生成新的客户端密钥。
     *
     * @return 新密钥
     */
    private String generateClientSecret() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 把客户端实体映射为接入说明视图，集中导出 client governance（客户端治理）需要的信息。
     *
     * @param ssoClient 客户端实体
     * @return 接入说明视图
     */
    private SsoClientIntegrationGuideView toIntegrationGuideView(SsoClient ssoClient) {
        SsoClientIntegrationGuideView view = new SsoClientIntegrationGuideView();
        view.setClientId(ssoClient.getClientId());
        view.setClientCode(ssoClient.getClientCode());
        view.setClientName(ssoClient.getClientName());
        view.setRedirectUris(ssoClient.getRedirectUris());
        view.setAllowPasswordLogin(ssoClient.getAllowPasswordLogin());
        view.setAllowWxworkLogin(ssoClient.getAllowWxworkLogin());
        view.setSyncEnabled(ssoClient.getSyncEnabled());
        view.setStatus(ssoClient.getStatus());
        view.setRedirectUriConfigured(hasConfiguredRedirectUri(ssoClient.getRedirectUris()));
        view.setLoginMethodConfigured(isLoginMethodConfigured(ssoClient));
        view.setClientEnabled("0".equals(ssoClient.getStatus()));
        view.setSyncEnabledReady("Y".equals(ssoClient.getSyncEnabled()));
        view.setAuthorizePath(AUTHORIZE_PATH);
        view.setExchangePath(EXCHANGE_PATH);
        view.setAuthorizeExample(buildAuthorizeExample(ssoClient));
        view.setExchangeRequestExample(buildExchangeRequestExample(ssoClient));
        view.setIdentityPayloadFields(IDENTITY_PAYLOAD_FIELDS);
        view.setLatestKnownSecretOperationTime(resolveLatestKnownSecretOperationTime(ssoClient));
        view.setSecretRotationInfo(buildSecretRotationInfo());
        return view;
    }

    /**
     * 判断客户端是否至少配置了一条回调地址。
     *
     * @param redirectUris 回调地址白名单原始文本
     * @return 是否已配置
     */
    private boolean hasConfiguredRedirectUri(String redirectUris) {
        if (StringUtils.isBlank(redirectUris)) {
            return false;
        }
        for (String redirectUri : redirectUris.split("[,\\n]")) {
            if (StringUtils.isNotBlank(redirectUri == null ? null : redirectUri.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断客户端是否至少启用了一个登录入口。
     *
     * @param ssoClient 客户端实体
     * @return 是否已配置登录方式
     */
    private boolean isLoginMethodConfigured(SsoClient ssoClient) {
        return "Y".equals(ssoClient.getAllowPasswordLogin()) || "Y".equals(ssoClient.getAllowWxworkLogin());
    }

    /**
     * 生成授权跳转示例，帮助下游系统直接验证 redirect/code/state 三元组。
     *
     * @param ssoClient 客户端实体
     * @return 授权跳转示例
     */
    private String buildAuthorizeExample(SsoClient ssoClient) {
        return UriComponentsBuilder
                .fromPath(AUTHORIZE_PATH)
                .queryParam("clientCode", UriUtils.encodeQueryParam(ssoClient.getClientCode(), StandardCharsets.UTF_8))
                .queryParam("redirectUri", UriUtils.encodeQueryParam(resolveGuideRedirectUri(ssoClient.getRedirectUris()), StandardCharsets.UTF_8))
                .queryParam("state", UriUtils.encodeQueryParam("demo-state", StandardCharsets.UTF_8))
                .build(false)
                .toUriString();
    }

    /**
     * 生成换票请求示例，明确 SSO 只交换标准化身份载荷，不直接签发下游 JWT。
     *
     * @param ssoClient 客户端实体
     * @return 换票请求示例 JSON
     */
    private String buildExchangeRequestExample(SsoClient ssoClient) {
        return """
                {
                  "clientCode":"%s",
                  "clientSecret":"<创建或轮换后获取的一次性明文>",
                  "code":"<浏览器回跳得到的一次性code>"
                }
                """.formatted(ssoClient.getClientCode());
    }

    /**
     * 解析接入说明要展示的示例回调地址；未配置时回退到固定占位示例，便于下游先对接。
     *
     * @param redirectUris 原始白名单文本
     * @return 首条回调地址或占位示例
     */
    private String resolveGuideRedirectUri(String redirectUris) {
        if (StringUtils.isBlank(redirectUris)) {
            return REDIRECT_URI_PLACEHOLDER;
        }
        for (String redirectUri : redirectUris.split("[,\\n]")) {
            String candidate = redirectUri == null ? null : redirectUri.trim();
            if (StringUtils.isNotBlank(candidate)) {
                return candidate;
            }
        }
        return REDIRECT_URI_PLACEHOLDER;
    }

    /**
     * 解析最近已知密钥操作时间。当前版本未引入独立轮换时间字段，因此取最近一次管理时间做 honest（诚实）展示。
     *
     * @param ssoClient 客户端实体
     * @return 最近已知操作时间
     */
    private Date resolveLatestKnownSecretOperationTime(SsoClient ssoClient) {
        return ssoClient.getUpdateTime() != null ? ssoClient.getUpdateTime() : ssoClient.getCreateTime();
    }

    /**
     * 构造密钥轮换提示语，明确当前时间语义是“最近已知操作时间”而不是精确轮换审计时间。
     *
     * @return 提示语
     */
    private String buildSecretRotationInfo() {
        return "当前版本未单独记录密钥轮换时间；请以最近一次客户端管理时间作为最近已知操作时间，并在轮换后立即同步下游配置。";
    }

    /**
     * 校验客户端写入输入，避免把非法配置直接落到数据库。
     *
     * @param ssoClient 客户端信息
     * @param requireClientId 是否要求必须携带主键
     */
    private void validateForWrite(SsoClient ssoClient, boolean requireClientId) {
        if (ssoClient == null) {
            throw new CustomException("客户端信息不能为空");
        }
        if (requireClientId && ssoClient.getClientId() == null) {
            throw new CustomException("客户端ID不能为空");
        }
        if (StringUtils.isBlank(ssoClient.getClientCode())) {
            throw new CustomException("clientCode不能为空");
        }
        if (StringUtils.isBlank(ssoClient.getClientName())) {
            throw new CustomException("clientName不能为空");
        }
        if (StringUtils.isBlank(ssoClient.getStatus())) {
            throw new CustomException("status不能为空");
        }
        validateClientStatus(ssoClient.getStatus());
        validateSwitchValue("allowPasswordLogin", ssoClient.getAllowPasswordLogin());
        validateSwitchValue("allowWxworkLogin", ssoClient.getAllowWxworkLogin());
        validateSwitchValue("syncEnabled", ssoClient.getSyncEnabled());
        validateRedirectUris(ssoClient.getRedirectUris());
    }

    /**
     * 对回调地址做受控校验：必须是可解析且协议受支持的 Web 回调地址。
     *
     * @param redirectUris 回调地址列表
     */
    private void validateRedirectUris(String redirectUris) {
        if (StringUtils.isBlank(redirectUris)) {
            return;
        }
        for (String redirectUri : redirectUris.split("[,\\n]")) {
            String candidate = redirectUri == null ? null : redirectUri.trim();
            if (StringUtils.isBlank(candidate)) {
                continue;
            }
            try {
                URI uri = URI.create(candidate);
                if (StringUtils.isBlank(uri.getScheme())
                        || !ALLOWED_REDIRECT_URI_SCHEME.contains(uri.getScheme().toLowerCase())
                        || StringUtils.isBlank(uri.getHost())) {
                    throw new CustomException("redirectUris中存在非法地址");
                }
            } catch (IllegalArgumentException exception) {
                throw new CustomException("redirectUris中存在非法地址");
            }
        }
    }

    /**
     * 校验客户端状态只允许为 0/1。
     *
     * @param status 客户端状态
     */
    private void validateClientStatus(String status) {
        if (StringUtils.isBlank(status)) {
            throw new CustomException("status不能为空");
        }
        if (!ALLOWED_CLIENT_STATUS.contains(status)) {
            throw new CustomException("status只允许为0或1");
        }
    }

    /**
     * 校验客户端开关字段只允许为 Y/N；空值继续沿用现有默认值/跳过更新语义。
     *
     * @param fieldName 字段名
     * @param fieldValue 字段值
     */
    private void validateSwitchValue(String fieldName, String fieldValue) {
        if (StringUtils.isBlank(fieldValue)) {
            return;
        }
        if (!ALLOWED_SWITCH_VALUE.contains(fieldValue)) {
            throw new CustomException(fieldName + "只允许为Y或N");
        }
    }
}
