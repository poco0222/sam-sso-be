/**
 * @file 身份中心客户端服务接口
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service;

import com.yr.common.core.domain.entity.SsoClient;
import com.yr.common.mybatisplus.service.ICustomService;
import com.yr.system.domain.dto.SsoClientIntegrationGuideView;
import com.yr.system.domain.dto.SsoClientSecretIssueResult;

import java.util.List;

/**
 * 身份中心客户端服务接口。
 */
public interface ISsoClientService extends ICustomService<SsoClient> {

    /**
     * 查询客户端列表。
     *
     * @param query 查询条件
     * @return 客户端列表
     */
    List<SsoClient> selectSsoClientList(SsoClient query);

    /**
     * 查询分发任务可选客户端，只返回启用且开启同步的客户端。
     *
     * @return 分发任务可选客户端列表
     */
    List<SsoClient> selectDistributionClientOptions();

    /**
     * 按客户端编码查询客户端。
     *
     * @param clientCode 客户端编码
     * @return 客户端；不存在时返回 null
     */
    SsoClient selectSsoClientByCode(String clientCode);

    /**
     * 构建客户端接入治理说明。
     *
     * @param clientId 客户端ID
     * @return 接入说明视图
     */
    SsoClientIntegrationGuideView buildIntegrationGuide(Long clientId);

    /**
     * 新增客户端。
     *
     * @param ssoClient 客户端信息
     * @return 影响行数
     */
    SsoClientSecretIssueResult insertSsoClient(SsoClient ssoClient);

    /**
     * 更新客户端。
     *
     * @param ssoClient 客户端信息
     * @return 影响行数
     */
    int updateSsoClient(SsoClient ssoClient);

    /**
     * 轮换客户端密钥。
     *
     * @param clientId 客户端ID
     * @return 新密钥
     */
    String rotateClientSecret(Long clientId);

    /**
     * 修改客户端状态。
     *
     * @param clientId 客户端ID
     * @param status 新状态
     * @return 影响行数
     */
    int changeStatus(Long clientId, String status);
}
