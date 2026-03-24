/**
 * @file 锁定 CodeRuleUtils 重置策略边界行为的单元测试
 * @author Codex
 * @date 2026-03-16
 */
package com.yr.system.utils;

import com.yr.common.utils.DateUtils;
import com.yr.system.constant.CodeRuleConstant;
import com.yr.system.service.ISysCodeRuleDetailService;
import com.yr.system.service.ISysCodeRuleLineService;
import com.yr.system.service.ISysCodeRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证编码规则在月度、日度重置场景下的时间边界判定。
 */
class CodeRuleUtilsResetStrategyTest {

    /** 被测试对象。 */
    private final CodeRuleUtils codeRuleUtils = new CodeRuleUtils(
            mock(ISysCodeRuleService.class),
            mock(ISysCodeRuleLineService.class),
            mock(ISysCodeRuleDetailService.class),
            mock(RedisTemplate.class)
    );

    /**
     * 跨年但月份相同，按月重置仍应生效。
     */
    @Test
    void shouldResetMonthlySequenceWhenYearChangesButMonthStaysSame() {
        Boolean reset = ReflectionTestUtils.invokeMethod(
                codeRuleUtils,
                "isResetSequence",
                CodeRuleConstant.ResetFrequency.MONTH,
                date("2025-03-01 00:00:00"),
                date("2026-03-01 00:00:00")
        );

        assertThat(reset).isTrue();
    }

    /**
     * 跨月但日期相同，按日重置仍应生效。
     */
    @Test
    void shouldResetDailySequenceWhenMonthChangesButDayStaysSame() {
        Boolean reset = ReflectionTestUtils.invokeMethod(
                codeRuleUtils,
                "isResetSequence",
                CodeRuleConstant.ResetFrequency.DAY,
                date("2026-03-16 00:00:00"),
                date("2026-04-16 00:00:00")
        );

        assertThat(reset).isTrue();
    }

    /**
     * 同年同月内不应错误重置月序列。
     */
    @Test
    void shouldNotResetMonthlySequenceWithinSameYearAndMonth() {
        Boolean reset = ReflectionTestUtils.invokeMethod(
                codeRuleUtils,
                "isResetSequence",
                CodeRuleConstant.ResetFrequency.MONTH,
                date("2026-03-01 00:00:00"),
                date("2026-03-31 23:59:59")
        );

        assertThat(reset).isFalse();
    }

    /**
     * 统一构造测试时间，避免不同测试重复解析逻辑。
     *
     * @param value 标准时间字符串
     * @return 对应日期对象
     */
    private Date date(String value) {
        return DateUtils.dateTime(DateUtils.YYYY_MM_DD_HH_MM_SS, value);
    }
}
