/**
 * @file legacy 身份快照加载器过滤语义测试
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service.support;

import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysOrg;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.enums.DataSourceType;
import com.yr.system.domain.dto.SsoIdentityImportSnapshot;
import com.yr.system.domain.entity.SysUserDept;
import com.yr.system.domain.entity.SysUserOrg;
import com.yr.system.mapper.SysDeptMapper;
import com.yr.system.mapper.SysOrgMapper;
import com.yr.system.mapper.SysUserDeptMapper;
import com.yr.system.mapper.SysUserMapper;
import com.yr.system.mapper.SysUserOrgMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 验证 INIT_IMPORT 快照读取会跳过逻辑删除数据，但保留停用状态，避免把垃圾数据带入身份中心。
 */
@ExtendWith(MockitoExtension.class)
class AbstractSsoLegacyIdentitySnapshotLoaderTest {

    /** 组织读取 Mapper。 */
    @Mock
    private SysOrgMapper sysOrgMapper;

    /** 部门读取 Mapper。 */
    @Mock
    private SysDeptMapper sysDeptMapper;

    /** 用户读取 Mapper。 */
    @Mock
    private SysUserMapper sysUserMapper;

    /** 用户组织关系读取 Mapper。 */
    @Mock
    private SysUserOrgMapper sysUserOrgMapper;

    /** 用户部门关系读取 Mapper。 */
    @Mock
    private SysUserDeptMapper sysUserDeptMapper;

    /**
     * 验证快照会过滤删除数据与无效关系，但保留停用 org/dept/user 的状态语义。
     */
    @Test
    void shouldExcludeDeletedDataButKeepDisabledStatusInSnapshot() {
        TestSnapshotLoader loader = new TestSnapshotLoader(
                sysOrgMapper,
                sysDeptMapper,
                sysUserMapper,
                sysUserOrgMapper,
                sysUserDeptMapper
        );

        when(sysOrgMapper.selectSysOrgList(any(SysOrg.class))).thenReturn(List.of(
                buildOrg(1L, "0"),
                buildOrg(2L, "1")
        ));
        when(sysDeptMapper.getAllDeptInfo()).thenReturn(List.of(
                buildDept(10L, "0", "0"),
                buildDept(11L, "1", "0"),
                buildDept(12L, "0", "2")
        ));
        when(sysUserMapper.selectUserListForExcel()).thenReturn(List.of(
                buildUser(100L, 10L, "0", "0"),
                buildUser(101L, 11L, "1", "0"),
                buildUser(102L, 12L, "0", "2"),
                buildUser(103L, 10L, "1", "1")
        ));
        when(sysUserOrgMapper.selectList(any())).thenReturn(List.of(
                buildUserOrg(1000L, 100L, 1L, 1),
                buildUserOrg(1001L, 101L, 2L, 1),
                buildUserOrg(1002L, 102L, 1L, 1),
                buildUserOrg(1003L, 100L, 2L, 0)
        ));
        when(sysUserDeptMapper.selectList(any())).thenReturn(List.of(
                buildUserDept(2000L, 100L, 10L, 1),
                buildUserDept(2001L, 101L, 11L, 1),
                buildUserDept(2002L, 102L, 12L, 1),
                buildUserDept(2003L, 100L, 11L, 0)
        ));

        SsoIdentityImportSnapshot snapshot = loader.loadSnapshot();

        assertThat(snapshot.getOrgList())
                .extracting(SysOrg::getOrgId)
                .containsExactlyInAnyOrder(1L, 2L);
        assertThat(snapshot.getOrgList())
                .extracting(SysOrg::getStatus)
                .containsExactlyInAnyOrder("0", "1");

        assertThat(snapshot.getDeptList())
                .extracting(SysDept::getDeptId)
                .containsExactlyInAnyOrder(10L, 11L);
        assertThat(snapshot.getDeptList())
                .extracting(SysDept::getStatus)
                .containsExactlyInAnyOrder("0", "1");

        assertThat(snapshot.getUserList())
                .extracting(SysUser::getUserId)
                .containsExactlyInAnyOrder(100L, 101L);
        assertThat(snapshot.getUserList())
                .extracting(SysUser::getStatus)
                .containsExactlyInAnyOrder("0", "1");

        assertThat(snapshot.getUserOrgRelationList())
                .extracting(SysUserOrg::getId)
                .containsExactlyInAnyOrder(1000L, 1001L);
        assertThat(snapshot.getUserDeptRelationList())
                .extracting(SysUserDept::getId)
                .containsExactlyInAnyOrder(2000L, 2001L);
    }

