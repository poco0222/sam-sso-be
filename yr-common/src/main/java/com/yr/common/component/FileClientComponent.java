package com.yr.common.component;

import com.yr.common.exception.CustomException;
import com.yr.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>
 * description
 * </p>
 *
 * @author Youngron 2021-12-29 9:50
 * @version V1.0
 */

@Component
public class FileClientComponent {

    /**
     * 文件保存路径
     */
    @Value("${file.constantPath:}")
    private String filePath;

    private IFileClientService fileClientService;

    public FileClientComponent(IFileClientService fileClientService) {
        this.fileClientService = fileClientService;
    }

    /**
     * 上传文件
     *
     * @param fileName    文件名称
     * @param inputStream 输入流
     */
    public void uploadFile(String fileName, InputStream inputStream) {
        this.uploadFileFinal(filePath, fileName, inputStream);
    }

    /**
     * 上传文件，指定后缀目录
     *
     * @param suffixPath  目录后缀
     * @param fileName    文件名称
     * @param inputStream 输入流
     */
    public void uploadFile(String suffixPath, String fileName, InputStream inputStream) {
        fileClientService.uploadFile(this.convertFilePath(suffixPath), fileName, inputStream);
    }

    /**
     * 下载文件
     *
     * @param fileName     文件名称
     * @param outputStream 输出流
     */
    public void downloadFile(String fileName, OutputStream outputStream) {
        this.downloadFileFinal(filePath, fileName, outputStream);
    }

    /**
     * 下载文件，指定后缀目录
     *
     * @param suffixPath   目录后缀
     * @param fileName     文件名称
     * @param outputStream 输出流
     */
    public void downloadFile(String suffixPath, String fileName, OutputStream outputStream) {
        this.downloadFileFinal(this.convertFilePath(suffixPath), fileName, outputStream);
    }

    /**
     * 删除文件
     *
     * @param fileName 文件名称
     */
    public void deleteFile(String fileName) {
        this.deleteFileFinal(filePath, fileName);
    }

    /**
     * 删除文件
     *
     * @param suffixPath 目录后缀
     * @param fileName   文件名称
     */
    public void deleteFile(String suffixPath, String fileName) {
        this.deleteFileFinal(this.convertFilePath(suffixPath), fileName);
    }

    private void uploadFileFinal(String filePath, String fileName, InputStream inputStream) {
        if (StringUtils.isBlank(fileName)) {
            throw new CustomException("文件名称不能为空");
        }
        if (inputStream == null) {
            throw new CustomException("inputStream 不能为空");
        }
        fileClientService.uploadFile(filePath, fileName, inputStream);
    }

    private void downloadFileFinal(String filePath, String fileName, OutputStream outputStream) {
        if (StringUtils.isBlank(fileName)) {
            throw new CustomException("文件名称不能为空");
        }
        if (outputStream == null) {
            throw new CustomException("outputStream 不能为空");
        }
        fileClientService.downloadFile(filePath, fileName, outputStream);
    }

    public void deleteFileFinal(String filePath, String fileName) {
        if (StringUtils.isBlank(fileName)) {
            throw new CustomException("文件名称不能为空");
        }
        fileClientService.deleteFile(filePath, fileName);
    }

    /**
     * 拼接文件路径
     *
     * @param suffixPath 路径后缀
     * @return 文件所在完整路径（不包括文件名称）
     */
    private String convertFilePath(String suffixPath) {
        if (StringUtils.isBlank(suffixPath)) {
            return filePath;
        }
        if (filePath.endsWith(IFileClientService.PATH_SPLIT) && suffixPath.startsWith(IFileClientService.PATH_SPLIT)) {
            suffixPath = suffixPath.substring(1);
        } else if (!filePath.endsWith(IFileClientService.PATH_SPLIT) && !suffixPath.startsWith(IFileClientService.PATH_SPLIT)) {
            suffixPath = IFileClientService.PATH_SPLIT + suffixPath;
        }
        return filePath + suffixPath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
