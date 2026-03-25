/**
 * @file INIT_IMPORT 执行服务测试
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.core.domain.entity.SsoSyncTaskItem;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysOrg;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.system.domain.dto.SsoIdentityImportExecutionResult;
import com.yr.system.domain.dto.SsoIdentityImportSnapshot;
import com.yr.system.domain.entity.SysUserDept;
import com.yr.system.domain.entity.SysUserOrg;
import com.yr.system.mapper.SysDeptMapper;
import com.yr.system.mapper.SysOrgMapper;
import com.yr.system.mapper.SysUserDeptMapper;
import com.yr.system.mapper.SysUserMapper;
import com.yr.system.mapper.SysUserOrgMapper;
import com.yr.system.service.ISsoLegacyIdentitySourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 INIT_IMPORT 执行服务能生成 item 明细、统计与 upsert 写入。
 */
@ExtendWith(MockitoExtension.class)
class SsoIdentityImportServiceImplTest {

    /** legacy source 读取服务。 */
    @Mock
    private ISsoLegacyIdentitySourceService ssoLegacyIdentitySourceService;

    /** 组织写入 Mapper。 */
    @Mock
    private SysOrgMapper sysOrgMapper;

    /** 部门写入 Mapper。 */
    @Mock
    private SysDeptMapper sysDeptMapper;

    /** 用户写入 Mapper。 */
    @Mock
    private SysUserMapper sysUserMapper;

    /** 用户组织写入 Mapper。 */
    @Mock
    private SysUserOrgMapper sysUserOrgMapper;

    /** 用户部门写入 Mapper。 */
    @Mock
    private SysUserDeptMapper sysUserDeptMapper;

    /**
     * 验证完整快照导入成功时会生成 success item 并执行五类 upsert。
     */
    @Test
    void shouldExecuteInitImportFromLegacySnapshot() {
        SsoIdentityImportServiceImpl service = createService();
        when(ssoLegacyIdentitySourceService.loadSnapshot()).thenReturn(buildSnapshot());
        when(sysOrgMapper.selectSysOrgById(101L)).thenReturn(null);
        when(sysDeptMapper.selectSysDeptByDeptId(201L)).thenReturn(null);
        when(sysUserMapper.selectSysUserByUserId(301L)).thenReturn(null);
        SysUserOrg persistedUserOrg = new SysUserOrg();
        persistedUserOrg.setId(401L);
        persistedUserOrg.setUserId(301L);
        persistedUserOrg.setOrgId(101L);
        SysUserDept persistedUserDept = new SysUserDept();
        persistedUserDept.setId(501L);
        persistedUserDept.setUserId(301L);
        persistedUserDept.setDeptId(201L);
        when(sysUserOrgMapper.selectOne(any())).thenReturn(null, persistedUserOrg);
        when(sysUserDeptMapper.selectOne(any())).thenReturn(null, persistedUserDept);
        when(sysUserOrgMapper.insertInitImport(anyLong(), anyLong(), any(), any(), anyLong(), any(Date.class))).thenReturn(1);
        when(sysUserDeptMapper.insertInitImport(anyLong(), anyLong(), any(), any(), anyLong(), any(Date.class))).thenReturn(1);

        SsoIdentityImportExecutionResult result = service.execute(buildTask(), null);

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getTotalItemCount()).isEqualTo(5);
        assertThat(result.getSuccessItemCount()).isEqualTo(5);
        assertThat(result.getFailedItemCount()).isZero();
        assertThat(result.getItemList()).hasSize(5);
        assertThat(result.getResultSummary()).contains("成功 5 条");
        assertThat(result.getItemList()).allSatisfy(item -> {
            assertThat(item.getStatus()).isEqualTo("SUCCESS");
            assertThat(item.getTargetId()).isNotBlank();
        });

