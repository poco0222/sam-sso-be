/**
 * @file 锁定 CodeRuleUtils 序列重置策略的回归测试
 * @author Codex
 * @date 2026-03-16
 */
package com.yr.system.utils;

import com.yr.system.service.ISysCodeRuleDetailService;
import com.yr.system.service.ISysCodeRuleLineService;
import com.yr.system.service.ISysCodeRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证编码序列重置不会再走“先删除再递增”的旧实现路径。
 */
class CodeRuleUtilsSequenceResetTest {

    /**
     * 重置序列时不应先删除 key，避免多实例窗口期出现重复编号风险。
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldNotDeleteSequenceKeyBeforeResettingValue() {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("code-rule:seq", 1L)).thenReturn(1L);
        when(redisTemplate.execute(any(), anyList(), any())).thenReturn(1L);

        CodeRuleUtils target = buildCodeRuleUtils(redisTemplate);

        Long nextValue = ReflectionTestUtils.invokeMethod(target, "resetSequence", "code-rule:seq", 1L);

        assertThat(nextValue).isEqualTo(1L);
        verify(redisTemplate, never()).delete("code-rule:seq");
    }

    /**
     * 构造使用指定 RedisTemplate 的最小可测对象。
     *
     * @param redisTemplate Redis 模板
     * @return 被测试对象
     */
    private CodeRuleUtils buildCodeRuleUtils(RedisTemplate<String, String> redisTemplate) {
        return new CodeRuleUtils(
                mock(ISysCodeRuleService.class),
                mock(ISysCodeRuleLineService.class),
                mock(ISysCodeRuleDetailService.class),
                redisTemplate
        );
    }
}
