/**
 * @file 文件下载 payload，承接 service 到 controller 的下载结果
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.domain.model;

/**
 * 文件下载结果对象。
 */
public class SysFileDownloadPayload {

    /**
     * 下载时展示给客户端的附件名称。
     */
    private String attachmentName;

    /**
     * 下载内容类型。
     */
    private String contentType;

    /**
     * 下载内容二进制。
     */
    private byte[] content;

    /**
     * 下载内容长度。
     */
    private long contentLength;

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }
}
