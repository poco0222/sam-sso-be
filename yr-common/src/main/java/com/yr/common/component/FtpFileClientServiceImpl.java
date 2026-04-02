package com.yr.common.component;

import com.yr.common.config.BeanConfiguration;
import com.yr.common.exception.CustomException;
import com.yr.common.utils.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * <p>
 * ftp 工具类，使用ftp服务进行文件存储
 * {@link BeanConfiguration#ftpFileClient(org.springframework.core.env.Environment)}
 * </p>
 *
 * @author PopoY 2021-12-23 17:08
 * @version V1.0
 */
public class FtpFileClientServiceImpl implements IFileClientService {

    private final static Logger LOGGER = LoggerFactory.getLogger(FtpFileClientServiceImpl.class);

    private String host;
    private Integer port;
    private String username;
    private String password;

    @Override
    public void uploadFile(String filePath, String fileName, InputStream inputStream) {
        FTPClient ftpClient = this.createConnect();
        try {
            // 切换到上传目录
            if (!ftpClient.changeWorkingDirectory(this.convertCharset(filePath))) {
                // 如果目录不存在就创建目录
                String[] dirs = filePath.split(PATH_SPLIT);
                String tempPath = "";
                for (String dir : dirs) {
                    if (StringUtils.isBlank(dir)) {
                        continue;
                    }
                    tempPath += PATH_SPLIT + dir;
                    if (!ftpClient.changeWorkingDirectory(this.convertCharset(tempPath))) {
                        if (!ftpClient.makeDirectory(this.convertCharset(tempPath))) {
                            throw new CustomException("upload file to ftp server fail, can't make directory: " + tempPath);
                        } else {
                            ftpClient.changeWorkingDirectory(this.convertCharset(tempPath));
                        }
                    }
                }
            }
            // 上传
            if (!ftpClient.storeFile(this.convertCharset(fileName), inputStream)) {
                throw new CustomException("upload file to ftp server fail");
            }
        } catch (IOException e) {
            throw new CustomException("upload file to ftp server fail", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LOGGER.error("close InputStream fail");
                }
            }
        }
        this.closeConnect(ftpClient);
    }

    @Override
    public void uploadFile(String filePath, String fileName, MultipartFile multipartFile) {
        try {
            this.uploadFile(filePath, fileName, multipartFile.getInputStream());
        } catch (IOException e) {
            throw new CustomException("", e);
        }
    }

    @Override
    public void downloadFile(String filePath, String fileName, OutputStream outputStream) {
        FTPClient ftpClient = this.createConnect();
        try {
            if (StringUtils.isNotBlank(filePath)) {
                if (!ftpClient.changeWorkingDirectory(this.convertCharset(filePath))) {
                    throw new CustomException("download file fail, can't change working directory, file path: " + filePath);
                }
            }
            ftpClient.retrieveFile(this.convertCharset(fileName), outputStream);
        } catch (IOException e) {
            throw new CustomException("download file fail, ", e);
        } finally {
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
        FTPClient ftpClient = this.createConnect();
        try {
            if (StringUtils.isNotBlank(filePath)) {
                if (!ftpClient.changeWorkingDirectory(this.convertCharset(filePath))) {
                    throw new CustomException("delete file fail, can't change working directory, file path: " + filePath);
                }
            }
            ftpClient.deleteFile(this.convertCharset(fileName));
        } catch (IOException e) {
            LOGGER.error("delete file fail", e);
        }
        this.closeConnect(ftpClient);
    }

    /**
     * 转换编码为ftp默认编码
     *
     * @param str
     * @return
     */
    private String convertCharset(String str) {
        try {
            return new String(str.getBytes(LOCAL_CHARSET), "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new CustomException("转换编码失败");
        }
    }

    /**
     * 创建ftp服务连接
     *
     * @return
     */
    public FTPClient createConnect() {
        try {
            FTPClient ftpClient = new FTPClient();
            ftpClient.connect(host, port);
            ftpClient.login(username, password);
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                ftpClient.disconnect();
                throw new CustomException("connect ftp server fail");
            }
            // 设置编码格式
            ftpClient.setControlEncoding(LOCAL_CHARSET);
            // 设置被动模式
            ftpClient.enterLocalPassiveMode();
            // 设置二进制传输
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            return ftpClient;
        } catch (IOException e) {
            throw new CustomException("connect ftp server fail", e);
        }
    }

    /**
     * 关闭ftp连接
     *
     * @param ftpClient
     */
    private void closeConnect(FTPClient ftpClient) {
        try {
            if (ftpClient != null) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        } catch (IOException e) {
            LOGGER.error("close ftp server fail");
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
