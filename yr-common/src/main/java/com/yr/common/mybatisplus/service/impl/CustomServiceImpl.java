package com.yr.common.mybatisplus.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.custommapper.CustomMapper;
import com.yr.common.mybatisplus.service.ICustomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author 1050696985@qq.com
 * @version V1.0
 * @Date 2021-8-9 10:51
 * @description service实现类基类
 */
public class CustomServiceImpl<M extends CustomMapper<T>, T> extends ServiceImpl<M, T> implements ICustomService<T> {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private M baseMapper;

    @Override
    public M getBaseMapper() {
        return this.baseMapper;
    }

    @Override
    public boolean updateAllColumnById(T entity) {
        return SqlHelper.retBool(this.baseMapper.updateAllColumnById(entity));
    }

    @Override
    public void updateAllColumnByIdThrow(T entity) {
        if (!SqlHelper.retBool(this.baseMapper.updateAllColumnById(entity))) {
            throw new CustomException("更新失败，数据可能已经被修改");
        }
    }
}
