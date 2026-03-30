package com.yr.common.utils.file;

import com.yr.common.utils.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * 文件处理工具类
 *
 * @author Youngron
 */
public class FileUtils {
    /** 文件工具日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    public static String FILENAME_PATTERN = "[a-zA-Z0-9_\\-\\|\\.\\u4e00-\\u9fa5]+";

    /**
     * 输出指定文件的byte数组
     *
     * @param filePath 文件路径
     * @param os       输出流
     * @return
     */
    public static void writeBytes(String filePath, OutputStream os) throws IOException {
        FileInputStream fis = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new FileNotFoundException(filePath);
            }
            fis = new FileInputStream(file);
            byte[] b = new byte[1024];
            int length;
            while ((length = fis.read(b)) > 0) {
                os.write(b, 0, length);
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e1) {
                    LOGGER.warn("关闭输出流失败，filePath={}", filePath, e1);
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e1) {
                    LOGGER.warn("关闭文件输入流失败，filePath={}", filePath, e1);
                }
            }
        }
    }

    /**
     * 删除文件
     *
     * @param filePath 文件
     * @return
     */
    public static boolean deleteFile(String filePath) {
        boolean flag = false;
        File file = new File(filePath);
        // 路径为文件且不为空则进行删除
        if (file.isFile() && file.exists()) {
            file.delete();
            flag = true;
        }
        return flag;
    }

    /**
     * 文件名称验证
     *
     * @param filename 文件名称
     * @return true 正常 false 非法
     */
    public static boolean isValidFilename(String filename) {
        return filename.matches(FILENAME_PATTERN);
    }

    /**
     * 检查文件是否可下载
     *
     * @param resource 需要下载的文件
     * @return true 正常 false 非法
     */
    public static boolean checkAllowDownload(String resource) {
        Path normalizedResource;
        try {
            normalizedResource = Path.of(resource).normalize();
        } catch (InvalidPathException exception) {
            return false;
        }

        if (normalizedResource.isAbsolute()) {
            return false;
        }

        if (normalizedResource.startsWith("..")) {
            return false;
        }

        String fileName = normalizedResource.getFileName() == null
                ? StringUtils.EMPTY
                : normalizedResource.getFileName().toString();
        if (!isValidFilename(fileName)) {
            return false;
        }

        // 检查允许下载的文件规则
        if (ArrayUtils.contains(MimeTypeUtils.DEFAULT_ALLOWED_EXTENSION, FileTypeUtils.getFileType(fileName))) {
            return true;
        }

        // 不在允许下载的文件规则
        return false;
    }

    /**
     * 在给定根目录下解析并校验相对路径，防止目录穿越与绝对路径逃逸。
     *
     * @param rootDir 根目录
     * @param relativePath 相对路径
     * @return 标准化后的目标路径
     * @throws IOException 路径非法时抛出
     */
    public static Path resolveSecurePath(String rootDir, String relativePath) throws IOException {
        Path normalizedRoot = Path.of(rootDir).toAbsolutePath().normalize();
        Path normalizedRelative;
        try {
            normalizedRelative = Path.of(relativePath).normalize();
        } catch (InvalidPathException exception) {
            throw new IOException("文件路径非法", exception);
        }

        if (normalizedRelative.isAbsolute() || normalizedRelative.startsWith("..")) {
            throw new IOException("文件路径非法");
        }

        Path targetPath = normalizedRoot.resolve(normalizedRelative).normalize();
        if (!targetPath.startsWith(normalizedRoot)) {
            throw new IOException("文件路径非法");
        }
        return targetPath;
    }

    /**
     * 下载文件名重新编码
     *
     * @param request  请求对象
     * @param fileName 文件名
     * @return 编码后的文件名
     */
    public static String setFileDownloadHeader(HttpServletRequest request, String fileName) throws UnsupportedEncodingException {
        final String agent = request.getHeader("USER-AGENT");
        String filename = fileName;
        if (agent.contains("MSIE")) {
            // IE浏览器
            filename = URLEncoder.encode(filename, "utf-8");
            filename = filename.replace("+", " ");
        } else if (agent.contains("Firefox")) {
            // 火狐浏览器
            filename = new String(fileName.getBytes(), "ISO8859-1");
        } else if (agent.contains("Chrome")) {
            // google浏览器
            filename = URLEncoder.encode(filename, "utf-8");
        } else {
            // 其它浏览器
            filename = URLEncoder.encode(filename, "utf-8");
        }
        return filename;
    }

    /**
     * 下载文件名重新编码
     *
     * @param response     响应对象
     * @param realFileName 真实文件名
     * @return
     */
    public static void setAttachmentResponseHeader(HttpServletResponse response, String realFileName) throws UnsupportedEncodingException {
        String percentEncodedFileName = percentEncode(realFileName);

        StringBuilder contentDispositionValue = new StringBuilder();
        contentDispositionValue.append("attachment; filename=")
                .append(percentEncodedFileName)
                .append(";")
                .append("filename*=")
                .append("utf-8''")
                .append(percentEncodedFileName);

        response.setHeader("Content-disposition", contentDispositionValue.toString());
    }

    /**
     * 百分号编码工具方法
     *
     * @param s 需要百分号编码的字符串
     * @return 百分号编码后的字符串
     */
    public static String percentEncode(String s) throws UnsupportedEncodingException {
        String encode = URLEncoder.encode(s, StandardCharsets.UTF_8.toString());
        return encode.replaceAll("\\+", "%20");
    }
}