    /**
     * 构造测试组织。
     *
     * @param orgId 组织 ID
     * @param status 组织状态
     * @return 组织对象
     */
    private SysOrg buildOrg(Long orgId, String status) {
        SysOrg org = new SysOrg();
        org.setOrgId(orgId);
        org.setOrgCode("ORG-" + orgId);
        org.setOrgName("org-" + orgId);
        org.setStatus(status);
        return org;
    }

    /**
     * 构造测试部门。
     *
     * @param deptId 部门 ID
     * @param status 部门状态
     * @param delFlag 删除标记
     * @return 部门对象
     */
    private SysDept buildDept(Long deptId, String status, String delFlag) {
        SysDept dept = new SysDept();
        dept.setDeptId(deptId);
        dept.setDeptCode("DEPT-" + deptId);
        dept.setDeptName("dept-" + deptId);
        dept.setStatus(status);
        dept.setDelFlag(delFlag);
        dept.setOrgId(deptId == 11L ? 2L : 1L);
        dept.setParentId(0L);
        dept.setOrderNum("1");
        return dept;
    }

    /**
     * 构造测试用户。
     *
     * @param userId 用户 ID
     * @param deptId 默认部门 ID
     * @param status 用户状态
     * @param delFlag 删除标记
     * @return 用户对象
     */
    private SysUser buildUser(Long userId, Long deptId, String status, String delFlag) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setDeptId(deptId);
        user.setUserName("user-" + userId);
        user.setNickName("nick-" + userId);
        user.setStatus(status);
        user.setDelFlag(delFlag);
        return user;
    }

    /**
     * 构造测试用户组织关系。
     *
     * @param id 关系 ID
     * @param userId 用户 ID
     * @param orgId 组织 ID
     * @param enabled 启用标志
     * @return 用户组织关系
     */
    private SysUserOrg buildUserOrg(Long id, Long userId, Long orgId, Integer enabled) {
        SysUserOrg userOrg = new SysUserOrg();
        userOrg.setId(id);
        userOrg.setUserId(userId);
        userOrg.setOrgId(orgId);
        userOrg.setIsDefault(1);
        userOrg.setEnabled(enabled);
        return userOrg;
    }

    /**
     * 构造测试用户部门关系。
     *
     * @param id 关系 ID
     * @param userId 用户 ID
     * @param deptId 部门 ID
     * @param enabled 启用标志
     * @return 用户部门关系
     */
    private SysUserDept buildUserDept(Long id, Long userId, Long deptId, Integer enabled) {
        SysUserDept userDept = new SysUserDept();
        userDept.setId(id);
        userDept.setUserId(userId);
        userDept.setDeptId(deptId);
        userDept.setIsDefault(1);
        userDept.setEnabled(enabled);
        return userDept;
    }

    /**
     * 用于单元测试的最小 loader 实现，只负责暴露抽象基类行为。
     */
    private static final class TestSnapshotLoader extends AbstractSsoLegacyIdentitySnapshotLoader {

        /**
         * @param sysOrgMapper 组织读取 Mapper
         * @param sysDeptMapper 部门读取 Mapper
         * @param sysUserMapper 用户读取 Mapper
         * @param sysUserOrgMapper 用户组织关系读取 Mapper
         * @param sysUserDeptMapper 用户部门关系读取 Mapper
         */
        private TestSnapshotLoader(SysOrgMapper sysOrgMapper,
                                   SysDeptMapper sysDeptMapper,
                                   SysUserMapper sysUserMapper,
                                   SysUserOrgMapper sysUserOrgMapper,
                                   SysUserDeptMapper sysUserDeptMapper) {
            super(sysOrgMapper, sysDeptMapper, sysUserMapper, sysUserOrgMapper, sysUserDeptMapper);
        }

        /**
         * @return 任意 secondary datasource 类型；本测试不依赖真实路由
         */
        @Override
        public DataSourceType getDataSourceType() {
            return DataSourceType.SLAVE;
        }
    }
}
