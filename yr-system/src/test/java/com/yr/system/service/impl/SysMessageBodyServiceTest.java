/**
 * @file 锁定消息主体分页实现与 Mapper SQL 契约
 * @author Codex
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yr.system.domain.entity.SysMessageBody;
import com.yr.system.domain.vo.message.MessageVo;
import com.yr.system.mapper.SysMessageBodyMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SysMessageBodyService 行为测试。
 */
class SysMessageBodyServiceTest {

    /**
     * 验证分页查询会委托给 Mapper，而不是返回 null。
     */
    @Test
    void shouldDelegatePageListToMapperInsteadOfReturningNull() {
        SysMessageBodyMapper mapper = mock(SysMessageBodyMapper.class);
        SysMessageBodyService service = new SysMessageBodyService(mapper);
        MessageVo request = buildRequest();
        IPage<SysMessageBody> expectedPage = new Page<>(request.getPage(), request.getRows());
        when(mapper.pageList(request)).thenReturn(expectedPage);

        IPage<SysMessageBody> actualPage = service.pageList(request);

        assertThat(actualPage).isSameAs(expectedPage);
    }

    /**
     * 验证 Mapper XML 已为分页查询提供实际 SQL，而不是空节点。
     *
     * @throws IOException 读取 XML 失败
     */
    @Test
    void shouldDefinePageListSqlInMapperXml() throws IOException {
        String xmlContent = Files.readString(Path.of("src/main/resources/mapper/system/SysMessageBodyMapper.xml"));

        assertThat(xmlContent)
                .contains("<select id=\"pageList\"")
                .contains("from sys_message_body");
    }

    /**
     * 构造最小分页请求对象。
     *
     * @return 分页请求
     */
    private MessageVo buildRequest() {
        MessageVo request = new MessageVo();
        request.setPage(1);
        request.setRows(10);
        request.setTitle("系统通知");
        return request;
    }
}
