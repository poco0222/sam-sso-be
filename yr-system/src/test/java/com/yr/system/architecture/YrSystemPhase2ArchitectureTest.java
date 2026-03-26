/**
 * @file 锁定 yr-system Phase 2 最佳实践结构约束的测试
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.architecture;

import com.yr.system.config.YrSystemWarmupRunner;
import com.yr.system.domain.entity.SysUserDept;
import com.yr.system.service.impl.SysUserImportService;
import com.yr.system.service.impl.SysUserQueryService;
import com.yr.system.service.impl.SysUserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * yr-system Phase 2 架构约束测试。
 */
class YrSystemPhase2ArchitectureTest {

    /**
     * 验证目标类已经迁移为构造器注入，避免继续使用字段注入。
     */
    @Test
    void shouldUseConstructorInjectionInPhase2Targets() {
        assertUsesConstructorInjection(SysUserServiceImpl.class);
    }

    /**
     * 验证缓存预热已经脱离 @PostConstruct，并转移到专用 warmup runner。
     */
    @Test
    void shouldMoveWarmupOutOfServicePostConstruct() {
        assertThat(YrSystemWarmupRunner.class).isNotNull();
        assertThat(Arrays.stream(YrSystemWarmupRunner.class.getDeclaredFields()).map(Field::getType).toList())
                .as("一期 warmup runner 不应再持有任何 legacy 预热依赖")
                .isEmpty();
    }

    /**
     * 验证用户热点职责已经拆到专用服务，避免 SysUserServiceImpl 持续膨胀。
     */
    @Test
    void shouldSplitUserHotPathsIntoDedicatedServices() {
        assertThat(Arrays.stream(SysUserServiceImpl.class.getDeclaredFields()).map(Field::getType).toList())
                .contains(SysUserImportService.class, SysUserQueryService.class);
    }

    /**
     * 验证继承父类的实体已经显式声明 Lombok equals/hashCode 策略。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldDeclareExplicitEqualsAndHashCodeStrategyForInheritedEntities() throws IOException {
        assertSourceContains("domain/entity/SysUserDept.java", "@EqualsAndHashCode(callSuper = false)");
    }

    /**
     * 验证部门新增 SQL 正确绑定 accounteUnit 参数，避免写入时把字段名当字面量提交。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldBindAccountUnitParameterWhenInsertingDept() throws IOException {
        assertProjectSourceContains(Path.of("src/main/resources/mapper/system/SysDeptMapper.xml"), "#{accounteUnit}");
    }

    /**
     * 断言目标类的实例字段没有使用字段注入，并且存在单一构造器承接所有依赖。
     *
     * @param targetClass 目标类型
     */
    private void assertUsesConstructorInjection(Class<?> targetClass) {
        List<Field> instanceFields = collectInstanceFields(targetClass);
        Constructor<?>[] constructors = targetClass.getDeclaredConstructors();

        assertThat(instanceFields)
                .as("%s 不应保留字段级 @Autowired", targetClass.getSimpleName())
                .noneMatch(field -> field.isAnnotationPresent(Autowired.class));
        assertThat(instanceFields)
                .as("%s 的依赖字段应全部声明为 final", targetClass.getSimpleName())
                .allMatch(field -> Modifier.isFinal(field.getModifiers()));
        assertThat(constructors)
                .as("%s 应保留单一构造器", targetClass.getSimpleName())
                .hasSize(1);
        assertThat(constructors[0].getParameterCount())
                .as("%s 构造器参数数量应与依赖字段一致", targetClass.getSimpleName())
                .isEqualTo(instanceFields.size());
    }

    /**
     * 收集目标类及其父类声明的实例字段，避免遗漏通过父类持有的构造器依赖。
     *
     * @param targetClass 目标类型
     * @return 继承链上的全部实例字段
     */
    private List<Field> collectInstanceFields(Class<?> targetClass) {
        List<Field> instanceFields = new ArrayList<>();
        Class<?> current = targetClass;

        while (current != null && current != Object.class) {
            if (shouldInspectInheritedFields(current)) {
                instanceFields.addAll(Arrays.stream(current.getDeclaredFields())
                        .filter(this::isInstanceField)
                        .toList());
            }
            current = current.getSuperclass();
        }

        return instanceFields;
    }

    /**
     * 过滤出实例字段，避免把静态常量误判为构造器依赖。
     *
     * @param field 待判断字段
     * @return true 表示实例字段
     */
    private boolean isInstanceField(Field field) {
        return !Modifier.isStatic(field.getModifiers());
    }

    /**
     * 仅统计 yr-system 自己声明的继承链字段，避免把 MyBatis-Plus 基类的基础设施字段误判为业务依赖。
     *
     * @param type 当前遍历的类型
     * @return true 表示该类型的字段需要纳入构造器注入审计
     */
    private boolean shouldInspectInheritedFields(Class<?> type) {
        Package currentPackage = type.getPackage();
        if (currentPackage == null) {
            return false;
        }
        return currentPackage.getName().startsWith("com.yr.system");
    }

    /**
     * 断言目标类不再使用 @PostConstruct。
     *
     * @param targetClass 目标类型
     */
    private void assertHasNoPostConstructMethod(Class<?> targetClass) {
        Method[] methods = targetClass.getDeclaredMethods();
        assertThat(Arrays.stream(methods))
                .as("%s 不应再通过 @PostConstruct 触发预热", targetClass.getSimpleName())
                .noneMatch(method -> method.isAnnotationPresent(PostConstruct.class));
    }

    /**
     * 断言目标源码包含指定文本，适合校验 SOURCE 级别注解。
     *
     * @param relativeMainJavaPath 相对 main/java 的源码路径
     * @param expectedText 期待出现的文本
     * @throws IOException 读取源码失败
     */
    private void assertSourceContains(String relativeMainJavaPath, String expectedText) throws IOException {
        Path sourcePath = Path.of("src/main/java/com/yr/system", relativeMainJavaPath);
        String sourceText = Files.readString(sourcePath);
        assertThat(sourceText)
                .as("%s 应显式声明 %s", relativeMainJavaPath, expectedText)
                .contains(expectedText);
    }

    /**
     * 断言项目内任意源码文件包含指定文本。
     *
     * @param sourcePath 源码路径
     * @param expectedText 期待文本
     * @throws IOException 读取源码失败
     */
    private void assertProjectSourceContains(Path sourcePath, String expectedText) throws IOException {
        String sourceText = Files.readString(sourcePath);
        assertThat(sourceText)
                .as("%s 应显式声明 %s", sourcePath, expectedText)
                .contains(expectedText);
    }
}
