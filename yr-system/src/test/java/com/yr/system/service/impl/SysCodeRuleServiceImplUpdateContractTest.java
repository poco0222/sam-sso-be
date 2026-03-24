/**
 * @file 验证编码规则头更新会合并旧实体字段，避免 partial payload 覆盖已有数据
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.system.domain.entity.SysCodeRule;
import com.yr.system.mapper.SysCodeRuleMapper;
import com.yr.system.service.ISysCodeRuleDetailService;
import com.yr.system.service.ISysCodeRuleLineService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 编码规则头更新契约测试。
 */
class SysCodeRuleServiceImplUpdateContractTest {

    /**
     * 验证更新编码规则时会保留旧实体中未在命令里显式提供的字段。
     */
    @Test
    void shouldPreserveExistingHeaderFieldsWhenUpdatingCodeRuleWithPartialPayload() {
        SysCodeRuleMapper mapper = mock(SysCodeRuleMapper.class);
        when(mapper.selectById(101L)).thenReturn(existingCodeRule());
        when(mapper.updateById(any(SysCodeRule.class))).thenReturn(1);

        SysCodeRuleServiceImpl service = new SysCodeRuleServiceImpl(
                mock(ISysCodeRuleLineService.class),
                mock(ISysCodeRuleDetailService.class),
                mock(HashOperations.class),
                mock(RedisTemplate.class)
        );
        injectCustomMapper(service, mapper);

        SysCodeRule command = new SysCodeRule();
        command.setRuleId(101L);
        command.setRuleCode("RULE-001");
        command.setRuleName("新的规则名称");

        service.insertOrUpdateCodeRule(command);

        ArgumentCaptor<SysCodeRule> codeRuleCaptor = ArgumentCaptor.forClass(SysCodeRule.class);
        verify(mapper).updateById(codeRuleCaptor.capture());
        SysCodeRule persistedRule = codeRuleCaptor.getValue();
        assertThat(persistedRule.getRuleName()).isEqualTo("新的规则名称");
        assertThat(persistedRule.getDescription()).isEqualTo("旧描述");
        assertThat(persistedRule.getOrgId()).isEqualTo(9L);
    }

    /**
     * @return 已存在的编码规则头
     */
    private SysCodeRule existingCodeRule() {
        SysCodeRule codeRule = new SysCodeRule();
        codeRule.setRuleId(101L);
        codeRule.setRuleCode("RULE-001");
        codeRule.setRuleName("旧规则名称");
        codeRule.setDescription("旧描述");
        codeRule.setOrgId(9L);
        return codeRule;
    }

    /**
     * 为继承 MyBatis-Plus 基类的服务补齐 mapper 注入。
     *
     * @param target 服务对象
     * @param mapper mapper 桩对象
     */
    private void injectCustomMapper(Object target, Object mapper) {
        injectField(ServiceImpl.class, target, "baseMapper", mapper);
        injectField(CustomServiceImpl.class, target, "baseMapper", mapper);
    }

    /**
     * 通过反射注入字段，保证测试可以复用真实继承链逻辑。
     *
     * @param owner 声明字段的类
     * @param target 目标对象
     * @param fieldName 字段名
     * @param value 字段值
     */
    private void injectField(Class<?> owner, Object target, String fieldName, Object value) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("无法注入字段: " + fieldName, exception);
        }
    }
}
