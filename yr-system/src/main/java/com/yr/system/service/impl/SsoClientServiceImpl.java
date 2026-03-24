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
import com.yr.system.mapper.SsoClientMapper;
import com.yr.system.service.ISsoClientService;
import org.springframework.stereotype.Service;

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
        LambdaQueryWrapper<SsoClient> queryWrapper = new LambdaQueryWrapper<SsoClient>()
                .like(query != null && query.getClientCode() != null && !query.getClientCode().isBlank(), SsoClient::getClientCode, query.getClientCode())
                .like(query != null && query.getClientName() != null && !query.getClientName().isBlank(), SsoClient::getClientName, query.getClientName())
                .eq(query != null && query.getStatus() != null && !query.getStatus().isBlank(), SsoClient::getStatus, query.getStatus())
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
    public int insertSsoClient(SsoClient ssoClient) {
        if (ssoClient == null) {
            throw new CustomException("客户端信息不能为空");
        }
        if (ssoClient.getClientSecret() == null || ssoClient.getClientSecret().isBlank()) {
            // 初始骨架默认自动生成密钥，避免控制台新增时还依赖额外密钥生成流程。
            ssoClient.setClientSecret(generateClientSecret());
        }
        return this.save(ssoClient) ? 1 : 0;
    }

    /**
     * 更新客户端。
     *
     * @param ssoClient 客户端信息
     * @return 影响行数
     */
    @Override
    public int updateSsoClient(SsoClient ssoClient) {
        if (ssoClient == null || ssoClient.getClientId() == null) {
            throw new CustomException("客户端ID不能为空");
        }
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
        ssoClient.setClientSecret(clientSecret);
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
}
