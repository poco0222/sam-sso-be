package com.yr.web.controller.common;

import com.yr.common.config.YrConfig;
import com.yr.common.constant.Constants;
import com.yr.common.core.domain.AjaxResult;
import com.yr.common.core.text.Convert;
import com.yr.common.exception.CustomException;
import com.yr.common.utils.StringUtils;
import com.yr.common.utils.file.FileUploadUtils;
import com.yr.common.utils.file.FileUtils;
import com.yr.framework.config.ServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * @file 通用文件控制器
 *
 * 该控制器仅保留模板项目所需的基础文件上传、下载与删除能力，
 * 已移除与 PDM、工程零件、MES 图片等业务模块强耦合的接口。
 */
@RestController
public class CommonController {
    /** 服务器访问地址配置。 */
    @Autowired
    private ServerConfig serverConfig;

    /**
     * 删除平台上传目录中的文件。
     *
     * @param fileName 文件访问路径
     * @return 统一返回结果
     */
    @PreAuthorize("@ss.hasPermi('common:file:remove')")
    @DeleteMapping("/common/fileDelete")
    public AjaxResult fileDelete(@RequestParam(value = "fileName") String fileName) {
        try {
            Path targetPath = resolveProfileResourcePath(fileName);
            if (!FileUtils.deleteFile(targetPath.toString())) {
                throw new IOException("目标文件不存在");
            }
        } catch (Exception exception) {
            throw new CustomException("删除文件失败", exception);
        }
        return AjaxResult.success("ok");
    }

    /**
     * 下载通用导出文件。
     *
     * @param fileName 文件名称
     * @param delete 下载后是否删除源文件
     * @param response HTTP 响应
     * @param request HTTP 请求
     */
    @GetMapping("common/download")
    public void fileDownload(String fileName, Boolean delete, HttpServletResponse response, HttpServletRequest request) {
        try {
            if (!FileUtils.checkAllowDownload(fileName)) {
                throw new IOException(StringUtils.format("文件名称({})非法，不允许下载。 ", fileName));
            }

            Path filePath = resolveUnderRoot(YrConfig.getDownloadPath(), fileName);
            File file = filePath.toFile();
            String realFileName = fileName.split("_").length == 7
                    ? fileName
                    : System.currentTimeMillis() + fileName.substring(fileName.indexOf("_") + 1);

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.addHeader("Content-Length", Convert.toStr(file.length()));
            FileUtils.setAttachmentResponseHeader(response, realFileName);
            FileUtils.writeBytes(filePath.toString(), response.getOutputStream());
            if (Boolean.TRUE.equals(delete)) {
                FileUtils.deleteFile(filePath.toString());
            }
        } catch (Exception exception) {
            throw new CustomException("下载文件失败", exception);
        }
    }

