/**
 * @file 字典数据服务实现，负责字典数据增删改查与缓存维护
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysDictData;
import com.yr.common.utils.DictUtils;
import com.yr.system.mapper.SysDictDataMapper;
import com.yr.system.service.ISysDictDataService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典 业务层处理
 *
 * @author PopoY
 */
@Service
public class SysDictDataServiceImpl implements ISysDictDataService {
    private final SysDictDataMapper dictDataMapper;

    /**
     * 显式声明字典数据服务依赖，避免字段注入带来的隐式耦合。
     *
     * @param dictDataMapper 字典数据 Mapper
     */
    public SysDictDataServiceImpl(SysDictDataMapper dictDataMapper) {
        this.dictDataMapper = dictDataMapper;
    }

    /**
     * 根据条件分页查询字典数据
     *
     * @param dictData 字典数据信息
     * @return 字典数据集合信息
     */
    @Override
    public List<SysDictData> selectDictDataList(SysDictData dictData) {
        return dictDataMapper.selectDictDataList(dictData);
    }

    /**
     * 根据字典类型和字典键值查询字典数据信息
     *
     * @param dictType  字典类型
     * @param dictValue 字典键值
     * @return 字典标签
     */
    @Override
    public String selectDictLabel(String dictType, String dictValue) {
        return dictDataMapper.selectDictLabel(dictType, dictValue);
    }

    /**
     * 根据字典数据ID查询信息
     *
     * @param dictCode 字典数据ID
     * @return 字典数据
     */
    @Override
    public SysDictData selectDictDataById(Long dictCode) {
        return dictDataMapper.selectDictDataById(dictCode);
    }

    /**
     * 批量删除字典数据信息
     *
     * @param dictCodes 需要删除的字典数据ID
     */
    @Override
    public void deleteDictDataByIds(Long[] dictCodes) {
        // defensive checks（防御式校验）：空入参直接返回，避免无意义的 data access（数据访问）与 NPE（空指针异常）。
        if (dictCodes == null || dictCodes.length == 0) {
            return;
        }
        for (Long dictCode : dictCodes) {
            // 跳过 null 元素，保持“跳过并继续（skip and continue，跳过并继续）”语义。
            if (dictCode == null) {
                continue;
            }
            SysDictData data = selectDictDataById(dictCode);
            // missing record（缺失记录）显式跳过并继续（skip and continue，跳过并继续），避免 NPE（空指针异常）。
            if (data == null) {
                continue;
            }
            dictDataMapper.deleteDictDataById(dictCode);
            List<SysDictData> dictDatas = dictDataMapper.selectDictDataByType(data.getDictType());
            DictUtils.setDictCache(data.getDictType(), dictDatas);
        }
    }

    /**
     * 新增保存字典数据信息
     *
     * @param data 字典数据信息
     * @return 结果
     */
    @Override
    public int insertDictData(SysDictData data) {
        int row = dictDataMapper.insertDictData(data);
        if (row > 0) {
            List<SysDictData> dictDatas = dictDataMapper.selectDictDataByType(data.getDictType());
            DictUtils.setDictCache(data.getDictType(), dictDatas);
        }
        return row;
    }

    /**
     * 修改保存字典数据信息
     *
     * @param data 字典数据信息
     * @return 结果
     */
    @Override
    public int updateDictData(SysDictData data) {
        int row = dictDataMapper.updateDictData(data);
        if (row > 0) {
            List<SysDictData> dictDatas = dictDataMapper.selectDictDataByType(data.getDictType());
            DictUtils.setDictCache(data.getDictType(), dictDatas);
        }
        return row;
    }

    /**
     * 根据字典值查询字典数据。
     *
     * @param dictValue 字典值
     * @return 匹配的字典数据
     */
    @Override
    public SysDictData selectDictByValue(String dictValue) {
        return dictDataMapper.selectDictByValue(dictValue);
    }
}
