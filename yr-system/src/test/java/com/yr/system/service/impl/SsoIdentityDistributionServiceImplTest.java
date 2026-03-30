/**
 * @file DISTRIBUTION 执行服务测试
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.enums.MqActionType;
import com.yr.common.service.MqProducerService;
import com.yr.system.domain.dto.SsoSyncTaskExecutionResult;
import com.yr.system.service.support.SsoCurrentIdentitySnapshotLoader;
import com.yr.system.service.support.SsoDistributionDispatchResultRecorder;
import com.yr.system.service.support.SsoSyncTaskFailureRecorder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 DISTRIBUTION 执行服务会从当前主数据快照生成 item，并逐条发送 RocketMQ。
 */
@ExtendWith(MockitoExtension.class)
class SsoIdentityDistributionServiceImplTest {

    /** 当前主数据快照读取器。 */
    @Mock
    private SsoCurrentIdentitySnapshotLoader ssoCurrentIdentitySnapshotLoader;

    /** MQ 发送服务。 */
    @Mock
    private MqProducerService mqProducerService;

    /** after-commit 结果回写器。 */
    @Mock
    private SsoDistributionDispatchResultRecorder ssoDistributionDispatchResultRecorder;

    /** after-commit 失败兜底记录器。 */
    @Mock
    private SsoSyncTaskFailureRecorder ssoSyncTaskFailureRecorder;

    /**
     * 验证 full-batch distribution 会生成五类 item，并用 UPSERT 契约发 MQ。
     */
    @Test
    void shouldExecuteDistributionFromCurrentMasterSnapshot() {
        SsoIdentityDistributionServiceImpl service = createService();
        when(ssoCurrentIdentitySnapshotLoader.loadSnapshot()).thenReturn(SsoDistributionTestFixtures.minimalSnapshot());
        when(mqProducerService.send(any(), any(), any(), any(), any()))
                .thenReturn(true, true, false, true, true);

        SsoSyncTaskExecutionResult result = service.execute(buildTask(), null);

        assertThat(result.getStatus()).isEqualTo("PARTIAL_SUCCESS");
        assertThat(result.getTotalItemCount()).isEqualTo(5);
        assertThat(result.getSuccessItemCount()).isEqualTo(4);
        assertThat(result.getFailedItemCount()).isEqualTo(1);
        assertThat(result.getItemList()).hasSize(5);
        assertThat(result.getItemList()).filteredOn(item -> "FAILED".equals(item.getStatus()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getMsgKey()).isEqualTo("DIST:88:user:301");
                    assertThat(item.getErrorMessage()).contains("MQ 发送失败");
                });
        assertThat(result.getItemList()).filteredOn(item -> "SUCCESS".equals(item.getStatus()))
                .allSatisfy(item -> assertThat(item.getTargetId()).isEqualTo(item.getSourceId()));

        verify(mqProducerService).send(
                eq("sso-identity-distribution"),
                eq("sam-mgmt"),
                eq(MqActionType.UPSERT),
                eq("DIST:88:org:101"),
                any()
        );
        verify(mqProducerService).send(
                eq("sso-identity-distribution"),
                eq("sam-mgmt"),
                eq(MqActionType.UPSERT),
                eq("DIST:88:user:301"),
                any()
        );
    }

    /**
     * 构造最小任务对象。
     */
    private SsoSyncTask buildTask() {
        SsoSyncTask task = new SsoSyncTask();
        task.setTaskId(88L);
        task.setBatchNo("DIST-88");
        task.setTargetClientCode("sam-mgmt");
        task.setCreateBy("tester");
        return task;
    }

    /**
     * 构造待测服务。
     */
    private SsoIdentityDistributionServiceImpl createService() {
        return new SsoIdentityDistributionServiceImpl(
                ssoCurrentIdentitySnapshotLoader,
                mqProducerService,
                new ObjectMapper(),
                ssoDistributionDispatchResultRecorder,
                ssoSyncTaskFailureRecorder
        );
    }
}
