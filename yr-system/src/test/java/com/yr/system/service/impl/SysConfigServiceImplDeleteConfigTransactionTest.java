/**
 * @file 验证 SysConfigServiceImpl 批量删除参数时的事务与前置校验契约
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.constant.Constants;
import com.yr.common.constant.UserConstants;
import com.yr.common.core.redis.RedisCache;
import com.yr.common.exception.CustomException;
import com.yr.system.domain.SysConfig;
import com.yr.system.mapper.SysConfigMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SysConfigServiceImpl 批量删除事务测试。
 */
class SysConfigServiceImplDeleteConfigTransactionTest {

    /**
     * 验证批量删除遇到内置参数时，会在任何删除发生前拒绝整批请求。
     */
    @Test
    void shouldRejectWholeBatchBeforeDeletingWhenBuiltInConfigExists() {
        SysConfigMapper configMapper = mock(SysConfigMapper.class);
        RedisCache redisCache = mock(RedisCache.class);
        SysConfigServiceImpl service = new SysConfigServiceImpl(configMapper, redisCache);
        SysConfig normalConfig = buildConfig(1L, "sys.demo.normal", "N");
        SysConfig builtInConfig = buildConfig(2L, "sys.demo.builtin", UserConstants.YES);

        when(configMapper.selectConfig(argThat(config -> config != null && Long.valueOf(1L).equals(config.getConfigId()))))
                .thenReturn(normalConfig);
        when(configMapper.selectConfig(argThat(config -> config != null && Long.valueOf(2L).equals(config.getConfigId()))))
                .thenReturn(builtInConfig);

        assertThatThrownBy(() -> service.deleteConfigByIds(new Long[]{1L, 2L}))
                .isInstanceOf(CustomException.class)
                .hasMessage("内置参数【sys.demo.builtin】不能删除 ");

        verify(configMapper, never()).deleteConfigById(1L);
        verify(configMapper, never()).deleteConfigById(2L);
    }

    /**
     * 验证批量删除未完成前，不会提前驱逐任何参数缓存。
     */
    @Test
    void shouldEvictCacheOnlyAfterConfigDeleteSucceeds() {
        SysConfigMapper configMapper = mock(SysConfigMapper.class);
        RedisCache redisCache = mock(RedisCache.class);
        SysConfigServiceImpl service = new SysConfigServiceImpl(configMapper, redisCache);
        SysConfig normalConfig = buildConfig(1L, "sys.demo.normal", "N");
        SysConfig builtInConfig = buildConfig(2L, "sys.demo.builtin", UserConstants.YES);

        when(configMapper.selectConfig(argThat(config -> config != null && Long.valueOf(1L).equals(config.getConfigId()))))
                .thenReturn(normalConfig);
        when(configMapper.selectConfig(argThat(config -> config != null && Long.valueOf(2L).equals(config.getConfigId()))))
                .thenReturn(builtInConfig);

        assertThatThrownBy(() -> service.deleteConfigByIds(new Long[]{1L, 2L}))
                .isInstanceOf(CustomException.class)
                .hasMessage("内置参数【sys.demo.builtin】不能删除 ");

        verify(redisCache, never()).deleteObject(Constants.SYS_CONFIG_KEY + normalConfig.getConfigKey());
        verify(redisCache, never()).deleteObject(Constants.SYS_CONFIG_KEY + builtInConfig.getConfigKey());
        verifyNoInteractions(redisCache);
    }

    /**
     * 构造最小参数配置。
     *
     * @param configId 参数 ID
     * @param configKey 参数键
     * @param configType 参数类型
     * @return 参数配置对象
     */
    private SysConfig buildConfig(Long configId, String configKey, String configType) {
        SysConfig config = new SysConfig();
        config.setConfigId(configId);
        config.setConfigKey(configKey);
        config.setConfigType(configType);
        return config;
    }
}
