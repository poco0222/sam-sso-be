/**
 * @file 一期边界下的字典缓存兼容工具
 * @author PopoY
 * @date 2026-03-26
 */
package com.yr.common.utils;

import com.yr.common.constant.Constants;
import com.yr.common.utils.spring.SpringUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 字典工具类。
 *
 * 身份中心一期已经移除了 dict（字典）模块，这里只保留对历史缓存结构的兼容读取，
 * 避免 `yr-common` 再直接依赖已删除的 `SysDictData` 类型。
 */
public class DictUtils {
    /**
     * 分隔符
     */
    public static final String SEPARATOR = ",";
    /**
     * 历史字典对象中的标签属性名。
     */
    private static final String DICT_LABEL_FIELD = "dictLabel";
    /**
     * 历史字典对象中的值属性名。
     */
    private static final String DICT_VALUE_FIELD = "dictValue";

    /**
     * 设置字典缓存
     *
     * @param key       参数键
     * @param dictDatas 字典数据列表
     */
    public static void setDictCache(String key, List<?> dictDatas) {
        invokeRedisMethod("setCacheObject", new Class<?>[]{String.class, Object.class}, getCacheKey(key), dictDatas);
    }

    /**
     * 获取字典缓存
     *
     * @param key 参数键
     * @return dictDatas 字典数据列表
     */
    public static List<?> getDictCache(String key) {
        Object cacheObj = invokeRedisMethod("getCacheObject", new Class<?>[]{String.class}, getCacheKey(key));
        if (StringUtils.isNotNull(cacheObj)) {
            List<?> dictDatas = StringUtils.cast(cacheObj);
            return dictDatas;
        }
        return null;
    }

    /**
     * 根据字典类型和字典值获取字典标签
     *
     * @param dictType  字典类型
     * @param dictValue 字典值
     * @return 字典标签
     */
    public static String getDictLabel(String dictType, String dictValue) {
        return getDictLabel(dictType, dictValue, SEPARATOR);
    }

    /**
     * 根据字典类型和字典标签获取字典值
     *
     * @param dictType  字典类型
     * @param dictLabel 字典标签
     * @return 字典值
     */
    public static String getDictValue(String dictType, String dictLabel) {
        return getDictValue(dictType, dictLabel, SEPARATOR);
    }

    /**
     * 根据字典类型和字典值获取字典标签
     *
     * @param dictType  字典类型
     * @param dictValue 字典值
     * @param separator 分隔符
     * @return 字典标签
     */
    public static String getDictLabel(String dictType, String dictValue, String separator) {
        StringBuilder propertyString = new StringBuilder();
        List<?> datas = getDictCache(dictType);

        if (StringUtils.isEmpty(dictValue) || StringUtils.isEmpty(datas)) {
            return StringUtils.EMPTY;
        }

        if (StringUtils.containsAny(separator, dictValue) && StringUtils.isNotEmpty(datas)) {
            for (Object dict : datas) {
                for (String value : dictValue.split(separator)) {
                    if (value.equals(readDictValue(dict))) {
                        propertyString.append(readDictLabel(dict)).append(separator);
                        break;
                    }
                }
            }
        } else {
            for (Object dict : datas) {
                if (dictValue.equals(readDictValue(dict))) {
                    return readDictLabel(dict);
                }
            }
        }
        return StringUtils.stripEnd(propertyString.toString(), separator);
    }

