/**
 * @file 消息模板服务实现，负责模板保存与参数占位符提取
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.common.utils.StringUtils;
import com.yr.system.domain.entity.SysMsgTemplate;
import com.yr.system.domain.vo.SysMsgTemplateVo;
import com.yr.system.mapper.SysMsgTemplateMapper;
import com.yr.system.service.ISysMsgTemplateService;
import com.yr.system.utils.MatcherUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author carl
 * @since 2022-01-04
 */
@Service
public class SysMsgTemplateService extends CustomServiceImpl<SysMsgTemplateMapper, SysMsgTemplate> implements ISysMsgTemplateService {

//    private static final Pattern ESCAPE_PLACEHOLDER = Pattern.compile("\\$\\{([^}]*)\\}");

    @Override
    public SysMsgTemplate saveMessageTemplate(SysMsgTemplate template) {
        if (!(StringUtils.isNotNull(template.getId()) && template.getId().longValue() > 0)) {
            // 新增时显式校验消息编码是否重复，避免依赖含副作用的 Optional.map 写法。
            if (get(template.getMsgCode()) != null) {
                throw new CustomException("消息编码已经存在，添加失败");
            }
        }
        //提取${}
        template.setMsgParams(getParameterEscape(template));
        saveOrUpdate(template);
        return template;
    }

    /**
     * 获取参数转义
     *
     * @return
     */
    private String getParameterEscape(SysMsgTemplate template) {
        List<String> parameterSegments = new ArrayList<>();
        // 按展示优先级提取占位符片段，最后统一拼接，避免手工删除尾逗号。
        appendResolvedSegment(parameterSegments, template.getMsgName());
        appendResolvedSegment(parameterSegments, template.getTitle());
        appendResolvedSegment(parameterSegments, template.getMsgContent());
        return String.join(",", parameterSegments);
    }

    /**
     * 把单个模板字段中的占位符片段追加到结果集合。
     *
     * @param parameterSegments 已解析的片段集合
     * @param templateField     模板字段值
     */
    private void appendResolvedSegment(List<String> parameterSegments, String templateField) {
        if (StringUtils.isBlank(templateField)) {
            return;
        }
        parameterSegments.add(MatcherUtils.resolve(templateField));
    }

    /**
     * 通过code获取消息模板
     *
     * @param msgName
     * @return
     */
    @Override
    public SysMsgTemplate get(String msgName) {
        return getOne(new LambdaQueryWrapper<SysMsgTemplate>()
                .eq(SysMsgTemplate::getMsgCode, msgName));
    }

    @Override
    public Integer del(String code) {
        SysMsgTemplate sysMsgTemplate = Optional.ofNullable(get(code)).orElseThrow(() -> new CustomException("删除失败，消息编码不存在"));
        removeById(sysMsgTemplate.getId());
        return 1;
    }

    @Override
    public IPage<SysMsgTemplate> pageList(SysMsgTemplateVo template) {
        return getBaseMapper().pageList(template);
    }
}
