/**
 * @file 个人信息控制器，收敛一期仍保留的用户基础资料入口
 * @author PopoY
 * @date 2026-03-26
 */
package com.yr.web.controller.system;

import com.yr.common.annotation.Log;
import com.yr.common.config.YrConfig;
import com.yr.common.constant.UserConstants;
import com.yr.common.core.controller.BaseController;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.enums.BusinessType;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.ServletUtils;
import com.yr.common.utils.StringUtils;
import com.yr.common.utils.file.FileUploadUtils;
import com.yr.framework.web.service.TokenService;
import com.yr.web.controller.system.dto.UpdateProfileRequest;
import com.yr.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 个人信息 业务处理
 *
 * @author Youngron
 */
@RestController
@RequestMapping("/system/user/profile")
public class SysProfileController extends BaseController {
    @Autowired
    private ISysUserService userService;

    @Autowired
    private TokenService tokenService;

    /**
     * 个人信息
     */
    @GetMapping
    public AjaxResult profile() {
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        SysUser user = loginUser.getUser();
        AjaxResult ajax = AjaxResult.success(user);
        // 一期个人信息页仅保留用户基础信息与部门关系，彻底移除 role/post/duty/rank 扩展块。
        ajax.put("deptList", userService.selectUserDeptListByUserIdAndOrgId(loginUser.getUserId(), user.getOrgId()));
        return ajax;
    }

    /**
     * 修改用户
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult updateProfile(@Validated @RequestBody UpdateProfileRequest request) {
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        SysUser user = buildProfileUpdateUser(loginUser, request);
        if (StringUtils.isNotEmpty(user.getPhonenumber())
                && UserConstants.NOT_UNIQUE.equals(userService.checkPhoneUnique(user))) {
            return AjaxResult.error("修改用户'" + loginUser.getUsername() + "'失败，手机号码已存在");
        }
        if (StringUtils.isNotEmpty(user.getEmail())
                && UserConstants.NOT_UNIQUE.equals(userService.checkEmailUnique(user))) {
            return AjaxResult.error("修改用户'" + loginUser.getUsername() + "'失败，邮箱账号已存在");
        }
        SysUser sysUser = loginUser.getUser();
        if (userService.updateUserProfile(user) > 0) {
            // 更新缓存用户信息
            sysUser.setNickName(user.getNickName());
            sysUser.setPhonenumber(user.getPhonenumber());
            sysUser.setEmail(user.getEmail());
            sysUser.setSex(user.getSex());
            tokenService.setLoginUser(loginUser);
            return AjaxResult.success();
        }
        return AjaxResult.error("修改个人信息异常，请联系管理员");
    }

    /**
     * 重置密码
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping("/updatePwd")
    public AjaxResult updatePwd(String oldPassword, String newPassword) {
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        String password = loginUser.getPassword();
        if (!SecurityUtils.matchesPassword(oldPassword, password)) {
            return AjaxResult.error("修改密码失败，旧密码错误");
        }
        if (SecurityUtils.matchesPassword(newPassword, password)) {
            return AjaxResult.error("新密码不能与旧密码相同");
        }
        String encodedPassword = SecurityUtils.encryptPassword(newPassword);
        SysUser resetUser = new SysUser();
        resetUser.setUserId(loginUser.getUserId());
        resetUser.setPassword(encodedPassword);
        resetUser.setFirstLogin("0");
        resetUser.setUpdateBy(loginUser.getUsername());
        if (userService.resetUserPwd(resetUser) > 0) {
            // 更新缓存用户密码
            SysUser sysUser = loginUser.getUser();
            sysUser.setPassword(encodedPassword);
            sysUser.setFirstLogin("0");
            tokenService.setLoginUser(loginUser);
            return AjaxResult.success();
        }
        return AjaxResult.error("修改密码异常，请联系管理员");
    }

    /**
     * 头像上传
     */
    @Log(title = "用户头像", businessType = BusinessType.UPDATE)
    @PostMapping("/avatar")
    public AjaxResult avatar(@RequestParam("avatarfile") MultipartFile file) throws IOException {
        if (!file.isEmpty()) {
            LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
            String avatar = FileUploadUtils.upload(YrConfig.getAvatarPath(), file);
            if (userService.updateUserAvatar(loginUser.getUsername(), avatar)) {
                AjaxResult ajax = AjaxResult.success();
                ajax.put("imgUrl", avatar);
                // 更新缓存用户头像
                loginUser.getUser().setAvatar(avatar);
                tokenService.setLoginUser(loginUser);
                return ajax;
            }
        }
        return AjaxResult.error("上传图片异常，请联系管理员");
    }

    /**
     * 构造个人资料写入对象，只保留允许自助修改的资料字段。
     *
     * @param loginUser 当前登录用户
     * @param request 个人资料更新请求
     * @return 精简后的用户写入对象
     */
    private SysUser buildProfileUpdateUser(LoginUser loginUser, UpdateProfileRequest request) {
        SysUser user = new SysUser();
        user.setUserId(loginUser.getUserId());
        user.setNickName(request.getNickName());
        user.setPhonenumber(request.getPhonenumber());
        user.setEmail(request.getEmail());
        user.setSex(request.getSex());
        return user;
    }
}
