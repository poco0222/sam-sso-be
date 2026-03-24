package com.yr.common.component;

import com.yr.common.config.BeanConfiguration;
import com.yr.common.exception.CustomException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;

/**
 * <p>
 * 默认的实现类，使用本地存储
 * {@link BeanConfiguration#defaultFileClient(org.springframework.core.env.Environment)}
 * </p>
 *
 * @author Youngron 2021-12-29 11:44
 * @version V1.0
 */
public class DefaultFileClientServiceImpl implements IFileClientService {

    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultFileClientServiceImpl.class);

    @Override
    public void uploadFile(String filePath, String fileName, InputStream inputStream) {
        try {
            File file = new File(filePath + File.separator + fileName);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            FileUtils.copyInputStreamToFile(inputStream, file);
        } catch (IOException e) {
            throw new CustomException("上传文件失败", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LOGGER.error("close InputStream fail");
                }
            }
        }
    }

    @Override
    public void uploadFile(String filePath, String fileName, MultipartFile multipartFile) {
        try {
            File file = new File(filePath + File.separator + fileName);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            multipartFile.transferTo(file);
        } catch (IOException e) {
            throw new CustomException("上传文件失败", e);
        }
    }

    @Override
    public void downloadFile(String filePath, String fileName, OutputStream outputStream) {
        FileInputStream fileInputStream = null;
        try {
            if (!filePath.endsWith(PATH_SPLIT)) {
                filePath += PATH_SPLIT;
            }
            File file = new File(filePath + fileName);
            if (!file.exists()) {
                throw new CustomException("下载的文件不存在: " + filePath + fileName);
            }
            fileInputStream = new FileInputStream(file);
            IOUtils.copyLarge(fileInputStream, outputStream);
        } catch (IOException e) {
            throw new CustomException("下载文件失败", e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    LOGGER.error("close FileInputStream fail");
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    LOGGER.error("close OutputStream fail");
                }
            }
        }
    }

    @Override
    public void deleteFile(String filePath, String fileName) {
        try {
            if (!filePath.endsWith(PATH_SPLIT)) {
                filePath += PATH_SPLIT;
            }
            File file = new File(filePath + fileName);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            throw new CustomException("删除文件失败", e);
        }
    }
}
