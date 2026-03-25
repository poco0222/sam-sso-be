/**
 * @file 当前主库身份快照加载器
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service.support;

import com.yr.common.enums.DataSourceType;
import com.yr.system.mapper.SysDeptMapper;
import com.yr.system.mapper.SysOrgMapper;
import com.yr.system.mapper.SysUserDeptMapper;
import com.yr.system.mapper.SysUserMapper;
import com.yr.system.mapper.SysUserOrgMapper;
import org.springframework.stereotype.Component;

/**
 * 从当前身份中心主库读取可分发的 org/dept/user 及关系快照。
 */
@Component
public class SsoCurrentIdentitySnapshotLoader extends AbstractSsoLegacyIdentitySnapshotLoader {

    /**
     * @param sysOrgMapper 组织读取 Mapper
     * @param sysDeptMapper 部门读取 Mapper
     * @param sysUserMapper 用户读取 Mapper
     * @param sysUserOrgMapper 用户组织关系读取 Mapper
     * @param sysUserDeptMapper 用户部门关系读取 Mapper
     */
    public SsoCurrentIdentitySnapshotLoader(SysOrgMapper sysOrgMapper,
                                            SysDeptMapper sysDeptMapper,
                                            SysUserMapper sysUserMapper,
                                            SysUserOrgMapper sysUserOrgMapper,
                                            SysUserDeptMapper sysUserDeptMapper) {
        super(sysOrgMapper, sysDeptMapper, sysUserMapper, sysUserOrgMapper, sysUserDeptMapper);
    }

    /**
     * 当前分发快照固定来自主库。
     *
     * @return 主库数据源类型
     */
    @Override
    public DataSourceType getDataSourceType() {
        return DataSourceType.MASTER;
    }
}
