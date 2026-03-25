package com.yr.system.service.impl;

import com.yr.common.constant.Constants;
import com.yr.common.constant.UserConstants;
import com.yr.common.core.domain.TreeSelect;
import com.yr.common.core.domain.entity.SysMenu;
import com.yr.common.core.domain.entity.SysRole;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.exception.CustomException;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.domain.vo.MetaVo;
import com.yr.system.domain.vo.RouterVo;
import com.yr.system.mapper.SysMenuMapper;
import com.yr.system.mapper.SysRoleMapper;
import com.yr.system.mapper.SysRoleMenuMapper;
import com.yr.system.service.ISysMenuService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜单 业务层处理
 *
 * @author Youngron
 */
@Service
public class SysMenuServiceImpl implements ISysMenuService {
    public static final String PREMISSION_STRING = "perms[\"{0}\"]";
    /** PopoY: 身份中心一期管理台只保留 system/client/sync-task 与首页相关模块。 */
    private static final Set<String> IDENTITY_CONSOLE_MODULE_WHITELIST =
            Set.of("system", "client", "sync-task", "dashboard", "index");
    /** PopoY: dashboard/index 只允许作为顶级模块出现，避免子级同名路径误命中。 */
    private static final Set<String> TOP_LEVEL_ONLY_MODULE_KEYS = Set.of("dashboard", "index");

    private final SysMenuMapper menuMapper;

    private final SysRoleMapper roleMapper;

    private final SysRoleMenuMapper roleMenuMapper;

