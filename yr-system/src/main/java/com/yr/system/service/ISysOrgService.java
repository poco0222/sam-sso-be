package com.yr.system.service;

import com.yr.common.core.domain.entity.SysOrg;

import java.util.List;

/**
 * 组织信息Service接口
 *
 * @author Youngron
 * @date 2021-09-09
 */
public interface ISysOrgService {
    /**
     * 查询组织信息
     *
     * @param orgId 组织信息ID
     * @return 组织信息
     */
    public SysOrg selectSysOrgById(Long orgId);

    /**
     * 查询组织信息列表
     *
     * @param sysOrg 组织信息
     * @return 组织信息集合
     */
    public List<SysOrg> selectSysOrgList(SysOrg sysOrg);

    /**
     * 新增组织信息
     *
     * @param sysOrg 组织信息
     * @return 结果
     */
    public int insertSysOrg(SysOrg sysOrg);

    /**
     * 修改组织信息
     *
     * @param sysOrg 组织信息
     * @return 结果
     */
    public int updateSysOrg(SysOrg sysOrg);

    /**
     * 批量删除组织信息
     *
     * @param orgIds 需要删除的组织信息ID
     * @return 结果
     */
    public int deleteSysOrgByIds(Long[] orgIds);

    /**
     * 删除组织信息信息
     *
     * @param orgId 组织信息ID
     * @return 结果
     */
    public int deleteSysOrgById(Long orgId);

    /**
     * 校验组织编码是否唯一
     *
     * @param orgCode
     * @return
     */
    public String checkOrgCodeUnique(String orgCode);

    /**
     * 状态修改
     *
     * @param sysOrg
     * @return
     */
    public int updateOrgStatus(SysOrg sysOrg);

    /**
     * 查询是否有子节点数据
     *
     * @param orgIds
     * @return
     */
    public boolean hasChildByOrgId(Long orgIds);

    /**
     * 查询正常状态的子组织数量
     *
     * @param orgId
     * @return
     */
    int selectNormalChildrenOrgById(Long orgId);
}
