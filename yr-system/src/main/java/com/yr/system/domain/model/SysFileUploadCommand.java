/**
 * @file 文件上传命令，承接 controller 到 service 的上传参数
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.domain.model;

import java.io.InputStream;

/**
 * 文件上传命令对象。
 */
public class SysFileUploadCommand {

    /**
     * 上传文件输入流。
     */
    private InputStream inputStream;

    /**
     * 原始文件名称。
     */
    private String originalFilename;

    /**
     * 文件 MIME 类型。
     */
    private String contentType;

    /**
     * 文件大小。
     */
    private long fileSize;

    /**
     * 附件目录编码。
     */
    private String leafCode;

    /**
     * 业务主键。
     */
    private String businessId;

    /**
     * 业务类型。
     */
    private String businessType;

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getLeafCode() {
        return leafCode;
    }

    public void setLeafCode(String leafCode) {
        this.leafCode = leafCode;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }
}
