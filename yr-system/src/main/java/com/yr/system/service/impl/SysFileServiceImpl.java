/**
 * @file 文件服务实现，移除 Web transport 类型并返回传输无关的下载结果
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.component.FileClientComponent;
import com.yr.common.core.domain.entity.SysAttachCategory;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.common.utils.FormatUtil;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.StringUtils;

import com.yr.system.domain.entity.SysAttachment;
import com.yr.system.domain.entity.SysFile;
import com.yr.system.domain.model.SysFileDownloadPayload;
import com.yr.system.domain.model.SysFileUploadCommand;
import com.yr.system.mapper.SysFileMapper;
import com.yr.system.service.ISysAttachCategoryService;
import com.yr.system.service.ISysAttachmentService;
import com.yr.system.service.ISysFileService;
import com.yr.system.utils.UUIDUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 系统文件表(SysFile)表服务实现类
 *
 * @author Youngron
 * @since 2021-12-30 10:48:54
 */
@Service
public class SysFileServiceImpl extends CustomServiceImpl<SysFileMapper, SysFile> implements ISysFileService {

    private final SysFileMapper sysFileMapper;

    private final ISysAttachCategoryService sysAttachCategoryService;

    private final ISysAttachmentService sysAttachmentService;

    private final FileClientComponent fileClientComponent;

    private final String fileUploadPath;

    public SysFileServiceImpl(SysFileMapper sysFileMapper,
                              ISysAttachCategoryService sysAttachCategoryService,
                              ISysAttachmentService sysAttachmentService,
                              FileClientComponent fileClientComponent,
                              @Value("${file.constantPath:}") String fileUploadPath) {
        this.sysFileMapper = sysFileMapper;
        this.sysAttachCategoryService = sysAttachCategoryService;
        this.sysAttachmentService = sysAttachmentService;
        this.fileClientComponent = fileClientComponent;
        this.fileUploadPath = fileUploadPath;
    }

    @Override
    public IPage<SysFile> pageByCondition(IPage<SysFile> page, SysFile sysFile) {
        IPage<SysFile> pageData = sysFileMapper.pageByCondition(page, sysFile);
        if (CollectionUtils.isNotEmpty(pageData.getRecords())) {
            pageData.getRecords().forEach(item -> item.setFileSizeDesc(FormatUtil.formatFileSize(item.getFileSize())));
        }
        return pageData;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadFile(SysFileUploadCommand command) throws IOException {
        if (command == null) {
            throw new CustomException("附件不能为空");
        }
        InputStream inputStream = command.getInputStream();
        if (inputStream == null) {
            throw new CustomException("附件不能为空");
        }
        String leafCode = command.getLeafCode();
        if (StringUtils.isBlank(leafCode)) {
            throw new CustomException("附件目录编码不能为空");
        }
        String businessId = command.getBusinessId();
        if (StringUtils.isBlank(businessId)) {
            throw new CustomException("业务主键不能为空");
        }
        // 校验业务主键长度
        if (businessId.length() > 40) {
            throw new CustomException("业务主键不能超过40个字符");
        }

        LambdaQueryWrapper<SysAttachCategory> sysAttachCategoryQueryWrapper = new LambdaQueryWrapper<>();
        sysAttachCategoryQueryWrapper.eq(SysAttachCategory::getLeafCode, leafCode);
        // 显式处理 0/1/多条结果，避免历史脏数据下继续依赖 getOne 的不稳定语义。
        List<SysAttachCategory> attachCategories = sysAttachCategoryService.list(sysAttachCategoryQueryWrapper);
        if (CollectionUtils.isEmpty(attachCategories)) {
            throw new CustomException("未找到附件目录：" + leafCode);
        }
        if (attachCategories.size() > 1) {
            throw new CustomException("附件目录编码存在重复，请联系管理员修复数据：" + leafCode);
        }
        SysAttachCategory attachCategory = attachCategories.get(0);

        String originalFilename = command.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename)) {
            throw new CustomException("文件名称不能为空");
        }
        int index = originalFilename.lastIndexOf(".");
        if (index == -1) {
            throw new CustomException("仅允许上传 " + attachCategory.getAllowedFileType() + " 格式文件。");
        }
        // 校验文件名称长度
        if (originalFilename.length() > 100) {
            throw new CustomException("文件名称不能超过100个字符");
        }
        String fileSuffix = originalFilename.substring(index + 1);
        // 校验文件后缀
        String[] allowedFileTypeArr = attachCategory.getAllowedFileType().replaceAll(",", "").split("\\.");
        boolean includeFlag = false;
        for (String allowedFileType : allowedFileTypeArr) {
            if (allowedFileType.equals(fileSuffix)) {
                includeFlag = true;
                break;
            }
        }
        if (!includeFlag) {
            throw new CustomException("仅允许上传 " + attachCategory.getAllowedFileType() + " 格式文件。");
        }
        // 校验文件大小 20971520 = 20M
        long fileSize = command.getFileSize();
        if ((!attachCategory.getAllowedFileSize().equals(0L) && fileSize > attachCategory.getAllowedFileSize())
                || fileSize > 20971520) {
            throw new CustomException("文件大小超出最大值");
        }

