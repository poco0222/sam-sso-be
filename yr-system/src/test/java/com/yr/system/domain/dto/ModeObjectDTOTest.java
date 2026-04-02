/**
 * @file ModeObjectDTO JSON 契约测试
 * @author PopoY
 * @date 2026-03-11
 */
package com.yr.system.domain.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ModeObjectDTO JSON 契约测试。
 */
class ModeObjectDTOTest {

    /** JSON 工具。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证 ModeObjectDTO 的 JSON 字段名在改造前后保持一致。
     */
    @Test
    void shouldKeepJsonFieldNamesStable() throws Exception {
        String json = "{\"id\":1,\"name\":\"张三\"}";

        ModeObjectDTO modeObjectDTO = objectMapper.readValue(json, ModeObjectDTO.class);
        JsonNode node = objectMapper.valueToTree(modeObjectDTO);

        assertThat(node.path("id").asLong()).isEqualTo(1L);
        assertThat(node.path("name").asText()).isEqualTo("张三");
    }
}
