/**
 * @file SysRegionDTO 省市区初始化传输对象
 * @author Codex
 * @date 2026-03-11
 */
package com.yr.system.domain.dto;

/**
 * 省市区初始化载荷。
 *
 * @param provinceName 省名称
 * @param provinceCode 省编码
 * @param cityName 市名称
 * @param cityCode 市编码
 * @param countyName 区县名称
 * @param countyCode 区县编码
 */
public record SysRegionDTO(
        String provinceName,
        String provinceCode,
        String cityName,
        String cityCode,
        String countyName,
        String countyCode) {

    /**
     * 兼容既有 Java Bean 读取方式。
     *
     * @return 省名称
     */
    public String getProvinceName() {
        return provinceName;
    }

    /**
     * 兼容既有 Java Bean 读取方式。
     *
     * @return 省编码
     */
    public String getProvinceCode() {
        return provinceCode;
    }

    /**
     * 兼容既有 Java Bean 读取方式。
     *
     * @return 市名称
     */
    public String getCityName() {
        return cityName;
    }

    /**
     * 兼容既有 Java Bean 读取方式。
     *
     * @return 市编码
     */
    public String getCityCode() {
        return cityCode;
    }

    /**
     * 兼容既有 Java Bean 读取方式。
     *
     * @return 区县名称
     */
    public String getCountyName() {
        return countyName;
    }

    /**
     * 兼容既有 Java Bean 读取方式。
     *
     * @return 区县编码
     */
    public String getCountyCode() {
        return countyCode;
    }
}
