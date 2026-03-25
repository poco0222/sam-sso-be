/**
 * @file legacy 身份快照加载器抽象基类
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service.support;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yr.common.core.domain.entity.SysOrg;
import com.yr.system.domain.dto.SsoIdentityImportSnapshot;
import com.yr.system.domain.entity.SysUserDept;
import com.yr.system.domain.entity.SysUserOrg;
import com.yr.system.mapper.SysDeptMapper;
import com.yr.system.mapper.SysOrgMapper;
import com.yr.system.mapper.SysUserDeptMapper;
import com.yr.system.mapper.SysUserMapper;
import com.yr.system.mapper.SysUserOrgMapper;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 复用 snapshot 读取逻辑，由具体 adapter 只负责声明 datasource annotation。
 * 这里读取的 legacy source 只用于 INIT_IMPORT bootstrap（初始化导入）阶段；
 * 导入完成后，`local_sam_empty` 会成为唯一主数据源，并继续承担后续分发来源。
 */
public abstract class AbstractSsoLegacyIdentitySnapshotLoader implements ISsoLegacyIdentitySnapshotLoader {

    /** 组织读取 Mapper。 */
    private final SysOrgMapper sysOrgMapper;

    /** 部门读取 Mapper。 */
    private final SysDeptMapper sysDeptMapper;

    /** 用户读取 Mapper。 */
    private final SysUserMapper sysUserMapper;

    /** 用户组织关联读取 Mapper。 */
    private final SysUserOrgMapper sysUserOrgMapper;

    /** 用户部门关联读取 Mapper。 */
    private final SysUserDeptMapper sysUserDeptMapper;

    /**
     * @param sysOrgMapper 组织读取 Mapper
     * @param sysDeptMapper 部门读取 Mapper
     * @param sysUserMapper 用户读取 Mapper
     * @param sysUserOrgMapper 用户组织关联读取 Mapper
     * @param sysUserDeptMapper 用户部门关联读取 Mapper
     */
    protected AbstractSsoLegacyIdentitySnapshotLoader(SysOrgMapper sysOrgMapper,
                                                      SysDeptMapper sysDeptMapper,
                                                      SysUserMapper sysUserMapper,
                                                      SysUserOrgMapper sysUserOrgMapper,
                                                      SysUserDeptMapper sysUserDeptMapper) {
        this.sysOrgMapper = sysOrgMapper;
        this.sysDeptMapper = sysDeptMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysUserOrgMapper = sysUserOrgMapper;
        this.sysUserDeptMapper = sysUserDeptMapper;
    }

    /**
     * 读取完整身份快照。
     *
     * @return legacy source 当前快照
     */
    @Override
    public SsoIdentityImportSnapshot loadSnapshot() {
        SsoIdentityImportSnapshot snapshot = new SsoIdentityImportSnapshot();

        // Phase 1 明确只导入 org/dept/user 及两类关系，避免把非主数据范围带进身份中心。
        // 语义上只跳过逻辑删除数据，停用状态继续保留给身份中心承接，供后续主数据分发沿用。
        snapshot.setOrgList(sysOrgMapper.selectSysOrgList(new SysOrg()));
        snapshot.setDeptList(sysDeptMapper.getAllDeptInfo().stream()
                .filter(this::shouldImportDept)
                .collect(Collectors.toList()));
        snapshot.setUserList(sysUserMapper.selectUserListForExcel().stream()
                .filter(this::shouldImportUser)
                .collect(Collectors.toList()));

        Set<Long> orgIdSet = snapshot.getOrgList().stream()
                .map(SysOrg::getOrgId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> deptIdSet = snapshot.getDeptList().stream()
                .map(com.yr.common.core.domain.entity.SysDept::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> userIdSet = snapshot.getUserList().stream()
                .map(com.yr.common.core.domain.entity.SysUser::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 关系快照只保留已启用且仍挂在未删除主体上的数据，避免把脏关系带入身份中心。
        snapshot.setUserOrgRelationList(sysUserOrgMapper.selectList(new QueryWrapper<SysUserOrg>().orderByAsc("id")).stream()
                .filter(relation -> shouldImportUserOrgRelation(relation, userIdSet, orgIdSet))
                .collect(Collectors.toList()));
        snapshot.setUserDeptRelationList(sysUserDeptMapper.selectList(new QueryWrapper<SysUserDept>().orderByAsc("id")).stream()
                .filter(relation -> shouldImportUserDeptRelation(relation, userIdSet, deptIdSet))
                .collect(Collectors.toList()));
        return snapshot;
    }

    /**
     * INIT_IMPORT 只跳过已逻辑删除的部门，停用状态继续保留给身份中心承接。
     *
     * @param dept 部门
     * @return 可导入时返回 true
     */
    private boolean shouldImportDept(com.yr.common.core.domain.entity.SysDept dept) {
        return dept != null && "0".equals(dept.getDelFlag());
    }

    /**
     * INIT_IMPORT 只跳过已逻辑删除的用户，停用状态继续保留给身份中心承接。
     *
     * @param user 用户
     * @return 可导入时返回 true
     */
    private boolean shouldImportUser(com.yr.common.core.domain.entity.SysUser user) {
        return user != null && "0".equals(user.getDelFlag());
    }

    /**
     * 用户组织关系需要同时满足“关系启用 + 主体仍在导入范围内”。
     *
     * @param relation 用户组织关系
     * @param userIdSet 已导入用户 ID 集合
     * @param orgIdSet 已导入组织 ID 集合
     * @return 可导入时返回 true
     */
    private boolean shouldImportUserOrgRelation(SysUserOrg relation, Set<Long> userIdSet, Set<Long> orgIdSet) {
        return relation != null
                && Integer.valueOf(1).equals(relation.getEnabled())
                && userIdSet.contains(relation.getUserId())
                && orgIdSet.contains(relation.getOrgId());
    }

    /**
     * 用户部门关系需要同时满足“关系启用 + 主体仍在导入范围内”。
     *
     * @param relation 用户部门关系
     * @param userIdSet 已导入用户 ID 集合
     * @param deptIdSet 已导入部门 ID 集合
     * @return 可导入时返回 true
     */
    private boolean shouldImportUserDeptRelation(SysUserDept relation, Set<Long> userIdSet, Set<Long> deptIdSet) {
        return relation != null
                && Integer.valueOf(1).equals(relation.getEnabled())
                && userIdSet.contains(relation.getUserId())
                && deptIdSet.contains(relation.getDeptId());
    }
}
