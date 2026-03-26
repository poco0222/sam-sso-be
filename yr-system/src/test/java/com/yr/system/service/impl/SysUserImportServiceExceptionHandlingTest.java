/**
 * @file 验证 SysUserImportService 的异常分层行为
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.exception.CustomException;
import com.yr.system.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysUserImportService 异常分层测试。
 */
class SysUserImportServiceExceptionHandlingTest {

    /**
     * 验证业务异常会被汇总，同时后续用户仍会继续处理。
     */
    @Test
    void shouldCollectBusinessFailureAndContinueProcessingLaterUsers() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserWriteService writeService = mock(SysUserWriteService.class);
        SysUserImportService importService = new SysUserImportService("Init@123", userMapper, writeService);
        SysUser invalidUser = buildUser("phase4-invalid");
        SysUser trailingUser = buildUser("phase4-trailing");

        when(userMapper.selectUserByUserName(anyString())).thenReturn(null);
        doThrow(new CustomException("主数据校验失败"))
                .when(writeService)
                .insertUser(argThat(user -> user != null && "phase4-invalid".equals(user.getUserName())));

        String result = importService.importUser(List.of(invalidUser, trailingUser), false, "phase4");

        assertThat(result)
                .contains("导入完成！成功 1 条，失败 1 条")
                .contains("账号 phase4-invalid 导入失败：主数据校验失败")
                .contains("账号 phase4-trailing 导入成功");
        verify(writeService).insertUser(invalidUser);
        verify(writeService).insertUser(trailingUser);
    }

    /**
     * 验证系统异常会立即中止导入，并保留原始异常语义。
     */
    @Test
    void shouldFailFastWhenUnexpectedRuntimeExceptionOccurs() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserWriteService writeService = mock(SysUserWriteService.class);
        SysUserImportService importService = new SysUserImportService("Init@123", userMapper, writeService);
        SysUser brokenUser = buildUser("phase4-broken");
        SysUser untouchedUser = buildUser("phase4-untouched");

        when(userMapper.selectUserByUserName(anyString())).thenReturn(null);
        doThrow(new IllegalStateException("db boom")).when(writeService).insertUser(brokenUser);

        assertThatThrownBy(() -> importService.importUser(List.of(brokenUser, untouchedUser), false, "phase4"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db boom");

        verify(writeService).insertUser(brokenUser);
        verify(writeService, never()).insertUser(untouchedUser);
    }

    /**
     * 构造最小导入测试用户。
     *
     * @param userName 用户名
     * @return 用户对象
     */
    private SysUser buildUser(String userName) {
        SysUser user = new SysUser();
        user.setUserName(userName);
        return user;
    }
}
