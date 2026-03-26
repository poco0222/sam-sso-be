/**
 * @file 验证 SysUserServiceImpl 已将热点职责委托给专用协作服务
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.page.PageDomain;
import com.yr.system.mapper.SysRoleMapper;
import com.yr.system.mapper.SysUserMapper;
import com.yr.system.mapper.SysUserPostMapper;
import com.yr.system.mapper.SysUserRoleMapper;
import com.yr.system.service.ISysOrgService;
import com.yr.system.service.ISysUserDeptService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysUserServiceImpl 委托行为测试。
 */
class SysUserServiceImplDelegationTest {

    /**
     * 验证导入请求已经委托给专用导入服务，避免继续堆在主服务中。
     */
    @Test
    void shouldDelegateImportWorkflowToDedicatedImportService() {
        SysUserImportService importService = mock(SysUserImportService.class);
        SysUserQueryService queryService = mock(SysUserQueryService.class);
        SysUserServiceImpl userService = buildUserService(importService, queryService);
        List<SysUser> userList = List.of(new SysUser());

        when(importService.importUser(userList, true, "phase2")).thenReturn("delegated");

        String result = userService.importUser(userList, true, "phase2");

        assertThat(result).isEqualTo("delegated");
        verify(importService).importUser(userList, true, "phase2");
    }

    /**
     * 验证按部门编码查用户已经委托给专用查询服务。
     */
    @Test
    void shouldDelegateDeptCodeLookupToDedicatedQueryService() {
        SysUserImportService importService = mock(SysUserImportService.class);
        SysUserQueryService queryService = mock(SysUserQueryService.class);
        SysUserServiceImpl userService = buildUserService(importService, queryService);
        SysUser user = new SysUser();
        user.setUserName("phase2-user");

        when(queryService.selectSysUserById("RD")).thenReturn(List.of(user));

        List<SysUser> result = userService.selectSysUserById("RD");

        assertThat(result).extracting(SysUser::getUserName).containsExactly("phase2-user");
        verify(queryService).selectSysUserById("RD");
    }

    /**
     * 验证模式分组分页查询仍委托给专用查询服务，避免把 controller 过滤逻辑重新揉回主服务。
     */
    @Test
    void shouldDelegateModeGroupPageQueryToDedicatedQueryService() {
        SysUserImportService importService = mock(SysUserImportService.class);
        SysUserQueryService queryService = mock(SysUserQueryService.class);
        SysUserServiceImpl userService = buildUserService(importService, queryService);
        PageDomain pageDomain = new PageDomain();
        pageDomain.setPageNum(2);
        pageDomain.setPageSize(5);
        SysUser command = new SysUser();
        command.setUserName("phase2");
        IPage<SysUser> expectedPage = new Page<>(pageDomain.getPageNum(), pageDomain.getPageSize());

        when(queryService.queryModeUserGroupInformationCollection(pageDomain, command)).thenReturn(expectedPage);

        IPage<SysUser> result = userService.queryModeUserGroupInformationCollection(pageDomain, command);

        assertThat(result).isSameAs(expectedPage);
        verify(queryService).queryModeUserGroupInformationCollection(pageDomain, command);
    }

    /**
     * 构造最小依赖的用户服务实例，仅保留本测试关注的导入与查询协作对象。
     *
     * @param importService 导入服务
     * @param queryService 查询服务
     * @return 用户服务实例
     */
    private SysUserServiceImpl buildUserService(SysUserImportService importService,
                                                SysUserQueryService queryService) {
        return new SysUserServiceImpl(
                mock(SysUserMapper.class),
                mock(SysRoleMapper.class),
                mock(SysUserRoleMapper.class),
                mock(SysUserPostMapper.class),
                mock(ISysUserDeptService.class),
                mock(ISysOrgService.class),
                mock(SysUserWriteService.class),
                importService,
                queryService
        );
    }
}
