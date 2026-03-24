/**
 * @file 验证 SysCodeRuleServiceImpl 对坏缓存数据的容错能力
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.system.domain.entity.SysCodeRuleDetail;
import com.yr.system.service.ISysCodeRuleDetailService;
import com.yr.system.service.ISysCodeRuleLineService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 编码规则缓存容错测试。
 */
class SysCodeRuleServiceImplCacheResilienceTest {

    /**
     * 验证加载缓存时会跳过空串和损坏的 JSON，而不是让整个查询失败。
     */
    @Test
    void shouldIgnoreCorruptedCacheEntriesWhenLoadingRuleDetails() {
        HashOperations<String, String, String> hashOperations = mock(HashOperations.class);
        SysCodeRuleServiceImpl codeRuleService = new SysCodeRuleServiceImpl(
                mock(ISysCodeRuleLineService.class),
                mock(ISysCodeRuleDetailService.class),
                hashOperations,
                mock(RedisTemplate.class)
        );
        Map<String, String> cachedEntries = new LinkedHashMap<>();
        cachedEntries.put("1", "");
        cachedEntries.put("2", "{invalid-json");
        cachedEntries.put("3", "{\"ruleDetailId\":3,\"orderSeq\":3,\"fieldType\":\"CONSTANT\",\"fieldValue\":\"YR\"}");

        when(hashOperations.entries("RULE-CACHE")).thenReturn(cachedEntries);

        List<SysCodeRuleDetail> result = codeRuleService.getRuleCodeDetailListFromCache("RULE-CACHE");

        assertThat(result)
                .extracting(SysCodeRuleDetail::getRuleDetailId)
                .containsExactly(3L);
    }
}
