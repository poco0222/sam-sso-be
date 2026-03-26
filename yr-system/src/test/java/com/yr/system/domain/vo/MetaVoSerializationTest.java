/**
 * @file MetaVo 与分页 VO JSON/泛型契约测试
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.domain.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MetaVo JSON 契约测试。
 */
class MetaVoSerializationTest {

    /** JSON 工具。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证 MetaVo 的 JSON 字段名与布尔字段语义保持稳定。
     */
    @Test
    void shouldKeepJsonFieldNamesStable() {
        MetaVo metaVo = new MetaVo("首页", "dashboard", true, "/internal", 1L);

        JsonNode node = objectMapper.valueToTree(metaVo);

        assertThat(node.path("title").asText()).isEqualTo("首页");
        assertThat(node.path("icon").asText()).isEqualTo("dashboard");
        assertThat(node.path("noCache").asBoolean()).isTrue();
        assertThat(node.has("link")).isTrue();
        assertThat(node.get("link").isNull()).isTrue();
        assertThat(node.path("menuId").asLong()).isEqualTo(1L);
    }

    /**
     * 验证不带 noCache 参数的构造器会保留原始 link 值。
     */
    @Test
    void shouldKeepRawLinkForFourArgsConstructor() {
        MetaVo metaVo = new MetaVo("首页", "dashboard", "/internal", 1L);

        JsonNode node = objectMapper.valueToTree(metaVo);

        assertThat(node.path("link").asText()).isEqualTo("/internal");
        assertThat(node.path("noCache").asBoolean()).isFalse();
    }

}
