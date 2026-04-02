package com.yr.web.controller.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.core.controller.BaseController;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.page.TableDataInfo;
import com.yr.system.service.ISysUserDeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Stream;

/**
 * @author PopoY
 * @version V1.0
 * @Date 2021-9-18 11:07
 * @description
 */

@RestController
@RequestMapping("/system/user-dept")
public class SysUserDeptController extends BaseController {

    @Autowired
    private ISysUserDeptService iSysUserDeptService;

    /**
     * 分页查询部门已分配的用户列表
     *
     * @param pageNum
     * @param pageSize
     * @param deptId
     * @param sysUser
     * @return
     */
    @PreAuthorize("@ss.hasPermi('system:userDept:addUser')")
    @GetMapping("/page")
    public TableDataInfo pageDeptAssignUserByDeptId(@RequestParam(value = "pageNum", defaultValue = "1", required = false) int pageNum,
                                                    @RequestParam(value = "pageSize", defaultValue = "10", required = false) int pageSize,
                                                    @RequestParam("deptId") Long deptId,
                                                    SysUser sysUser) {
        IPage<SysUser> page = iSysUserDeptService.pageDeptAssignUserByDeptId(pageNum, pageSize, deptId, sysUser);
        return getDataTableByPage(page);
    }

    /**
     * 分页查询未分配部门的用户列表
     *
     * @param pageNum
     * @param pageSize
     * @param deptId
     * @param sysUser
     * @return
     */
    @PreAuthorize("@ss.hasPermi('system:userDept:addUser')")
    @GetMapping("/page/unassign")
    public TableDataInfo pageDeptUnAssignUserByDeptId(@RequestParam(value = "pageNum", defaultValue = "1", required = false) int pageNum,
                                                      @RequestParam(value = "pageSize", defaultValue = "10", required = false) int pageSize,
                                                      @RequestParam("deptId") Long deptId,
                                                      SysUser sysUser) {
        IPage<SysUser> page = iSysUserDeptService.pageDeptUnAssignUserByDeptId(pageNum, pageSize, deptId, sysUser);
        return getDataTableByPage(page);
    }

    /**
     * 给部门分配用户
     *
     * @param deptId
     * @param userIdStr 多个用户用逗号分隔
     * @return
     */
    @PreAuthorize("@ss.hasPermi('system:userDept:addUser')")
    @PostMapping("/add")
    public AjaxResult addAssignUserDept(@RequestParam("deptId") Long deptId, @RequestParam("userIds") String userIdStr) {
        String[] arr = userIdStr.split(",");
        Long[] userIds = Stream.of(arr).map(Long::valueOf).toArray(Long[]::new);
        iSysUserDeptService.addAssignUserDept(deptId, userIds);
        return AjaxResult.success();
    }

    /**
     * 将部门中的用户移除
     *
     * @param idStr 部门用户关系表id
     * @return
     */
    @PreAuthorize("@ss.hasAnyPermi('system:userDept:addUser, system:userDept:addDept')")
    @DeleteMapping("/remove")
    public AjaxResult removeAssignUserDept(@RequestParam("ids") String idStr) {
        String[] arr = idStr.split(",");
        Long[] ids = Stream.of(arr).map(Long::valueOf).toArray(Long[]::new);
        iSysUserDeptService.removeAssignUserDeptByIds(ids);
        return AjaxResult.success();
    }

    /**
     * 根据用户ID查询用户已分配的部门
     *
     * @param pageNum
     * @param pageSize
     * @param userId
     * @return
     */
    @PreAuthorize("@ss.hasPermi('system:userDept:addDept')")
    @GetMapping("/page/dept")
    public TableDataInfo pageDeptByUserId(@RequestParam(value = "pageNum", defaultValue = "1", required = false) int pageNum,
                                          @RequestParam(value = "pageSize", defaultValue = "10", required = false) int pageSize,
                                          @RequestParam("userId") Long userId) {
        IPage<SysDept> page = iSysUserDeptService.pageDeptByUserId(pageNum, pageSize, userId);
        return getDataTableByPage(page);
    }

    /**
     * 为用户添加部门
     *
     * @param userId
     * @param deptId
     * @return
     */
    @PreAuthorize("@ss.hasPermi('system:userDept:addDept')")
    @PostMapping("/addDeptForUser")
    public AjaxResult addDeptForUser(@RequestParam("userId") Long userId, @RequestParam("deptId") Long deptId) {
        iSysUserDeptService.addDeptForUser(userId, deptId);
        return AjaxResult.success();
    }

    /**
     * 设置用户默认的部门
     *
     * @param userId
     * @param deptId
     * @return
     */
    @PreAuthorize("@ss.hasPermi('system:userDept:addDept')")
    @PutMapping("/setDefault")
    public AjaxResult setDefaultUserDept(@RequestParam("userId") Long userId, @RequestParam("deptId") Long deptId) {
        iSysUserDeptService.setDefaultUserDept(userId, deptId);
        return AjaxResult.success();
    }

}
