/**
 * @file 同步任务 fail-fast 契约测试
 * @author PopoY
 * @date 2026-03-30
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.exception.CustomException;
import com.yr.system.service.ISsoSyncTaskItemService;
import com.yr.system.service.support.SsoSyncTaskFailureRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

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
        ISsoSyncTaskItemService ssoSyncTaskItemService = mock(ISsoSyncTaskItemService.class);
        SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder = mock(SsoSyncTaskFailureRecorder.class);
        SsoSyncTask command = new SsoSyncTask();

        command.setTargetClientCode("sam-mgmt");
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
}
