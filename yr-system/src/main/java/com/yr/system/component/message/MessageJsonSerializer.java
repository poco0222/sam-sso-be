/**
 * @file 站内消息链路统一 JSON serializer（JSON 序列化器）
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.component.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * 站内消息链路统一 JSON 序列化入口。
 *
 * <p>约束点：</p>
 * <ul>
 *     <li>优先复用 Spring Boot 默认提供的 {@link ObjectMapper}（Jackson 对象映射器）。</li>
 *     <li>避免在业务类中散落多个 try/catch 或 {@code new ObjectMapper()}。</li>
 * </ul>
 */
@Component
public class MessageJsonSerializer {

    /** Spring Boot 管理的 Jackson ObjectMapper。 */
    private final ObjectMapper objectMapper;

    /**
     * 构造统一序列化器。
     *
     * @param objectMapper Spring 注入的 ObjectMapper
     */
    public MessageJsonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 把对象序列化为 JSON 字符串。
     *
     * @param value 待序列化对象
     * @return JSON 字符串
     * @throws IllegalStateException 当序列化失败时抛出（保持调用侧签名不引入 checked exception）
     */
    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化消息为 JSON 失败", ex);
        } catch (RuntimeException ex) {
            // ObjectMapper 也可能抛出 runtime exception，这里统一 wrap 保持 JavaDoc 与实现一致。
            throw new IllegalStateException("序列化消息为 JSON 失败", ex);
        }
    }

    /**
     * 以“尽量不抛异常”的方式把对象序列化为 JSON 字符串，主要用于日志场景。
     *
     * <p>日志链路中如果序列化再次抛错，往往会掩盖原始异常；因此这里提供一个兜底入口。</p>
     *
     * @param value 待序列化对象
     * @return JSON 字符串；若序列化失败则返回固定兜底字符串（不会依赖 value.toString()）
     */
    public String toJsonQuietly(Object value) {
        try {
            return toJson(value);
        } catch (Throwable ex) {
            // 绝不能因为日志序列化失败而影响主流程（例如覆盖原始异常）。
            String typeName = value == null ? "null" : value.getClass().getName();
            return "<message-json-serialize-failed type=" + typeName + ">";
        }
    }
}
