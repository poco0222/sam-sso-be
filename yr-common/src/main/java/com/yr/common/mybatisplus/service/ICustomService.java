package com.yr.common.mybatisplus.service;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author 1050696985@qq.com
 * @version V1.0
 * @Date 2021-8-9 10:48
 * @description service接口基类
 */
public interface ICustomService<T> extends IService<T> {

    /**
     * 根据 ID 修改所有字段，包括为null的字段
     *
     * @param entity 实体对象
     * @return 是否成功
     */
    boolean updateAllColumnById(T entity);

    /**
     * 根据 ID 修改所有字段，包括为null的字段，如果修改失败会抛出异常
     *
     * @param entity 实体对象
     */
    void updateAllColumnByIdThrow(T entity);

}
