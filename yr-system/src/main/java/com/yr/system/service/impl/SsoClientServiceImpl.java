/**
 * @file 身份中心客户端服务实现
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yr.common.core.domain.entity.SsoClient;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.domain.dto.SsoClientSecretIssueResult;
import com.yr.system.mapper.SsoClientMapper;
import com.yr.system.service.ISsoClientService;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * 身份中心客户端服务实现。
 */
@Service
public class SsoClientServiceImpl extends CustomServiceImpl<SsoClientMapper, SsoClient> implements ISsoClientService {

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
        this.updateById(ssoClient);
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
        validateRedirectUris(ssoClient.getRedirectUris());
    }

    /**
     * 对回调地址做最小合法性校验：只要求每个条目都能解析出 scheme。
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
            URI uri = URI.create(candidate);
            if (StringUtils.isBlank(uri.getScheme())) {
                throw new CustomException("redirectUris中存在非法地址");
            }
        }
    }
}
