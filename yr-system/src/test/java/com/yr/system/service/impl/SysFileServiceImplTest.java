/**
 * @file 验证 SysFileServiceImpl 在附件目录编码重复时的失败优先级与分层边界
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.component.FileClientComponent;
import com.yr.common.core.domain.entity.SysAttachCategory;
import com.yr.system.domain.entity.SysAttachment;
import com.yr.system.domain.model.SysFileDownloadPayload;
import com.yr.system.domain.model.SysFileUploadCommand;
import com.yr.system.domain.entity.SysFile;
import com.yr.system.mapper.SysFileMapper;
import com.yr.system.service.ISysAttachCategoryService;
import com.yr.system.service.ISysAttachmentService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SysFileServiceImpl 附件上传契约测试。
 */
class SysFileServiceImplTest {

    /** yr-system 模块根目录，用于稳定读取源码。 */
    private static final Path MODULE_ROOT = locateModuleRoot();

    /**
     * 验证命中重复 leafCode 时会优先抛出明确业务异常，而不是继续执行后续文件校验。
     *
     * @throws Exception 当上传过程抛出受检异常时透传给断言
     */
    @Test
    void shouldFailFastWhenLeafCodeMatchesMultipleCategories() throws Exception {
        ISysAttachCategoryService attachCategoryService = mock(ISysAttachCategoryService.class);
        when(attachCategoryService.list(any())).thenReturn(List.of(category(1L), category(2L)));
        when(attachCategoryService.getOne(any())).thenReturn(category(1L));

        SysFileServiceImpl service = new SysFileServiceImpl(
                mock(SysFileMapper.class),
                attachCategoryService,
                mock(ISysAttachmentService.class),
                mock(FileClientComponent.class),
                "/tmp/upload"
        );

        assertThatThrownBy(() -> service.uploadFile(buildUploadCommand("evidence.pdf", "application/octet-stream")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("附件目录编码存在重复");
    }

    /**
     * 验证实现类不再直接依赖 MultipartFile / HttpServletResponse，避免把 Web 传输细节留在 service 层。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldKeepWebSpecificTypesOutOfServiceImplementation() throws IOException {
        String serviceSource = Files.readString(
                MODULE_ROOT.resolve("src/main/java/com/yr/system/service/impl/SysFileServiceImpl.java"),
                StandardCharsets.UTF_8
        );

        assertThat(serviceSource).doesNotContain("MultipartFile");
        assertThat(serviceSource).doesNotContain("HttpServletResponse");
        assertThat(serviceSource).contains("SysFileUploadCommand");
        assertThat(serviceSource).contains("SysFileDownloadPayload");
    }

    /**
     * 验证下载附件时会返回 transport-agnostic payload，并保留附件名称与内容类型。
     *
     * @throws Exception 当下载过程抛出受检异常时透传给断言
     */
    @Test
    void shouldBuildDownloadPayloadFromStoredAttachmentMetadata() throws Exception {
        ISysAttachCategoryService attachCategoryService = mock(ISysAttachCategoryService.class);
        ISysAttachmentService attachmentService = mock(ISysAttachmentService.class);
        FileClientComponent fileClientComponent = mock(FileClientComponent.class);
        SysFileServiceImpl service = new SysFileServiceImpl(
                mock(SysFileMapper.class),
                attachCategoryService,
                attachmentService,
                fileClientComponent,
                "/tmp/upload"
        );
        SysFileServiceImpl spyService = org.mockito.Mockito.spy(service);

        SysFile sysFile = new SysFile();
        sysFile.setAttachmentId(11L);
        sysFile.setFileName("stored.pdf");
        sysFile.setFileType("application/pdf");

        SysAttachment sysAttachment = new SysAttachment();
        sysAttachment.setId(11L);
        sysAttachment.setCategoryId(9L);
        sysAttachment.setAttachName("合同.pdf");

        SysAttachCategory attachCategory = new SysAttachCategory();
        attachCategory.setId(9L);
        attachCategory.setLeafCode("DOC");

        doReturn(sysFile).when(spyService).getById(55L);
        when(attachmentService.getById(11L)).thenReturn(sysAttachment);
        when(attachCategoryService.getById(9L)).thenReturn(attachCategory);
        doAnswer(invocation -> {
            OutputStream outputStream = invocation.getArgument(2);
            outputStream.write("phase3-download".getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(fileClientComponent).downloadFile(eq("DOC"), eq("stored.pdf"), any(OutputStream.class));

        SysFileDownloadPayload payload = spyService.downloadFileByFileId(55L);

        assertThat(payload.getAttachmentName()).isEqualTo("合同.pdf");
        assertThat(payload.getContentType()).isEqualTo("application/pdf");
        assertThat(new String(payload.getContent(), StandardCharsets.UTF_8)).isEqualTo("phase3-download");
        assertThat(payload.getContentLength()).isEqualTo(payload.getContent().length);
    }

    /**
     * 构造满足上传前置校验的附件目录，便于聚焦 leafCode 重复分支。
     *
     * @param id 目录主键
     * @return 附件目录
     */
    private SysAttachCategory category(Long id) {
        SysAttachCategory category = new SysAttachCategory();
        category.setId(id);
        category.setLeafCode("DUP_CODE");
        category.setAllowedFileType(".pdf");
        category.setAllowedFileSize(0L);
        return category;
    }

    /**
     * 构造最小可用的上传命令，便于聚焦 service 层校验逻辑。
     *
     * @param originalFilename 原始文件名
     * @param contentType 内容类型
     * @return 上传命令
     */
    private SysFileUploadCommand buildUploadCommand(String originalFilename, String contentType) {
        SysFileUploadCommand command = new SysFileUploadCommand();
        command.setInputStream(new ByteArrayInputStream("phase3".getBytes(StandardCharsets.UTF_8)));
        command.setOriginalFilename(originalFilename);
        command.setContentType(contentType);
        command.setFileSize("phase3".getBytes(StandardCharsets.UTF_8).length);
        command.setLeafCode("DUP_CODE");
        command.setBusinessId("biz-1");
        command.setBusinessType("BIZ");
        return command;
    }

    /**
     * 基于测试类的 CodeSource 回溯定位 Maven 模块根目录。
     *
     * @return 模块根目录
     */
    private static Path locateModuleRoot() {
        try {
            Path codeSourcePath = Paths.get(SysFileServiceImplTest.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            Path currentPath = Files.isDirectory(codeSourcePath) ? codeSourcePath : codeSourcePath.getParent();
            while (currentPath != null) {
                if (Files.exists(currentPath.resolve("pom.xml"))) {
                    return currentPath;
                }
                currentPath = currentPath.getParent();
            }
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("无法定位 yr-system 模块根目录", exception);
        }
        throw new IllegalStateException("无法定位 yr-system 模块根目录");
    }
}
