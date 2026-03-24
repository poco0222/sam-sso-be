package com.yr.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.mybatisplus.service.ICustomService;
import com.yr.system.domain.entity.SysMsgTemplate;
import com.yr.system.domain.vo.SysMsgTemplateVo;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author carl
 * @since 2022-01-04
 */
public interface ISysMsgTemplateService extends ICustomService<SysMsgTemplate> {

    /**
     * 保存消息模块
     *
     * @param template
     * @return
     */
    SysMsgTemplate saveMessageTemplate(SysMsgTemplate template);

    /**
     * 分页查询
     *
     * @param template
     * @return
     */
    IPage<SysMsgTemplate> pageList(SysMsgTemplateVo template);

    /**
     * 通过名称获取消息模板
     *
     * @param msgName
     * @return
     */
    SysMsgTemplate get(String msgName);

    /**
     * 通过ID删除
     *
     * @param id
     * @return
     */
    Integer del(String id);
}
