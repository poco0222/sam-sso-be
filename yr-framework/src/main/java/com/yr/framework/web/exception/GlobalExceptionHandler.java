/**
 * @file 全局异常处理器，统一输出受控错误语义
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.framework.web.exception;

import com.yr.common.constant.HttpStatus;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.exception.BaseException;
import com.yr.common.exception.CustomException;
import com.yr.common.exception.DemoModeException;
import com.yr.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    /** 通用未知异常提示语，避免把内部细节直接暴露给前端。 */
    private static final String GENERIC_ERROR_MESSAGE = "系统繁忙，请稍后再试";

    /** 用户名或密码错误时的统一登录提示。 */
    private static final String INVALID_LOGIN_MESSAGE = "账号或密码错误，请重新登录";

    /** 登录态过期时的统一提示。 */
    private static final String LOGIN_EXPIRED_MESSAGE = "登录状态已过期，请重新登录";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 基础异常
     */
    @ExceptionHandler(BaseException.class)
    public AjaxResult baseException(BaseException e) {
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(CustomException.class)
    public AjaxResult businessException(CustomException e) {
        if (StringUtils.isNull(e.getCode())) {
            return AjaxResult.error(e.getMessage());
        }
        return AjaxResult.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public AjaxResult handlerNoFoundException(Exception e) {
        log.error(e.getMessage(), e);
        return AjaxResult.error(HttpStatus.NOT_FOUND, "路径不存在，请检查路径是否正确");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public AjaxResult handleAuthorizationException(AccessDeniedException e) {
        log.error(e.getMessage());
        return AjaxResult.error(HttpStatus.FORBIDDEN, "没有权限，请联系管理员授权");
    }

    @ExceptionHandler(AccountExpiredException.class)
    public AjaxResult handleAccountExpiredException(AccountExpiredException e) {
        log.error(e.getMessage(), e);
        return AjaxResult.error(LOGIN_EXPIRED_MESSAGE);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public AjaxResult handleUsernameNotFoundException(UsernameNotFoundException e) {
        log.error(e.getMessage(), e);
        return AjaxResult.error(INVALID_LOGIN_MESSAGE);
    }

    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e) {
        log.error(e.getMessage(), e);
        return AjaxResult.error(GENERIC_ERROR_MESSAGE);
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<AjaxResult> validatedBindException(BindException e) {
        log.error(e.getMessage(), e);
        String message = resolveValidationMessage(e.getBindingResult());
        return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                .body(AjaxResult.error(HttpStatus.BAD_REQUEST, message));
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AjaxResult> validExceptionHandler(MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e);
        String message = resolveValidationMessage(e.getBindingResult());
        return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                .body(AjaxResult.error(HttpStatus.BAD_REQUEST, message));
    }

    /**
     * 从 BindingResult（绑定结果）中解析最合适的校验错误消息。
     *
     * @author PopoY
     * @param bindingResult Spring 校验绑定结果
     * @return 可对外暴露的受控错误消息
     */
    private String resolveValidationMessage(BindingResult bindingResult) {
        FieldError fieldError;
        ObjectError objectError;

        if (bindingResult == null) {
            return "请求参数校验失败";
        }
        fieldError = bindingResult.getFieldError();
        if (fieldError != null && fieldError.getDefaultMessage() != null && !fieldError.getDefaultMessage().isBlank()) {
            return fieldError.getDefaultMessage();
        }
        objectError = bindingResult.getAllErrors().isEmpty() ? null : bindingResult.getAllErrors().get(0);
        if (objectError != null && objectError.getDefaultMessage() != null && !objectError.getDefaultMessage().isBlank()) {
            return objectError.getDefaultMessage();
        }
        return "请求参数校验失败";
    }

    /**
     * 演示模式异常
     */
    @ExceptionHandler(DemoModeException.class)
    public AjaxResult demoModeException(DemoModeException e) {
        return AjaxResult.error("演示模式，不允许操作");
    }
}
