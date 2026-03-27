package com.yr.framework.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;

/**
 * Redis 使用 Jackson（Jackson JSON 库）序列化
 *
 * @author Youngron
 */
public class FastJson2JsonRedisSerializer<T> implements RedisSerializer<T> {
    public static final java.nio.charset.Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    @SuppressWarnings("unused")
    private ObjectMapper objectMapper = new ObjectMapper();
    private Class<T> clazz;

    public FastJson2JsonRedisSerializer(Class<T> clazz) {
        super();
        this.clazz = clazz;
    }

    @Override
    public byte[] serialize(T t) throws SerializationException {
        if (t == null) {
            return new byte[0];
        }
        try {
            return objectMapper.writeValueAsString(t).getBytes(DEFAULT_CHARSET);
        } catch (JsonProcessingException ex) {
            throw new SerializationException("Redis 序列化失败", ex);
        }
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length <= 0) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, clazz);
        } catch (Exception ex) {
            throw new SerializationException("Redis 反序列化失败", ex);
        }
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "'objectMapper' must not be null");
        this.objectMapper = objectMapper;
    }
}
