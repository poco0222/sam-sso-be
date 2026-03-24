package com.yr.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yr.common.mybatisplus.entity.CustomEntity;

/**
 * 系统文件表(SysFile)表实体类
 *
 * @author Youngron
 * @since 2021-12-30 10:48:54
 */

@TableName("sys_file")
public class SysFile extends CustomEntity {

    /**
     * 表ID，主键
     */
    @TableId
    private Long id;

    /**
     * 附件ID,sys_attachment.id
     */
    private Long attachmentId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件虚拟路径
     */
    private String filePath;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 组织ID
     */
    private Long orgId;

    // 非数据库字段
    //----------------------------------

    /**
     * 附件目录编码
     */
    @TableField(exist = false)
    private String leafCode;

    /**
     * 附件目录
     */
    @TableField(exist = false)
    private String categoryName;

    /**
     * 业务主键
     */
    @TableField(exist = false)
    private String businessId;

    /**
     * 业务分类
     */
    @TableField(exist = false)
    private String businessType;

    /**
     * 允许上传的附件类型
     */
    @TableField(exist = false)
    private String allowedFileType;

    /**
     * 文件大小，常见格式
     */
    @TableField(exist = false)
    private String fileSizeDesc;

    /**
     * 附件目录ID
     */
    @TableField(exist = false)
    private Long attachCategoryId;

    // getter setter
    //----------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAttachmentId() {
        return attachmentId;
    }

    public void setAttachmentId(Long attachmentId) {
        this.attachmentId = attachmentId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public String getLeafCode() {
        return leafCode;
    }

    public void setLeafCode(String leafCode) {
        this.leafCode = leafCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getAllowedFileType() {
        return allowedFileType;
    }

    public void setAllowedFileType(String allowedFileType) {
        this.allowedFileType = allowedFileType;
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

    public String getFileSizeDesc() {
        return fileSizeDesc;
    }

    public void setFileSizeDesc(String fileSizeDesc) {
        this.fileSizeDesc = fileSizeDesc;
    }

    public Long getAttachCategoryId() {
        return attachCategoryId;
    }

    public void setAttachCategoryId(Long attachCategoryId) {
        this.attachCategoryId = attachCategoryId;
    }
}
