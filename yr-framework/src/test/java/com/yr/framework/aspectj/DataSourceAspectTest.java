/**
 * @file 多数据源切面回归测试
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.framework.aspectj;

import com.yr.common.annotation.DataSource;
import com.yr.common.enums.DataSourceType;
import com.yr.framework.datasource.DynamicDataSourceContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 锁定 interface proxy（接口代理）场景下的数据源注解解析，避免 INIT_IMPORT 再读回 master。
 */
class DataSourceAspectTest {

    /**
     * 验证当 signature 来自接口方法时，切面仍能解析 target implementation（目标实现类）上的 class-level 注解。
     *
     * @throws NoSuchMethodException 反射获取方法失败
     */
    @Test
    void shouldResolveClassLevelAnnotationFromTargetImplementationWhenSignatureDeclaresInterface()
            throws NoSuchMethodException {
        DataSourceAspect aspect = new DataSourceAspect();
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        when(point.getSignature()).thenReturn(signature);
        when(point.getTarget()).thenReturn(new ClassLevelAnnotatedLoader());
        when(signature.getMethod()).thenReturn(LegacySnapshotLoader.class.getMethod("loadSnapshot"));
        when(signature.getDeclaringType()).thenReturn(LegacySnapshotLoader.class);

        DataSource dataSource = aspect.getDataSource(point);

        assertThat(dataSource).isNotNull();
        assertThat(dataSource.value()).isEqualTo(DataSourceType.SLAVE);
    }

    /**
     * 验证当实现类方法本身声明注解时，切面会优先解析 most specific method（最具体的方法）。
     *
     * @throws NoSuchMethodException 反射获取方法失败
     */
    @Test
    void shouldResolveMethodLevelAnnotationFromTargetImplementationMethod() throws NoSuchMethodException {
        DataSourceAspect aspect = new DataSourceAspect();
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        when(point.getSignature()).thenReturn(signature);
        when(point.getTarget()).thenReturn(new MethodLevelAnnotatedLoader());
        when(signature.getMethod()).thenReturn(LegacySnapshotLoader.class.getMethod("loadSnapshot"));
        when(signature.getDeclaringType()).thenReturn(LegacySnapshotLoader.class);

        DataSource dataSource = aspect.getDataSource(point);

        assertThat(dataSource).isNotNull();
        assertThat(dataSource.value()).isEqualTo(DataSourceType.SLAVEEC);
    }

    /**
     * 验证 class-level 注解在 JDK proxy 真实调用链上也会触发数据源切换。
     */
    @Test
    void shouldApplyClassLevelAnnotationOnJdkProxyInvocation() {
        LegacySnapshotLoader proxy = createJdkProxy(new ClassLevelAnnotatedLoader());

        String currentDatasource = proxy.loadSnapshot().toString();

        assertThat(currentDatasource).isEqualTo(DataSourceType.SLAVE.name());
        assertThat(DynamicDataSourceContextHolder.getDataSourceType()).isNull();
    }

    /**
     * 验证 method-level 注解在 JDK proxy 真实调用链上也会触发数据源切换。
     */
    @Test
    void shouldApplyMethodLevelAnnotationOnJdkProxyInvocation() {
        LegacySnapshotLoader proxy = createJdkProxy(new MethodLevelAnnotatedLoader());

        String currentDatasource = proxy.loadSnapshot().toString();

        assertThat(currentDatasource).isEqualTo(DataSourceType.SLAVEEC.name());
        assertThat(DynamicDataSourceContextHolder.getDataSourceType()).isNull();
    }

    /**
     * 构造一个尽量贴近线上 loader 形态的 JDK proxy。
     *
     * @param target 真实 target implementation（目标实现）
     * @return 带切面的代理对象
     */
    private LegacySnapshotLoader createJdkProxy(LegacySnapshotLoader target) {
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.setProxyTargetClass(false);
        proxyFactory.addAspect(new DataSourceAspect());
        return proxyFactory.getProxy();
    }

    /**
     * 抽象出 INIT_IMPORT loader 的最小接口形态，模拟 Spring JDK proxy 常见签名。
     */
    private interface LegacySnapshotLoader {

        /**
         * @return 任意快照对象
         */
        Object loadSnapshot();
    }

    /**
     * 通过 class-level 注解声明 slave 数据源。
     */
    @DataSource(DataSourceType.SLAVE)
    private static class ClassLevelAnnotatedLoader implements LegacySnapshotLoader {

        /**
         * @return 空快照
         */
        @Override
        public Object loadSnapshot() {
            return DynamicDataSourceContextHolder.getDataSourceType();
        }
    }

    /**
     * 通过 method-level 注解声明 slaveec 数据源。
     */
    private static class MethodLevelAnnotatedLoader implements LegacySnapshotLoader {

        /**
         * @return 空快照
         */
        @Override
        @DataSource(DataSourceType.SLAVEEC)
        public Object loadSnapshot() {
            return DynamicDataSourceContextHolder.getDataSourceType();
        }
    }

}
