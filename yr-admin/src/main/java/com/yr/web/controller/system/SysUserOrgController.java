package com.yr.web.controller.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.core.controller.BaseController;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.page.TableDataInfo;
import com.yr.system.domain.entity.SysUserOrg;
import com.yr.system.service.ISysUserOrgService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * @author Youngron
 * @version V1.0
 * @date 2021-9-14 14:30
 */

@RestController
@RequestMapping("/system/user-org")
public class SysUserOrgController extends BaseController {

    @Autowired
    private ISysUserOrgService sysUserOrgService;

    /**
     * 分页查询用户组织
     *
     * @param pageNum
     * @param pageSize
     * @param userId
     * @return
     */
    @PreAuthorize("@ss.hasPermi('system:userOrg:addOrg')")
    @GetMapping("/page/user-id")
    public TableDataInfo pageByUserId(@RequestParam(value = "pageNum", defaultValue = "1", required = false) int pageNum,
                                      @RequestParam(value = "pageSize", defaultValue = "10", required = false) int pageSize,
                                      @RequestParam(value = "userId", required = false) Long userId) {
        IPage<SysUserOrg> page = sysUserOrgService.pageByUserId(pageNum, pageSize, userId);
        return getDataTableByPage(page);
    }

    /**
     * 给用户新增组织
     *
     * @param sysUserOrg
     * @return
     */
    @PreAuthorize("@ss.hasPermi('system:userOrg:addOrg')")
    @PostMapping
    public AjaxResult addSysUserOrg(@RequestBody SysUserOrg sysUserOrg) {
        sysUserOrgService.addSysUserOrg(sysUserOrg);
        return AjaxResult.success(sysUserOrg);
    }

    /**
     * 激活/禁用组织
     *
     * @param sysUserOrg
     * @return
     */
    @PreAuthorize("@ss.hasPermi('system:userOrg:addOrg')")
    @PutMapping("/changeEnabled")
    public AjaxResult changeEnabledById(@RequestBody SysUserOrg sysUserOrg) {
        return toAjax(sysUserOrgService.changeEnabledById(sysUserOrg.getId(), sysUserOrg.getEnabled()));
    }

    /**
     * 设置用户的默认组织
     *
     * @param userId 用户ID
     * @param orgId  组织ID
     * @return 结果
     */
    @PreAuthorize("@ss.hasPermi('system:userOrg:addOrg')")
    @PutMapping("/setDefaultUserOrg")
    public AjaxResult setDefaultUserOrg(@RequestParam("userId") Long userId,
                                        @RequestParam("orgId") Long orgId) {
        return toAjax(sysUserOrgService.setDefaultUserOrg(userId, orgId));
    }

    /**
     * 获取当前登录用户所有组织信息
     *
     * @return
     */
    @GetMapping("/user-all-org")
    public AjaxResult getCurrUserAllOrg() {
        return AjaxResult.success(sysUserOrgService.getCurrUserAllOrg());
    }

}
