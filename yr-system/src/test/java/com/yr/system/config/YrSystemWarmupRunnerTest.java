/**
 * @file 验证 yr-system 启动期缓存预热 runner 的行为
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.config;

import com.yr.system.service.ISysCodeRuleService;
import com.yr.system.service.ISysConfigService;
import com.yr.system.service.ISysDictTypeService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * YrSystemWarmupRunner 行为测试。
 */
class YrSystemWarmupRunnerTest {

    /**
     * 验证应用启动完成后会依次触发三类缓存预热。
     *
     * @throws Exception runner 执行异常
     */
    @Test
    void shouldWarmUpAllCachesOnApplicationRun() throws Exception {
        ISysCodeRuleService codeRuleService = mock(ISysCodeRuleService.class);
        ISysConfigService configService = mock(ISysConfigService.class);
        ISysDictTypeService dictTypeService = mock(ISysDictTypeService.class);
        YrSystemWarmupRunner warmupRunner = new YrSystemWarmupRunner(codeRuleService, configService, dictTypeService);

        when(codeRuleService.warmUpCodeRuleCache()).thenReturn(3);
        when(configService.warmUpConfigCache()).thenReturn(5);
        when(dictTypeService.warmUpDictCache()).thenReturn(7);

        warmupRunner.run(new DefaultApplicationArguments(new String[0]));

        verify(codeRuleService).warmUpCodeRuleCache();
        verify(configService).warmUpConfigCache();
        verify(dictTypeService).warmUpDictCache();
    }

    /**
     * 验证单个预热任务失败时只记录告警，不阻断其余预热与应用启动。
     */
    @Test
    void shouldContinueStartupWhenSingleWarmupFails() {
        ISysCodeRuleService codeRuleService = mock(ISysCodeRuleService.class);
        ISysConfigService configService = mock(ISysConfigService.class);
        ISysDictTypeService dictTypeService = mock(ISysDictTypeService.class);
        YrSystemWarmupRunner warmupRunner = new YrSystemWarmupRunner(codeRuleService, configService, dictTypeService);

        when(codeRuleService.warmUpCodeRuleCache()).thenReturn(3);
        doThrow(new IllegalStateException("boom")).when(configService).warmUpConfigCache();
        when(dictTypeService.warmUpDictCache()).thenReturn(7);

        assertThatCode(() -> warmupRunner.run(new DefaultApplicationArguments(new String[0])))
                .doesNotThrowAnyException();

        verify(codeRuleService).warmUpCodeRuleCache();
        verify(configService).warmUpConfigCache();
        verify(dictTypeService).warmUpDictCache();
    }
}
