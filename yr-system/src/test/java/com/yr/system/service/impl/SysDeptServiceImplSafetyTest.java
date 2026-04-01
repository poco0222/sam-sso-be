/**
 * @file 验证 SysDeptServiceImpl 的安全性契约（safety contract，安全契约）
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.constant.UserConstants;
import com.yr.common.exception.CustomException;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.system.mapper.SysDeptMapper;
import com.yr.system.mapper.SysUserDeptMapper;
import com.yr.system.service.ISysDeptService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
     * 验证新增部门时如果父节点不存在，会直接抛出 CustomException，而不是继续触发空指针或写入脏数据。
     */
    @Test
    void shouldFailFastWhenParentDeptIsMissingOnInsert() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysUserDeptMapper sysUserDeptMapper = mock(SysUserDeptMapper.class);
        ISysDeptService selfProxy = mock(ISysDeptService.class);
        SysDeptServiceImpl service = new SysDeptServiceImpl(deptMapper, sysUserDeptMapper, selfProxy);
        SysDept command = new SysDept();
        command.setParentId(88L);

        when(deptMapper.selectDeptById(88L)).thenReturn(null);

        assertThatThrownBy(() -> service.insertDept(command))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("上级部门不存在");

        verify(deptMapper).selectDeptById(88L);
        verify(deptMapper, never()).insertDept(command);
    }

    /**
     * 验证新增部门时即使请求体携带了其它 orgId，服务端仍会以父部门 orgId 为真值来源。
     */
    @Test
    void shouldDeriveOrgIdFromParentDeptWhenInsertingDept() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysUserDeptMapper sysUserDeptMapper = mock(SysUserDeptMapper.class);
        ISysDeptService selfProxy = mock(ISysDeptService.class);
        SysDeptServiceImpl service = new SysDeptServiceImpl(deptMapper, sysUserDeptMapper, selfProxy);
        SysDept parentDept = new SysDept();
        SysDept command = new SysDept();
        parentDept.setDeptId(88L);
        parentDept.setOrgId(20L);
        parentDept.setStatus(UserConstants.DEPT_NORMAL);
        parentDept.setAncestors("0");
        command.setParentId(88L);
        command.setOrgId(999L);

        when(deptMapper.selectDeptById(88L)).thenReturn(parentDept);
        when(deptMapper.insertDept(any(SysDept.class))).thenReturn(1);

        service.insertDept(command);

        verify(deptMapper).insertDept(argThat(dept ->
                Long.valueOf(20L).equals(dept.getOrgId())
                        && "0,88".equals(dept.getAncestors())));
    }

    /**
     * 验证部门编码唯一性校验在请求未携带 orgId 时，仍会以父部门 orgId 作为查重真值。
     */
    @Test
    void shouldDeriveOrgIdFromParentDeptWhenCheckingDeptCodeUniqueness() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysUserDeptMapper sysUserDeptMapper = mock(SysUserDeptMapper.class);
        ISysDeptService selfProxy = mock(ISysDeptService.class);
        SysDeptServiceImpl service = new SysDeptServiceImpl(deptMapper, sysUserDeptMapper, selfProxy);
        SysDept parentDept = new SysDept();
        SysDept duplicatedDept = new SysDept();
        SysDept command = new SysDept();
        parentDept.setDeptId(88L);
        parentDept.setOrgId(20L);
        duplicatedDept.setDeptId(99L);
        command.setParentId(88L);
        command.setDeptCode("DEPT-01");

        when(deptMapper.selectDeptById(88L)).thenReturn(parentDept);
        when(deptMapper.checkDeptCodeUnique("DEPT-01", 88L, 20L)).thenReturn(duplicatedDept);

        String result = service.checkDeptCodeUnique(command);

        verify(deptMapper).selectDeptById(88L);
        verify(deptMapper).checkDeptCodeUnique("DEPT-01", 88L, 20L);
        assertThat(result).isEqualTo(UserConstants.NOT_UNIQUE);
    }
}
