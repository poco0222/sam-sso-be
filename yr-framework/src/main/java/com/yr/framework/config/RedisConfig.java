/**
 * @file Redis 序列化配置
 * @author PopoY
 * @date 2026-03-31
 */
package com.yr.framework.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;

/**
 * Redis（缓存）基础配置。
 *
 * @author PopoY
 */
@Configuration
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport {
    /**
     * 构建 RedisTemplate（Redis 模板），统一约束 key/value 的序列化方式。
     *
     * @param connectionFactory Redis 连接工厂
     * @return RedisTemplate 实例
     */
    @Bean
    @SuppressWarnings(value = {"unchecked", "rawtypes"})
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        FastJson2JsonRedisSerializer serializer = new FastJson2JsonRedisSerializer(Object.class);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        ObjectMapper mapper = buildRedisObjectMapper();
        serializer.setObjectMapper(mapper);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(serializer);

        // Hash的key也采用StringRedisSerializer的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(serializer);

        template.setDefaultSerializer(stringRedisSerializer);
        template.setStringSerializer(stringRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        buildRedisTemplate(redisTemplate, redisConnectionFactory);
        return redisTemplate;
    }

    /**
     * @return Hash 处理类
     */
    @Bean
    public HashOperations<String, String, String> hashOperations(StringRedisTemplate redisTemplate) {
        return redisTemplate.opsForHash();
    }

    /**
     * 构建 Redis 专用 ObjectMapper（对象映射器）。
     *
     * @return Redis 专用对象映射器
     */
    private ObjectMapper buildRedisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.addMixIn(SimpleGrantedAuthority.class, SimpleGrantedAuthorityRedisMixin.class);
        mapper.activateDefaultTyping(
                buildRedisTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }

    /**
     * 构建受限的多态类型校验器，仅允许登录缓存当前需要的白名单类型进入反序列化。
     *
     * @return Redis 多态类型校验器
     */
    private PolymorphicTypeValidator buildRedisTypeValidator() {
        return BasicPolymorphicTypeValidator.builder()
                // 兼容历史 NON_FINAL payload，同时把新写入多态面收敛到 live 代码已证实的最小类型集合。
                .allowIfSubType(LoginUser.class)
                .allowIfSubType(SysUser.class)
                .allowIfSubType(SysDept.class)
                .allowIfSubType(LinkedHashSet.class)
                .allowIfSubType(ArrayList.class)
                .allowIfSubType(HashMap.class)
                .allowIfSubType(Date.class)
                .allowIfSubType(Long.class)
                .allowIfSubType(SimpleGrantedAuthority.class)
                .build();
    }

    /**
     * 为 SimpleGrantedAuthority 提供 Redis 反序列化所需的 Jackson 构造签名。
     */
    private abstract static class SimpleGrantedAuthorityRedisMixin {

        /**
         * @param role Spring Security 角色编码
         */
        @JsonCreator
        SimpleGrantedAuthorityRedisMixin(@JsonProperty("role") String role) {
        }
    }

    /**
     * 为 StringRedisTemplate（字符串 Redis 模板）设置纯字符串序列化器。
     *
     * @param redisTemplate StringRedisTemplate 实例
     * @param redisConnectionFactory Redis 连接工厂
     */
    private void buildRedisTemplate(RedisTemplate<String, String> redisTemplate, RedisConnectionFactory redisConnectionFactory) {
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setStringSerializer(stringRedisSerializer);
        redisTemplate.setDefaultSerializer(stringRedisSerializer);
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        redisTemplate.setHashValueSerializer(stringRedisSerializer);
        redisTemplate.setValueSerializer(stringRedisSerializer);
        redisTemplate.setConnectionFactory(redisConnectionFactory);
    }
}
