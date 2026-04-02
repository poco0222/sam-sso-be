package com.yr.common.component;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>
 * file文件操作接口
 * </p>
 *
 * @author PopoY 2021-12-23 17:07
 * @version V1.0
 */
public interface IFileClientService {

    String PATH_SPLIT = "/";

    String LOCAL_CHARSET = "UTF-8";

    /**
     * 上传文件
     *
     * @param filePath    文件所在目录
     * @param fileName    文件名
     * @param inputStream 输入流
     */
    void uploadFile(String filePath, String fileName, InputStream inputStream);

    /**
     * 上传文件
     *
     * @param filePath      文件所在目录
     * @param fileName      文件名
     * @param multipartFile MultipartFile 对象
     */
    void uploadFile(String filePath, String fileName, MultipartFile multipartFile);

    /**
     * 下载文件
     *
     * @param filePath     文件所在目录
     * @param fileName     文件名
     * @param outputStream 输出流
     */
    void downloadFile(String filePath, String fileName, OutputStream outputStream);

    /**
     * 删除文件
     *
     * @param filePath 文件所在目录
     * @param fileName 文件名
     */
    void deleteFile(String filePath, String fileName);

}
