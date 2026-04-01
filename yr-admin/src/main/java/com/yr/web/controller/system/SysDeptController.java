/**
 * @file 部门控制器，补齐列表与树查询入口的权限边界
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.web.controller.system;

import com.yr.common.annotation.Log;
import com.yr.common.constant.UserConstants;
import com.yr.common.core.controller.BaseController;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.enums.BusinessType;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.mapper.SysDeptMapper;
import com.yr.system.service.ISysDeptService;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Iterator;
import java.util.List;

/**
 * 部门信息
 *
 * @author Youngron
 */
@RestController
@RequestMapping("/system/dept")
public class SysDeptController extends BaseController {
    @Autowired
    private ISysDeptService deptService;
    @Autowired
    private SysDeptMapper sysDeptMapper;

    /**
     * 获取部门列表
     */
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/list")
    public AjaxResult list(SysDept dept) {
        List<SysDept> depts = deptService.selectDeptList(dept);
        return AjaxResult.success(depts);
    }

    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/getCheckDeptList")
    public AjaxResult getCheckDeptList() {
        List<SysDept> depts = deptService.getCheckDeptList();
        return AjaxResult.success(depts);
    }

    /**
     * 查询部门列表（排除节点）
     */
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/list/exclude/{deptId}")
    public AjaxResult excludeChild(@PathVariable(value = "deptId", required = false) Long deptId) {
        List<SysDept> depts = deptService.selectDeptList(new SysDept());
        Iterator<SysDept> it = depts.iterator();
        while (it.hasNext()) {
            SysDept d = (SysDept) it.next();
            if (d.getDeptId().intValue() == deptId
                    || ArrayUtils.contains(StringUtils.split(d.getAncestors(), ","), deptId + "")) {
                it.remove();
            }
        }
        return AjaxResult.success(depts);
    }

    /**
     * 根据部门编号获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:dept:query')")
    @GetMapping(value = "/{deptId}")
    public AjaxResult getInfo(@PathVariable Long deptId) {
        return AjaxResult.success(deptService.selectDeptById(deptId));
    }

    /**
     * 获取部门下拉树列表
     */
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/treeselect")
    public AjaxResult treeselect(SysDept dept) {
        List<SysDept> depts = deptService.selectDeptList(dept);
        /*
         * 1、查询物料大类别；
         * 2、找出部门表最大id；
         * 3、根据ID、parID找出depts的最末端节点；
         * 4、循环在最末端节点下增加大类别；形成新的depts，
         * （1）准备ID
         * （）
         * */
        for (SysDept ll : depts) {
            if (ll.getAccounteUnit() != null && ll.getAccounteUnit() != "" && ll.getAccounteUnit().equals("0")) {
                ll.setDeptName(ll.getDeptName() + "(核算单位)");
            }
        }
        return AjaxResult.success(deptService.buildDeptTreeSelect(depts));
    }

    /**
     * 新增部门
     */
    @PreAuthorize("@ss.hasPermi('system:dept:add')")
    @Log(title = "部门管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysDept dept) {
        if (UserConstants.NOT_UNIQUE.equals(deptService.checkDeptCodeUnique(dept))) {
            return AjaxResult.error("新增部门'" + dept.getDeptCode() + "'失败，部门编码已存在");
        }
        dept.setCreateBy(SecurityUtils.getUsername());
        return toAjax(deptService.insertDept(dept));
    }

    /**
     * 修改部门
     */
    @PreAuthorize("@ss.hasPermi('system:dept:edit')")
    @Log(title = "部门管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysDept dept) {
        if (UserConstants.NOT_UNIQUE.equals(deptService.checkDeptCodeUnique(dept))) {
            return AjaxResult.error("修改部门'" + dept.getDeptCode() + "'失败，部门编码已存在");
        } else if (dept.getParentId().equals(dept.getDeptId())) {
            return AjaxResult.error("修改部门'" + dept.getDeptName() + "'失败，上级部门不能是自己");
        } else if (StringUtils.equals(UserConstants.DEPT_DISABLE, dept.getStatus())
                && deptService.selectNormalChildrenDeptById(dept.getDeptId()) > 0) {
            return AjaxResult.error("该部门包含未停用的子部门！");
        }
        dept.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(deptService.updateDept(dept));
    }

    /**
     * 删除部门
     */
    @PreAuthorize("@ss.hasPermi('system:dept:remove')")
    @Log(title = "部门管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{deptId}")
    public AjaxResult remove(@PathVariable Long deptId) {
        if (deptService.hasChildByDeptId(deptId)) {
            return AjaxResult.error("存在下级部门,不允许删除");
        }
        if (deptService.checkDeptExistUser(deptId)) {
            return AjaxResult.error("部门存在用户,不允许删除");
        }
        SysDept sysDept = deptService.selectDeptById(deptId);
        if (sysDept.getParentId().compareTo(0L) == 0) {
            return AjaxResult.error("根部门,不允许删除");
        }
        return toAjax(deptService.deleteDeptById(deptId));
    }

    /*
    获取启用的部门名称和编号
     */
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/selectSysDept")
    public AjaxResult selectSysDept() {
        List<SysDept> list = deptService.selectSysDept();
        return AjaxResult.success(list);
    }

    /**
     * @CodingBy PopoY
     * @DateTime 2024/9/12 15:02
     * @Description 部门转字典表
     * @Return com.yr.common.core.domain.AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/getAllSysDeptForOptions")
    public AjaxResult getAllSysDeptForOptions() {
        return AjaxResult.success(sysDeptMapper.getAllSysDeptForOptions());
    }

    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/getChildrenDept/{deptId}")
    public AjaxResult getChildrenDept(@PathVariable("deptId") String deptId) {
        List<SysDept> sysDepts = sysDeptMapper.getChildrenDept(deptId);
        return AjaxResult.success(sysDepts);
    }
}
