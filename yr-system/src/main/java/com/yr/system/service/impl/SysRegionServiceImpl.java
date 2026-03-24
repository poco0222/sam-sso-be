/**
 * @file 区域初始化服务，负责构建中国省市区层级并校验重复初始化
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.system.domain.dto.SysRegionDTO;
import com.yr.system.domain.entity.SysRegion;
import com.yr.system.mapper.SysRegionMapper;
import com.yr.system.service.ISysRegionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统区域表(SysRegion)表服务实现类
 *
 * @author Youngron
 * @since 2021-10-20 18:48:34
 */
@Service
public class SysRegionServiceImpl extends CustomServiceImpl<SysRegionMapper, SysRegion> implements ISysRegionService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initData(List<SysRegionDTO> list) {
        assertInitTargetNotExists(list);

        SysRegion china = new SysRegion();
        china.setParentId(0L);
        china.setAncestors("0");
        china.setRegionCode("CHINA");
        china.setRegionName("中国");
        china.setRegionLevel(1);
        china.setHasChildren(Boolean.TRUE);
        this.save(china);
        Long chinaId = china.getId();
        String chinaAncestors = "0," + chinaId;

        Set<String> provinceSet = new HashSet<>(31);
        List<SysRegion> insertProvinceList = new ArrayList<>();
        for (SysRegionDTO sysRegionDTO : list) {
            if (!provinceSet.contains(sysRegionDTO.getProvinceCode())) {
                provinceSet.add(sysRegionDTO.getProvinceCode());
                SysRegion province = new SysRegion();
                province.setParentId(chinaId);
                province.setAncestors(chinaAncestors);
                province.setRegionCode(sysRegionDTO.getProvinceCode());
                province.setRegionName(sysRegionDTO.getProvinceName());
                province.setRegionLevel(2);
                province.setHasChildren(Boolean.TRUE);
                insertProvinceList.add(province);
            }
        }
        this.saveBatch(insertProvinceList);
        Map<String, Long> provinceIdMap = insertProvinceList.stream().collect(Collectors.toMap(SysRegion::getRegionCode, SysRegion::getId, (k1, k2) -> k2));

        Set<String> citySet = new HashSet<>();
        List<SysRegion> insertCityList = new ArrayList<>();
        list.forEach(sysRegionDTO -> {
            if (!citySet.contains(sysRegionDTO.getCityCode())) {
                citySet.add(sysRegionDTO.getCityCode());
                SysRegion city = new SysRegion();
                city.setParentId(provinceIdMap.get(sysRegionDTO.getProvinceCode()));
                city.setAncestors(buildAncestors(provinceIdMap.get(sysRegionDTO.getProvinceCode()), chinaAncestors));
                city.setRegionCode(sysRegionDTO.getCityCode());
                city.setRegionName(sysRegionDTO.getCityName());
                city.setRegionLevel(3);
                city.setHasChildren(Boolean.TRUE);
                insertCityList.add(city);
            }
        });
        this.saveBatch(insertCityList);
        Map<String, Long> cityMap = insertCityList.stream().collect(Collectors.toMap(SysRegion::getRegionCode, SysRegion::getId, (k1, k2) -> k2));
        Map<String, String> cityAncestorsMap = insertCityList.stream().collect(Collectors.toMap(SysRegion::getRegionCode, SysRegion::getAncestors, (k1, k2) -> k2));

        List<SysRegion> insertCountyList = new ArrayList<>();
        list.forEach(sysRegionDTO -> {
            SysRegion county = new SysRegion();
            county.setParentId(cityMap.get(sysRegionDTO.getCityCode()));
            county.setAncestors(buildAncestors(county.getParentId(), cityAncestorsMap.get(sysRegionDTO.getCityCode())));
            county.setRegionCode(sysRegionDTO.getCountyCode());
            county.setRegionName(sysRegionDTO.getCountyName());
            county.setRegionLevel(4);
            insertCountyList.add(county);
        });
        this.saveBatch(insertCountyList);
    }

    /**
     * 在初始化前校验目标区域编码是否已存在，避免重复导入污染树结构。
     *
     * @param list 初始化载荷
     */
    private void assertInitTargetNotExists(List<SysRegionDTO> list) {
        assertRegionCodeNotExists("CHINA", "区域数据已初始化，请勿重复导入");
        buildTargetRegionCodeSet(list).forEach(regionCode ->
                assertRegionCodeNotExists(regionCode, "区域编码 " + regionCode + " 已存在，不能重复初始化"));
    }

    /**
     * 聚合本次初始化中会落库的所有行政区划编码，便于统一校验重复数据。
     *
     * @param list 初始化载荷
     * @return 去重后的区域编码集合
     */
    private Set<String> buildTargetRegionCodeSet(List<SysRegionDTO> list) {
        Set<String> regionCodes = new HashSet<>();
        for (SysRegionDTO sysRegionDTO : list) {
            regionCodes.add(sysRegionDTO.getProvinceCode());
            regionCodes.add(sysRegionDTO.getCityCode());
            regionCodes.add(sysRegionDTO.getCountyCode());
        }
        return regionCodes;
    }

    /**
     * 检查指定区域编码是否已经存在。
     *
     * @param regionCode 区域编码
     * @param message 已存在时抛出的异常消息
     */
    private void assertRegionCodeNotExists(String regionCode, String message) {
        LambdaQueryWrapper<SysRegion> queryWrapper = new LambdaQueryWrapper<SysRegion>()
                .eq(SysRegion::getRegionCode, regionCode);
        if (this.getOne(queryWrapper) != null) {
            throw new CustomException(message);
        }
    }

    /**
     * 基于父节点祖级链路与父节点 ID 生成当前节点的 ancestors。
     *
     * @param parentId 父节点 ID
     * @param parentAncestors 父节点 ancestors
     * @return 当前节点 ancestors
     */
    private String buildAncestors(Long parentId, String parentAncestors) {
        return parentAncestors + "," + parentId;
    }

    @Override
    public boolean checkCodeUnique(SysRegion sysRegion) {
        LambdaQueryWrapper<SysRegion> queryWrapper = new LambdaQueryWrapper<SysRegion>()
                .eq(SysRegion::getRegionCode, sysRegion.getRegionCode())
                .ne(sysRegion.getId() != null, SysRegion::getId, sysRegion.getId());
        SysRegion checkRegion = this.getOne(queryWrapper);
        return checkRegion != null;
    }
}
