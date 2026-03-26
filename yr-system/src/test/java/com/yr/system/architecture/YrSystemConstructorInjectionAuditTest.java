/**
 * @file 审计 yr-system 剩余字段注入 Bean 的架构测试
 * @author Codex
 * @date 2026-03-16
 */
package com.yr.system.architecture;

import com.yr.system.service.impl.SysDeptServiceImpl;
import com.yr.system.service.impl.SysLogininforServiceImpl;
import com.yr.system.service.impl.SysOperLogServiceImpl;
import com.yr.system.service.impl.SysOrgServiceImpl;
import com.yr.system.service.impl.SysUserDeptServiceImpl;
import com.yr.system.service.impl.SysUserDprServiceImpl;
import com.yr.system.service.impl.SysUserOrgServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定 yr-system Phase 3 构造器注入迁移清单的架构测试。
 */
class YrSystemConstructorInjectionAuditTest {

    /**
     * 验证当前迁移清单完整，避免漏掉 review 已识别的问题 Bean。
     */
    @Test
    void shouldTrackAllPendingConstructorInjectionTargets() {
        assertThat(pendingConstructorInjectionTargets())
                .as("Phase 3 字段注入迁移清单应保持稳定")
                .hasSize(7);
    }

    /**
     * 验证 yr-system Bean 已不再使用字段级注入。
     */
    @Test
    void shouldAvoidFieldInjectionInYrSystemBeans() {
        assertThat(pendingConstructorInjectionTargets())
                .as("以下 Bean 仍使用字段注入，需要迁移到构造器注入: %s",
                        pendingConstructorInjectionTargets().stream()
                                .filter(this::usesFieldInjection)
                                .map(Class::getSimpleName)
                                .toList())
                .allSatisfy(type -> assertThat(usesFieldInjection(type))
                        .as("%s 不应保留字段级 @Autowired", type.getSimpleName())
                        .isFalse());
    }

    /**
     * 收敛本轮 review 识别出的剩余字段注入 Bean。
     *
     * @return 待迁移 Bean 列表
     */
    private List<Class<?>> pendingConstructorInjectionTargets() {
        return List.of(
                SysDeptServiceImpl.class,
                SysLogininforServiceImpl.class,
                SysOperLogServiceImpl.class,
                SysOrgServiceImpl.class,
                SysUserDeptServiceImpl.class,
                SysUserDprServiceImpl.class,
                SysUserOrgServiceImpl.class
        );
    }

    /**
     * 判断目标类是否仍存在字段级注入。
     *
     * @param type 待检查类型
     * @return true 表示存在字段注入
     */
    private boolean usesFieldInjection(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .filter(this::isInstanceField)
                .anyMatch(field -> field.isAnnotationPresent(Autowired.class));
    }

    /**
     * 过滤出实例字段，避免把静态常量误判为依赖。
     *
     * @param field 待判断字段
     * @return true 表示实例字段
     */
    private boolean isInstanceField(Field field) {
        return !Modifier.isStatic(field.getModifiers());
    }
}
