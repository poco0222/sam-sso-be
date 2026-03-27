/**
 * @file Swagger/OpenAPI 示例控制器
 * @author PopoY
 * @date 2026-03-26
 */
package com.yr.web.controller.tool;

import com.yr.common.core.controller.BaseController;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * swagger 用户测试方法
 *
 * @author PopoY
 */
@Tag(name = "用户信息管理")
@Profile("local")
@RestController
@RequestMapping("/test/user")
public class TestController extends BaseController {

    private final static Logger LOGGER = LoggerFactory.getLogger(TestController.class);

    private final static Map<Integer, UserEntity> USER_S = new LinkedHashMap<Integer, UserEntity>();

    {
        USER_S.put(1, new UserEntity(1, "admin", "admin123", "15888888888"));
        USER_S.put(2, new UserEntity(2, "ry", "admin123", "15666666666"));
    }

    @GetMapping("/log")
    public AjaxResult testLog() {
        LOGGER.debug("我是 debug, {}", 1);
        LOGGER.info("我是 info, {}", 1);
        LOGGER.warn("我是 warn, {}", 1);
        LOGGER.error("我是 error, {}", 1);
        return AjaxResult.success();
    }

    @Operation(summary = "获取用户列表")
    @GetMapping("/list")
    public AjaxResult userList() {
        List<UserEntity> userList = new ArrayList<UserEntity>(USER_S.values());
        return AjaxResult.success(userList);
    }

    @Operation(summary = "获取用户详细")
    @GetMapping("/{userId}")
    public AjaxResult getUser(@PathVariable Integer userId) {
        if (!USER_S.isEmpty() && USER_S.containsKey(userId)) {
            return AjaxResult.success(USER_S.get(userId));
        } else {
            return error("用户不存在");
        }
    }

    @Operation(summary = "新增用户")
    @PostMapping("/save")
    public AjaxResult save(UserEntity user) {
        if (StringUtils.isNull(user) || StringUtils.isNull(user.getUserId())) {
            return error("用户ID不能为空");
        }
        return AjaxResult.success(USER_S.put(user.getUserId(), user));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/update")
    public AjaxResult update(@RequestBody UserEntity user) {
        if (StringUtils.isNull(user) || StringUtils.isNull(user.getUserId())) {
            return error("用户ID不能为空");
        }
        if (USER_S.isEmpty() || !USER_S.containsKey(user.getUserId())) {
            return error("用户不存在");
        }
        USER_S.remove(user.getUserId());
        return AjaxResult.success(USER_S.put(user.getUserId(), user));
    }
    @Operation(summary = "测试")
    @ResponseBody
    @PostMapping("/hello")
    public String hello(){
        return "hello";
    }

    @Operation(summary = "删除用户信息")
    @DeleteMapping("/{userId}")
    public AjaxResult delete(@PathVariable Integer userId) {
        if (!USER_S.isEmpty() && USER_S.containsKey(userId)) {
            USER_S.remove(userId);
            return success();
        } else {
            return error("用户不存在");
        }
    }
}

@Schema(name = "UserEntity", description = "用户实体")
class UserEntity {
    @Schema(description = "用户ID")
    private Integer userId;

    @Schema(description = "用户名称")
    private String username;

    @Schema(description = "用户密码")
    private String password;

    @Schema(description = "用户手机")
    private String mobile;

    public UserEntity() {

    }

    public UserEntity(Integer userId, String username, String password, String mobile) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.mobile = mobile;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}
