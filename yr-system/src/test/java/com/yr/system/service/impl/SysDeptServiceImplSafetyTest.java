/**
 * @file 验证 SysDeptServiceImpl 的安全性契约（safety contract，安全契约）
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.exception.CustomException;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.system.mapper.SysDeptMapper;
import com.yr.system.mapper.SysRoleMapper;
import com.yr.system.mapper.SysUserDeptMapper;
import com.yr.system.service.ISysDeptService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 部门服务安全性测试。
 *
 * <p>关注点是避免缺失角色数据导致的 NPE（NullPointerException，空指针异常），并确保对外抛出
 * CustomException（business exception，业务异常）。</p>
 */
class SysDeptServiceImplSafetyTest {

    /**
     * 验证当 roleId 对应角色缺失（role == null）时，服务层 fail-fast（快速失败）抛出 CustomException，
     * 并且不会继续调用下游 deptMapper.selectDeptListByRoleId(...)。
     */
    @Test
    void shouldFailFastWhenRoleIsMissingForDeptSelection() {
        Long roleId = 3003L;

        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserDeptMapper sysUserDeptMapper = mock(SysUserDeptMapper.class);
        ISysDeptService selfProxy = mock(ISysDeptService.class);

        SysDeptServiceImpl service = new SysDeptServiceImpl(deptMapper, roleMapper, sysUserDeptMapper, selfProxy);

        when(roleMapper.selectRoleById(roleId)).thenReturn(null);

        assertThatThrownBy(() -> service.selectDeptListByRoleId(roleId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("roleId=" + roleId)
                // 额外防御：确保不是 NPE（NullPointerException，空指针异常）被包裹或泄漏。
                .satisfies(ex -> assertThat(ex.getCause()).isNull());

        verify(roleMapper).selectRoleById(roleId);
        verify(deptMapper, never()).selectDeptListByRoleId(eq(roleId), anyBoolean());
        // 注意：selfProxy 属于 AOP（Aspect-Oriented Programming，面向切面编程）代理注入，不应纳入零交互约束。
    }

    /**
     * 验证新增部门时如果父节点不存在，会直接抛出 CustomException，而不是继续触发空指针或写入脏数据。
     */
    @Test
    void shouldFailFastWhenParentDeptIsMissingOnInsert() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysUserDeptMapper sysUserDeptMapper = mock(SysUserDeptMapper.class);
        ISysDeptService selfProxy = mock(ISysDeptService.class);
        SysDeptServiceImpl service = new SysDeptServiceImpl(deptMapper, roleMapper, sysUserDeptMapper, selfProxy);
        SysDept command = new SysDept();
        command.setParentId(88L);

        when(deptMapper.selectDeptById(88L)).thenReturn(null);

        assertThatThrownBy(() -> service.insertDept(command))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("上级部门不存在");

        verify(deptMapper).selectDeptById(88L);
        verify(deptMapper, never()).insertDept(command);
    }
}
