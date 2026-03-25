/**
 * @file 验证 SysMenuServiceImpl 的安全性契约（safety contract，安全契约）
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysMenu;
import com.yr.common.exception.CustomException;
import com.yr.system.mapper.SysMenuMapper;
import com.yr.system.mapper.SysRoleMapper;
import com.yr.system.mapper.SysRoleMenuMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 菜单服务安全性测试。
 *
 * <p>关注点是避免因为缺失依赖数据导致的 NPE（NullPointerException，空指针异常），并确保服务层
 * 统一抛出 CustomException（business exception，业务异常），避免异常泄漏到 controller。</p>
 */
class SysMenuServiceImplSafetyTest {

    /**
     * 验证当 roleId 对应角色缺失（role == null）时，服务层 fail-fast（快速失败）抛出 CustomException，
     * 并且不会继续调用下游 menuMapper.selectMenuListByRoleId(...)。
     */
    @Test
    void shouldFailFastWhenRoleIsMissingForMenuSelection() {
        Long roleId = 1001L;

        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysRoleMenuMapper roleMenuMapper = mock(SysRoleMenuMapper.class);

        SysMenuServiceImpl service = new SysMenuServiceImpl(menuMapper, roleMapper, roleMenuMapper);

        // 关键依赖缺失：roleMapper 返回 null，当前实现会触发 NPE；期望修复后抛 CustomException。
        when(roleMapper.selectRoleByIdV2(roleId)).thenReturn(null);

        assertThatThrownBy(() -> service.selectMenuListByRoleId(roleId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("roleId=" + roleId)
                // 额外防御：确保不是 NPE（NullPointerException，空指针异常）被包裹或泄漏。
                .satisfies(ex -> assertThat(ex.getCause()).isNull());

        // 缺失角色时应当短路，不再触发下游查询。
        verify(roleMapper).selectRoleByIdV2(roleId);
        verify(menuMapper, never()).selectMenuListByRoleId(eq(roleId), anyBoolean());
    }

    /**
     * 验证当 menuId 对应菜单缺失（sysMenu == null）时，服务层 fail-fast（快速失败）抛出 CustomException，
     * 并且只会进行一次 selectMenuById 查询，不会继续对 mapper 发起其他调用。
     */
    @Test
    void shouldFailFastWhenMenuIsMissingById() {
        Long menuId = 2002L;

        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysRoleMenuMapper roleMenuMapper = mock(SysRoleMenuMapper.class);

        SysMenuServiceImpl service = new SysMenuServiceImpl(menuMapper, roleMapper, roleMenuMapper);

        when(menuMapper.selectMenuById(menuId)).thenReturn(null);

        assertThatThrownBy(() -> service.selectMenuById(menuId))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("menuId=" + menuId)
                // 额外防御：确保不是 NPE（NullPointerException，空指针异常）被包裹或泄漏。
                .satisfies(ex -> assertThat(ex.getCause()).isNull());

        verify(menuMapper).selectMenuById(menuId);
    }

    /**
     * 验证身份中心管理台菜单树会过滤掉 monitor 等超出一期边界的 legacy module（旧模块），
     * 避免前端在登录后初始化动态路由时被缺失页面击穿。
     */
    @Test
    void shouldFilterLegacyMgmtModulesOutOfIdentityConsoleTree() {
        SysMenuMapper menuMapper = mock(SysMenuMapper.class);
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysRoleMenuMapper roleMenuMapper = mock(SysRoleMenuMapper.class);
        SysMenuServiceImpl service = new SysMenuServiceImpl(menuMapper, roleMapper, roleMenuMapper);

        when(menuMapper.selectMenuTreeAll("mgmt")).thenReturn(List.of(
                buildMenu(9000L, 0L, "identity"),
                buildMenu(9001L, 9000L, "client"),
                buildMenu(9002L, 9000L, "sync-task"),
                buildMenu(100L, 0L, "system"),
                buildMenu(101L, 100L, "user"),
                buildMenu(200L, 0L, "monitor"),
                buildMenu(201L, 200L, "operlog")
        ));

        List<SysMenu> menuTree = service.selectMenuTreeByUserId(1L, 1L, "mgmt");

        assertThat(menuTree)
                .extracting(SysMenu::getPath)
                .containsExactly("identity", "system");
        assertThat(menuTree)
                .flatExtracting(SysMenu::getChildren)
                .extracting(SysMenu::getPath)
                .contains("client", "sync-task", "user")
                .doesNotContain("operlog");
    }

    /**
     * 构造最小菜单对象，供菜单边界测试使用。
     *
     * @param menuId 菜单 ID
     * @param parentId 父菜单 ID
     * @param path 路由路径
     * @return 菜单对象
     */
    private SysMenu buildMenu(Long menuId, Long parentId, String path) {
        SysMenu sysMenu = new SysMenu();
        sysMenu.setMenuId(menuId);
        sysMenu.setParentId(parentId);
        sysMenu.setPath(path);
        return sysMenu;
    }
}
