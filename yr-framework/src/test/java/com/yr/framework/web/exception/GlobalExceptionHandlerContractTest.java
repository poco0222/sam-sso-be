/**
 * @file 全局异常处理器契约测试
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.framework.web.exception;

import com.yr.common.constant.HttpStatus;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定全局异常处理器对已知业务异常和未知异常的对外语义。
 */
class GlobalExceptionHandlerContractTest {

    /** 待测异常处理器。 */
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /**
     * 验证业务异常仍保留业务消息。
     */
    @Test
    void shouldKeepBusinessMessageForCustomException() {
        AjaxResult result = handler.businessException(new CustomException("企业微信 corpId 配置缺失"));

        assertThat(result.get(AjaxResult.CODE_TAG)).isEqualTo(HttpStatus.ERROR);
        assertThat(result.get(AjaxResult.MSG_TAG)).isEqualTo("企业微信 corpId 配置缺失");
    }

    /**
     * 验证未知异常只返回统一受控文案，不泄露底层异常文本。
     */
    @Test
    void shouldHideUnexpectedExceptionMessage() {
        AjaxResult result = handler.handleException(new RuntimeException("jdbc password leaked"));

        assertThat(result.get(AjaxResult.CODE_TAG)).isEqualTo(HttpStatus.ERROR);
        assertThat(result.get(AjaxResult.MSG_TAG)).isEqualTo("系统繁忙，请稍后再试");
    }

    /**
     * 验证用户不存在错误不会再把用户名透传给前端。
     */
    @Test
    void shouldHideUsernameLookupDetails() {
        AjaxResult result = handler.handleUsernameNotFoundException(
                new UsernameNotFoundException("登录用户：admin 不存在")
        );

        assertThat(result.get(AjaxResult.MSG_TAG)).isEqualTo("账号或密码错误，请重新登录");
    }

    /**
     * 验证账号过期错误使用受控登录语义。
     */
    @Test
    void shouldReturnControlledMessageForExpiredAccount() {
        AjaxResult result = handler.handleAccountExpiredException(new AccountExpiredException("账号已过期"));

        assertThat(result.get(AjaxResult.MSG_TAG)).isEqualTo("登录状态已过期，请重新登录");
    }
}
