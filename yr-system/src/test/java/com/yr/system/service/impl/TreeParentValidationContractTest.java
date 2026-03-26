/**
 * @file 验证一期树形服务在父节点缺失与换父节点时的部门校验契约
 * @author PopoY
 * @date 2026-03-26
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.exception.CustomException;
import com.yr.system.mapper.SysDeptMapper;
import com.yr.system.mapper.SysUserDeptMapper;
import com.yr.system.service.ISysDeptService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 树形父节点校验契约测试。
 */
class TreeParentValidationContractTest {

    /**
     * 验证新增部门时会拒绝不存在的父节点，避免一期仅保留 dept 树后再次退化成静默错误。
     */
    @Test
    void shouldRejectMissingParentWhenInsertingDept() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        when(deptMapper.selectDeptById(999L)).thenReturn(null);

        SysDeptServiceImpl service = new SysDeptServiceImpl(
                deptMapper,
                mock(SysUserDeptMapper.class),
                mock(ISysDeptService.class)
        );
        SysDept command = new SysDept();
        command.setParentId(999L);

        assertThatThrownBy(() -> service.insertDept(command))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("上级部门不存在");
    }

    /**
     * 验证部门换父节点时会重写子树祖级链路，避免树重排语义在一期边界内回退。
     */
    @Test
    void shouldRewriteDeptChildAncestorsWhenReparentingDept() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysDept newParentDept = new SysDept();
        newParentDept.setDeptId(2L);
        newParentDept.setAncestors("0");
        SysDept oldDept = new SysDept();
        oldDept.setDeptId(101L);
        oldDept.setAncestors("0,1");
        SysDept childDept = new SysDept();
        childDept.setDeptId(1001L);
        childDept.setAncestors("0,1,101");

        when(deptMapper.selectDeptById(2L)).thenReturn(newParentDept);
        when(deptMapper.selectDeptById(101L)).thenReturn(oldDept);
        when(deptMapper.selectChildrenDeptById(101L)).thenReturn(List.of(childDept));
        when(deptMapper.updateDept(any(SysDept.class))).thenReturn(1);
        when(deptMapper.updateDeptChildren(any())).thenReturn(1);

        SysDeptServiceImpl service = new SysDeptServiceImpl(
                deptMapper,
                mock(SysUserDeptMapper.class),
                mock(ISysDeptService.class)
        );
        SysDept command = new SysDept();
        command.setDeptId(101L);
        command.setParentId(2L);

        service.updateDept(command);

        verify(deptMapper).updateDeptChildren(argThat(children ->
                children.size() == 1 && "0,2,101".equals(children.get(0).getAncestors())));
    }
}
