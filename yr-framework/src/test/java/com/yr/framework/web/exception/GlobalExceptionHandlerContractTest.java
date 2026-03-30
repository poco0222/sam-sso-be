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
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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

    /**
     * 验证 BindException（绑定异常）应映射为 400 Bad Request（错误请求）。
     *
     * @author PopoY
     */
    @Test
    void shouldReturnBadRequestForBindException() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new ValidationRequest(), "request");
        bindingResult.rejectValue("clientCode", "NotBlank", "clientCode不能为空");
        BindException bindException = new BindException(bindingResult);

        ResponseEntity<AjaxResult> result = handler.validatedBindException(bindException);

        assertThat(result.getStatusCodeValue()).isEqualTo(400);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().get(AjaxResult.CODE_TAG)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(result.getBody().get(AjaxResult.MSG_TAG))).contains("clientCode不能为空");
    }

    /**
     * 验证 MethodArgumentNotValidException（方法参数校验异常）应映射为 400 Bad Request（错误请求）。
     *
     * @author PopoY
     * @throws NoSuchMethodException 反射方法签名不存在时抛出
     */
    @Test
    void shouldReturnBadRequestForMethodArgumentNotValidException() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new ValidationRequest(), "request");
        bindingResult.rejectValue("clientCode", "NotBlank", "clientCode不能为空");
        Method method = ValidationMethodSamples.class.getDeclaredMethod("submit", String.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<AjaxResult> result = handler.validExceptionHandler(exception);

        assertThat(result.getStatusCodeValue()).isEqualTo(400);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().get(AjaxResult.CODE_TAG)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(result.getBody().get(AjaxResult.MSG_TAG))).contains("clientCode不能为空");
    }

    /**
     * 验证 MethodArgumentNotValidException（方法参数校验异常）只有对象级错误时仍会返回受控 400。
     *
     * @author PopoY
     * @throws NoSuchMethodException 反射方法签名不存在时抛出
     */
    @Test
    void shouldFallbackToObjectErrorMessageWhenFieldErrorIsMissing() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new ValidationRequest(), "request");
        AtomicReference<ResponseEntity<AjaxResult>> resultRef = new AtomicReference<>();
        Method method = ValidationMethodSamples.class.getDeclaredMethod("submit", String.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);

        bindingResult.reject("InvalidClientDefinition", "clientCode与clientName组合不合法");
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        assertThatCode(() -> resultRef.set(handler.validExceptionHandler(exception)))
                .as("class-level validation 不应因为缺少 FieldError 而退化成 500")
                .doesNotThrowAnyException();
        assertThat(resultRef.get()).isNotNull();
        assertThat(resultRef.get().getStatusCodeValue()).isEqualTo(400);
        assertThat(resultRef.get().getBody()).isNotNull();
        assertThat(resultRef.get().getBody().get(AjaxResult.CODE_TAG)).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(String.valueOf(resultRef.get().getBody().get(AjaxResult.MSG_TAG))).contains("clientCode与clientName组合不合法");
    }

    /**
     * MethodArgumentNotValidException（方法参数校验异常）构造样例。
     *
     * @author PopoY
     */
    private static final class ValidationMethodSamples {

        /**
         * 占位方法，仅用于构造 MethodParameter（方法参数元数据）。
         *
         * @author PopoY
         * @param clientCode 客户端编码
         */
        @SuppressWarnings("unused")
        private void submit(String clientCode) {
            // author: PopoY，测试占位方法无需实现。
        }
    }

    /**
     * 参数校验请求体样例。
     *
     * @author PopoY
     */
    private static final class ValidationRequest {

        /** author: PopoY，客户端编码。 */
        private String clientCode;

        /**
         * @author PopoY
         * @return 客户端编码
         */
        @SuppressWarnings("unused")
        public String getClientCode() {
            return clientCode;
        }

        /**
         * @author PopoY
         * @param clientCode 客户端编码
         */
        @SuppressWarnings("unused")
        public void setClientCode(String clientCode) {
            this.clientCode = clientCode;
        }
    }
}
