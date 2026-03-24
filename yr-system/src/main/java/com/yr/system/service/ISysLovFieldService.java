package com.yr.system.service;

import com.yr.common.core.domain.entity.SysLovField;

import java.util.List;

/**
 * 值集视图字段 业务层
 *
 * @author Youngron
 * @date 2021-09-13
 */
public interface ISysLovFieldService {
    /**
     * 查询值集视图字段
     *
     * @param lovId 值集视图字段ID
     * @return 值集视图字段信息
     */
    public SysLovField selectLovFieldById(Long lovId);

    /**
     * 查询值集视图字段列表
     *
     * @param sysLovField 值集视图字段
     * @return 值集视图字段集合
     */
    public List<SysLovField> selectLovFieldList(SysLovField sysLovField);

    /**
     * 新增值集视图字段
     *
     * @param sysLovField 值集视图字段
     * @return 结果
     */
    public int insertLovField(SysLovField sysLovField);

    /**
     * 修改值集视图字段
     *
     * @param sysLovField 值集视图字段
     * @return 结果
     */
    public int updateLovField(SysLovField sysLovField);

    /**
     * 删除值集视图字段信息
     *
     * @param fieldId 值集视图字段ID
     * @return 结果
     */
    public int deleteLovFieldById(Long fieldId);

    /**
     * 批量删除值集视图字段
     *
     * @param fieldIds 值集视图字段ID
     */
    public void deleteLovFieldByIds(Long[] fieldIds);

}
