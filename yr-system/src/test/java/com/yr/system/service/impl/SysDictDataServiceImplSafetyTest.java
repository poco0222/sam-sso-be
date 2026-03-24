/**
 * @file 验证 SysDictDataServiceImpl 的安全性契约（safety contract，安全契约）
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysDictData;
import com.yr.common.core.redis.RedisCache;
import com.yr.common.utils.DictUtils;
import com.yr.common.utils.spring.SpringUtils;
import com.yr.system.mapper.SysDictDataMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 字典数据服务安全性测试。
 */
class SysDictDataServiceImplSafetyTest {

    /**
     * 验证 dictCodes == null 时直接 return（提前返回），不抛异常，也不访问 mapper（data access，数据访问）。
     */
    @Test
    void deleteDictDataByIdsShouldReturnWhenDictCodesNull() {
        SysDictDataMapper dictDataMapper = mock(SysDictDataMapper.class);
        SysDictDataServiceImpl service = new SysDictDataServiceImpl(dictDataMapper);

        assertThatCode(() -> service.deleteDictDataByIds(null))
                .doesNotThrowAnyException();

        verifyNoInteractions(dictDataMapper);
    }

    /**
     * 验证 deleteDictDataByIds 在批量删除时遇到缺失记录（missing record，缺失记录）会跳过并继续（skip and continue，跳过并继续），
     * 并且 cache refresh（缓存刷新）只会针对实际存在的记录执行。
     */
    @Test
    void deleteDictDataByIdsShouldSkipMissingRecordsAndContinueWithExistingOnes() {
        Long missingId = 1L;
        Long existingId = 2L;
        String dictType = "type-A-" + System.nanoTime();

        SysDictDataMapper dictDataMapper = mock(SysDictDataMapper.class);
        SysDictDataServiceImpl service = new SysDictDataServiceImpl(dictDataMapper);

        // mock RedisCache，通过 SpringUtils.getBean(RedisCache.class) 获取。
        Object originalBeanFactory = ReflectionTestUtils.getField(SpringUtils.class, "beanFactory");
        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        RedisCache redisCache = mock(RedisCache.class);
        when(beanFactory.getBean(RedisCache.class)).thenReturn(redisCache);
        ReflectionTestUtils.setField(SpringUtils.class, "beanFactory", beanFactory);

        SysDictData existingData = new SysDictData();
        existingData.setDictCode(existingId);
        existingData.setDictType(dictType);

        List<SysDictData> refreshedDictDatas = Collections.singletonList(existingData);

        when(dictDataMapper.selectDictDataById(missingId)).thenReturn(null);
        when(dictDataMapper.selectDictDataById(existingId)).thenReturn(existingData);
        when(dictDataMapper.selectDictDataByType(dictType)).thenReturn(refreshedDictDatas);

        try {
            assertThatCode(() -> service.deleteDictDataByIds(new Long[]{missingId, existingId}))
                    .doesNotThrowAnyException();

            // 缺失记录应当跳过：不执行 delete。
            verify(dictDataMapper, never()).deleteDictDataById(missingId);

            // 后续存在记录仍会继续删除并刷新对应 dictType 的 cache。
            verify(dictDataMapper).deleteDictDataById(existingId);
            verify(dictDataMapper, times(1)).selectDictDataByType(dictType);
            // cache refresh 只应发生一次（缺失记录不会触发 cache refresh）。
            verify(redisCache, times(1)).setCacheObject(anyString(), any());
            verify(redisCache, times(1)).setCacheObject(DictUtils.getCacheKey(dictType), refreshedDictDatas);
        } finally {
            ReflectionTestUtils.setField(SpringUtils.class, "beanFactory", originalBeanFactory);
        }
    }

    /**
     * 验证 dictCodes 中包含 null 元素时会跳过并继续（skip and continue，跳过并继续），并且后续存在的 id 仍会正常删除与刷新缓存。
     */
    @Test
    void deleteDictDataByIdsShouldSkipNullElementAndContinueWithExistingOnes() {
        Long nullId = null;
        Long existingId = 2L;
        String dictType = "type-B-" + System.nanoTime();

        SysDictDataMapper dictDataMapper = mock(SysDictDataMapper.class);
        SysDictDataServiceImpl service = new SysDictDataServiceImpl(dictDataMapper);

        Object originalBeanFactory = ReflectionTestUtils.getField(SpringUtils.class, "beanFactory");
        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        RedisCache redisCache = mock(RedisCache.class);
        when(beanFactory.getBean(RedisCache.class)).thenReturn(redisCache);
        ReflectionTestUtils.setField(SpringUtils.class, "beanFactory", beanFactory);

        SysDictData existingData = new SysDictData();
        existingData.setDictCode(existingId);
        existingData.setDictType(dictType);

        List<SysDictData> refreshedDictDatas = Collections.singletonList(existingData);

        when(dictDataMapper.selectDictDataById(existingId)).thenReturn(existingData);
        when(dictDataMapper.selectDictDataByType(dictType)).thenReturn(refreshedDictDatas);

        try {
            assertThatCode(() -> service.deleteDictDataByIds(new Long[]{nullId, existingId}))
                    .doesNotThrowAnyException();

            // null 元素必须跳过，不得触发 mapper 访问。
            verify(dictDataMapper, never()).selectDictDataById(null);
            verify(dictDataMapper, never()).deleteDictDataById(null);

            verify(dictDataMapper).deleteDictDataById(existingId);
            verify(dictDataMapper).selectDictDataByType(dictType);
            verify(redisCache, times(1)).setCacheObject(anyString(), any());
            verify(redisCache, times(1)).setCacheObject(DictUtils.getCacheKey(dictType), refreshedDictDatas);
        } finally {
            ReflectionTestUtils.setField(SpringUtils.class, "beanFactory", originalBeanFactory);
        }
    }
}
