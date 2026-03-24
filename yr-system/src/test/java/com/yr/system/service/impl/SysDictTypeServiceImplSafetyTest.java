/**
 * @file 验证 SysDictTypeServiceImpl 的安全性契约（safety contract，安全契约）
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysDictData;
import com.yr.common.core.domain.entity.SysDictType;
import com.yr.common.core.redis.RedisCache;
import com.yr.common.exception.CustomException;
import com.yr.common.utils.DictUtils;
import com.yr.common.utils.spring.SpringUtils;
import com.yr.system.mapper.SysDictDataMapper;
import com.yr.system.mapper.SysDictTypeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 字典类型服务安全性测试。
 *
 * <p>关注点是避免因为缺失数据导致的 NPE（NullPointerException，空指针异常），并确保 cache refresh（缓存刷新）
 * 的行为在成功路径上仍然正确。</p>
 */
class SysDictTypeServiceImplSafetyTest {

    /**
     * 验证当旧字典类型记录缺失（oldDict == null）时，服务层抛出 CustomException（business exception，业务异常），
     * 而不是泄漏 NullPointerException（空指针异常）。
     */
    @Test
    void updateDictTypeShouldThrowCustomExceptionWhenOldDictMissing() {
        Long dictId = 1001L;
        String newDictType = "new-" + System.nanoTime();

        // mock Mapper 依赖，避免触发真实数据库访问。
        SysDictTypeMapper dictTypeMapper = mock(SysDictTypeMapper.class);
        SysDictDataMapper dictDataMapper = mock(SysDictDataMapper.class);

        // mock RedisCache，通过 SpringUtils.getBean(RedisCache.class) 获取。
        Object originalBeanFactory = ReflectionTestUtils.getField(SpringUtils.class, "beanFactory");
        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        RedisCache redisCache = mock(RedisCache.class);
        when(beanFactory.getBean(RedisCache.class)).thenReturn(redisCache);
        ReflectionTestUtils.setField(SpringUtils.class, "beanFactory", beanFactory);

        SysDictTypeServiceImpl service = new SysDictTypeServiceImpl(dictTypeMapper, dictDataMapper);

        SysDictType dict = new SysDictType();
        dict.setDictId(dictId);
        dict.setDictType(newDictType);

        // 缺失旧记录：当前 root cause 会导致 oldDict.getDictType() 触发 NPE。
        when(dictTypeMapper.selectDictTypeById(dictId)).thenReturn(null);

        try {
            assertThatThrownBy(() -> service.updateDictType(dict))
                    .isInstanceOf(CustomException.class);

            // 缺失记录时应当短路，避免误触发 cache refresh（缓存刷新）。
            verify(redisCache, never()).setCacheObject(anyString(), any());
            verify(dictDataMapper, never()).updateDictDataType(anyString(), anyString());
            verify(dictTypeMapper, never()).updateDictType(any(SysDictType.class));
        } finally {
            // restore SpringUtils 静态注入，保证测试隔离（test isolation，测试隔离性）。
            ReflectionTestUtils.setField(SpringUtils.class, "beanFactory", originalBeanFactory);
        }
    }

    /**
     * 验证 updateDictType 成功更新时，仍会刷新新字典类型（new dictType，新类型）的缓存。
     */
    @Test
    void updateDictTypeShouldRefreshCacheForNewDictTypeWhenUpdateSucceeded() {
        Long dictId = 2002L;
        String oldDictType = "old-" + System.nanoTime();
        String newDictType = "new-" + System.nanoTime();

        SysDictTypeMapper dictTypeMapper = mock(SysDictTypeMapper.class);
        SysDictDataMapper dictDataMapper = mock(SysDictDataMapper.class);

        Object originalBeanFactory = ReflectionTestUtils.getField(SpringUtils.class, "beanFactory");
        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        RedisCache redisCache = mock(RedisCache.class);
        when(beanFactory.getBean(RedisCache.class)).thenReturn(redisCache);
        ReflectionTestUtils.setField(SpringUtils.class, "beanFactory", beanFactory);

        SysDictTypeServiceImpl service = new SysDictTypeServiceImpl(dictTypeMapper, dictDataMapper);

        SysDictType oldDict = new SysDictType();
        oldDict.setDictId(dictId);
        oldDict.setDictType(oldDictType);

        SysDictType dict = new SysDictType();
        dict.setDictId(dictId);
        dict.setDictType(newDictType);

        List<SysDictData> refreshedDictDatas = Collections.singletonList(new SysDictData());

        when(dictTypeMapper.selectDictTypeById(dictId)).thenReturn(oldDict);
        when(dictTypeMapper.updateDictType(dict)).thenReturn(1);
        when(dictDataMapper.selectDictDataByType(newDictType)).thenReturn(refreshedDictDatas);

        try {
            assertThatCode(() -> service.updateDictType(dict)).doesNotThrowAnyException();

            // 成功路径应迁移 dict_type，清理旧 cache key，并刷新新 dictType 的 cache。
            verify(dictDataMapper).updateDictDataType(oldDictType, newDictType);
            verify(dictTypeMapper).updateDictType(dict);
            verify(dictDataMapper).selectDictDataByType(newDictType);
            verify(redisCache, times(1)).deleteObject(DictUtils.getCacheKey(oldDictType));
            verify(redisCache, times(1))
                    .setCacheObject(DictUtils.getCacheKey(newDictType), refreshedDictDatas);
        } finally {
            ReflectionTestUtils.setField(SpringUtils.class, "beanFactory", originalBeanFactory);
        }
    }

    /**
     * 验证当 update 行数为 0（update row == 0，未更新任何行）时，服务层必须视为失败并抛 CustomException，
     * 且不应继续执行 dict_type 迁移与 cache refresh（缓存刷新）。
     */
    @Test
    void updateDictTypeShouldThrowCustomExceptionWhenUpdateRowIsZero() {
        Long dictId = 3003L;
        String oldDictType = "old-" + System.nanoTime();
        String newDictType = "new-" + System.nanoTime();

        SysDictTypeMapper dictTypeMapper = mock(SysDictTypeMapper.class);
        SysDictDataMapper dictDataMapper = mock(SysDictDataMapper.class);

        Object originalBeanFactory = ReflectionTestUtils.getField(SpringUtils.class, "beanFactory");
        ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
        RedisCache redisCache = mock(RedisCache.class);
        when(beanFactory.getBean(RedisCache.class)).thenReturn(redisCache);
        ReflectionTestUtils.setField(SpringUtils.class, "beanFactory", beanFactory);

        SysDictTypeServiceImpl service = new SysDictTypeServiceImpl(dictTypeMapper, dictDataMapper);

        SysDictType oldDict = new SysDictType();
        oldDict.setDictId(dictId);
        oldDict.setDictType(oldDictType);

        SysDictType dict = new SysDictType();
        dict.setDictId(dictId);
        dict.setDictType(newDictType);

        when(dictTypeMapper.selectDictTypeById(dictId)).thenReturn(oldDict);
        when(dictTypeMapper.updateDictType(dict)).thenReturn(0);

        try {
            assertThatThrownBy(() -> service.updateDictType(dict))
                    .isInstanceOf(CustomException.class);

            // update 失败时不应发生 dict_type 迁移与 cache refresh（缓存刷新）。
            verify(dictDataMapper, never()).updateDictDataType(anyString(), anyString());
            verify(redisCache, never()).deleteObject(anyString());
            verify(redisCache, never()).setCacheObject(anyString(), any());
        } finally {
            ReflectionTestUtils.setField(SpringUtils.class, "beanFactory", originalBeanFactory);
        }
    }
}