        verify(sysOrgMapper).insertSysOrg(any(SysOrg.class));
        verify(sysDeptMapper).insertDept(any(SysDept.class));
        verify(sysUserMapper).insertUser(any(SysUser.class));
        verify(sysUserOrgMapper).insertInitImport(anyLong(), anyLong(), any(), any(), anyLong(), any(Date.class));
        verify(sysUserDeptMapper).insertInitImport(anyLong(), anyLong(), any(), any(), anyLong(), any(Date.class));
    }

    /**
     * 验证 scoped retry/compensation 里部分 sourceId 未命中时返回 PARTIAL_SUCCESS。
     */
    @Test
    void shouldMarkPartialSuccessWhenScopedItemsContainUnknownSource() {
        SsoIdentityImportServiceImpl service = createService();
        when(ssoLegacyIdentitySourceService.loadSnapshot()).thenReturn(buildSnapshot());
        when(sysOrgMapper.selectSysOrgById(101L)).thenReturn(null);

        SsoSyncTaskItem orgItem = new SsoSyncTaskItem();
        orgItem.setEntityType("org");
        orgItem.setSourceId("101");
        orgItem.setDetailJson("{\"orgId\":101}");

        SsoSyncTaskItem missingUserItem = new SsoSyncTaskItem();
        missingUserItem.setEntityType("user");
        missingUserItem.setSourceId("999");
        missingUserItem.setDetailJson("{\"userId\":999}");

        SsoIdentityImportExecutionResult result = service.execute(buildTask(), List.of(orgItem, missingUserItem));

        assertThat(result.getStatus()).isEqualTo("PARTIAL_SUCCESS");
        assertThat(result.getSuccessItemCount()).isEqualTo(1);
        assertThat(result.getFailedItemCount()).isEqualTo(1);
        assertThat(result.getItemList()).filteredOn(item -> "FAILED".equals(item.getStatus()))
                .singleElement()
                .satisfies(item -> assertThat(item.getErrorMessage()).contains("未找到来源用户"));
    }

    /**
     * 验证过长错误信息会被裁剪到任务明细字段长度内，避免失败明细再次触发落库异常。
     */
    @Test
    void shouldTruncateOverlongErrorMessageToFitTaskItemColumn() {
        SsoIdentityImportServiceImpl service = createService();
        SysUser failingUser = new SysUser();
        failingUser.setUserId(1L);
        failingUser.setUserName("admin");
        failingUser.setNickName("超级管理员");
        SsoIdentityImportSnapshot snapshot = new SsoIdentityImportSnapshot();
        snapshot.setUserList(List.of(failingUser));
        when(ssoLegacyIdentitySourceService.loadSnapshot()).thenReturn(snapshot);
        when(sysUserMapper.selectSysUserByUserId(1L)).thenReturn(null);
        when(sysUserMapper.insertUser(any(SysUser.class))).thenThrow(new RuntimeException("x".repeat(800)));

        SsoIdentityImportExecutionResult result = service.execute(buildTask(), null);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getItemList()).singleElement().satisfies(item -> {
            assertThat(item.getStatus()).isEqualTo("FAILED");
            assertThat(item.getErrorMessage()).hasSizeLessThanOrEqualTo(500);
            assertThat(item.getErrorMessage()).endsWith("...");
        });
    }

    /**
     * 构造最小任务对象。
     */
    private SsoSyncTask buildTask() {
        SsoSyncTask task = new SsoSyncTask();
        task.setTaskId(1L);
        task.setCreateBy("tester");
        return task;
    }

    /**
     * 构造最小快照。
     */
    private SsoIdentityImportSnapshot buildSnapshot() {
        SysOrg org = new SysOrg();
        org.setOrgId(101L);
        org.setOrgCode("ORG-101");
        org.setOrgName("总部");
        org.setParentId(0L);
        org.setStatus("0");

        SysDept dept = new SysDept();
        dept.setDeptId(201L);
        dept.setDeptCode("DEPT-201");
        dept.setDeptName("技术中心");
        dept.setParentId(1L);
        dept.setOrgId(101L);
        dept.setStatus("0");
        dept.setDelFlag("0");
        dept.setOrderNum("1");

        SysUser user = new SysUser();
        user.setUserId(301L);
        user.setUserName("zhangsan");
        user.setNickName("张三");
        user.setDeptId(201L);
        user.setStatus("0");

        SysUserOrg userOrg = new SysUserOrg();
        userOrg.setId(9001L);
        userOrg.setUserId(301L);
        userOrg.setOrgId(101L);
        userOrg.setIsDefault(1);
        userOrg.setEnabled(1);

        SysUserDept userDept = new SysUserDept();
        userDept.setId(9002L);
        userDept.setUserId(301L);
        userDept.setDeptId(201L);
        userDept.setIsDefault(1);
        userDept.setEnabled(1);

        SsoIdentityImportSnapshot snapshot = new SsoIdentityImportSnapshot();
        snapshot.setOrgList(List.of(org));
        snapshot.setDeptList(List.of(dept));
        snapshot.setUserList(List.of(user));
        snapshot.setUserOrgRelationList(List.of(userOrg));
        snapshot.setUserDeptRelationList(List.of(userDept));
        return snapshot;
    }

    /**
     * 构造待测服务。
     */
    private SsoIdentityImportServiceImpl createService() {
        return new SsoIdentityImportServiceImpl(
                ssoLegacyIdentitySourceService,
                sysOrgMapper,
                sysDeptMapper,
                sysUserMapper,
                sysUserOrgMapper,
                sysUserDeptMapper,
                new ObjectMapper()
        );
    }
}
