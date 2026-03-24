package com.yr.system.mapper;

import com.yr.common.core.domain.entity.SysOrg;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 组织信息Mapper接口
 *
 * @author Youngron
 * @date 2021-09-09
 */
public interface SysOrgMapper {
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
     * 删除组织信息
     *
     * @param orgId 组织信息ID
     * @return 结果
     */
    public int deleteSysOrgById(Long orgId);

    /**
     * 批量删除组织信息
     *
     * @param orgIds 需要删除的数据ID
     * @return 结果
     */
    public int deleteSysOrgByIds(Long[] orgIds);

    /**
     * 校验组织编码是否唯一
     *
     * @param orgCode
     * @return
     */
    public int checkOrgCodeUnique(String orgCode);

    /**
     * 查询是否有子节点数据
     *
     * @param orgId
     * @return
     */
    public int hasChildByOrgId(Long orgId);

    /**
     * 查询正常状态的子组织数量
     *
     * @param orgId
     * @return
     */
    int selectNormalChildrenOrgById(@Param("orgId") Long orgId);

    /**
     * 查询子组织信息
     *
     * @param orgId
     * @return
     */
    List<SysOrg> selectChildrenOrgById(@Param("orgId") Long orgId);

    /**
     * 批量更新子组织的祖级节点
     *
     * @param orgs
     * @return
     */
    int updateOrgChildren(@Param("orgs") List<SysOrg> orgs);

    /**
     * 修改所在组织正常状态
     *
     * @param orgIds
     * @return
     */
    int updateOrgStatusNormal(@Param("orgIds") Long[] orgIds);
}
