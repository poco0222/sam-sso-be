/**
 * @file 同步任务重试安全契约测试
 * @author PopoY
 * @date 2026-04-01
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.exception.CustomException;
import com.yr.system.mapper.SsoSyncTaskMapper;
import com.yr.system.service.support.SsoSyncTaskFailureRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定同步任务重试只能在失败态发生，并对并发状态漂移保持受控失败。
 */
class SsoSyncTaskRetrySafetyTest {

    /**
     * 验证非失败态任务不允许直接重试。
     */
    @Test
    void shouldRejectRetryWhenTaskIsNotFailed() {
        SsoSyncTaskMapper mapper = mock(SsoSyncTaskMapper.class);
        SsoSyncTaskServiceImpl service = createService(mapper);
        SsoSyncTask task = buildTask(SsoSyncTask.STATUS_SUCCESS);

        when(mapper.selectById(7L)).thenReturn(task);

        assertThatThrownBy(() -> service.retryTask(7L))
                .isInstanceOf(CustomException.class)
                .hasMessage("只有失败态任务允许重试");
        verify(mapper, never()).update(any(), any());
    }

    /**
     * 验证失败态任务在状态条件更新失败时会返回受控并发提示。
     */
    @Test
    void shouldRejectRetryWhenTaskStateTransitionIsLost() {
        SsoSyncTaskMapper mapper = mock(SsoSyncTaskMapper.class);
        SsoSyncTaskServiceImpl service = createService(mapper);
        SsoSyncTask task = buildTask(SsoSyncTask.STATUS_FAILED);

        when(mapper.selectById(7L)).thenReturn(task);
        when(mapper.update(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.retryTask(7L))
                .isInstanceOf(CustomException.class)
                .hasMessage("任务状态已变化，请刷新后重试");
    }

    /**
     * 构造待测服务。
     *
     * @param mapper 同步任务 Mapper
     * @return 待测服务
     */
    private SsoSyncTaskServiceImpl createService(SsoSyncTaskMapper mapper) {
        SsoSyncTaskServiceImpl service = new SsoSyncTaskServiceImpl();

        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "ssoSyncTaskFailureRecorder", mock(SsoSyncTaskFailureRecorder.class));
        return service;
    }

    /**
     * 构造最小同步任务。
     *
     * @param status 任务状态
     * @return 测试任务
     */
    private SsoSyncTask buildTask(String status) {
        SsoSyncTask task = new SsoSyncTask();

        task.setTaskId(7L);
        task.setTaskType(SsoSyncTask.TASK_TYPE_INIT_IMPORT);
        task.setStatus(status);
        task.setRetryCount(0);
        task.setCreateBy("retry-tester");
        return task;
    }
}