        String businessType = command.getBusinessType();
        Long orgId = SecurityUtils.getOrgId();
        SysAttachment sysAttachment = new SysAttachment();
        sysAttachment.setCategoryId(attachCategory.getId());
        sysAttachment.setAttachName(originalFilename);
        sysAttachment.setBusinessId(businessId);
        sysAttachment.setBusinessType(businessType);
        sysAttachment.setOrgId(orgId);
        sysAttachmentService.save(sysAttachment);

        String fileName = UUIDUtils.getUUID(32) + originalFilename.substring(index);
        SysFile sysFile = new SysFile();
        sysFile.setAttachmentId(sysAttachment.getId());
        sysFile.setFileName(fileName);
        sysFile.setFilePath(fileUploadPath + "/" + leafCode);
        sysFile.setFileSize(fileSize);
        sysFile.setFileType(command.getContentType());
        sysFile.setOrgId(orgId);
        this.save(sysFile);

        try (inputStream) {
            fileClientComponent.uploadFile(leafCode, fileName, inputStream);
        }
    }

    @Override
    public SysFileDownloadPayload downloadFileByFileId(Long fileId) throws IOException {
        SysFile sysFile = this.getById(fileId);
        if (sysFile == null) {
            throw new CustomException("未找到附件");
        }
        SysAttachment sysAttachment = sysAttachmentService.getById(sysFile.getAttachmentId());
        if (sysAttachment == null) {
            throw new CustomException("未找到附件");
        }
        SysAttachCategory sysAttachCategory = sysAttachCategoryService.getById(sysAttachment.getCategoryId());
        if (sysAttachCategory == null) {
            throw new CustomException("未找到附件目录");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        fileClientComponent.downloadFile(sysAttachCategory.getLeafCode(), sysFile.getFileName(), outputStream);

        SysFileDownloadPayload payload = new SysFileDownloadPayload();
        payload.setAttachmentName(sysAttachment.getAttachName());
        payload.setContentType(StringUtils.isBlank(sysFile.getFileType()) ? "application/octet-stream" : sysFile.getFileType());
        payload.setContent(outputStream.toByteArray());
        payload.setContentLength(payload.getContent().length);
        return payload;
    }

    @Override
    public void deleteFileByFileIds(Long[] fileIds) {
        if (fileIds == null || fileIds.length <= 0) {
            return;
        }
        for (Long fileId : fileIds) {
            SysFile sysFile = this.getById(fileId);
            if (sysFile == null) {
                continue;
            }
            SysAttachment sysAttachment = sysAttachmentService.getById(sysFile.getAttachmentId());
            if (sysAttachment == null) {
                continue;
            }
            SysAttachCategory sysAttachCategory = sysAttachCategoryService.getById(sysAttachment.getCategoryId());
            if (sysAttachCategory == null) {
                continue;
            }

            fileClientComponent.deleteFile(sysAttachCategory.getLeafCode(), sysFile.getFileName());

            this.removeById(fileId);
            sysAttachmentService.removeById(sysFile.getAttachmentId());
        }
    }
}
