/**
 * @file 部门新增控制器契约测试
 * @author PopoY
 * @date 2026-04-01
 */
package com.yr.web.controller.system;

import com.yr.common.constant.UserConstants;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.system.service.ISysDeptService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 锁定部门新增入口不再由 controller 擅自决定 orgId。
 */
class SysDeptControllerContractTest {

    /**
     * 每个用例后清理安全上下文，避免 orgId / username 串用。
     */
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 验证新增部门时 controller 不再回填当前登录组织，而是把 orgId 留给 service 依据父部门派生。
     */
    @Test
    void shouldNotBackfillOrgIdFromCurrentLoginContextWhenAddingDept() {
        SysDeptController controller = new SysDeptController();
        ISysDeptService deptService = mock(ISysDeptService.class);
        ArgumentCaptor<SysDept> deptCaptor = ArgumentCaptor.forClass(SysDept.class);
        SysDept request = new SysDept();

        setAuthenticatedUser("dept-operator", 999L);
        request.setParentId(88L);
        request.setDeptCode("RD-01");
        request.setDeptName("研发一部");
        request.setOrderNum("1");
        request.setStatus("0");
        ReflectionTestUtils.setField(controller, "deptService", deptService);
        when(deptService.checkDeptCodeUnique(any(SysDept.class))).thenReturn(UserConstants.UNIQUE);
        when(deptService.insertDept(any(SysDept.class))).thenReturn(1);

        AjaxResult result = controller.add(request);

        verify(deptService).insertDept(deptCaptor.capture());
        assertThat(result.get("code")).isEqualTo(200);
        assertThat(deptCaptor.getValue().getOrgId()).as("orgId 应由 service 根据父部门派生").isNull();
        assertThat(deptCaptor.getValue().getCreateBy()).isEqualTo("dept-operator");
    }

    /**
     * 写入最小登录态，供 SecurityUtils.getUsername/getOrgId 使用。
     *
     * @param username 当前用户名
     * @param orgId 当前组织 ID
     */
    private void setAuthenticatedUser(String username, Long orgId) {
        SysUser currentUser = new SysUser();
        currentUser.setUserId(99L);
        currentUser.setUserName(username);
        currentUser.setOrgId(orgId);
        LoginUser loginUser = new LoginUser(currentUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, Collections.emptyList())
        );
    }
}
