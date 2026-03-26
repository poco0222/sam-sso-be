/**
 * @file 验证 SysUserQueryService 的查询优化与行为稳定性
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysUser;
import com.yr.system.mapper.SysUserDeptMapper;
import com.yr.system.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysUserQueryService 查询行为测试。
 */
class SysUserQueryServiceTest {

    /**
     * 验证用户不存在时会直接返回 null，并避免额外加载一期已删除的扩展维度。
     */
    @Test
    void shouldReturnNullWhenUserMissing() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserDeptMapper userDeptMapper = mock(SysUserDeptMapper.class);
        SysUserQueryService queryService = new SysUserQueryService(
                userMapper,
                userDeptMapper
        );

        when(userMapper.selectUserByUserId(404L)).thenReturn(null);

        assertThat(queryService.getUserById(404L)).isNull();
        verify(userMapper).selectUserByUserId(404L);
    }

    /**
     * 验证按部门编码查用户时采用单次批量查询，而不是逐个 userId 触发 N+1。
     */
    @Test
    void shouldQueryUsersByDeptCodeWithSingleMapperCall() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserQueryService queryService = new SysUserQueryService(
                userMapper,
                mock(SysUserDeptMapper.class)
        );
        SysUser user = new SysUser();
        user.setUserName("phase2-batch");

        when(userMapper.selectSysUsersByDeptCode("RD")).thenReturn(List.of(user));

        List<SysUser> result = queryService.selectSysUserById("RD");

        assertThat(result).extracting(SysUser::getUserName).containsExactly("phase2-batch");
        verify(userMapper).selectSysUsersByDeptCode("RD");
        verify(userMapper, never()).selectSysUserByUserId(anyLong());
    }

    /**
     * 验证按用户 ID 批量查询时会去重、过滤空值，并按输入顺序返回结果。
     */
    @Test
    void shouldReturnUsersByIdsInInputOrderAfterDeduplication() {
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserQueryService queryService = new SysUserQueryService(
                userMapper,
                mock(SysUserDeptMapper.class)
        );
        SysUser userOne = new SysUser();
        userOne.setUserId(1L);
        userOne.setUserName("user-one");
        SysUser userTwo = new SysUser();
        userTwo.setUserId(2L);
        userTwo.setUserName("user-two");

        when(userMapper.selectSysUserByUserIds(argThat(userIds -> Arrays.equals(userIds, new Long[]{2L, 1L}))))
                .thenReturn(List.of(userOne, userTwo));

        List<SysUser> result = queryService.listUsersByIds(Arrays.asList(2L, null, 1L, 2L));

        assertThat(result).extracting(SysUser::getUserId).containsExactly(2L, 1L);
        verify(userMapper).selectSysUserByUserIds(argThat(userIds -> Arrays.equals(userIds, new Long[]{2L, 1L})));
    }
}
