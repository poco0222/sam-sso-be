/**
 * @file 一期固定控制台路由服务，替代基于 sys_menu 的动态路由装配
 * @author PopoY
 * @date 2026-03-26
 */
package com.yr.web.service;

import com.yr.common.enums.PlatformType;
import com.yr.system.domain.vo.MetaVo;
import com.yr.system.domain.vo.RouterVo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 为身份中心一期提供固定控制台路由。
 */
@Service
public class PhaseOneConsoleRouteService {

    /** 组织架构根菜单 ID。 */
    private static final long ORG_STRUCTURE_ROOT_MENU_ID = 9000L;

    /** 身份管理根菜单 ID。 */
    private static final long IDENTITY_ROOT_MENU_ID = 9007L;

    /** 客户端管理菜单 ID。 */
    private static final long CLIENT_MENU_ID = 9001L;

    /** 同步任务菜单 ID。 */
    private static final long SYNC_TASK_MENU_ID = 9002L;

    /** 用户管理菜单 ID。 */
    private static final long USER_MENU_ID = 9003L;

    /** 组织管理菜单 ID。 */
    private static final long ORG_MENU_ID = 9004L;

    /** 部门管理菜单 ID。 */
    private static final long DEPT_MENU_ID = 9005L;

    /** 同步日志菜单 ID。 */
    private static final long SYNC_TASK_LOG_MENU_ID = 9006L;

    /**
     * 按平台返回一期固定路由树。
     *
     * @param platform 平台标识
     * @return 路由树
     */
    public List<RouterVo> getRouters(String platform) {
        if (PlatformType.DESKTOP.getName().equals(platform)) {
            return buildDesktopRoutes();
        }
        return buildMgmtRoutes();
    }

    /**
     * 构造管理后台固定路由。
     *
     * @return 管理后台路由
     */
    private List<RouterVo> buildMgmtRoutes() {
        List<RouterVo> routes = new ArrayList<>();
        routes.add(buildOrganizationRoute());
        routes.add(buildIdentityRoute());
        return routes;
    }

    /**
     * 构造桌面端固定路由。
     *
     * @return 桌面端路由
     */
    private List<RouterVo> buildDesktopRoutes() {
        // 一期桌面端与管理后台共享同一套固定业务入口，避免再维护一份动态菜单树。
        return buildMgmtRoutes();
    }

    /**
     * 构造组织架构根路由及其子项。
     *
     * @return 组织架构根路由
     */
    private RouterVo buildOrganizationRoute() {
        RouterVo organizationRoute = buildTopLevelRoute("/system", "System", "组织架构", "tree", ORG_STRUCTURE_ROOT_MENU_ID);
        organizationRoute.setChildren(List.of(
                buildChildRoute("user", "system/user/index", "SystemUser", "用户管理", "user", USER_MENU_ID, false),
                buildChildRoute("org", "system/org/index", "SystemOrg", "组织管理", "tree", ORG_MENU_ID, false),
                buildChildRoute("dept", "system/dept/index", "SystemDept", "部门管理", "tree-table", DEPT_MENU_ID, false)
        ));
        return organizationRoute;
    }

    /**
     * 构造身份管理根路由及其子项。
     *
     * @return 身份管理根路由
     */
    private RouterVo buildIdentityRoute() {
        RouterVo identityRoute = buildTopLevelRoute("/identity", "IdentityRoot", "身份管理", "peoples", IDENTITY_ROOT_MENU_ID);
        identityRoute.setChildren(List.of(
                // 使用绝对路径保留既有访问地址，只调整侧栏展示层级。
                buildChildRoute("/client", "client/index", "Client", "客户端管理", "link", CLIENT_MENU_ID, false),
                buildChildRoute("/sync-task", "sync-task/index", "SyncTask", "同步任务控制台", "job", SYNC_TASK_MENU_ID, false),
                buildChildRoute("/sync-task/log", "sync-task/log", "SyncTaskLog", "任务日志", "history", SYNC_TASK_LOG_MENU_ID, true)
        ));
        return identityRoute;
    }

    /**
     * 构造顶级目录路由。
     *
     * @param path 路由路径
     * @param name 路由名称
     * @param title 菜单标题
     * @param icon 菜单图标
     * @param menuId 菜单 ID
     * @return 顶级路由
     */
    private RouterVo buildTopLevelRoute(String path, String name, String title, String icon, long menuId) {
        RouterVo route = new RouterVo();
        route.setPath(path);
        route.setName(name);
        route.setComponent("Layout");
        route.setAlwaysShow(true);
        route.setRedirect("noRedirect");
        route.setMeta(new MetaVo(title, icon, false, menuId));
        return route;
    }

    /**
     * 构造叶子节点路由。
     *
     * @param path 子路径
     * @param component 组件路径
     * @param name 路由名称
     * @param title 菜单标题
     * @param icon 菜单图标
     * @param menuId 菜单 ID
     * @param hidden 是否隐藏
     * @return 子路由
     */
    private RouterVo buildChildRoute(String path,
                                     String component,
                                     String name,
                                     String title,
                                     String icon,
                                     long menuId,
                                     boolean hidden) {
        RouterVo route = new RouterVo();
        route.setPath(path);
        route.setName(name);
        route.setComponent(component);
        route.setHidden(hidden);
        route.setMeta(new MetaVo(title, icon, false, menuId));
        return route;
    }
}
