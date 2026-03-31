/**
 * @file 客户端密钥存储契约测试
 * @author PopoY
 * @date 2026-03-31
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SsoClient;
import com.yr.common.utils.SecurityUtils;
import com.yr.system.domain.dto.SsoClientSecretIssueResult;
import com.yr.system.mapper.SsoClientMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定客户端密钥不能继续以明文写入 service 持久化链路。
 */
class SsoClientServiceImplSecretStorageContractTest {

    /**
     * 验证新增客户端时，数据库持久化的是哈希密钥而不是明文。
     */
    @Test
    void shouldPersistHashedClientSecretInsteadOfPlainSecret() {
        SsoClientMapper mapper = mock(SsoClientMapper.class);
        SsoClientServiceImpl service = new SsoClientServiceImpl();
        ArgumentCaptor<SsoClient> clientCaptor = ArgumentCaptor.forClass(SsoClient.class);

        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        when(mapper.insert(any(SsoClient.class))).thenAnswer(invocation -> {
            SsoClient ssoClient = invocation.getArgument(0);
            ssoClient.setClientId(7L);
            return 1;
        });

        SsoClientSecretIssueResult issueResult = service.insertSsoClient(buildValidClient());

        verify(mapper).insert(clientCaptor.capture());
        assertThat(issueResult.getClientSecret()).isNotBlank();
        assertThat(clientCaptor.getValue().getClientSecret()).isNotEqualTo(issueResult.getClientSecret());
        assertThat(SecurityUtils.matchesPassword(issueResult.getClientSecret(), clientCaptor.getValue().getClientSecret())).isTrue();
    }

    /**
     * 构造最小有效客户端，供密钥持久化测试复用。
     *
     * @return 最小有效客户端
     */
    private SsoClient buildValidClient() {
        SsoClient ssoClient = new SsoClient();
        ssoClient.setClientCode("sam-mgmt");
        ssoClient.setClientName("SAM 管理后台");
        ssoClient.setRedirectUris("https://sso.example.com/callback");
        ssoClient.setAllowPasswordLogin("Y");
        ssoClient.setAllowWxworkLogin("N");
        ssoClient.setSyncEnabled("Y");
        ssoClient.setStatus("0");
        ssoClient.setCreateBy("phase5-operator");
        return ssoClient;
    }
}
