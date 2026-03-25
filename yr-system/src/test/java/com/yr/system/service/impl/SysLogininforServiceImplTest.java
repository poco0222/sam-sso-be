/**
 * @file 验证登录日志服务在底层表缺失时不会中断主流程
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service.impl;

import com.yr.system.domain.SysLogininfor;
import com.yr.system.mapper.SysLogininforMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * SysLogininforServiceImpl 容错行为测试。
 */
class SysLogininforServiceImplTest {

    /**
     * 验证底层日志表缺失时，服务层会记录并吞掉异常，避免异步登录日志影响登录主流程。
     */
    @Test
    void shouldFailOpenWhenLoginAuditTableIsMissing() {
        SysLogininforMapper logininforMapper = mock(SysLogininforMapper.class);
        SysLogininforServiceImpl service = new SysLogininforServiceImpl(logininforMapper);
        SysLogininfor logininfor = new SysLogininfor();
        doThrow(new BadSqlGrammarException(
                "insertLogininfor",
                "insert into sys_logininfor (...) values (...)",
                new SQLException("Table 'local_sam_empty.sys_logininfor' doesn't exist")
        )).when(logininforMapper).insertLogininfor(logininfor);

        assertThatCode(() -> service.insertLogininfor(logininfor)).doesNotThrowAnyException();
        verify(logininforMapper).insertLogininfor(logininfor);
    }
}
