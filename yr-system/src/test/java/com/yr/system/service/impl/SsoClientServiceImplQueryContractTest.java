/**
 * @file 客户端查询契约测试
 * @author PopoY
 * @date 2026-04-01
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yr.common.core.domain.entity.SsoClient;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

/**
 * 锁定客户端查询辅助接口的最小查询语义，避免分发下拉与分发校验再次回退到宽查询。
 */
class SsoClientServiceImplQueryContractTest {

    /**
     * 验证分发下拉选项查询只返回启用且开启同步的客户端。
     */
    @Test
    void selectDistributionClientOptionsShouldBuildActiveAndSyncEnabledWrapper() {
        SsoClientServiceImpl service = spy(new SsoClientServiceImpl());
        AtomicReference<String> sqlSegmentRef = new AtomicReference<>();
        AtomicReference<String> sqlSelectRef = new AtomicReference<>();

        doAnswer(invocation -> {
            Wrapper<SsoClient> wrapper = invocation.getArgument(0);
            sqlSegmentRef.set(wrapper.getSqlSegment());
            sqlSelectRef.set(((QueryWrapper<SsoClient>) wrapper).getSqlSelect());
            return List.of();
        }).when(service).list(any());

        assertThatCode(service::selectDistributionClientOptions)
                .doesNotThrowAnyException();
        assertThat(sqlSelectRef.get())
                .contains("client_code")
                .contains("client_name")
                .doesNotContain("client_secret");
        assertThat(sqlSegmentRef.get())
                .contains("status")
                .contains("sync_enabled")
                .contains("ORDER BY");
    }

    /**
     * 验证按客户端编码查询会构造稳定的精确匹配条件。
     */
    @Test
    void selectSsoClientByCodeShouldBuildExactClientCodeWrapper() {
        SsoClientServiceImpl service = spy(new SsoClientServiceImpl());
        AtomicReference<String> sqlSegmentRef = new AtomicReference<>();
        AtomicReference<String> sqlSelectRef = new AtomicReference<>();

        doAnswer(invocation -> {
            Wrapper<SsoClient> wrapper = invocation.getArgument(0);
            sqlSegmentRef.set(wrapper.getSqlSegment());
            sqlSelectRef.set(((QueryWrapper<SsoClient>) wrapper).getSqlSelect());
            return null;
        }).when(service).getOne(any());

        assertThatCode(() -> service.selectSsoClientByCode("sam-mgmt"))
                .doesNotThrowAnyException();
        assertThat(sqlSelectRef.get())
                .contains("client_id")
                .contains("client_code")
                .contains("status")
                .contains("sync_enabled")
                .doesNotContain("client_secret");
        assertThat(sqlSegmentRef.get())
                .contains("client_code");
    }
}
