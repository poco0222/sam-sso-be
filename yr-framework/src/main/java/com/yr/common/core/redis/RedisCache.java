/**
 * @file Redis 缓存工具实现，迁移到 framework 模块承载 Redis 基础设施
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.common.core.redis;

import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Spring Redis（Redis 访问框架）工具类。
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@Component
public class RedisCache {
    /** Redis 模板，统一封装常用缓存操作。 */
    private final RedisTemplate redisTemplate;

    /**
     * 构造 Redis 缓存工具。
     *
     * @param redisTemplate Spring Redis 模板
     */
    public RedisCache(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 缓存基本对象。
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param <T> 值类型
     */
    public <T> void setCacheObject(final String key, final T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本对象并指定过期时间。
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     * @param <T> 值类型
     */
    public <T> void setCacheObject(final String key, final T value, final Integer timeout, final TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 设置缓存过期时间，默认单位为秒。
     *
     * @param key Redis 键
     * @param timeout 超时时间
     * @return 是否设置成功
     */
    public boolean expire(final String key, final long timeout) {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置缓存过期时间。
     *
     * @param key Redis 键
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 是否设置成功
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取缓存对象。
     *
     * @param key 缓存键
     * @param <T> 返回值类型
     * @return 缓存值
     */
    public <T> T getCacheObject(final String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取缓存中的整数值。
     *
     * @param key 缓存键
     * @return 整数值；不存在时返回 null
     */
    public Integer getCacheInteger(final String key) {
        Object cacheValue = redisTemplate.opsForValue().get(key);
        if (cacheValue == null) {
            return null;
        }
        return Integer.valueOf(cacheValue.toString());
    }

    /**
     * 删除单个缓存对象。
     *
     * @param key 缓存键
     * @return 是否删除成功
     */
    public boolean deleteObject(final String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存对象。
     *
     * @param collection 缓存键集合
     * @return 删除数量
     */
    public long deleteObject(final Collection collection) {
        return redisTemplate.delete(collection);
    }

    /**
     * 缓存 List（列表）数据。
     *
     * @param key 缓存键
     * @param dataList 列表数据
     * @param <T> 元素类型
     * @return 写入数量
     */
    public <T> long setCacheList(final String key, final List<T> dataList) {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 : count;
    }

    /**
     * 获取缓存中的列表数据。
     *
     * @param key 缓存键
     * @param <T> 元素类型
     * @return 列表数据
     */
    public <T> List<T> getCacheList(final String key) {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * 缓存 Set（集合）数据。
     *
     * @param key 缓存键
     * @param dataSet 集合数据
     * @param <T> 元素类型
     * @return Set 操作句柄
     */
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet) {
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        Iterator<T> iterator = dataSet.iterator();
        while (iterator.hasNext()) {
            setOperation.add(iterator.next());
        }
        return setOperation;
    }

    /**
     * 获取缓存中的集合数据。
     *
     * @param key 缓存键
     * @param <T> 元素类型
     * @return 集合数据
     */
    public <T> Set<T> getCacheSet(final String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 缓存 Map（映射）数据。
     *
     * @param key 缓存键
     * @param dataMap Map 数据
     * @param <T> 值类型
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap) {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 获取缓存中的 Map（映射）数据。
     *
     * @param key 缓存键
     * @param <T> 值类型
     * @return Map 数据
     */
    public <T> Map<String, T> getCacheMap(final String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 向 Hash（哈希）中写入单个字段。
     *
     * @param key Redis 键
     * @param hKey Hash 键
     * @param value 值
     * @param <T> 值类型
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value) {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 获取 Hash（哈希）中的单个字段。
     *
     * @param key Redis 键
     * @param hKey Hash 键
     * @param <T> 值类型
     * @return 值
     */
    public <T> T getCacheMapValue(final String key, final String hKey) {
        HashOperations<String, String, T> hashOperations = redisTemplate.opsForHash();
        return hashOperations.get(key, hKey);
    }

    /**
     * 批量获取 Hash（哈希）字段值。
     *
     * @param key Redis 键
     * @param hKeys Hash 键集合
     * @param <T> 值类型
     * @return 值列表
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys) {
        return redisTemplate.opsForHash().multiGet(key, hKeys);
    }

    /**
     * 按模式匹配缓存键。
     *
     * @param pattern 键模式
     * @return 匹配到的缓存键集合
     */
    public Collection<String> keys(final String pattern) {
        return redisTemplate.keys(pattern);
    }
}
