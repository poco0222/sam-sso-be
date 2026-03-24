/**
 * @file MessageJsonSerializer 行为测试，锁定 quietly 序列化不抛异常语义
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.component.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * MessageJsonSerializer 行为测试。
 */
class MessageJsonSerializerTest {

    /**
     * 验证 toJsonQuietly 在 ObjectMapper 序列化失败且对象 toString 也不可靠时，仍然绝不抛异常。
     */
    @Test
    void shouldNeverThrowFromToJsonQuietlyWhenSerializationFails() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("boom"));
        MessageJsonSerializer serializer = new MessageJsonSerializer(objectMapper);

        Object value = new Object() {
            @Override
            public String toString() {
                throw new RuntimeException("toString boom");
            }
        };

        assertThatCode(() -> serializer.toJsonQuietly(value)).doesNotThrowAnyException();
        assertThat(serializer.toJsonQuietly(value))
                .contains("message-json-serialize-failed")
                .contains(value.getClass().getName());
    }

    /**
     * 验证 toJsonQuietly 在序列化成功时返回真实 JSON 字符串。
     */
    @Test
    void shouldReturnJsonWhenToJsonQuietlySucceeds() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString("ok")).thenReturn("\"ok\"");
        MessageJsonSerializer serializer = new MessageJsonSerializer(objectMapper);

        assertThat(serializer.toJsonQuietly("ok")).isEqualTo("\"ok\"");
    }
}
