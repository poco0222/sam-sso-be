/**
 * @file 锁定 yr-system 空值契约显式声明，避免隐式返回 null
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.architecture;

import com.yr.system.service.ISysUserOnlineService;
import com.yr.system.service.ISysUserService;
import com.yr.system.service.impl.SysUserOnlineServiceImpl;
import com.yr.system.service.impl.SysUserQueryService;
import com.yr.system.service.impl.SysUserServiceImpl;
import com.yr.system.utils.Reflections;
import org.junit.jupiter.api.Test;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 空值契约显式声明测试。
 */
class YrSystemNullabilityContractTest {

    /**
     * 验证反射查找类方法显式标注了可能返回 null 的契约。
     *
     * @throws NoSuchMethodException 方法不存在
     */
    @Test
    void shouldDeclareNullableLookupContractsInReflections() throws NoSuchMethodException {
        assertNullable(Reflections.class.getDeclaredMethod("getAccessibleField", Object.class, String.class));
        assertNullable(Reflections.class.getDeclaredMethod("getAccessibleMethod", Object.class, String.class, Class[].class));
        assertNullable(Reflections.class.getDeclaredMethod("getAccessibleMethodByName", Object.class, String.class));
    }

    /**
     * 验证在线用户服务接口与实现显式声明了可空返回值。
     *
     * @throws NoSuchMethodException 方法不存在
     */
    @Test
    void shouldDeclareNullableContractsInOnlineUserService() throws NoSuchMethodException {
        assertNullable(ISysUserOnlineService.class.getMethod("selectOnlineByIpaddr", String.class,
                com.yr.common.core.domain.model.LoginUser.class));
        assertNullable(ISysUserOnlineService.class.getMethod("selectOnlineByUserName", String.class,
                com.yr.common.core.domain.model.LoginUser.class));
        assertNullable(ISysUserOnlineService.class.getMethod("selectOnlineByInfo", String.class, String.class,
                com.yr.common.core.domain.model.LoginUser.class));
        assertNullable(ISysUserOnlineService.class.getMethod("loginUserToUserOnline",
                com.yr.common.core.domain.model.LoginUser.class));

        assertNullable(SysUserOnlineServiceImpl.class.getMethod("selectOnlineByIpaddr", String.class,
                com.yr.common.core.domain.model.LoginUser.class));
        assertNullable(SysUserOnlineServiceImpl.class.getMethod("selectOnlineByUserName", String.class,
                com.yr.common.core.domain.model.LoginUser.class));
        assertNullable(SysUserOnlineServiceImpl.class.getMethod("selectOnlineByInfo", String.class, String.class,
                com.yr.common.core.domain.model.LoginUser.class));
        assertNullable(SysUserOnlineServiceImpl.class.getMethod("loginUserToUserOnline",
                com.yr.common.core.domain.model.LoginUser.class));
    }

    /**
     * 验证用户查询相关公开接口显式声明了可能返回 null 的契约。
     *
     * @throws NoSuchMethodException 方法不存在
     */
    @Test
    void shouldDeclareNullableContractsInUserLookupServices() throws NoSuchMethodException {
        assertNullable(ISysUserService.class.getMethod("selectUserByUserName", String.class, Long.class));
        assertNullable(ISysUserService.class.getMethod("selectUserById", Long.class, Long.class));
        assertNullable(SysUserServiceImpl.class.getMethod("selectUserByUserName", String.class, Long.class));
        assertNullable(SysUserServiceImpl.class.getMethod("selectUserById", Long.class, Long.class));
        assertNullable(SysUserQueryService.class.getMethod("getUserById", Long.class));
    }

    /**
     * 断言方法显式声明了 Spring Nullable 契约。
     *
     * @param method 目标方法
     */
    private void assertNullable(Method method) {
        assertThat(method.isAnnotationPresent(Nullable.class))
                .as("%s 应显式标注 @Nullable", method)
                .isTrue();
    }
}
