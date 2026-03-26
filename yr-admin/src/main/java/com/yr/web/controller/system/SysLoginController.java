/**
 * @file 一期认证控制器，收敛账号密码登录、用户信息获取与组织切换入口
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.web.controller.system;

import com.yr.common.constant.Constants;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginBody;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.utils.ServletUtils;
import com.yr.framework.web.service.SysLoginService;
import com.yr.framework.web.service.SysPermissionService;
import com.yr.framework.web.service.TokenService;
import com.yr.system.domain.vo.RouterVo;
import com.yr.web.service.PhaseOneConsoleRouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * 一期标准登录控制器。
 */
@RestController
public class SysLoginController {

    /** 登录服务，负责密码登录与组织切换令牌刷新。 */
    @Autowired
    private SysLoginService loginService;

    /** 一期固定路由服务，用于构建控制台路由树。 */
    @Autowired
    private PhaseOneConsoleRouteService routeService;

    /** 权限服务，用于提取角色与菜单权限集合。 */
    @Autowired
    private SysPermissionService permissionService;

    /** Token 服务，用于从当前请求读取登录态。 */
    @Autowired
    private TokenService tokenService;

    /**
     * 执行账号密码登录，并返回一期统一 token 字段。
     *
     * @param loginBody 登录请求体
     * @return 登录结果
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody) {
        AjaxResult ajaxResult = AjaxResult.success("登录成功");
        String token = loginService.login(
                loginBody.getUsername(),
                loginBody.getPassword(),
                loginBody.getCode(),
                loginBody.getUuid(),
                loginBody.getPlatform()
        );
        ajaxResult.put(Constants.TOKEN, token);
        return ajaxResult;
    }

    /**
     * 切换当前登录用户的默认组织，并返回刷新后的 token。
     *
     * @param orgId 目标组织 ID
     * @return 切换组织结果
     */
    @PostMapping("/change-org/{orgId}")
    public AjaxResult changeOrg(@PathVariable("orgId") Long orgId) {
        AjaxResult ajaxResult = AjaxResult.success("切换组织成功");
        ajaxResult.put(Constants.TOKEN, loginService.changeOrg(orgId));
        return ajaxResult;
    }

    /**
     * 获取当前登录用户的一期基础身份信息。
     *
     * @return 当前用户、角色与权限集合
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo() {
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        SysUser user = loginUser.getUser();
        Set<String> roles = permissionService.getRolePermission(user);
        Set<String> permissions = permissionService.getMenuPermission(user);
        AjaxResult ajaxResult = AjaxResult.success();
        ajaxResult.put("user", user);
        ajaxResult.put("roles", roles);
        ajaxResult.put("permissions", permissions);
        return ajaxResult;
    }

    /**
     * 根据平台类型生成当前用户可访问的路由树。
     *
     * @param platform 平台标识，例如 `mgmt` 或 `desktop`
     * @return 平台路由列表
     */
    @GetMapping("getRouters/{platform}")
    public AjaxResult getRouters(@PathVariable("platform") String platform) {
        List<RouterVo> routerVos = routeService.getRouters(platform);
        return AjaxResult.success(routerVos);
    }
}
