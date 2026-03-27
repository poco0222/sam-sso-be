package com.yr.framework.security.handle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yr.common.constant.Constants;
import com.yr.common.constant.HttpStatus;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.common.utils.ServletUtils;
import com.yr.common.utils.StringUtils;
import com.yr.framework.manager.AsyncManager;
import com.yr.framework.manager.factory.AsyncFactory;
import com.yr.framework.web.service.TokenService;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义退出处理类 返回成功
 *
 * @author Youngron
 */
@Component
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler {
    /** JSON 序列化器。 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Token 服务。 */
    private final TokenService tokenService;

    /**
     * 构造退出成功处理器。
     *
     * @param tokenService Token 服务
     */
    public LogoutSuccessHandlerImpl(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    /**
     * 退出处理
     *
     * @return
     */
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser)) {
            String userName = loginUser.getUsername();
            // 删除用户缓存记录
            tokenService.delLoginUser(loginUser.getToken());
            // 记录用户退出日志
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(userName, Constants.LOGOUT, "退出成功"));
        }
        ServletUtils.renderString(response, OBJECT_MAPPER.writeValueAsString(AjaxResult.error(HttpStatus.SUCCESS, "退出成功")));
    }
}