    public SysMenuServiceImpl(SysMenuMapper menuMapper, SysRoleMapper roleMapper, SysRoleMenuMapper roleMenuMapper) {
        this.menuMapper = menuMapper;
        this.roleMapper = roleMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    /**
     * 根据用户查询系统菜单列表
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    @Override
    public List<SysMenu> selectMenuList(Long userId) {
        return selectMenuList(new SysMenu(), userId);
    }

    /**
     * 查询系统菜单列表
     *
     * @param menu 菜单信息
     * @return 菜单列表
     */
    @Override
    public List<SysMenu> selectMenuList(SysMenu menu, Long userId) {
        List<SysMenu> menuList = null;
        // 管理员显示所有菜单信息
        if (SysUser.isAdmin(userId)) {
            menuList = menuMapper.selectMenuList(menu);
        } else {
            menu.getParams().put("userId", userId);
            menu.getParams().put("orgId", SecurityUtils.getOrgId());
            menuList = menuMapper.selectMenuListByUserId(menu);
        }
        return menuList;
    }

    /**
     * 根据用户ID查询权限
     *
     * @param userId 用户ID
     * @param orgId  指定组织的权限
     * @return 权限列表
     */
    @Override
    public Set<String> selectMenuPermsByUserId(Long userId, Long orgId) {
        List<String> perms = menuMapper.selectMenuPermsByUserId(userId, orgId);
        Set<String> permsSet = new HashSet<>();
        for (String perm : perms) {
            if (StringUtils.isNotEmpty(perm)) {
                permsSet.addAll(Arrays.asList(perm.trim().split(",")));
            }
        }
        return permsSet;
    }

    /**
     * 根据用户ID查询菜单
     *
     * @param userId 用户ID
     * @param orgId  指定组织的权限
     * @return 菜单列表
     */
    @Override
    public List<SysMenu> selectMenuTreeByUserId(Long userId, Long orgId, String platform) {
        List<SysMenu> menus = null;
        if (SecurityUtils.isAdmin(userId)) {
            menus = menuMapper.selectMenuTreeAll(platform);
        } else {
            menus = menuMapper.selectMenuTreeByUserId(userId, orgId, platform);
        }
        List<SysMenu> menuTree = getChildPerms(menus, 0);
        if ("mgmt".equals(platform)) {
            return filterIdentityConsoleMgmtMenus(menuTree);
        }
        return menuTree;
    }

    /**
     * 根据角色ID查询菜单树信息
     *
     * @param roleId 角色ID
     * @return 选中菜单列表
     */
    @Override
    public List<Integer> selectMenuListByRoleId(Long roleId) {
        SysRole role = roleMapper.selectRoleByIdV2(roleId);
        // fail-fast（快速失败）：角色缺失直接抛 CustomException，避免 NPE（NullPointerException，空指针异常）泄漏到 controller。
        if (role == null) {
            throw new CustomException("角色不存在或已删除: roleId=" + roleId);
        }
        return menuMapper.selectMenuListByRoleId(roleId, role.isMenuCheckStrictly());
    }

    /**
     * 构建前端路由所需要的菜单
     *
     * @param menus 菜单列表
     * @return 路由列表
     */
    @Override
    public List<RouterVo> buildMenus(List<SysMenu> menus) {
        List<RouterVo> routers = new LinkedList<RouterVo>();
        for (SysMenu menu : menus) {
            RouterVo router = new RouterVo();
            router.setHidden("1".equals(menu.getVisible()));
            router.setName(getRouteName(menu));
            router.setPath(getRouterPath(menu));
            router.setComponent(getComponent(menu));
            router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtils.equals("1", menu.getIsCache()), menu.getPath(), menu.getMenuId()));
            List<SysMenu> cMenus = menu.getChildren();
            if (!cMenus.isEmpty() && cMenus.size() > 0 && UserConstants.TYPE_DIR.equals(menu.getMenuType())) {
                router.setAlwaysShow(true);
                router.setRedirect("noRedirect");
                router.setChildren(buildMenus(cMenus));
            } else if (isMenuFrame(menu)) {
                router.setMeta(null);
                List<RouterVo> childrenList = new ArrayList<RouterVo>();
                RouterVo children = new RouterVo();
                children.setPath(menu.getPath());
                children.setComponent(menu.getComponent());
                children.setName(StringUtils.capitalize(menu.getPath()));
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtils.equals("1", menu.getIsCache()), menu.getPath(), menu.getMenuId()));
                childrenList.add(children);
                router.setChildren(childrenList);
            } else if (menu.getParentId().intValue() == 0 && isInnerLink(menu)) {
                router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), menu.getMenuId()));
                router.setPath("/inner");
                List<RouterVo> childrenList = new ArrayList<RouterVo>();
                RouterVo children = new RouterVo();
                String routerPath = StringUtils.replaceEach(menu.getPath(), new String[]{Constants.HTTP, Constants.HTTPS}, new String[]{"", ""});
                children.setPath(routerPath);
                children.setComponent(UserConstants.INNER_LINK);
                children.setName(StringUtils.capitalize(routerPath));
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), menu.getPath(), menu.getMenuId()));
                childrenList.add(children);
                router.setChildren(childrenList);
            }
            routers.add(router);
        }
        return routers;
    }


    /**
     * 返回桌面端菜单。
     *
     * @param menus 菜单列表
     * @return
     */
    @Override
    public List<RouterVo> buildMenusForDesktop(List<SysMenu> menus) {
        List<RouterVo> routers = new LinkedList<RouterVo>();
        for (SysMenu menu : menus) {
            RouterVo router = new RouterVo();
            router.setHidden("1".equals(menu.getVisible()));
            router.setName(getRouteName(menu));
            router.setPath(getRouterPath(menu));
            router.setComponent(getComponent(menu));
            router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtils.equals("1", menu.getIsCache()), menu.getPath(), menu.getMenuId()));
            List<SysMenu> cMenus = menu.getChildren();
            if (!cMenus.isEmpty() && cMenus.size() > 0 &&
                    (UserConstants.TYPE_DIR.equals(menu.getMenuType()) || UserConstants.TYPE_MENU.equals(menu.getMenuType()))) {
                router.setAlwaysShow(true);
                router.setRedirect("noRedirect");
                router.setChildren(buildMenusForDesktop(cMenus));
            } else if (isMenuFrame(menu)) {
                router.setMeta(null);
                List<RouterVo> childrenList = new ArrayList<RouterVo>();
                RouterVo children = new RouterVo();
                children.setPath(menu.getPath());
                children.setComponent(menu.getComponent());
                children.setName(StringUtils.capitalize(menu.getPath()));
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtils.equals("1", menu.getIsCache()), menu.getPath(), menu.getMenuId()));
                childrenList.add(children);
                router.setChildren(childrenList);
            } else if (menu.getParentId().intValue() == 0 && isInnerLink(menu)) {
                router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), menu.getMenuId()));
                router.setPath("/inner");
                List<RouterVo> childrenList = new ArrayList<RouterVo>();
                RouterVo children = new RouterVo();
                String routerPath = StringUtils.replaceEach(menu.getPath(), new String[]{Constants.HTTP, Constants.HTTPS}, new String[]{"", ""});
                children.setPath(routerPath);
                children.setComponent(UserConstants.INNER_LINK);
                children.setName(StringUtils.capitalize(routerPath));
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), menu.getPath(), menu.getMenuId()));
                childrenList.add(children);
                router.setChildren(childrenList);
            }
            routers.add(router);
        }
        return routers;
    }

    /**
     * 构建前端所需要树结构
     *
     * @param menus 菜单列表
     * @return 树结构列表
     */
    @Override
    public List<SysMenu> buildMenuTree(List<SysMenu> menus) {
        List<SysMenu> returnList = new ArrayList<SysMenu>();
        List<Long> tempList = new ArrayList<Long>();
        for (SysMenu dept : menus) {
            tempList.add(dept.getMenuId());
        }
        for (Iterator<SysMenu> iterator = menus.iterator(); iterator.hasNext(); ) {
            SysMenu menu = (SysMenu) iterator.next();
            // 如果是顶级节点, 遍历该父节点的所有子节点
            if (!tempList.contains(menu.getParentId())) {
                recursionFn(menus, menu);
                returnList.add(menu);
            }
        }
        if (returnList.isEmpty()) {
            returnList = menus;
        }
        return returnList;
    }

    /**
     * 构建前端所需要下拉树结构
     *
     * @param menus 菜单列表
     * @return 下拉树结构列表
     */
    @Override
    public List<TreeSelect> buildMenuTreeSelect(List<SysMenu> menus) {
        List<SysMenu> menuTrees = buildMenuTree(menus);
        return menuTrees.stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    /**
     * 根据菜单ID查询信息
     *
     * @param menuId 菜单ID
     * @return 菜单信息
     */
    @Override
    public SysMenu selectMenuById(Long menuId) {
        SysMenu sysMenu = menuMapper.selectMenuById(menuId);
        // fail-fast（快速失败）：菜单缺失直接抛 CustomException，避免 NPE（NullPointerException，空指针异常）泄漏到 controller。
        if (sysMenu == null) {
            throw new CustomException("菜单不存在或已删除: menuId=" + menuId);
        }

        if (StringUtils.isEmpty(sysMenu.getPlatform())) {
            sysMenu.setPlatform("mgmt");
        }
        return sysMenu;
    }

    /**
     * 是否存在菜单子节点
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public boolean hasChildByMenuId(Long menuId) {
        int result = menuMapper.hasChildByMenuId(menuId);
        return result > 0 ? true : false;
    }

    /**
     * 查询菜单使用数量
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public boolean checkMenuExistRole(Long menuId) {
        int result = roleMenuMapper.checkMenuExistRole(menuId);
        return result > 0 ? true : false;
    }

    /**
     * 新增保存菜单信息
     *
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public int insertMenu(SysMenu menu) {
        return menuMapper.insertMenu(menu);
    }

    /**
     * 修改保存菜单信息
     *
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public int updateMenu(SysMenu menu) {
        return menuMapper.updateMenu(menu);
    }

    /**
     * 删除菜单管理信息
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public int deleteMenuById(Long menuId) {
        return menuMapper.deleteMenuById(menuId);
    }

    /**
     * 校验菜单名称是否唯一
     *
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public String checkMenuNameUnique(SysMenu menu) {
        Long menuId = StringUtils.isNull(menu.getMenuId()) ? -1L : menu.getMenuId();
        SysMenu info = menuMapper.checkMenuNameUnique(menu.getMenuName(), menu.getParentId());
        if (StringUtils.isNotNull(info) && info.getMenuId().longValue() != menuId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 获取路由名称
     *
     * @param menu 菜单信息
     * @return 路由名称
     */
    public String getRouteName(SysMenu menu) {
        String routerName = StringUtils.capitalize(menu.getPath());
        // 非外链并且是一级目录（类型为目录）
        if (isMenuFrame(menu)) {
            routerName = StringUtils.EMPTY;
        }
        return routerName;
    }

    /**
     * 获取路由地址
     *
     * @param menu 菜单信息
     * @return 路由地址
     */
    public String getRouterPath(SysMenu menu) {
        String routerPath = menu.getPath();
        // 内链打开外网方式
        if (menu.getParentId().intValue() != 0 && isInnerLink(menu)) {
            routerPath = StringUtils.replaceEach(routerPath, new String[]{Constants.HTTP, Constants.HTTPS}, new String[]{"", ""});
        }
        // 非外链并且是一级目录（类型为目录）
        if (0 == menu.getParentId().intValue() && UserConstants.TYPE_DIR.equals(menu.getMenuType())
                && UserConstants.NO_FRAME.equals(menu.getIsFrame())) {
            routerPath = "/" + menu.getPath();
        }
        // 非外链并且是一级目录（类型为菜单）
        else if (isMenuFrame(menu)) {
            routerPath = "/";
        }
        return routerPath;
    }

    /**
     * 获取组件信息
     *
     * @param menu 菜单信息
     * @return 组件信息
     */
    public String getComponent(SysMenu menu) {
        String component = UserConstants.LAYOUT;
        if (StringUtils.isNotEmpty(menu.getComponent()) && !isMenuFrame(menu)) {
            component = menu.getComponent();
        } else if (StringUtils.isEmpty(menu.getComponent()) && menu.getParentId().intValue() != 0 && isInnerLink(menu)) {
            component = UserConstants.INNER_LINK;
        } else if (StringUtils.isEmpty(menu.getComponent()) && isParentView(menu)) {
            component = UserConstants.PARENT_VIEW;
        }
        return component;
    }

    /**
     * 是否为菜单内部跳转
     *
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isMenuFrame(SysMenu menu) {
        return menu.getParentId().intValue() == 0 && UserConstants.TYPE_MENU.equals(menu.getMenuType())
                && menu.getIsFrame().equals(UserConstants.NO_FRAME);
    }

    /**
     * 是否为内链组件
     *
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isInnerLink(SysMenu menu) {
        return menu.getIsFrame().equals(UserConstants.NO_FRAME) && StringUtils.ishttp(menu.getPath());
    }

    /**
     * 是否为parent_view组件
     *
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isParentView(SysMenu menu) {
        return menu.getParentId().intValue() != 0 && UserConstants.TYPE_DIR.equals(menu.getMenuType());
    }

    /**
     * 根据父节点的ID获取所有子节点
     *
     * @param list     分类表
     * @param parentId 传入的父节点ID
     * @return String
     */
    public List<SysMenu> getChildPerms(List<SysMenu> list, int parentId) {
        List<SysMenu> returnList = new ArrayList<SysMenu>();
        for (Iterator<SysMenu> iterator = list.iterator(); iterator.hasNext(); ) {
            SysMenu t = (SysMenu) iterator.next();
            // 一、根据传入的某个父节点ID,遍历该父节点的所有子节点
            if (t.getParentId() == parentId) {
                recursionFn(list, t);
                returnList.add(t);
            }
        }
        return returnList;
    }

    /**
     * 递归列表
     *
     * @param list
     * @param t
     */
    private void recursionFn(List<SysMenu> list, SysMenu t) {
        // 得到子节点列表
        List<SysMenu> childList = getChildList(list, t);
        t.setChildren(childList);
        for (SysMenu tChild : childList) {
            if (hasChild(list, tChild)) {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<SysMenu> getChildList(List<SysMenu> list, SysMenu t) {
        List<SysMenu> tlist = new ArrayList<SysMenu>();
        Iterator<SysMenu> it = list.iterator();
        while (it.hasNext()) {
            SysMenu n = (SysMenu) it.next();
            if (n.getParentId().longValue() == t.getMenuId().longValue()) {
                tlist.add(n);
            }
        }
        return tlist;
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<SysMenu> list, SysMenu t) {
        return getChildList(list, t).size() > 0 ? true : false;
    }

    /**
     * PopoY: 后端统一收口身份中心管理台菜单边界，避免 legacy module（旧模块）击穿前端动态路由初始化。
     *
     * @param menuTree 已按父子关系组装好的菜单树
     * @return 收口后的菜单树
     */
    private List<SysMenu> filterIdentityConsoleMgmtMenus(List<SysMenu> menuTree) {
        return filterIdentityConsoleMgmtMenus(menuTree, false, 0);
    }

    /**
     * 递归过滤身份中心一期管理台菜单树。
     *
     * @param menuTree 当前层级菜单树
     * @param parentMatched 父级是否已命中允许模块
     * @param depth 当前深度，根节点为 0
     * @return 过滤后的菜单树
     */
    private List<SysMenu> filterIdentityConsoleMgmtMenus(List<SysMenu> menuTree, boolean parentMatched, int depth) {
        List<SysMenu> filteredMenus = new ArrayList<>();

        for (SysMenu menu : menuTree) {
            String moduleKey = getRouteModuleKey(menu);
            boolean ownMatched = shouldKeepIdentityConsoleMenu(menu, depth);
            boolean currentMatched = parentMatched || ownMatched;
            List<SysMenu> filteredChildren = Collections.emptyList();

            if (menu.getChildren() != null && !menu.getChildren().isEmpty()) {
                filteredChildren = filterIdentityConsoleMgmtMenus(menu.getChildren(), currentMatched, depth + 1);
                menu.setChildren(filteredChildren);
            }

            // 壳路由自身即使未命中，只要子级有允许模块也必须保留，例如 identity -> client/sync-task。
            if (!currentMatched && filteredChildren.isEmpty()) {
                continue;
            }

            filteredMenus.add(menu);
        }

        return filteredMenus;
    }

    /**
     * 判断当前菜单是否命中身份中心一期允许模块。
     *
     * @param menu 当前菜单
     * @param depth 当前深度，根节点为 0
     * @return true 表示允许保留
     */
    private boolean shouldKeepIdentityConsoleMenu(SysMenu menu, int depth) {
        String moduleKey = getRouteModuleKey(menu);

        if (StringUtils.isEmpty(moduleKey)) {
            return true;
        }

        if (!IDENTITY_CONSOLE_MODULE_WHITELIST.contains(moduleKey)) {
            return false;
        }

        return !TOP_LEVEL_ONLY_MODULE_KEYS.contains(moduleKey) || depth == 0;
    }

    /**
     * 提取菜单路径所属的顶级模块标识。
     *
     * @param menu 当前菜单
     * @return 顶级模块标识
     */
    private String getRouteModuleKey(SysMenu menu) {
        String menuPath = menu == null ? StringUtils.EMPTY : StringUtils.nvl(menu.getPath(), StringUtils.EMPTY);
        String normalizedPath = menuPath.replaceAll("^/+", "");
        String[] pathSegments = normalizedPath.split("/");
        return pathSegments.length == 0 ? normalizedPath : pathSegments[0];
    }
}
