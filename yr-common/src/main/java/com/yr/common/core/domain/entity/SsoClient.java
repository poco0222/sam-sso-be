/**
 * @file 身份中心客户端实体
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.common.core.domain.entity;

import com.yr.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 身份中心客户端实体，承载一期客户端管理控制台的最小字段集。
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SsoClient extends BaseEntity {

    /** 客户端主键。 */
    private Long clientId;

    /** 客户端编码，作为跨系统稳定标识。 */
    private String clientCode;

    /** 客户端名称。 */
    private String clientName;

    /** 客户端密钥。 */
    private String clientSecret;

    /** 允许回调的 URI 列表，使用文本保存。 */
    private String redirectUris;

    /** 是否允许账号密码登录。 */
    private String allowPasswordLogin;

    /** 是否允许企业微信登录。 */
    private String allowWxworkLogin;

    /** 是否启用主数据同步。 */
    private String syncEnabled;

    /** 客户端状态。 */
    private String status;
}
