/**
 * @file 锁定 MatcherUtils 的占位符替换与解析行为
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MatcherUtils 行为测试。
 */
class MatcherUtilsTest {

    /**
     * 验证 parse 会安全替换包含特殊字符的值，而不是把它们当成正则替换语法。
     */
    @Test
    void shouldParseTemplateWithDollarAndBackslashSafely() {
        String parsed = MatcherUtils.parse(
                "name=${name},path=${path}",
                Map.of("${name}", "A$B", "${path}", "C\\D")
        );

        assertThat(parsed).isEqualTo("name=A$B,path=C\\D");
    }

    /**
     * 验证 resolve 会提取全部占位符且不带尾逗号。
     */
    @Test
    void shouldResolveTemplateVariablesWithoutTrailingComma() {
        String resolved = MatcherUtils.resolve("编码${code}和名称${name}");

        assertThat(resolved).isEqualTo("${code},${name}");
    }

    /**
     * 验证 parse 在占位符缺失映射值时会 fail-fast，而不是静默替换成 null 或抛出裸 NPE。
     */
    @Test
    void shouldFailFastWhenPlaceholderValueIsMissing() {
        assertThatThrownBy(() -> MatcherUtils.parse("name=${name}", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("${name}");
    }

    /**
     * 验证 parse 在映射表为 null 时会给出明确异常。
     */
    @Test
    void shouldFailFastWhenPlaceholderMapIsNull() {
        assertThatThrownBy(() -> MatcherUtils.parse("name=${name}", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kvs");
    }
}