    /**
     * 根据字典类型和字典标签获取字典值
     *
     * @param dictType  字典类型
     * @param dictLabel 字典标签
     * @param separator 分隔符
     * @return 字典值
     */
    public static String getDictValue(String dictType, String dictLabel, String separator) {
        StringBuilder propertyString = new StringBuilder();
        List<?> datas = getDictCache(dictType);

        if (StringUtils.isEmpty(dictLabel) || StringUtils.isEmpty(datas)) {
            return StringUtils.EMPTY;
        }

        if (StringUtils.containsAny(separator, dictLabel) && StringUtils.isNotEmpty(datas)) {
            for (Object dict : datas) {
                for (String label : dictLabel.split(separator)) {
                    if (label.equals(readDictLabel(dict))) {
                        propertyString.append(readDictValue(dict)).append(separator);
                        break;
                    }
                }
            }
        } else {
            for (Object dict : datas) {
                if (dictLabel.equals(readDictLabel(dict))) {
                    return readDictValue(dict);
                }
            }
        }
        return StringUtils.stripEnd(propertyString.toString(), separator);
    }

    /**
     * 删除指定字典缓存
     *
     * @param key 字典键
     */
    public static void removeDictCache(String key) {
        invokeRedisMethod("deleteObject", new Class<?>[]{String.class}, getCacheKey(key));
    }

    /**
     * 清空字典缓存
     */
    public static void clearDictCache() {
        Collection<String> keys = invokeRedisMethod("keys", new Class<?>[]{String.class}, Constants.SYS_DICT_KEY + "*");
        if (keys != null && !keys.isEmpty()) {
            invokeRedisMethod("deleteObject", new Class<?>[]{Collection.class}, keys);
        }
    }

    /**
     * 设置cache key
     *
     * @param configKey 参数键
     * @return 缓存键key
     */
    public static String getCacheKey(String configKey) {
        return Constants.SYS_DICT_KEY + configKey;
    }

    /**
     * 兼容读取缓存对象中的字典标签字段。
     *
     * @param dict 缓存中的单条字典对象
     * @return 字典标签
     */
    private static String readDictLabel(Object dict) {
        return readDictField(dict, DICT_LABEL_FIELD);
    }

    /**
     * 兼容读取缓存对象中的字典值字段。
     *
     * @param dict 缓存中的单条字典对象
     * @return 字典值
     */
    private static String readDictValue(Object dict) {
        return readDictField(dict, DICT_VALUE_FIELD);
    }

    /**
     * 以 Map（映射）或 JavaBean（Java Bean）方式读取历史字典缓存字段。
     *
     * @param dict 缓存中的单条字典对象
     * @param fieldName 目标字段名
     * @return 字段值字符串；不存在时返回空串
     */
    private static String readDictField(Object dict, String fieldName) {
        if (dict instanceof Map<?, ?> dictMap) {
            Object fieldValue = dictMap.get(fieldName);
            return fieldValue == null ? StringUtils.EMPTY : String.valueOf(fieldValue);
        }
        try {
            BeanWrapper beanWrapper = new BeanWrapperImpl(dict);
            Object fieldValue = beanWrapper.getPropertyValue(fieldName);
            return fieldValue == null ? StringUtils.EMPTY : String.valueOf(fieldValue);
        } catch (Exception ex) {
            return StringUtils.EMPTY;
        }
    }

    /**
     * 通过反射访问迁移到 framework 模块的 RedisCache Bean，避免 common 再直接绑定 redis 依赖。
     *
     * @param methodName 方法名
     * @param parameterTypes 参数类型
     * @param args 实参数组
     * @param <T> 返回值类型
     * @return Redis 调用结果；Bean 缺失或调用失败时返回 null
     */
    @SuppressWarnings("unchecked")
    private static <T> T invokeRedisMethod(String methodName, Class<?>[] parameterTypes, Object... args) {
        if (!SpringUtils.containsBean("redisCache")) {
            if ("keys".equals(methodName)) {
                return (T) Collections.emptyList();
            }
            return null;
        }
        try {
            Object redisCacheBean = SpringUtils.getBean("redisCache");
            Method method = redisCacheBean.getClass().getMethod(methodName, parameterTypes);
            return (T) method.invoke(redisCacheBean, args);
        } catch (Exception ex) {
            if ("keys".equals(methodName)) {
                return (T) Collections.emptyList();
            }
            return null;
        }
    }
}
