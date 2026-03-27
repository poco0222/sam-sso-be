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
import com.yr.system.service.ISsoClientService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        return getDataTable(list);
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
    public AjaxResult add(@RequestBody SsoClient ssoClient) {
        ssoClient.setCreateBy(resolveOperator());
        return toAjax(ssoClientService.insertSsoClient(ssoClient));
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
    public AjaxResult edit(@RequestBody SsoClient ssoClient) {
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
     * @param ssoClient 请求体中的状态信息
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('sso:client:edit')")
    @Log(title = "客户端管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{clientId}/status")
    public AjaxResult changeStatus(@PathVariable Long clientId, @RequestBody SsoClient ssoClient) {
        return toAjax(ssoClientService.changeStatus(clientId, ssoClient.getStatus()));
    }

    /**
     * 解析当前操作人；缺少安全上下文时应直接失败，避免把审计字段静默写成 null。
     *
     * @return 当前操作人账号
     */
    private String resolveOperator() {
        return SecurityUtils.getUsername();
    }
}
