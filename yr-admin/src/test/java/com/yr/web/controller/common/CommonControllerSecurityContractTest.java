/**
 * @file 通用文件接口安全契约测试
 * @author PopoY
 * @date 2026-03-27
 */
package com.yr.web.controller.common;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定文件下载/删除接口的路径安全与权限边界。
 */
class CommonControllerSecurityContractTest {

    /** CommonController 源码路径。 */
    private static final Path COMMON_CONTROLLER_PATH = Path.of("src/main/java/com/yr/web/controller/common/CommonController.java");

    /** FileUtils 源码路径。 */
    private static final Path FILE_UTILS_PATH = Path.of("../yr-common/src/main/java/com/yr/common/utils/file/FileUtils.java");

    /** FileUploadUtils 源码路径。 */
    private static final Path FILE_UPLOAD_UTILS_PATH = Path.of("../yr-common/src/main/java/com/yr/common/utils/file/FileUploadUtils.java");

    /** SecurityConfig 源码路径。 */
    private static final Path SECURITY_CONFIG_PATH = Path.of("../yr-framework/src/main/java/com/yr/framework/config/SecurityConfig.java");

    /**
     * 验证删除接口不再以 GET 暴露，并声明显式权限。
     *
     * @throws NoSuchMethodException 方法签名变化时抛出
     */
    @Test
    void shouldRequireProtectedNonGetDeleteEndpoint() throws NoSuchMethodException {
        Method fileDeleteMethod = CommonController.class.getMethod("fileDelete", String.class);

        assertThat(fileDeleteMethod.getAnnotation(GetMapping.class))
                .as("fileDelete 不应继续使用 GET 语义")
                .isNull();
        assertThat(fileDeleteMethod.getAnnotation(DeleteMapping.class) != null
                || fileDeleteMethod.getAnnotation(PostMapping.class) != null)
                .as("fileDelete 必须改为 DELETE 或 POST")
                .isTrue();
        assertThat(fileDeleteMethod.getAnnotation(PreAuthorize.class))
                .as("fileDelete 必须声明显式权限控制")
                .isNotNull();
    }

    /**
     * 验证上传接口同样必须声明显式权限，避免仅依赖全局 authenticated（已认证）兜底。
     *
     * @throws NoSuchMethodException 方法签名变化时抛出
     */
    @Test
    void shouldRequireExplicitPermissionOnUploadEndpoints() throws NoSuchMethodException {
        assertUploadMethodProtected("uploadFile", org.springframework.web.multipart.MultipartFile.class);
        assertUploadMethodProtected("uploadFile2", org.springframework.web.multipart.MultipartFile.class);
        assertUploadMethodProtected("uploadFileWithParamName", org.springframework.web.multipart.MultipartFile.class);
    }

    /**
     * 验证文件删除与下载链路使用标准化路径解析并做根目录前缀校验。
     *
     * @throws IOException 读取源码失败时抛出
     */
    @Test
    void shouldNormalizeAndConstrainFilePathsUnderRootDirectory() throws IOException {
        String commonControllerSource = Files.readString(COMMON_CONTROLLER_PATH, StandardCharsets.UTF_8);
        String fileUtilsSource = Files.readString(FILE_UTILS_PATH, StandardCharsets.UTF_8);
        String fileUploadUtilsSource = Files.readString(FILE_UPLOAD_UTILS_PATH, StandardCharsets.UTF_8);

        assertThat(commonControllerSource).contains(".normalize()");
        assertThat(commonControllerSource).contains(".startsWith(");
        assertThat(fileUtilsSource).contains(".normalize()");
        assertThat(fileUtilsSource).contains(".startsWith(");
        assertThat(fileUploadUtilsSource).contains(".normalize()");
        assertThat(fileUploadUtilsSource).contains(".startsWith(");

        assertThat(commonControllerSource).doesNotContain("YrConfig.getProfile() +");
        assertThat(commonControllerSource).doesNotContain("YrConfig.getDownloadPath() + fileName");
        assertThat(commonControllerSource).doesNotContain("localPath + StringUtils.substringAfter");
        assertThat(fileUploadUtilsSource).doesNotContain("new File(uploadDir + File.separator + fileName)");
    }

    /**
     * 验证下载/删除失败不再静默吞异常，而是抛出受控错误。
     *
     * @throws IOException 读取源码失败时抛出
     */
    @Test
    void shouldFailWithControlledErrorsInsteadOfSilentLogging() throws IOException {
        String commonControllerSource = Files.readString(COMMON_CONTROLLER_PATH, StandardCharsets.UTF_8);

        assertThat(commonControllerSource).doesNotContain("log.error(\"下载文件失败\", exception);");
        assertThat(commonControllerSource).doesNotContain("log.error(\"下载模板文件失败\", exception);");
        assertThat(commonControllerSource).doesNotContain("log.error(\"下载资源文件失败\", exception);");
        assertThat(commonControllerSource).contains("throw new CustomException(\"下载文件失败\"");
        assertThat(commonControllerSource).contains("throw new CustomException(\"下载模板文件失败\"");
        assertThat(commonControllerSource).contains("throw new CustomException(\"下载资源文件失败\"");
        assertThat(commonControllerSource).doesNotContain("return AjaxResult.error(exception.getMessage())");
        assertThat(commonControllerSource).contains("throw new CustomException(\"上传文件失败\"");
        assertThat(commonControllerSource).contains("throw new CustomException(\"上传文件失败（兼容模式）\"");
        assertThat(commonControllerSource).contains("throw new CustomException(\"上传文件失败（指定参数名）\"");
    }

    /**
     * 验证通用下载入口不会继续以匿名方式暴露，避免文件接口绕过登录边界。
     *
     * @throws IOException 读取源码失败时抛出
     */
    @Test
    void shouldNotExposeCommonDownloadEndpointsAnonymously() throws IOException {
        String securityConfigSource = Files.readString(SECURITY_CONFIG_PATH, StandardCharsets.UTF_8);

        assertThat(securityConfigSource).doesNotContain(".antMatchers(\"/common/download**\").anonymous()");
        assertThat(securityConfigSource).doesNotContain(".antMatchers(\"/common/tmplDownload**\").anonymous()");
        assertThat(securityConfigSource).doesNotContain(".antMatchers(\"/common/download/resource**\").anonymous()");
    }

    /**
     * 断言上传方法声明了显式权限。
     *
     * @param methodName 方法名
     * @param parameterTypes 方法参数
     * @throws NoSuchMethodException 方法签名变化时抛出
     */
    private void assertUploadMethodProtected(String methodName,
                                             Class<?>... parameterTypes) throws NoSuchMethodException {
        Method uploadMethod = CommonController.class.getMethod(methodName, parameterTypes);

        assertThat(uploadMethod.getAnnotation(PreAuthorize.class))
                .as("%s 必须声明显式权限控制", methodName)
                .isNotNull();
    }
}
