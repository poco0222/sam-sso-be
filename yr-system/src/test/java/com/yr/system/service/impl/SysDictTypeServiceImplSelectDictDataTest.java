/**
 * @file 验证 SysDictTypeServiceImpl 字典数据查询契约
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.utils.DictUtils;
import com.yr.common.utils.spring.SpringUtils;
import com.yr.common.core.redis.RedisCache;
import com.yr.system.mapper.SysDictDataMapper;
import com.yr.system.mapper.SysDictTypeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 字典数据查询契约测试。
 */
class SysDictTypeServiceImplSelectDictDataTest {

    /**
     * 验证缓存和数据库都没有数据时，服务层返回空集合而不是 null。
     */
    @Test
    void shouldReturnEmptyListWhenDictDataMissing() {
        String dictType = "missing-" + System.nanoTime();
        SysDictTypeMapper dictTypeMapper = mock(SysDictTypeMapper.class);
        SysDictDataMapper dictDataMapper = mock(SysDictDataMapper.class);
        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        RedisCache redisCache = mock(RedisCache.class);
        SysDictTypeServiceImpl dictTypeService = new SysDictTypeServiceImpl(dictTypeMapper, dictDataMapper);

        when(beanFactory.getBean(RedisCache.class)).thenReturn(redisCache);
        when(redisCache.getCacheObject(DictUtils.getCacheKey(dictType))).thenReturn(null);
        when(dictDataMapper.selectDictDataByType(dictType)).thenReturn(Collections.emptyList());
        ReflectionTestUtils.setField(SpringUtils.class, "beanFactory", beanFactory);

        try {
            List<?> result = dictTypeService.selectDictDataByType(dictType);

            assertThat(result).isEmpty();
            verify(redisCache).getCacheObject(DictUtils.getCacheKey(dictType));
            verify(dictDataMapper).selectDictDataByType(dictType);
        } finally {
            ReflectionTestUtils.setField(SpringUtils.class, "beanFactory", null);
        }
    }
}
