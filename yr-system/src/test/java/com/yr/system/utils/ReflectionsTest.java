/**
 * @file 锁定 Reflections 当前行为的单元测试
 * @author PopoY
 * @date 2026-03-11
 */
package com.yr.system.utils;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reflections 行为测试。
 */
class ReflectionsTest {

    /**
     * 验证能够解析父类上的泛型参数。
     */
    @Test
    void shouldResolveDeclaredGenericType() {
        assertThat(Reflections.getClassGenricType(LongGenericChild.class, 0)).isEqualTo(Long.class);
    }

    /**
     * 验证无法解析泛型时会回退到 Object.class。
     */
    @Test
    void shouldFallbackToObjectClassWhenSuperclassIsNotParameterizedType() {
        assertThat(Reflections.getClassGenricType(RawGenericChild.class, 0)).isEqualTo(Object.class);
    }

    /**
     * 验证 InvocationTargetException 的原始 cause 会被继续包装返回。
     */
    @Test
    void shouldWrapInvocationTargetExceptionCause() {
        RuntimeException runtimeException = Reflections.convertReflectionExceptionToUnchecked(
                new InvocationTargetException(new IllegalStateException("boom"))
        );
        assertThat(runtimeException).hasCauseInstanceOf(IllegalStateException.class);
    }

    /**
     * 验证其他异常分支的分类逻辑保持不变。
     */
    @Test
    void shouldPreserveReflectionExceptionClassification() {
        assertThat(Reflections.convertReflectionExceptionToUnchecked(new IllegalArgumentException("bad")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(Reflections.convertReflectionExceptionToUnchecked(new RuntimeException("rt")))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * 验证找不到字段时会返回 null，供上层辅助方法自行决定如何处理。
     */
    @Test
    void shouldReturnNullWhenAccessibleFieldDoesNotExist() {
        assertThat(Reflections.getAccessibleField(new SampleBean(), "missingField")).isNull();
    }

    /**
     * 验证找不到同名方法时会返回 null，而不是直接抛出异常。
     */
    @Test
    void shouldReturnNullWhenAccessibleMethodByNameDoesNotExist() {
        assertThat(Reflections.getAccessibleMethodByName(new SampleBean(), "missingMethod")).isNull();
    }

    /**
     * 验证访问 JDK 强封装字段失败时会 fail-fast 抛异常，而不是吞掉 IllegalAccessException。
     */
    @Test
    void shouldFailFastWhenFieldReadRemainsInaccessible() {
        assertThatThrownBy(() -> Reflections.getFieldValue("demo", "value"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 验证写入 JDK 强封装字段失败时也会 fail-fast 抛异常。
     */
    @Test
    void shouldFailFastWhenFieldWriteRemainsInaccessible() {
        assertThatThrownBy(() -> Reflections.setFieldValue("demo", "value", new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 用于构造泛型父类。
     *
     * @param <T> 泛型类型
     */
    private static class GenericParent<T> {
    }

    /**
     * 具备明确泛型声明的子类。
     */
    private static class LongGenericChild extends GenericParent<Long> {
    }

    /**
     * 使用原始类型的子类，用于验证回退逻辑。
     */
    @SuppressWarnings("rawtypes")
    private static class RawGenericChild extends GenericParent {
    }

    /**
     * 最小示例 Bean。
     */
    private static class SampleBean {

        /**
         * 供反射测试使用的字段。
         */
        @SuppressWarnings("unused")
        private String name = "demo";

        /**
         * 供反射测试使用的方法。
         *
         * @return 示例值
         */
        @SuppressWarnings("unused")
        private String currentName() {
            return name;
        }
    }
}
