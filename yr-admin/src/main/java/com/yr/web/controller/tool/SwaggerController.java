/**
 * @file Swagger 文档入口控制器
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.web.controller.tool;

import com.yr.common.core.controller.BaseController;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * swagger 接口
 *
 * @author PopoY
 */
@Profile("local")
@Controller
@RequestMapping("/tool/swagger")
public class SwaggerController extends BaseController {
    /**
     * 仅在 local profile（本地环境）下提供文档入口跳转。
     *
     * @return Swagger UI 页面重定向
     */
    @GetMapping()
    public String index() {
        return redirect("/swagger-ui.html");
    }
}
