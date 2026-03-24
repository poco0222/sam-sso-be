/**
 * @file MetaVo 与分页 VO JSON/泛型契约测试
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.domain.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

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

    /**
     * 验证 PageVo 子类的泛型声明与自身语义一致，避免查询 VO 借错模板类型。
     */
    @Test
    void shouldKeepPageVoSubclassGenericAlignedWithSelfType() {
        assertThat(extractPageVoGenericArgument(SysMsgTemplateVo.class)).isEqualTo(SysMsgTemplateVo.class);
        assertThat(extractPageVoGenericArgument(SysReceiveGroupVo.class)).isEqualTo(SysReceiveGroupVo.class);
    }

    /**
     * 验证消息模板分页 VO 的 JSON 结构在泛型整理后保持稳定。
     */
    @Test
    void shouldKeepMsgTemplatePageVoJsonShapeStable() {
        SysMsgTemplateVo vo = new SysMsgTemplateVo();
        vo.setPage(2);
        vo.setRows(20);
        vo.setOrder("asc");
        vo.setSort("msg_name");
        vo.setId(7L);
        vo.setMsgCode("TMP-001");
        vo.setMsgName("立项通知");
        vo.setMsgContent("内容");
        vo.setMsgParams("{name}");
        vo.setStatus("normal");

        JsonNode node = objectMapper.valueToTree(vo);

        assertThat(node.path("page").asInt()).isEqualTo(2);
        assertThat(node.path("rows").asInt()).isEqualTo(20);
        assertThat(node.path("order").asText()).isEqualTo("asc");
        assertThat(node.path("sort").asText()).isEqualTo("msg_name");
        assertThat(node.path("msgCode").asText()).isEqualTo("TMP-001");
        assertThat(node.path("msgName").asText()).isEqualTo("立项通知");
        assertThat(node.path("status").asText()).isEqualTo("normal");
    }

    /**
     * 验证接收组分页 VO 的 JSON 结构在泛型整理后保持稳定。
     */
    @Test
    void shouldKeepReceiveGroupPageVoJsonShapeStable() {
        SysReceiveGroupVo vo = new SysReceiveGroupVo();
        vo.setPage(3);
        vo.setRows(15);
        vo.setOrder("desc");
        vo.setSort("re_code");
        vo.setReCode("GROUP-001");
        vo.setReMode("USER_GROUP");
        vo.setStatus("normal");

        JsonNode node = objectMapper.valueToTree(vo);

        assertThat(node.path("page").asInt()).isEqualTo(3);
        assertThat(node.path("rows").asInt()).isEqualTo(15);
        assertThat(node.path("order").asText()).isEqualTo("desc");
        assertThat(node.path("sort").asText()).isEqualTo("re_code");
        assertThat(node.path("reCode").asText()).isEqualTo("GROUP-001");
        assertThat(node.path("reMode").asText()).isEqualTo("USER_GROUP");
        assertThat(node.path("status").asText()).isEqualTo("normal");
    }

    /**
     * 提取 PageVo 子类的泛型参数。
     *
     * @param voClass 分页 VO 类型
     * @return 泛型实参
     */
    private Class<?> extractPageVoGenericArgument(Class<?> voClass) {
        Type genericSuperclass = voClass.getGenericSuperclass();
        assertThat(genericSuperclass).isInstanceOf(ParameterizedType.class);
        ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
        Type actualType = parameterizedType.getActualTypeArguments()[0];
        assertThat(actualType).isInstanceOf(Class.class);
        return (Class<?>) actualType;
    }
}
