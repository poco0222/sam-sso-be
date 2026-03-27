/**
 * @file 锁定 CaptchaController 的一期配置边界
 * @author PopoY
 * @date 2026-03-26
 */
package com.yr.web.controller.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CaptchaController 一期契约测试。
 */
class CaptchaControllerContractTest {

    /** CaptchaController 源码路径。 */
    private static final Path SOURCE_PATH = Path.of("src/main/java/com/yr/web/controller/common/CaptchaController.java");

    /**
     * 验证验证码开关改为读取 application 配置，而不是系统参数服务。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldReadCaptchaSwitchFromApplicationProperty() throws IOException {
        String source = Files.readString(SOURCE_PATH);

        assertThat(source).contains("@Value(\"${yr.captcha.enabled:true}\")");
        assertThat(source).doesNotContain("ISysConfigService");
        assertThat(source).doesNotContain("selectCaptchaOnOff()");
    }

    /**
     * 验证验证码图片生成失败时返回受控错误文案，而不是直接透传底层异常消息。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldReturnControlledMessageWhenCaptchaImageGenerationFails() throws IOException {
        String source = Files.readString(SOURCE_PATH);

        assertThat(source).doesNotContain("AjaxResult.error(e.getMessage())");
        assertThat(source).contains("AjaxResult.error(\"生成验证码失败\")");
    }
}
