/**
 * @file DISTRIBUTION MQ 契约测试
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service.impl;

import com.yr.common.enums.MqActionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定 DISTRIBUTION 的 transport actionType 与现有 MQ 履历表结构兼容。
 */
class SsoDistributionMqContractTest {

    /**
     * 验证 UPSERT 语义在 transport 层仍使用单字符编码，避免打爆 `mq_message_log.action_type`。
     */
    @Test
    void shouldKeepUpsertTransportCodeCompatibleWithLegacyMqLogSchema() {
        assertThat(MqActionType.UPSERT.getCode()).isEqualTo("U");
        assertThat(MqActionType.UPSERT.getCode()).hasSize(1);
    }
}
