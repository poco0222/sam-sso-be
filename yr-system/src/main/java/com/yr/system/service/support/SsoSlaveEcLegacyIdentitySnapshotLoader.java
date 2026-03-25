/**
 * @file SQL Server slaveec 快照加载器
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service.support;

import com.yr.common.annotation.DataSource;
import com.yr.common.enums.DataSourceType;
import com.yr.system.domain.dto.SsoIdentityImportSnapshot;
import com.yr.system.mapper.SysDeptMapper;
import com.yr.system.mapper.SysOrgMapper;
import com.yr.system.mapper.SysUserDeptMapper;
import com.yr.system.mapper.SysUserMapper;
import com.yr.system.mapper.SysUserOrgMapper;
import org.springframework.stereotype.Component;

/**
 * 负责从 SQL Server `slaveec` 读取 legacy 身份快照。
 */
@Component
@DataSource(DataSourceType.SLAVEEC)
public class SsoSlaveEcLegacyIdentitySnapshotLoader extends AbstractSsoLegacyIdentitySnapshotLoader {

    /**
     * @param sysOrgMapper 组织读取 Mapper
     * @param sysDeptMapper 部门读取 Mapper
     * @param sysUserMapper 用户读取 Mapper
     * @param sysUserOrgMapper 用户组织关联读取 Mapper
     * @param sysUserDeptMapper 用户部门关联读取 Mapper
     */
    public SsoSlaveEcLegacyIdentitySnapshotLoader(SysOrgMapper sysOrgMapper,
                                                  SysDeptMapper sysDeptMapper,
                                                  SysUserMapper sysUserMapper,
                                                  SysUserOrgMapper sysUserOrgMapper,
                                                  SysUserDeptMapper sysUserDeptMapper) {
        super(sysOrgMapper, sysDeptMapper, sysUserMapper, sysUserOrgMapper, sysUserDeptMapper);
    }

    /**
     * @return 当前 adapter 对应的数据源类型
     */
    @Override
    public DataSourceType getDataSourceType() {
        return DataSourceType.SLAVEEC;
    }

    /**
     * 显式把 inherited snapshot 读取方法落到 concrete loader 上，确保 AOP 能命中真实 source 读链路。
     *
     * @return 当前数据源上的身份快照
     */
    @Override
    @DataSource(DataSourceType.SLAVEEC)
    public SsoIdentityImportSnapshot loadSnapshot() {
        return super.loadSnapshot();
    }
}
