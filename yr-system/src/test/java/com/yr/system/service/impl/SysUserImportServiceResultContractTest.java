/**
 * @file 验证 SysUserImportService 的导入结果契约
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.exception.CustomException;
import com.yr.system.mapper.SysUserMapper;
import com.yr.system.service.ISysConfigService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysUserImportService 结果契约测试。
 */
class SysUserImportServiceResultContractTest {

    /**
     * 验证全部成功时返回成功摘要。
     */
    @Test
    void shouldReturnSuccessSummaryWhenAllUsersImportSuccessfully() {
        ISysConfigService configService = mock(ISysConfigService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserWriteService writeService = mock(SysUserWriteService.class);
        SysUserImportService importService = new SysUserImportService(configService, userMapper, writeService);
        SysUser firstUser = buildUser("contract-success-1");
        SysUser secondUser = buildUser("contract-success-2");

        when(configService.selectConfigByKey("sys.user.initPassword")).thenReturn("Init@123");
        when(userMapper.selectUserByUserName(anyString())).thenReturn(null);

        String result = importService.importUser(List.of(firstUser, secondUser), false, "phase4");

        assertThat(result)
                .contains("数据已全部导入成功")
                .contains("账号 contract-success-1 导入成功")
                .contains("账号 contract-success-2 导入成功");
        verify(writeService).insertUser(firstUser);
        verify(writeService).insertUser(secondUser);
    }

    /**
     * 验证部分成功时返回混合结果摘要，而不是统一抛业务异常。
     */
    @Test
    void shouldReturnMixedSummaryWhenSomeUsersSucceedAndSomeFail() {
        ISysConfigService configService = mock(ISysConfigService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserWriteService writeService = mock(SysUserWriteService.class);
        SysUserImportService importService = new SysUserImportService(configService, userMapper, writeService);
        SysUser invalidUser = buildUser("contract-mixed-invalid");
        SysUser successUser = buildUser("contract-mixed-success");

        when(configService.selectConfigByKey("sys.user.initPassword")).thenReturn("Init@123");
        when(userMapper.selectUserByUserName(anyString())).thenReturn(null);
        doThrow(new CustomException("职级不能为空"))
                .when(writeService)
                .insertUser(argThat(user -> user != null && "contract-mixed-invalid".equals(user.getUserName())));

        String result = importService.importUser(List.of(invalidUser, successUser), false, "phase4");

        assertThat(result)
                .contains("导入完成！成功 1 条，失败 1 条")
                .contains("账号 contract-mixed-invalid 导入失败：职级不能为空")
                .contains("账号 contract-mixed-success 导入成功");
        verify(writeService).insertUser(invalidUser);
        verify(writeService).insertUser(successUser);
    }

    /**
     * 验证全部失败时仍保持业务异常语义。
     */
    @Test
    void shouldThrowBusinessExceptionWhenAllUsersFailValidation() {
        ISysConfigService configService = mock(ISysConfigService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserWriteService writeService = mock(SysUserWriteService.class);
        SysUserImportService importService = new SysUserImportService(configService, userMapper, writeService);
        SysUser firstUser = buildUser("contract-failure-1");
        SysUser secondUser = buildUser("contract-failure-2");

        when(configService.selectConfigByKey("sys.user.initPassword")).thenReturn("Init@123");
        when(userMapper.selectUserByUserName(anyString())).thenReturn(null);
        doThrow(new CustomException("职级不能为空")).when(writeService).insertUser(any(SysUser.class));

        assertThatThrownBy(() -> importService.importUser(List.of(firstUser, secondUser), false, "phase4"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("很抱歉，导入失败！共 2 条数据格式不正确")
                .hasMessageContaining("账号 contract-failure-1 导入失败：职级不能为空")
                .hasMessageContaining("账号 contract-failure-2 导入失败：职级不能为空");
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
        user.setRankId(1L);
        return user;
    }
}
