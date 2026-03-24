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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @file 通用文件控制器
 *
 * 该控制器仅保留模板项目所需的基础文件上传、下载与删除能力，
 * 已移除与 PDM、工程零件、MES 图片等业务模块强耦合的接口。
 */
@RestController
public class CommonController {
    private static final Logger log = LoggerFactory.getLogger(CommonController.class);

    /** 服务器访问地址配置。 */
    @Autowired
    private ServerConfig serverConfig;

    /**
     * 删除平台上传目录中的文件。
     *
     * @param fileName 文件访问路径
     * @return 统一返回结果
     */
    @GetMapping("/common/fileDelete")
    public AjaxResult fileDelete(@RequestParam(value = "fileName") String fileName) {
        try {
            FileUtils.deleteFile(YrConfig.getProfile() + StringUtils.substringAfter(fileName.trim(), Constants.RESOURCE_PREFIX));
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
                throw new Exception(StringUtils.format("文件名称({})非法，不允许下载。 ", fileName));
            }

            String filePath = YrConfig.getDownloadPath() + fileName;
            java.io.File file = new java.io.File(filePath);
            String realFileName = fileName.split("_").length == 7
                    ? fileName
                    : System.currentTimeMillis() + fileName.substring(fileName.indexOf("_") + 1);

            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            response.addHeader("Content-Length", Convert.toStr(file.length()));
            FileUtils.setAttachmentResponseHeader(response, realFileName);
            FileUtils.writeBytes(filePath, response.getOutputStream());
            if (Boolean.TRUE.equals(delete)) {
                FileUtils.deleteFile(filePath);
            }
        } catch (Exception exception) {
            log.error("下载文件失败", exception);
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
                throw new Exception(StringUtils.format("文件名称({})非法，不允许下载。 ", fileName));
            }

            String filePath = YrConfig.getTmplDownloadPath() + fileName;
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, fileName);
            FileUtils.writeBytes(filePath, response.getOutputStream());
        } catch (Exception exception) {
            log.error("下载模板文件失败", exception);
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
                throw new Exception(StringUtils.format("资源文件({})非法，不允许下载。 ", resource));
            }
            String localPath = YrConfig.getProfile();
            String downloadPath = localPath + StringUtils.substringAfter(resource, Constants.RESOURCE_PREFIX);
            String downloadName = StringUtils.substringAfterLast(downloadPath, "/");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
            FileUtils.setAttachmentResponseHeader(response, downloadName);
            FileUtils.writeBytes(downloadPath, response.getOutputStream());
        } catch (Exception exception) {
            log.error("下载资源文件失败", exception);
        }
    }
}
