/**
 * @file SysMessageBody 时间类型与 JSON 序列化契约测试
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.domain.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yr.system.domain.entity.SysMessageBody;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SysMessageBody JSON 契约测试。
 */
class SysMessageBodySerializationTest {

    /** JSON 工具，显式注册 JavaTimeModule 以模拟 Spring Boot 默认时间序列化行为。 */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * 验证消息主体时间字段收敛到 LocalDateTime，同时保持稳定的字符串格式。
     *
     * @throws NoSuchFieldException 反射读取字段失败
     * @throws IllegalAccessException 字段写入失败
     */
    @Test
    void shouldUseLocalDateTimeAndKeepStableJsonFormat() throws NoSuchFieldException, IllegalAccessException {
        Field createAtField = SysMessageBody.class.getDeclaredField("createAt");
        Field updateAtField = SysMessageBody.class.getDeclaredField("updateAt");

        assertThat(createAtField.getType()).isEqualTo(LocalDateTime.class);
        assertThat(updateAtField.getType()).isEqualTo(LocalDateTime.class);

        SysMessageBody messageBody = new SysMessageBody();
        createAtField.setAccessible(true);
        updateAtField.setAccessible(true);
        createAtField.set(messageBody, LocalDateTime.of(2026, 3, 24, 16, 30, 45));
        updateAtField.set(messageBody, LocalDateTime.of(2026, 3, 24, 17, 45, 0));

        JsonNode node = objectMapper.valueToTree(messageBody);

        assertThat(node.path("createAt").asText()).isEqualTo("2026-03-24 16:30:45");
        assertThat(node.path("updateAt").asText()).isEqualTo("2026-03-24 17:45:00");
    }
}
