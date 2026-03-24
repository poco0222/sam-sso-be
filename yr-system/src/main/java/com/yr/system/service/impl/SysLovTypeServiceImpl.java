package com.yr.system.service.impl;

import com.yr.common.constant.UserConstants;
import com.yr.common.core.domain.entity.SysLovField;
import com.yr.common.core.domain.entity.SysLovType;
import com.yr.common.utils.StringUtils;
import com.yr.system.mapper.SysLovFieldMapper;
import com.yr.system.mapper.SysLovTypeMapper;
import com.yr.system.service.ISysLovTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 值集视图 业务层处理
 *
 * @author Youngron
 * @date 2021-09-13
 */
@Service
public class SysLovTypeServiceImpl implements ISysLovTypeService {
    private final SysLovTypeMapper sysLovTypeMapper;

    private final SysLovFieldMapper sysLovFieldMapper;

    public SysLovTypeServiceImpl(SysLovTypeMapper sysLovTypeMapper, SysLovFieldMapper sysLovFieldMapper) {
        this.sysLovTypeMapper = sysLovTypeMapper;
        this.sysLovFieldMapper = sysLovFieldMapper;
    }

    /**
     * 查询值集视图
     *
     * @param lovId 值集视图ID
     * @return 值集视图
     */
    @Override
    public SysLovType selectLovTypeById(Long lovId) {
        return sysLovTypeMapper.selectLovTypeById(lovId);
    }

    /**
     * 查询值集视图列表
     *
     * @param sysLovType 值集视图
     * @return 值集视图
     */
    @Override
    public List<SysLovType> selectLovTypeList(SysLovType sysLovType) {
        return sysLovTypeMapper.selectLovTypeList(sysLovType);
    }

    /**
     * 新增值集视图
     *
     * @param sysLovType 值集视图
     * @return 结果
     */
    @Override
    public int insertLovType(SysLovType sysLovType) {
        return sysLovTypeMapper.insertLovType(sysLovType);
    }

    /**
     * 修改值集视图
     *
     * @param sysLovType 值集视图
     * @return 结果
     */
    @Override
    public int updateLovType(SysLovType sysLovType) {
        return sysLovTypeMapper.updateLovType(sysLovType);
    }

    /**
     * 校验视图类型是否唯一
     *
     * @param sysLovType 值集视图
     * @return 结果
     */
    @Override
    public String checkLovTypeUnique(SysLovType sysLovType) {
        Long lovId = StringUtils.isNull(sysLovType.getLovId()) ? -1L : sysLovType.getLovId();
        SysLovType lovType = sysLovTypeMapper.checkLovTypeUnique(sysLovType.getLovType());
        if (StringUtils.isNotNull(lovType) && lovType.getLovId().longValue() != lovId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public SysLovType selectLovByType(String lovType) {
        // 获取值集视图头配置
        SysLovType lov = sysLovTypeMapper.selectLovByType(lovType);

        // 获取值集视图字段配置
        List<SysLovField> fields = sysLovFieldMapper.selectLovFieldListByLovId(lov.getLovId());

        lov.setFields(fields);

        return lov;
    }
}
