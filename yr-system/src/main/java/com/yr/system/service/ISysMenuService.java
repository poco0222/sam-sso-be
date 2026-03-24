package com.yr.system.service;

import com.yr.common.core.domain.TreeSelect;
import com.yr.common.core.domain.entity.SysMenu;
import com.yr.system.domain.vo.RouterVo;

import java.util.List;
import java.util.Set;

/**
 * 菜单 业务层
 *
 * @author Youngron
 */
public interface ISysMenuService {
    /**
     * 根据用户查询系统菜单列表
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    public List<SysMenu> selectMenuList(Long userId);

    /**
     * 根据用户查询系统菜单列表
     *
     * @param menu   菜单信息
     * @param userId 用户ID
     * @return 菜单列表
     */
    public List<SysMenu> selectMenuList(SysMenu menu, Long userId);

    /**
     * 根据用户ID查询权限
     *
     * @param userId 用户ID
     * @param orgId  指定组织的权限
     * @return 权限列表
     */
    public Set<String> selectMenuPermsByUserId(Long userId, Long orgId);

    /**
     * 根据用户ID查询菜单树信息
     *
     * @param userId 用户ID
     * @param orgId  指定组织的权限
     * @return 菜单列表
     */
    public List<SysMenu> selectMenuTreeByUserId(Long userId, Long orgId, String platform);

    /**
     * 根据角色ID查询菜单树信息
     *
     * @param roleId 角色ID
     * @return 选中菜单列表
     */
    public List<Integer> selectMenuListByRoleId(Long roleId);

    /**
     * 构建前端路由所需要的菜单(WEB端)
     *
     * @param menus 菜单列表
     * @return 路由列表
     */
    public List<RouterVo> buildMenus(List<SysMenu> menus);

    /**
     * 返回桌面端菜单和按钮
     * @param menus 菜单列表
     * @return 路由列表
     */
    List<RouterVo> buildMenusForDesktop(List<SysMenu> menus);

    /**
     * 构建前端所需要树结构
     *
     * @param menus 菜单列表
     * @return 树结构列表
     */
    public List<SysMenu> buildMenuTree(List<SysMenu> menus);

    /**
     * 构建前端所需要下拉树结构
     *
     * @param menus 菜单列表
     * @return 下拉树结构列表
     */
    public List<TreeSelect> buildMenuTreeSelect(List<SysMenu> menus);

    /**
     * 根据菜单ID查询信息
     *
     * @param menuId 菜单ID
     * @return 菜单信息
     */
    public SysMenu selectMenuById(Long menuId);

    /**
     * 是否存在菜单子节点
     *
     * @param menuId 菜单ID
     * @return 结果 true 存在 false 不存在
     */
    public boolean hasChildByMenuId(Long menuId);

    /**
     * 查询菜单是否存在角色
     *
     * @param menuId 菜单ID
     * @return 结果 true 存在 false 不存在
     */
    public boolean checkMenuExistRole(Long menuId);

    /**
     * 新增保存菜单信息
     *
     * @param menu 菜单信息
     * @return 结果
     */
    public int insertMenu(SysMenu menu);

    /**
     * 修改保存菜单信息
     *
     * @param menu 菜单信息
     * @return 结果
     */
    public int updateMenu(SysMenu menu);

    /**
     * 删除菜单管理信息
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    public int deleteMenuById(Long menuId);

    /**
     * 校验菜单名称是否唯一
     *
     * @param menu 菜单信息
     * @return 结果
     */
    public String checkMenuNameUnique(SysMenu menu);
}
