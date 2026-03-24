package com.yr.system.service;

import com.yr.common.core.domain.entity.SysLovType;

import java.util.List;

/**
 * 值集视图 业务层
 *
 * @author Youngron
 * @date 2021-09-13
 */
public interface ISysLovTypeService {
    /**
     * 查询值集视图
     *
     * @param lovId 值集视图ID
     * @return 值集视图
     */
    public SysLovType selectLovTypeById(Long lovId);

    /**
     * 查询值集视图列表
     *
     * @param sysLovType 值集视图
     * @return 值集视图集合
     */
    public List<SysLovType> selectLovTypeList(SysLovType sysLovType);

    /**
     * 新增值集视图
     *
     * @param sysLovType 值集视图
     * @return 结果
     */
    public int insertLovType(SysLovType sysLovType);

    /**
     * 修改值集视图
     *
     * @param sysLovType 值集视图
     * @return 结果
     */
    public int updateLovType(SysLovType sysLovType);

    /**
     * 校验视图类型是否唯一
     *
     * @param sysLovType 值集视图
     * @return 结果
     */
    public String checkLovTypeUnique(SysLovType sysLovType);

    /**
     * 查询值集视图配置
     *
     * @param lovType 值集视图类型
     * @return 值集视图
     */
    public SysLovType selectLovByType(String lovType);

}
