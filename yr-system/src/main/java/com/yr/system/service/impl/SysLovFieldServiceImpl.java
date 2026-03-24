package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysLovField;
import com.yr.system.mapper.SysLovFieldMapper;
import com.yr.system.service.ISysLovFieldService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 值集视图字段 业务层处理
 *
 * @author Youngron
 * @date 2021-09-13
 */
@Service
public class SysLovFieldServiceImpl implements ISysLovFieldService {
    private final SysLovFieldMapper sysLovFieldMapper;

    public SysLovFieldServiceImpl(SysLovFieldMapper sysLovFieldMapper) {
        this.sysLovFieldMapper = sysLovFieldMapper;
    }

    /**
     * 查询值集视图字段
     *
     * @param lovId 值集视图字段ID
     * @return 值集视图字段
     */
    @Override
    public SysLovField selectLovFieldById(Long lovId) {
        return sysLovFieldMapper.selectLovFieldById(lovId);
    }

    /**
     * 查询值集视图字段列表
     *
     * @param sysLovField 值集视图字段
     * @return 值集视图字段
     */
    @Override
    public List<SysLovField> selectLovFieldList(SysLovField sysLovField) {
        return sysLovFieldMapper.selectLovFieldList(sysLovField);
    }

    /**
     * 新增值集视图字段
     *
     * @param sysLovField 值集视图字段
     * @return 结果
     */
    @Override
    public int insertLovField(SysLovField sysLovField) {
        return sysLovFieldMapper.insertLovField(sysLovField);
    }

    /**
     * 修改值集视图字段
     *
     * @param sysLovField 值集视图字段
     * @return 结果
     */
    @Override
    public int updateLovField(SysLovField sysLovField) {
        return sysLovFieldMapper.updateLovField(sysLovField);
    }

    /**
     * 删除值集视图字段信息
     *
     * @param fieldId 值集视图字段ID
     * @return 结果
     */
    @Override
    public int deleteLovFieldById(Long fieldId) {
        return sysLovFieldMapper.deleteLovFieldById(fieldId);
    }

    /**
     * 批量删除值集视图字段
     *
     * @param fieldIds 值集视图字段ID
     */
    @Override
    public void deleteLovFieldByIds(Long[] fieldIds) {
        sysLovFieldMapper.deleteLovFieldByIds(fieldIds);
    }
}
