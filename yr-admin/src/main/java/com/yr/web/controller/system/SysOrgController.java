package com.yr.web.controller.system;

import com.yr.common.annotation.Log;
import com.yr.common.constant.Constants;
import com.yr.common.constant.UserConstants;
import com.yr.common.core.controller.BaseController;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.domain.entity.SysOrg;
import com.yr.common.enums.BusinessType;
import com.yr.common.utils.StringUtils;
import com.yr.common.utils.poi.ExcelUtil;
import com.yr.system.service.ISysOrgService;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组织信息Controller
 *
 * @author Youngron
 * @date 2021-09-09
 */
@RestController
@RequestMapping("/system/org")
public class SysOrgController extends BaseController {
    @Autowired
    private ISysOrgService sysOrgService;

    /**
     * 查询组织信息列表
     */
    @PreAuthorize("@ss.hasAnyPermi('system:org:list,system:userOrg:addOrg')")
    @GetMapping("/list")
    public AjaxResult list(SysOrg sysOrg) {
        List<SysOrg> list = sysOrgService.selectSysOrgList(sysOrg);
        return AjaxResult.success(list);
    }

    /**
     * 导出组织信息列表
     */
    @PreAuthorize("@ss.hasPermi('system:org:export')")
    @Log(title = "组织信息", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public AjaxResult export(SysOrg sysOrg) {
        List<SysOrg> list = sysOrgService.selectSysOrgList(sysOrg);
        ExcelUtil<SysOrg> util = new ExcelUtil<SysOrg>(SysOrg.class);
        return util.exportExcel(list, "组织信息数据");
    }

    /**
     * 获取组织信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:org:query')")
    @GetMapping(value = "/{orgId}")
    public AjaxResult getInfo(@PathVariable("orgId") Long orgId) {
        SysOrg sysOrg = sysOrgService.selectSysOrgById(orgId);
        if (StringUtils.isNull(sysOrg)) {
            return AjaxResult.error("查询组织失败，无法找到组织ID对应的组织信息");
        }
        return AjaxResult.success(sysOrg);
    }

    /**
     * 新增组织信息
     */
    @PreAuthorize("@ss.hasPermi('system:org:add')")
    @Log(title = "组织信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody SysOrg sysOrg) {
        if (Constants.FAIL.equals(sysOrgService.checkOrgCodeUnique(sysOrg.getOrgCode()))) {
            return AjaxResult.error("新增组织'" + sysOrg.getOrgCode() + "'失败，组织编码已存在");
        }
        return toAjax(sysOrgService.insertSysOrg(sysOrg));
    }

    /**
     * 修改组织信息
     */
    @PreAuthorize("@ss.hasPermi('system:org:edit')")
    @Log(title = "组织信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody SysOrg sysOrg) {
        if (sysOrg.getParentId().equals(sysOrg.getOrgId())) {
            return AjaxResult.error("失败，上级组织不能是自己");
        }
        if (StringUtils.equals(UserConstants.DEPT_DISABLE, sysOrg.getStatus())
                && sysOrgService.selectNormalChildrenOrgById(sysOrg.getOrgId()) > 0) {
            return AjaxResult.error("该组织包含未停用的子组织！");
        }
        return toAjax(sysOrgService.updateSysOrg(sysOrg));
    }

    /**
     * 删除组织信息
     */
    @PreAuthorize("@ss.hasPermi('system:org:remove')")
    @Log(title = "组织信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{orgIds}")
    public AjaxResult remove(@PathVariable Long orgIds) {
        // 不允许删除组织
        return AjaxResult.success();
        // 查询是否有子节点数据
//        if (sysOrgService.hasChildByOrgId(orgIds)) {
//            return AjaxResult.error("存在下级组织信息,不允许删除");
//        }
//        return toAjax(sysOrgService.deleteSysOrgById(orgIds));
    }

    /**
     * 状态修改
     *
     * @param sysOrg
     * @return
     */
    @PreAuthorize("@ss.hasPermi('system:org:edit')")
    @Log(title = "组织信息", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysOrg sysOrg) {
        return toAjax(sysOrgService.updateOrgStatus(sysOrg));
    }

    /**
     * 查询组织信息列表（排除子节点）
     *
     * @param orgId
     * @return
     */
    @PreAuthorize("@ss.hasPermi('system:org:edit')")
    @GetMapping("/list/exclude/{orgId}")
    public AjaxResult excludeChild(@PathVariable(value = "orgId") Long orgId) {
        if (orgId == null) {
            return AjaxResult.error("组织ID为空");
        }
        List<SysOrg> list = sysOrgService.selectSysOrgList(new SysOrg());
        list.removeIf(d -> d.getOrgId().intValue() == orgId
                || ArrayUtils.contains(StringUtils.split(d.getAncestors(), ","), orgId + ""));
        return AjaxResult.success(list);
    }
}
