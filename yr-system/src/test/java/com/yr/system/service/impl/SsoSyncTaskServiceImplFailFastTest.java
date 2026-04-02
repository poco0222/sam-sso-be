/**
 * @file 同步任务 fail-fast 契约测试
 * @author PopoY
 * @date 2026-03-30
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SsoClient;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.exception.CustomException;
import com.yr.system.service.ISsoClientService;
import com.yr.system.service.ISsoSyncTaskItemService;
import com.yr.system.service.support.SsoSyncTaskFailureRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 锁定同步任务在关键执行器缺失时的 fail-fast 行为。
 */
class SsoSyncTaskServiceImplFailFastTest {

    /**
     * 验证 DISTRIBUTION 执行器缺失时必须抛出明确异常，而不是返回伪成功任务。
     */
    @Test
    void shouldFailFastWhenDistributionExecutorIsMissing() {
        SsoSyncTaskServiceImpl service = new SsoSyncTaskServiceImpl();
        ISsoClientService ssoClientService = mock(ISsoClientService.class);
        ISsoSyncTaskItemService ssoSyncTaskItemService = mock(ISsoSyncTaskItemService.class);
        SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder = mock(SsoSyncTaskFailureRecorder.class);
        SsoSyncTask command = new SsoSyncTask();

        command.setTargetClientCode("sam-mgmt");
        when(ssoClientService.selectSsoClientByCode(anyString()))
                .thenAnswer(invocation -> buildEnabledDistributionClient(invocation.getArgument(0)));
        ReflectionTestUtils.setField(service, "ssoClientService", ssoClientService);
        ReflectionTestUtils.setField(service, "ssoSyncTaskItemService", ssoSyncTaskItemService);
        ReflectionTestUtils.setField(service, "ssoSyncTaskFailureRecorder", ssoSyncTaskFailureRecorder);
        doAnswer(invocation -> {
            SsoSyncTask task = invocation.getArgument(0);
            task.setTaskId(31L);
            return null;
        }).when(ssoSyncTaskFailureRecorder).persistNewTask(any(SsoSyncTask.class));

        assertThatThrownBy(() -> service.distributionTask(command))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("执行器");
    }

    /**
     * 验证目标客户端不存在时，分发任务必须在执行前明确失败。
     */
    @Test
    void shouldFailWhenDistributionTargetClientDoesNotExist() {
        SsoSyncTaskServiceImpl service = new SsoSyncTaskServiceImpl();
        ISsoClientService ssoClientService = mock(ISsoClientService.class);
        SsoSyncTask command = new SsoSyncTask();

        command.setTargetClientCode("missing-client");
        when(ssoClientService.selectSsoClientByCode("missing-client")).thenReturn(null);
        ReflectionTestUtils.setField(service, "ssoClientService", ssoClientService);

        assertThatThrownBy(() -> service.distributionTask(command))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("目标客户端不存在");
    }

    /**
     * 验证目标客户端停用时，分发任务必须在执行前明确失败。
     */
    @Test
    void shouldFailWhenDistributionTargetClientIsDisabled() {
        SsoSyncTaskServiceImpl service = new SsoSyncTaskServiceImpl();
        ISsoClientService ssoClientService = mock(ISsoClientService.class);
        SsoSyncTask command = new SsoSyncTask();

        command.setTargetClientCode("sam-disabled");
        when(ssoClientService.selectSsoClientByCode("sam-disabled"))
                .thenReturn(buildClient("sam-disabled", "1", "Y"));
        ReflectionTestUtils.setField(service, "ssoClientService", ssoClientService);

        assertThatThrownBy(() -> service.distributionTask(command))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("目标客户端已停用");
    }

    /**
     * 验证目标客户端未开启同步时，分发任务必须在执行前明确失败。
     */
    @Test
    void shouldFailWhenDistributionTargetClientSyncIsDisabled() {
        SsoSyncTaskServiceImpl service = new SsoSyncTaskServiceImpl();
        ISsoClientService ssoClientService = mock(ISsoClientService.class);
        SsoSyncTask command = new SsoSyncTask();

        command.setTargetClientCode("sam-no-sync");
        when(ssoClientService.selectSsoClientByCode("sam-no-sync"))
                .thenReturn(buildClient("sam-no-sync", "0", "N"));
        ReflectionTestUtils.setField(service, "ssoClientService", ssoClientService);

        assertThatThrownBy(() -> service.distributionTask(command))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("目标客户端未启用同步");
    }

    /**
     * 构造默认可用于 DISTRIBUTION 的合法客户端。
     *
     * @param clientCode 客户端编码
     * @return 合法客户端
     */
    private SsoClient buildEnabledDistributionClient(String clientCode) {
        return buildClient(clientCode, "0", "Y");
    }

    /**
     * 构造客户端夹具。
     *
     * @param clientCode 客户端编码
     * @param status 客户端状态
     * @param syncEnabled 是否开启同步
     * @return 客户端
     */
    private SsoClient buildClient(String clientCode, String status, String syncEnabled) {
        SsoClient client = new SsoClient();
        client.setClientCode(clientCode);
        client.setClientName("test-" + clientCode);
        client.setStatus(status);
        client.setSyncEnabled(syncEnabled);
        client.setAllowPasswordLogin("Y");
        client.setAllowWxworkLogin("Y");
        return client;
    }
}
