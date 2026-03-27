/**
 * @file 一期固定控制台路由展示层级测试
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.web.service;

import com.yr.system.domain.vo.RouterVo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定固定菜单展示层级，避免身份中心与组织架构分组再次漂移。
 */
class PhaseOneConsoleRouteServiceDisplayHierarchyTest {

    /** 固定路由服务，当前无外部依赖，可直接实例化。 */
    private final PhaseOneConsoleRouteService routeService = new PhaseOneConsoleRouteService();

    /**
     * 验证管理台固定路由应展示为“组织架构”与“身份管理”两棵顶层树。
     */
    @Test
    void shouldExposeOrganizationAndIdentityAsTopLevelGroups() {
        List<RouterVo> routes = routeService.getRouters("mgmt");
        Map<String, RouterVo> routesByTitle = routes.stream()
                .collect(Collectors.toMap(route -> route.getMeta().getTitle(), Function.identity()));

        assertThat(routes)
                .extracting(route -> route.getMeta().getTitle())
                .containsExactly("组织架构", "身份管理");

        RouterVo organizationRoute = routesByTitle.get("组织架构");
        assertThat(organizationRoute).isNotNull();
        assertThat(organizationRoute.getPath()).isEqualTo("/system");
        assertThat(organizationRoute.getChildren())
                .extracting(child -> child.getMeta().getTitle())
                .containsExactly("用户管理", "组织管理", "部门管理");

        RouterVo identityRoute = routesByTitle.get("身份管理");
        assertThat(identityRoute).isNotNull();
        assertThat(identityRoute.getPath()).isEqualTo("/identity");
        assertThat(identityRoute.getChildren().stream().filter(child -> !child.getHidden()))
                .extracting(child -> child.getMeta().getTitle())
                .containsExactly("客户端管理", "同步任务控制台");
        assertThat(identityRoute.getChildren().stream().filter(RouterVo::getHidden))
                .extracting(child -> child.getPath())
                .containsExactly("/sync-task/log");
    }
}
