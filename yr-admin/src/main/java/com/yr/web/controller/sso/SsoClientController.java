/**
 * @file 身份中心客户端控制器
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.web.controller.sso;

import com.yr.common.annotation.Log;
import com.yr.common.core.controller.BaseController;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.domain.entity.SsoClient;
import com.yr.common.core.page.TableDataInfo;
import com.yr.common.enums.BusinessType;
import com.yr.common.utils.SecurityUtils;
import com.yr.system.domain.dto.SsoClientSecretIssueResult;
import com.yr.system.service.ISsoClientService;
import com.yr.web.controller.sso.dto.SsoClientCreateRequest;
import com.yr.web.controller.sso.dto.SsoClientOptionView;
import com.yr.web.controller.sso.dto.SsoClientStatusUpdateRequest;
import com.yr.web.controller.sso.dto.SsoClientUpdateRequest;
import com.yr.web.controller.sso.dto.SsoClientView;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.pagehelper.PageInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 身份中心客户端控制器。
 */
@RestController
@RequestMapping("/sso/client")
public class SsoClientController extends BaseController {

    /** 客户端服务。 */
    private final ISsoClientService ssoClientService;

    /**
     * @param ssoClientService 客户端服务
     */
    public SsoClientController(ISsoClientService ssoClientService) {
        this.ssoClientService = ssoClientService;
    }

    /**
     * 查询客户端列表。
     *
     * @param query 查询条件
     * @return 客户端分页结果
     */
    @PreAuthorize("@ss.hasPermi('sso:client:list')")
    @GetMapping("/list")
    public TableDataInfo list(SsoClient query) {
        startPage();
        List<SsoClient> list = ssoClientService.selectSsoClientList(query);
        long total = new PageInfo<>(list).getTotal();
        List<SsoClientView> viewList = new ArrayList<>(list.size());
        for (SsoClient ssoClient : list) {
            viewList.add(toView(ssoClient));
        }
        return getDataTable(viewList, total);
    }

    /**
     * 查询分发任务客户端下拉选项。
     *
     * @return 客户端选项列表
     */
    @PreAuthorize("@ss.hasAnyPermi('sso:client:list,sso:sync-task:add')")
    @GetMapping("/options")
    public AjaxResult options() {
        List<SsoClient> list = ssoClientService.selectDistributionClientOptions();
        List<SsoClientOptionView> viewList = new ArrayList<>(list.size());
        for (SsoClient ssoClient : list) {
            viewList.add(toOptionView(ssoClient));
        }
        return AjaxResult.success(viewList);
    }

    /**
     * 新增客户端。
     *
     * @param ssoClient 客户端信息
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('sso:client:add')")
    @Log(title = "客户端管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SsoClientCreateRequest request) {
        SsoClient ssoClient = toCreateEntity(request);
        ssoClient.setCreateBy(resolveOperator());
        SsoClientSecretIssueResult issueResult = ssoClientService.insertSsoClient(ssoClient);
        AjaxResult ajaxResult = AjaxResult.success("新增客户端成功");
        ajaxResult.put("clientId", issueResult.getClientId());
        ajaxResult.put("clientCode", issueResult.getClientCode());
        ajaxResult.put("clientSecret", issueResult.getClientSecret());
        return ajaxResult;
    }

    /**
     * 修改客户端。
     *
     * @param ssoClient 客户端信息
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('sso:client:edit')")
    @Log(title = "客户端管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SsoClientUpdateRequest request) {
        SsoClient ssoClient = toUpdateEntity(request);
        ssoClient.setUpdateBy(resolveOperator());
        return toAjax(ssoClientService.updateSsoClient(ssoClient));
    }

    /**
     * 轮换客户端密钥。
     *
     * @param clientId 客户端ID
     * @return 包含新密钥的响应体
     */
    @PreAuthorize("@ss.hasPermi('sso:client:edit')")
    @Log(title = "客户端管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{clientId}/rotate-secret")
    public AjaxResult rotateSecret(@PathVariable Long clientId) {
        AjaxResult ajaxResult = AjaxResult.success("轮换客户端密钥成功");
        ajaxResult.put("clientSecret", ssoClientService.rotateClientSecret(clientId));
        return ajaxResult;
    }

