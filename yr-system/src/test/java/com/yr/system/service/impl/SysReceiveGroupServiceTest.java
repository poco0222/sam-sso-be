/**
 * @file 验证接收组服务的核心行为契约
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.yr.common.exception.CustomException;
import com.yr.system.domain.entity.SysReceiveGroup;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * SysReceiveGroupService 行为测试。
 */
class SysReceiveGroupServiceTest {

    /**
     * 验证新增接收组时会在服务层拒绝重复编码。
     */
    @Test
    void shouldRejectDuplicatedReceiveGroupCodeOnCreate() {
        SysReceiveGroupService service = spy(new SysReceiveGroupService(mock(SysUserQueryService.class)));
        SysReceiveGroup existingGroup = new SysReceiveGroup();
        existingGroup.setId(9L);
        existingGroup.setReCode("GROUP_A");
        doReturn(existingGroup).when(service).get("GROUP_A");
        doReturn(true).when(service).saveOrUpdate(any(SysReceiveGroup.class));
        SysReceiveGroup command = buildCommand("GROUP_A", "USER_GROUP");

        assertThatThrownBy(() -> service.saveReceiveGroup(command))
                .isInstanceOf(CustomException.class)
                .hasMessage("接收编码已经存在，添加失败");
    }

    /**
     * 验证更新接收组时也会拒绝把编码改成其他分组已占用的值。
     */
    @Test
    void shouldRejectDuplicatedReceiveGroupCodeOnUpdateWhenOwnedByAnotherGroup() {
        SysReceiveGroupService service = spy(new SysReceiveGroupService(mock(SysUserQueryService.class)));
        SysReceiveGroup existingGroup = new SysReceiveGroup();
        existingGroup.setId(2L);
        existingGroup.setReCode("GROUP_A");
        doReturn(existingGroup).when(service).get("GROUP_A");
        doReturn(true).when(service).saveOrUpdate(any(SysReceiveGroup.class));
        SysReceiveGroup command = buildCommand("GROUP_A", "USER_GROUP");
        command.setId(1L);

        assertThatThrownBy(() -> service.saveReceiveGroup(command))
                .isInstanceOf(CustomException.class)
                .hasMessage("接收编码已经存在，添加失败");
    }

    /**
     * 验证删除不存在的接收组时会返回准确的业务异常文案。
     */
    @Test
    void shouldReportMissingReceiveGroupWhenDeletingAbsentGroup() {
        SysReceiveGroupService service = spy(new SysReceiveGroupService(mock(SysUserQueryService.class)));
        doReturn(null).when(service).getById(88L);

        assertThatThrownBy(() -> service.del(88L))
                .isInstanceOf(CustomException.class)
                .hasMessage("接收分组不存在，删除失败");
    }

    /**
     * 构造最小可用的接收组命令对象。
     *
     * @param reCode 接收编码
     * @param reMode 接收模式
     * @return 接收组命令
     */
    private SysReceiveGroup buildCommand(String reCode, String reMode) {
        SysReceiveGroup sysReceiveGroup = new SysReceiveGroup();
        sysReceiveGroup.setReCode(reCode);
        sysReceiveGroup.setReMode(reMode);
        return sysReceiveGroup;
    }
}
