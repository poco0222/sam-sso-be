/**
 * @file 用户控制器，补齐高风险查询入口的权限边界
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.web.controller.system;

import com.yr.common.annotation.Log;
import com.yr.common.constant.UserConstants;
import com.yr.common.core.controller.BaseController;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.core.page.TableDataInfo;
import com.yr.common.core.redis.RedisCache;
import com.yr.common.enums.BusinessType;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.ServletUtils;
import com.yr.common.utils.StringUtils;
import com.yr.common.utils.poi.ExcelUtil;
import com.yr.framework.web.service.TokenService;
import com.yr.web.controller.system.dto.ChangeUserStatusRequest;
import com.yr.web.controller.system.dto.ResetUserPasswordRequest;
import com.yr.web.controller.system.dto.UpdateUserRequest;
import com.yr.system.mapper.SysUserMapper;
import com.yr.system.service.ISysDeptService;
import com.yr.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 用户信息
 *
 * @author Youngron
 */
@RestController
@RequestMapping("/system/user")
public class SysUserController extends BaseController {
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private RedisCache redisCache;
    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * 获取用户列表
     */
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysUser user) {
        startPage();
        List<SysUser> list = userService.selectUserList(user);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/list/v2")
    public TableDataInfo listV2(SysUser user) {
        if (user.getOrgId() == null) {
            user.setOrgId(SecurityUtils.getOrgId());
        }
        if (user.getDeptId() != null) {
            // 组织下有些用户没有关联部门，当点击根部门时，需要去掉部门的参数，不然查不出来
            SysDept sysDept = deptService.selectDeptById(user.getDeptId());
            if (sysDept.getParentId().equals(0L)) {
                user.setDeptId(null);
            }
        }
        startPage();
        List<SysUser> list = userService.selectUserListV2(user);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/alllist")
    public AjaxResult allList() {
        return AjaxResult.success(userService.selectUserList(new SysUser()));
    }

    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/getAllUserForOptions")
    public AjaxResult getAllUserForOptions() {
        return AjaxResult.success(sysUserMapper.getAllUserForOptions());
    }

    @Log(title = "用户管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:user:export')")
    @GetMapping("/export")
    public AjaxResult export(SysUser user) {
        List<SysUser> list = userService.selectUserList(user);
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        return util.exportExcel(list, "用户数据");
    }

    @Log(title = "用户管理", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('system:user:import')")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        List<SysUser> userList = util.importExcel(file.getInputStream());
        LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        String operName = loginUser.getUsername();
        String message = userService.importUser(userList, updateSupport, operName);
        return AjaxResult.success(message);
    }

    @PreAuthorize("@ss.hasPermi('system:user:import')")
    @GetMapping("/importTemplate")
    public AjaxResult importTemplate() {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        return util.importTemplateExcel("用户数据");
    }

    /**
     * 根据用户编号获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping(value = {"/", "/{userId}"})
    public AjaxResult getInfo(@PathVariable(value = "userId", required = false) Long userId) {
        AjaxResult ajax = AjaxResult.success();
        if (StringUtils.isNotNull(userId)) {
            ajax.put(AjaxResult.DATA_TAG, userService.selectUserById(userId, SecurityUtils.getOrgId()));
        }
        return ajax;
    }

    /**
     * 根据用户编号获取详细信息
     */
    @PreAuthorize("@ss.hasAnyPermi('system:user:query,system:userOrg:addOrg')")
    @GetMapping("/v2/{userId}")
    public AjaxResult getUserById(@PathVariable(value = "userId") Long userId) {
        return AjaxResult.success(userService.getUserById(userId));
    }

    /**
     * 新增用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysUser user) {
        if (UserConstants.NOT_UNIQUE.equals(userService.checkUserNameUnique(user.getUserName()))) {
            return AjaxResult.error("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        } else if (StringUtils.isNotEmpty(user.getPhonenumber())
                && UserConstants.NOT_UNIQUE.equals(userService.checkPhoneUnique(user))) {
            return AjaxResult.error("新增用户'" + user.getUserName() + "'失败，手机号码已存在");
        } else if (StringUtils.isNotEmpty(user.getEmail())
                && UserConstants.NOT_UNIQUE.equals(userService.checkEmailUnique(user))) {
            return AjaxResult.error("新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setCreateBy(SecurityUtils.getUsername());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        return toAjax(userService.insertUser(user));
    }

    /**
     * 修改用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody UpdateUserRequest request) {
        SysUser user = buildUpdateUser(request);
        userService.checkUserAllowed(user);
        if (StringUtils.isNotEmpty(user.getPhonenumber())
                && UserConstants.NOT_UNIQUE.equals(userService.checkPhoneUnique(user))) {
            return AjaxResult.error("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        } else if (StringUtils.isNotEmpty(user.getEmail())
                && UserConstants.NOT_UNIQUE.equals(userService.checkEmailUnique(user))) {
            return AjaxResult.error("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.updateUser(user));
    }

    /**
     * 删除用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds) {
        return toAjax(userService.deleteUserByIds(userIds));
    }

    /**
     * 重置密码
     */
    @PreAuthorize("@ss.hasPermi('system:user:resetPwd')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@Validated @RequestBody ResetUserPasswordRequest request) {
        SysUser user = buildPasswordResetUser(request);
        userService.checkUserAllowed(user);
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.resetPwd(user));
    }

    /**
     * 解锁用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:unlock')")
    @Log(title = "解锁用户", businessType = BusinessType.UPDATE)
    @GetMapping("/unlock")
    public AjaxResult unlock(@RequestParam("username") String username) {
        if (username == null) {
            return AjaxResult.error("username不能为空");
        }
        redisCache.deleteObject("login_error:" + username);
        return AjaxResult.success();
    }

    /**
     * 状态修改
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@Validated @RequestBody ChangeUserStatusRequest request) {
        SysUser user = buildStatusChangeUser(request);
        userService.checkUserAllowed(user);
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.updateUserStatus(user));
    }

    /**
     * 构造密码重置专用用户写入对象，只保留允许下沉到 service 的字段。
     *
     * @param request 密码重置请求
     * @return 精简后的用户写入对象
     */
    private SysUser buildPasswordResetUser(ResetUserPasswordRequest request) {
        SysUser user = new SysUser();
        user.setUserId(request.getUserId());
        user.setPassword(request.getPassword());
        user.setFirstLogin(request.getFirstLogin());
        return user;
    }

    /**
     * 构造状态修改专用用户写入对象，只保留状态修改必要字段。
     *
     * @param request 状态修改请求
     * @return 精简后的用户写入对象
     */
    private SysUser buildStatusChangeUser(ChangeUserStatusRequest request) {
        SysUser user = new SysUser();
        user.setUserId(request.getUserId());
        user.setStatus(request.getStatus());
        return user;
    }

    /**
     * 构造普通编辑专用用户写入对象，只保留允许下沉的基础资料字段。
     *
     * @param request 普通编辑请求
     * @return 精简后的用户写入对象
     */
    private SysUser buildUpdateUser(UpdateUserRequest request) {
        SysUser user = new SysUser();
        user.setUserId(request.getUserId());
        user.setDeptId(request.getDeptId());
        user.setUserName(request.getUserName());
        user.setNickName(request.getNickName());
        user.setEmail(request.getEmail());
        user.setPhonenumber(request.getPhonenumber());
        user.setSex(request.getSex());
        user.setAvatar(request.getAvatar());
        user.setRemark(request.getRemark());
        return user;
    }

    /**
     * 获取部门树列表
     */
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/deptTree")
    public AjaxResult deptTree(SysDept dept) {
        return success(deptService.selectDeptTreeList(dept));
    }

    /**
     * 根据部门编号获取 获取对应的用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/selectSysUserById/{deptCode}")
    public AjaxResult selectSysUserById(@PathVariable String deptCode) {
        List<SysUser> listUser = userService.selectSysUserById(deptCode);
        return AjaxResult.success(listUser);
    }

    /**
     * 根据部门id获取 获取对应的用户
     */
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/batchSelectUserByDeptId/{deptIds}")
    public AjaxResult batchSelectUserByDeptId(@PathVariable String deptIds) {
        Long[] deptIdArray = Arrays.stream(deptIds.split(","))//分割后产生的数组转流
                .map(Long::valueOf)//转换流中的每个元素(转成Long类型)
                .toArray(Long[]::new);//收集到新的Long数组
        Map<Long, List<SysUser>> listUser = userService.batchSelectUserByDeptId(deptIdArray);
        return AjaxResult.success(listUser);
    }

    /**
     * @CodingBy PopoY
     * @DateTime 2024/12/19 13:53
     * @Description 查询用户,不分页版
     * @Param user 用户对象
     * @Return com.yr.common.core.domain.AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/listV2ForAF")
    public AjaxResult listV2ForAF(SysUser user) {
        if (user.getOrgId() == null) {
            user.setOrgId(SecurityUtils.getOrgId());
        }
        if (user.getDeptId() != null) {
            // 组织下有些用户没有关联部门，当点击根部门时，需要去掉部门的参数，不然查不出来
            SysDept sysDept = deptService.selectDeptById(user.getDeptId());
            if (sysDept.getParentId().equals(0L)) {
                user.setDeptId(null);
            }
        }
        return AjaxResult.success(userService.selectUserListV2(user));
    }

    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("getChildDeptUser/{deptId}")
    public AjaxResult getChildDeptUser(@PathVariable("deptId") String deptId){
        List<SysUser> childDeptUsers = sysUserMapper.getChildDeptUser(deptId);
        return AjaxResult.success(childDeptUsers);
    }
}