    /**
     * 修改客户端状态。
     *
     * @param clientId 客户端ID
     * @param request 请求体中的状态信息
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('sso:client:edit')")
    @Log(title = "客户端管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{clientId}/status")
    public AjaxResult changeStatus(@PathVariable Long clientId, @Validated @RequestBody SsoClientStatusUpdateRequest request) {
        return toAjax(ssoClientService.changeStatus(clientId, request.getStatus()));
    }

    /**
     * 解析当前操作人；缺少安全上下文时应直接失败，避免把审计字段静默写成 null。
     *
     * @return 当前操作人账号
     */
    private String resolveOperator() {
        return SecurityUtils.getUsername();
    }

    /**
     * 把创建请求转换为领域对象，避免控制器直接暴露 entity 写接口。
     *
     * @param request 创建请求
     * @return 领域对象
     */
    private SsoClient toCreateEntity(SsoClientCreateRequest request) {
        SsoClient ssoClient = new SsoClient();
        ssoClient.setClientCode(request.getClientCode());
        ssoClient.setClientName(request.getClientName());
        ssoClient.setRedirectUris(request.getRedirectUris());
        ssoClient.setAllowPasswordLogin(request.getAllowPasswordLogin());
        ssoClient.setAllowWxworkLogin(request.getAllowWxworkLogin());
        ssoClient.setSyncEnabled(request.getSyncEnabled());
        ssoClient.setStatus(request.getStatus());
        return ssoClient;
    }

    /**
     * 把更新请求转换为领域对象。
     *
     * @param request 更新请求
     * @return 领域对象
     */
    private SsoClient toUpdateEntity(SsoClientUpdateRequest request) {
        SsoClient ssoClient = new SsoClient();
        ssoClient.setClientId(request.getClientId());
        ssoClient.setClientCode(request.getClientCode());
        ssoClient.setClientName(request.getClientName());
        ssoClient.setRedirectUris(request.getRedirectUris());
        ssoClient.setAllowPasswordLogin(request.getAllowPasswordLogin());
        ssoClient.setAllowWxworkLogin(request.getAllowWxworkLogin());
        ssoClient.setSyncEnabled(request.getSyncEnabled());
        ssoClient.setStatus(request.getStatus());
        return ssoClient;
    }

    /**
     * 把 entity 映射为不含密钥的视图对象。
     *
     * @param ssoClient 领域对象
     * @return 列表视图
     */
    private SsoClientView toView(SsoClient ssoClient) {
        SsoClientView view = new SsoClientView();
        view.setClientId(ssoClient.getClientId());
        view.setClientCode(ssoClient.getClientCode());
        view.setClientName(ssoClient.getClientName());
        view.setRedirectUris(ssoClient.getRedirectUris());
        view.setAllowPasswordLogin(ssoClient.getAllowPasswordLogin());
        view.setAllowWxworkLogin(ssoClient.getAllowWxworkLogin());
        view.setSyncEnabled(ssoClient.getSyncEnabled());
        view.setStatus(ssoClient.getStatus());
        view.setCreateBy(ssoClient.getCreateBy());
        view.setCreateTime(ssoClient.getCreateTime());
        view.setUpdateBy(ssoClient.getUpdateBy());
        view.setUpdateTime(ssoClient.getUpdateTime());
        return view;
    }

    /**
     * 把 entity 映射为分发任务下拉选项视图。
     *
     * @param ssoClient 领域对象
     * @return 下拉选项视图
     */
    private SsoClientOptionView toOptionView(SsoClient ssoClient) {
        SsoClientOptionView view = new SsoClientOptionView();
        view.setClientCode(ssoClient.getClientCode());
        view.setClientName(ssoClient.getClientName());
        return view;
    }
}
