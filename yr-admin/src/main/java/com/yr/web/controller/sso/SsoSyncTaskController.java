/**
 * @file 身份中心同步任务控制器
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.web.controller.sso;

import com.yr.common.annotation.Log;
import com.yr.common.core.controller.BaseController;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.domain.entity.SsoSyncTask;
import com.yr.common.core.page.TableDataInfo;
import com.yr.common.enums.BusinessType;
import com.yr.common.utils.SecurityUtils;
import com.yr.system.service.ISsoSyncTaskService;
import com.yr.web.controller.sso.dto.SsoSyncTaskDistributionRequest;
import com.yr.web.controller.sso.dto.SsoSyncTaskInitImportRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 身份中心同步任务控制器。
 */
@RestController
@RequestMapping("/sso/sync-task")
public class SsoSyncTaskController extends BaseController {

    /** 同步任务服务。 */
    @Autowired
    private ISsoSyncTaskService ssoSyncTaskService;

    /**
     * 查询同步任务列表。
     *
     * @param query 查询条件
     * @return 同步任务分页结果
     */
    @PreAuthorize("@ss.hasPermi('sso:sync-task:list')")
    @GetMapping("/list")
    public TableDataInfo list(SsoSyncTask query) {
        startPage();
        List<SsoSyncTask> list = ssoSyncTaskService.selectSsoSyncTaskList(query);
        return getDataTable(list);
    }

    /**
     * 创建初始化导入任务。
     *
     * @param request 任务请求
     * @return 包含任务ID的响应体
     */
    @PreAuthorize("@ss.hasPermi('sso:sync-task:add')")
    @Log(title = "同步任务控制台", businessType = BusinessType.INSERT)
    @PostMapping("/init-import")
    public AjaxResult initImport(@RequestBody SsoSyncTaskInitImportRequest request) {
        SsoSyncTask task = toInitImportTask(request);
        String operator = resolveOperator();
        if (operator != null) {
            task.setCreateBy(operator);
        }
        SsoSyncTask createdTask = ssoSyncTaskService.initImportTask(task);
        AjaxResult ajaxResult = AjaxResult.success("初始化导入任务创建成功");
        ajaxResult.put("taskId", createdTask.getTaskId());
        return ajaxResult;
    }

    /**
     * 创建手工全量分发任务。
     *
     * @param request 任务请求
     * @return 包含任务 ID 的响应体
     */
    @PreAuthorize("@ss.hasPermi('sso:sync-task:add')")
    @Log(title = "同步任务控制台", businessType = BusinessType.INSERT)
    @PostMapping("/distribution")
    public AjaxResult distribution(@Validated @RequestBody SsoSyncTaskDistributionRequest request) {
        SsoSyncTask task = toDistributionTask(request);
        String operator = resolveOperator();
        if (operator != null) {
            task.setCreateBy(operator);
        }
        SsoSyncTask createdTask = ssoSyncTaskService.distributionTask(task);
        AjaxResult ajaxResult = AjaxResult.success("分发任务创建成功");
        ajaxResult.put("taskId", createdTask.getTaskId());
        return ajaxResult;
    }

    /**
     * 重试失败任务。
     *
     * @param taskId 任务ID
     * @return 包含任务ID的响应体
     */
    @PreAuthorize("@ss.hasPermi('sso:sync-task:edit')")
    @Log(title = "同步任务控制台", businessType = BusinessType.UPDATE)
    @PostMapping("/{taskId}/retry")
    public AjaxResult retry(@PathVariable Long taskId) {
        SsoSyncTask task = ssoSyncTaskService.retryTask(taskId);
        AjaxResult ajaxResult = AjaxResult.success("同步任务重试已提交");
        ajaxResult.put("taskId", task.getTaskId());
        return ajaxResult;
    }

    /**
     * 触发补偿任务。
     *
     * @param taskId 任务ID
     * @return 包含任务ID的响应体
     */
    @PreAuthorize("@ss.hasPermi('sso:sync-task:edit')")
    @Log(title = "同步任务控制台", businessType = BusinessType.UPDATE)
    @PostMapping("/{taskId}/compensate")
    public AjaxResult compensate(@PathVariable Long taskId) {
        SsoSyncTask task = ssoSyncTaskService.compensateTask(taskId);
        AjaxResult ajaxResult = AjaxResult.success("同步任务补偿已提交");
        ajaxResult.put("taskId", task.getTaskId());
        return ajaxResult;
    }

    /**
     * 查询同步任务详情。
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    @PreAuthorize("@ss.hasPermi('sso:sync-task:query')")
    @GetMapping("/{taskId}")
    public AjaxResult detail(@PathVariable Long taskId) {
        return AjaxResult.success(ssoSyncTaskService.selectSsoSyncTaskById(taskId));
    }

    /**
     * 解析当前操作人；缺少安全上下文时直接 fail-fast，避免静默写入空审计字段。
     *
     * @return 当前操作人账号
     */
    private String resolveOperator() {
        return SecurityUtils.getUsername();
    }

    /**
     * 把初始化导入请求映射为任务实体，避免客户端覆盖服务端生成字段。
     *
     * @param request 初始化导入请求
     * @return 任务实体
     */
    private SsoSyncTask toInitImportTask(SsoSyncTaskInitImportRequest request) {
        return new SsoSyncTask();
    }

    /**
     * 把分发请求映射为任务实体，只保留目标客户端编码。
     *
     * @param request 分发请求
     * @return 任务实体
     */
    private SsoSyncTask toDistributionTask(SsoSyncTaskDistributionRequest request) {
        SsoSyncTask task = new SsoSyncTask();
        task.setTargetClientCode(request.getTargetClientCode());
        return task;
    }
}
