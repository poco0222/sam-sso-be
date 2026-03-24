/**
 * @file 验证接收组对象树接口的返回契约
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.ObjectTree;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.exception.CustomException;
import com.yr.system.domain.entity.SysReceiveGroup;
import com.yr.system.domain.entity.SysReceiveGroupObject;
import com.yr.system.domain.vo.SysObjectTreeVo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SysReceiveGroupService 对象树测试。
 */
class SysReceiveGroupServiceObjectTreeTest {

    /**
     * 验证用户组模式会返回非空对象树和选中节点列表。
     */
    @Test
    void shouldBuildNonNullUserGroupObjectTree() {
        SysUserQueryService userQueryService = mock(SysUserQueryService.class);
        SysReceiveGroupService service = spy(new SysReceiveGroupService(userQueryService));
        SysReceiveGroup summaryGroup = buildGroup(1L, "GROUP_A", "USER_GROUP", List.of());
        SysReceiveGroup detailGroup = buildGroup(1L, "GROUP_A", "USER_GROUP",
                List.of(buildGroupObject(100L), buildGroupObject(200L)));
        doReturn(summaryGroup).when(service).getById(1L);
        doReturn(detailGroup).when(service).getReceiveGroupList("GROUP_A");
        when(userQueryService.listUsersByIds(List.of(100L, 200L))).thenReturn(List.of(
                buildUser(100L, "张三", "zhangsan"),
                buildUser(200L, null, "lisi")
        ));

        SysObjectTreeVo result = service.getSpecificObjects(1L);

        assertThat(result).isNotNull();
        assertThat(result.getCheckIds()).containsExactly(100L, 200L);
        assertThat(result.getTreeList())
                .isNotNull()
                .extracting(ObjectTree::getId, ObjectTree::getLabel)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(100L, "张三"),
                        org.assertj.core.groups.Tuple.tuple(200L, "lisi")
                );
        verify(userQueryService).listUsersByIds(List.of(100L, 200L));
    }

    /**
     * 验证对象树只按选中的用户 ID 定向查询，并对空值与重复值做去重过滤。
     */
    @Test
    void shouldQueryOnlyCheckedUsersWhenBuildingUserGroupObjectTree() {
        SysUserQueryService userQueryService = mock(SysUserQueryService.class);
        SysReceiveGroupService service = spy(new SysReceiveGroupService(userQueryService));
        SysReceiveGroup summaryGroup = buildGroup(3L, "GROUP_C", "USER_GROUP", List.of());
        SysReceiveGroup detailGroup = buildGroup(3L, "GROUP_C", "USER_GROUP",
                List.of(buildGroupObject(200L), buildGroupObject(null), buildGroupObject(100L), buildGroupObject(200L)));
        doReturn(summaryGroup).when(service).getById(3L);
        doReturn(detailGroup).when(service).getReceiveGroupList("GROUP_C");
        when(userQueryService.listUsersByIds(List.of(200L, 100L))).thenReturn(List.of(
                buildUser(200L, "李四", "lisi"),
                buildUser(100L, "张三", "zhangsan")
        ));

        SysObjectTreeVo result = service.getSpecificObjects(3L);

        assertThat(result.getCheckIds()).containsExactly(200L, 100L);
        assertThat(result.getTreeList())
                .extracting(ObjectTree::getId, ObjectTree::getLabel)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(200L, "李四"),
                        org.assertj.core.groups.Tuple.tuple(100L, "张三")
                );
        verify(userQueryService).listUsersByIds(List.of(200L, 100L));
    }

    /**
     * 验证没有选中用户时直接返回空树，避免触发无意义的用户查询。
     */
    @Test
    void shouldReturnEmptyTreeListWhenCheckedIdsMissing() {
        SysUserQueryService userQueryService = mock(SysUserQueryService.class);
        SysReceiveGroupService service = spy(new SysReceiveGroupService(userQueryService));
        SysReceiveGroup summaryGroup = buildGroup(4L, "GROUP_D", "USER_GROUP", List.of());
        SysReceiveGroup detailGroup = buildGroup(4L, "GROUP_D", "USER_GROUP", List.of(buildGroupObject(null)));
        doReturn(summaryGroup).when(service).getById(4L);
        doReturn(detailGroup).when(service).getReceiveGroupList("GROUP_D");

        SysObjectTreeVo result = service.getSpecificObjects(4L);

        assertThat(result.getCheckIds()).isEmpty();
        assertThat(result.getTreeList()).isEmpty();
        verifyNoInteractions(userQueryService);
    }

    /**
     * 验证不支持的接收模式会抛出明确的业务异常。
     */
    @Test
    void shouldRejectUnsupportedReceiveModeWhenBuildingObjectTree() {
        SysReceiveGroupService service = spy(new SysReceiveGroupService(mock(SysUserQueryService.class)));
        SysReceiveGroup summaryGroup = buildGroup(2L, "GROUP_B", "ORG_GROUP", List.of());
        SysReceiveGroup detailGroup = buildGroup(2L, "GROUP_B", "ORG_GROUP", List.of());
        doReturn(summaryGroup).when(service).getById(2L);
        doReturn(detailGroup).when(service).getReceiveGroupList("GROUP_B");

        assertThatThrownBy(() -> service.getSpecificObjects(2L))
                .isInstanceOf(CustomException.class)
                .hasMessage("暂不支持接收模式: ORG_GROUP");
    }

    /**
     * 构造最小接收组对象。
     *
     * @param id 分组 ID
     * @param reCode 接收编码
     * @param reMode 接收模式
     * @param groupObjects 关联对象列表
     * @return 接收组对象
     */
    private SysReceiveGroup buildGroup(Long id, String reCode, String reMode, List<SysReceiveGroupObject> groupObjects) {
        SysReceiveGroup sysReceiveGroup = new SysReceiveGroup();
        sysReceiveGroup.setId(id);
        sysReceiveGroup.setReCode(reCode);
        sysReceiveGroup.setReMode(reMode);
        sysReceiveGroup.setGroupObjectList(groupObjects);
        return sysReceiveGroup;
    }

    /**
     * 构造接收组对象关联。
     *
     * @param objectId 关联对象 ID
     * @return 接收组对象关联
     */
    private SysReceiveGroupObject buildGroupObject(Long objectId) {
        SysReceiveGroupObject sysReceiveGroupObject = new SysReceiveGroupObject();
        sysReceiveGroupObject.setReObjectId(objectId);
        return sysReceiveGroupObject;
    }

    /**
     * 构造最小用户对象。
     *
     * @param userId 用户 ID
     * @param nickName 用户昵称
     * @param userName 用户账号
     * @return 用户对象
     */
    private SysUser buildUser(Long userId, String nickName, String userName) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setNickName(nickName);
        sysUser.setUserName(userName);
        return sysUser;
    }
}
