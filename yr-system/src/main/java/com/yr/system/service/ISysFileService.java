/**
 * @file 文件服务接口，收口上传命令与下载 payload 的传输无关契约
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.mybatisplus.service.ICustomService;
import com.yr.system.domain.entity.SysFile;
import com.yr.system.domain.model.SysFileDownloadPayload;
import com.yr.system.domain.model.SysFileUploadCommand;

import java.io.IOException;

/**
 * 系统文件表(SysFile)表服务接口
 *
 * @author Youngron
 * @since 2021-12-30 10:48:54
 */
public interface ISysFileService extends ICustomService<SysFile> {

    /**
     * 分页查询附件
     *
     * @param page
     * @param sysFile
     * @return
     */
    IPage<SysFile> pageByCondition(IPage<SysFile> page, SysFile sysFile);

    /**
     * 上传附件
     *
     * @param command 文件上传命令
     * @throws IOException
     */
    void uploadFile(SysFileUploadCommand command) throws IOException;

    /**
     * 根据附件ID下载附件
     *
     * @param fileId   附件ID
     * @return 下载 payload
     * @throws IOException
     */
    SysFileDownloadPayload downloadFileByFileId(Long fileId) throws IOException;

    /**
     * 根据附件ID删除附件
     *
     * @param fileIds 附件ID
     */
    void deleteFileByFileIds(Long[] fileIds);
}