    /**
     * 下载模板文件。
     *
     * @param fileName 模板文件名
     * @param response HTTP 响应
     * @param request HTTP 请求
     */
    @GetMapping("common/tmplDownload")
    public void fileTmplDownload(String fileName, HttpServletResponse response, HttpServletRequest request) {
        try {
            if (!FileUtils.checkAllowDownload(fileName)) {
                throw new IOException(StringUtils.format("文件名称({})非法，不允许下载。 ", fileName));
            }

            Path filePath = resolveUnderRoot(YrConfig.getTmplDownloadPath(), fileName);
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, fileName);
            FileUtils.writeBytes(filePath.toString(), response.getOutputStream());
        } catch (Exception exception) {
            throw new CustomException("下载模板文件失败", exception);
        }
    }

    /**
     * 通用上传请求。
     *
     * @param file 上传文件
     * @return 包含文件地址的返回结果
     * @throws Exception 上传异常
     */
    @PostMapping("/common/upload")
    public AjaxResult uploadFile(MultipartFile file) throws Exception {
        try {
            String filePath = YrConfig.getUploadPath();
            String fileName = FileUploadUtils.upload(filePath, file);
            String url = serverConfig.getUrl() + fileName;
            AjaxResult ajax = AjaxResult.success();
            ajax.put("fileName", fileName);
            ajax.put("url", url);
            return ajax;
        } catch (Exception exception) {
            return AjaxResult.error(exception.getMessage());
        }
    }

    /**
     * 通用上传请求（兼容旧版上传工具）。
     *
     * @param file 上传文件
     * @return 包含文件地址的返回结果
     * @throws Exception 上传异常
     */
    @PostMapping("/common/upload2")
    public AjaxResult uploadFile2(MultipartFile file) throws Exception {
        try {
            String filePath = YrConfig.getUploadPath();
            String fileName = FileUploadUtils.upload2(filePath, file);
            String url = serverConfig.getUrl() + fileName;
            AjaxResult ajax = AjaxResult.success();
            ajax.put("fileName", fileName);
            ajax.put("url", url);
            return ajax;
        } catch (Exception exception) {
            return AjaxResult.error(exception.getMessage());
        }
    }

    /**
     * 通用上传请求，兼容指定参数名的表单。
     *
     * @param file 上传文件
     * @return 包含文件地址的返回结果
     * @throws Exception 上传异常
     */
    @PostMapping("/common/upload-param")
    public AjaxResult uploadFileWithParamName(@RequestParam("uploadFile") MultipartFile file) throws Exception {
        try {
            String filePath = YrConfig.getUploadPath();
            if ("bpmn".equals(FileUploadUtils.getExtension(file))) {
                // 流程定义文件统一放在独立目录，便于模板项目复用。
                filePath += "/bpmnFile";
            }
            String fileName = FileUploadUtils.upload(filePath, file);
            String url = serverConfig.getUrl() + fileName;
            AjaxResult ajax = AjaxResult.success();
            ajax.put("fileName", fileName);
            ajax.put("url", url);
            return ajax;
        } catch (Exception exception) {
            return AjaxResult.error(exception.getMessage());
        }
    }

    /**
     * 下载本地资源目录下的文件。
     *
     * @param resource 资源路径
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @throws Exception 下载异常
     */
    @GetMapping("/common/download/resource")
    public void resourceDownload(String resource, HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            if (!FileUtils.checkAllowDownload(resource)) {
                throw new IOException(StringUtils.format("资源文件({})非法，不允许下载。 ", resource));
            }
            Path downloadPath = resolveProfileResourcePath(resource);
            String downloadName = downloadPath.getFileName().toString();
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, downloadName);
            FileUtils.writeBytes(downloadPath.toString(), response.getOutputStream());
        } catch (Exception exception) {
            throw new CustomException("下载资源文件失败", exception);
        }
    }

    /**
     * 在给定根目录下解析并校验文件路径，避免目录穿越。
     *
     * @param rootDir 根目录
     * @param relativePath 相对路径
     * @return 安全的目标路径
     * @throws Exception 路径非法时抛出
     */
    private Path resolveUnderRoot(String rootDir, String relativePath) throws Exception {
        Path normalizedRoot = Path.of(rootDir).toAbsolutePath().normalize();
        Path targetPath = FileUtils.resolveSecurePath(rootDir, relativePath).normalize();
        if (!targetPath.startsWith(normalizedRoot)) {
            throw new IOException("文件路径非法");
        }
        return targetPath;
    }

    /**
     * 解析 `/profile/**` 资源路径到上传根目录下的真实文件。
     *
     * @param resourcePath 前端传入的资源路径
     * @return 安全的目标路径
     * @throws Exception 路径非法时抛出
     */
    private Path resolveProfileResourcePath(String resourcePath) throws Exception {
        if (!StringUtils.contains(resourcePath, Constants.RESOURCE_PREFIX)) {
            throw new IOException("文件路径非法");
        }
        String relativePath = StringUtils.substringAfter(resourcePath.trim(), Constants.RESOURCE_PREFIX);
        relativePath = StringUtils.removeStart(relativePath, "/");
        Path profileRoot = Path.of(YrConfig.getProfile()).toAbsolutePath().normalize();
        Path targetPath = profileRoot.resolve(relativePath).normalize();
        if (!targetPath.startsWith(profileRoot)) {
            throw new IOException("文件路径非法");
        }
        return targetPath;
    }
}
