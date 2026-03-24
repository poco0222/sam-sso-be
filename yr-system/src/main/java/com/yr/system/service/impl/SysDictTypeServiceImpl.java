/**
 * @file 字典类型服务实现，负责字典查询与缓存维护
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.yr.common.constant.UserConstants;
import com.yr.common.core.domain.entity.SysDictData;
import com.yr.common.core.domain.entity.SysDictType;
import com.yr.common.exception.CustomException;
import com.yr.common.utils.DictUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.mapper.SysDictDataMapper;
import com.yr.system.mapper.SysDictTypeMapper;
import com.yr.system.service.ISysDictTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 字典 业务层处理
 *
 * @author PopoY
 */
@Service
public class SysDictTypeServiceImpl implements ISysDictTypeService {
    private final SysDictTypeMapper dictTypeMapper;

    private final SysDictDataMapper dictDataMapper;

    /**
     * 显式声明字典服务依赖，避免字段注入带来的隐式耦合。
     *
     * @param dictTypeMapper 字典类型 Mapper
     * @param dictDataMapper 字典数据 Mapper
     */
    public SysDictTypeServiceImpl(SysDictTypeMapper dictTypeMapper, SysDictDataMapper dictDataMapper) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictDataMapper = dictDataMapper;
    }

    /**
     * 根据条件分页查询字典类型
     *
     * @param dictType 字典类型信息
     * @return 字典类型集合信息
     */
    @Override
    public List<SysDictType> selectDictTypeList(SysDictType dictType) {
        return dictTypeMapper.selectDictTypeList(dictType);
    }

    /**
     * 根据所有字典类型
     *
     * @return 字典类型集合信息
     */
    @Override
    public List<SysDictType> selectDictTypeAll() {
        return dictTypeMapper.selectDictTypeAll();
    }

    /**
     * 根据字典类型查询字典数据
     *
     * @param dictType 字典类型
     * @return 字典数据集合信息
     */
    @Override
    public List<SysDictData> selectDictDataByType(String dictType) {
        List<SysDictData> dictDatas = DictUtils.getDictCache(dictType);
        if (StringUtils.isNotEmpty(dictDatas)) {
            return dictDatas;
        }
        dictDatas = dictDataMapper.selectDictDataByType(dictType);
        if (StringUtils.isNotEmpty(dictDatas)) {
            DictUtils.setDictCache(dictType, dictDatas);
            return dictDatas;
        }
        return Collections.emptyList();
    }

    /**
     * 根据字典类型ID查询信息
     *
     * @param dictId 字典类型ID
     * @return 字典类型
     */
    @Override
    public SysDictType selectDictTypeById(Long dictId) {
        return dictTypeMapper.selectDictTypeById(dictId);
    }

    /**
     * 根据字典类型查询信息
     *
     * @param dictType 字典类型
     * @return 字典类型
     */
    @Override
    public SysDictType selectDictTypeByType(String dictType) {
        return dictTypeMapper.selectDictTypeByType(dictType);
    }

    /**
     * 批量删除字典类型信息
     *
     * @param dictIds 需要删除的字典ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDictTypeByIds(Long[] dictIds) {
        for (Long dictId : dictIds) {
            SysDictType dictType = selectDictTypeById(dictId);
            if (dictDataMapper.countDictDataByType(dictType.getDictType()) > 0) {
                throw new CustomException(String.format("%1$s已分配,不能删除", dictType.getDictName()));
            }
            dictTypeMapper.deleteDictTypeById(dictId);
            DictUtils.removeDictCache(dictType.getDictType());
        }
    }

    /**
     * 加载字典缓存数据
     */
    @Override
    public void loadingDictCache() {
        warmUpDictCache();
    }

    /**
     * 启动后批量预热字典缓存，并返回处理的字典类型数量。
     *
     * @return 预热的字典类型数量
     */
    @Override
    public int warmUpDictCache() {
        List<SysDictType> dictTypeList = dictTypeMapper.selectDictTypeAll();
        for (SysDictType dictType : dictTypeList) {
            List<SysDictData> dictDatas = dictDataMapper.selectDictDataByType(dictType.getDictType());
            DictUtils.setDictCache(dictType.getDictType(), dictDatas);
        }
        return dictTypeList.size();
    }

    /**
     * 清空字典缓存数据
     */
    @Override
    public void clearDictCache() {
        DictUtils.clearDictCache();
    }

    /**
     * 重置字典缓存数据
     */
    @Override
    public void resetDictCache() {
        clearDictCache();
        loadingDictCache();
    }

    /**
     * 新增保存字典类型信息
     *
     * @param dict 字典类型信息
     * @return 结果
     */
    @Override
    public int insertDictType(SysDictType dict) {
        int row = dictTypeMapper.insertDictType(dict);
        if (row > 0) {
            DictUtils.setDictCache(dict.getDictType(), null);
        }
        return row;
    }

    /**
     * 修改保存字典类型信息
     *
     * @param dict 字典类型信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updateDictType(SysDictType dict) {
        SysDictType oldDict = dictTypeMapper.selectDictTypeById(dict.getDictId());
        // oldDict 缺失时抛出明确的 business exception（业务异常），避免泄漏 NPE（NullPointerException，空指针异常）。
        if (oldDict == null) {
            throw new CustomException("字典类型不存在");
        }
        String oldDictType = oldDict.getDictType();
        String newDictType = dict.getDictType();

        // update 行数为 0 表示未更新任何记录，需要显式失败（fail fast，快速失败）以避免“部分成功”。
        int row = dictTypeMapper.updateDictType(dict);
        if (row <= 0) {
            throw new CustomException("字典类型更新失败");
        }

        // oldDictType != newDictType 时同步迁移字典数据，并清理旧 cache key（缓存键）。
        if (!StringUtils.equals(oldDictType, newDictType)) {
            dictDataMapper.updateDictDataType(oldDictType, newDictType);
        }

        // cache refresh（缓存刷新）：成功路径总是刷新新 dictType 的缓存，并在类型变更时额外清理旧缓存。
        List<SysDictData> dictDatas = dictDataMapper.selectDictDataByType(newDictType);
        if (!StringUtils.equals(oldDictType, newDictType)) {
            DictUtils.removeDictCache(oldDictType);
        }
        DictUtils.setDictCache(newDictType, dictDatas);

        return row;
    }

    /**
     * 校验字典类型称是否唯一
     *
     * @param dict 字典类型
     * @return 结果
     */
    @Override
    public String checkDictTypeUnique(SysDictType dict) {
        Long dictId = StringUtils.isNull(dict.getDictId()) ? -1L : dict.getDictId();
        SysDictType dictType = dictTypeMapper.checkDictTypeUnique(dict.getDictType());
        if (StringUtils.isNotNull(dictType) && dictType.getDictId().longValue() != dictId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
}
