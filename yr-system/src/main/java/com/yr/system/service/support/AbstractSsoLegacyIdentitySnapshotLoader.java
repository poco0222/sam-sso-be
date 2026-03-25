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

/**
 * 复用 snapshot 读取逻辑，由具体 adapter 只负责声明 datasource annotation。
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
        snapshot.setOrgList(sysOrgMapper.selectSysOrgList(new SysOrg()));
        snapshot.setDeptList(sysDeptMapper.getAllDeptInfo());
        snapshot.setUserList(sysUserMapper.selectUserListForExcel());
        // 这里显式使用字符串列名，避免 Java 17 下 LambdaQueryWrapper 触发 `SerializedLambda` 反射限制。
        snapshot.setUserOrgRelationList(sysUserOrgMapper.selectList(new QueryWrapper<SysUserOrg>().orderByAsc("id")));
        snapshot.setUserDeptRelationList(sysUserDeptMapper.selectList(new QueryWrapper<SysUserDept>().orderByAsc("id")));
        return snapshot;
    }
}
