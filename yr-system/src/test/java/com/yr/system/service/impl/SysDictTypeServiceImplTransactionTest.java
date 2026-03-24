/**
 * @file 验证 SysDictTypeServiceImpl 批量删除字典类型时具备事务边界
 * @author Codex
 * @date 2026-03-17
 */
package com.yr.system.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 字典类型批量删除事务契约测试。
 */
class SysDictTypeServiceImplTransactionTest {

    /**
     * 验证批量删除方法显式声明事务，避免部分删除成功后才抛业务异常。
     *
     * @throws NoSuchMethodException 反射读取方法失败
     */
    @Test
    void shouldDeclareTransactionalBoundaryForBatchDelete() throws NoSuchMethodException {
        Method method = SysDictTypeServiceImpl.class.getMethod("deleteDictTypeByIds", Long[].class);

        assertThat(method.getAnnotation(Transactional.class))
                .as("deleteDictTypeByIds 应声明事务边界")
                .isNotNull();
    }
}
