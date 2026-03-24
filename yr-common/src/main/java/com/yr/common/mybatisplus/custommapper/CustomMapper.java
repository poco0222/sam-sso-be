package com.yr.common.mybatisplus.custommapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;

/**
 * @author 1050696985@qq.com
 * @version V1.0
 * @Date 2021-8-9 10:32
 * @description 基类mapper，自定义的通用方法可写在这里
 */
public interface CustomMapper<T> extends BaseMapper<T> {

    /**
     * 根据 ID 修改所有字段，包括为null的字段
     *
     * @param entity 实体对象
     * @return 返回影响的行数
     */
    int updateAllColumnById(@Param(Constants.ENTITY) T entity);

}
