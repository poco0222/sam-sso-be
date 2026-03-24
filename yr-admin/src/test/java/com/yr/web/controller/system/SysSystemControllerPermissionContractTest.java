/**
 * @file 锁定系统管理域高风险 controller 入口必须声明权限边界
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.web.controller.system;

import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysRole;
import com.yr.common.core.domain.entity.SysUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 系统管理域 controller 权限契约测试。
 */
class SysSystemControllerPermissionContractTest {

    /** 暂无允许匿名暴露的高风险入口；若后续确需放开，必须在这里写明原因。 */
    private static final Map<String, String> ALLOWLIST = Map.of();

    /**
     * 验证用户管理新增暴露的查询入口必须声明权限。
     *
     * @throws NoSuchMethodException 当方法签名变化时抛出
     */
    @Test
    void shouldProtectHighRiskSysUserControllerEndpoints() throws NoSuchMethodException {
        assertProtectedOrAllowlisted(SysUserController.class, "pageQueryModeGroupingObjects", SysUser.class);
        assertProtectedOrAllowlisted(SysUserController.class, "selectSysUserById", String.class);
        assertProtectedOrAllowlisted(SysUserController.class, "batchSelectUserByDeptId", String.class);
        assertProtectedOrAllowlisted(SysUserController.class, "listV2ForAF", SysUser.class);
    }

    /**
     * 验证部门管理对外暴露的列表/树查询入口必须声明权限。
     *
     * @throws NoSuchMethodException 当方法签名变化时抛出
     */
    @Test
    void shouldProtectHighRiskSysDeptControllerEndpoints() throws NoSuchMethodException {
        assertProtectedOrAllowlisted(SysDeptController.class, "list", SysDept.class);
        assertProtectedOrAllowlisted(SysDeptController.class, "treeselect", SysDept.class);
        assertProtectedOrAllowlisted(SysDeptController.class, "deptRoletreeselect", SysDept.class);
        assertProtectedOrAllowlisted(SysDeptController.class, "getAllSysDeptForOptions");
        assertProtectedOrAllowlisted(SysDeptController.class, "getChildrenDept", String.class);
    }

    /**
     * 验证角色管理额外的角色列表与已分配/未分配用户接口必须声明权限。
     *
     * @throws NoSuchMethodException 当方法签名变化时抛出
     */
    @Test
    void shouldProtectRoleAssignmentEndpoints() throws NoSuchMethodException {
        assertProtectedOrAllowlisted(SysRoleController.class, "listAll", SysRole.class);
        assertProtectedOrAllowlisted(SysRoleController.class, "allocatedList", SysUser.class);
        assertProtectedOrAllowlisted(SysRoleController.class, "unallocatedList", SysUser.class);
    }

    /**
     * 断言 controller 入口要么声明了 `@PreAuthorize`，要么被显式列入 allowlist。
     *
     * @param controllerClass controller 类型
     * @param methodName 方法名
     * @param parameterTypes 方法参数
     * @throws NoSuchMethodException 当目标方法不存在时抛出
     */
    private void assertProtectedOrAllowlisted(Class<?> controllerClass,
                                              String methodName,
                                              Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = controllerClass.getMethod(methodName, parameterTypes);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        String methodKey = buildMethodKey(controllerClass, method);

        if (preAuthorize != null) {
            assertThat(preAuthorize.value())
                    .as("%s 的 PreAuthorize 表达式不能为空", methodKey)
                    .isNotBlank();
            return;
        }

        assertThat(ALLOWLIST)
                .as("%s 必须声明 @PreAuthorize，或在 allowlist 中写明放开原因", methodKey)
                .containsKey(methodKey);
    }

    /**
     * 生成稳定的方法标识，用于 allowlist。
     *
     * @param controllerClass controller 类型
     * @param method 目标方法
     * @return 方法标识
     */
    private String buildMethodKey(Class<?> controllerClass, Method method) {
        return controllerClass.getSimpleName() + "#" + method.getName();
    }
}
