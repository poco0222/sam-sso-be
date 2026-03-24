/**
 * @file 锁定 CodeRuleUtils 局部生成逻辑的单元测试
 * @author Codex
 * @date 2026-03-11
 */
package com.yr.system.utils;

import com.yr.system.constant.CodeRuleConstant;
import com.yr.system.domain.entity.SysCodeRuleDetail;
import com.yr.system.service.ISysCodeRuleDetailService;
import com.yr.system.service.ISysCodeRuleLineService;
import com.yr.system.service.ISysCodeRuleService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * CodeRuleUtils 的局部行为测试。
 */
class CodeRuleUtilsTest {

    /** 被测试对象。 */
    private final CodeRuleUtils codeRuleUtils = new CodeRuleUtils(
            mock(ISysCodeRuleService.class),
            mock(ISysCodeRuleLineService.class),
            mock(ISysCodeRuleDetailService.class),
            mock(RedisTemplate.class)
    );

    /**
     * 验证常量段与变量段会按排序结果依次拼接。
     */
    @Test
    void shouldConcatenateConstantAndVariableFieldsInOrder() {
        List<SysCodeRuleDetail> detailList = List.of(
                buildDetail(2, CodeRuleConstant.FieldType.CONSTANT, "-YR-", null),
                buildDetail(1, CodeRuleConstant.FieldType.VARIABLE, "name", null)
        );

        String generatedCode = ReflectionTestUtils.invokeMethod(
                codeRuleUtils,
                "generate",
                "RULE",
                CodeRuleConstant.LevelCode.GLOBAL,
                CodeRuleConstant.LevelCode.GLOBAL,
                Map.of("name", "SYS"),
                detailList
        );

        assertThat(generatedCode).isEqualTo("SYS-YR-");
    }

    /**
     * 验证变量缺失时会回退为空串，而不是抛出异常。
     */
    @Test
    void shouldUseEmptyStringWhenVariableMissing() {
        List<SysCodeRuleDetail> detailList = List.of(
                buildDetail(1, CodeRuleConstant.FieldType.CONSTANT, "PRE-", null),
                buildDetail(2, CodeRuleConstant.FieldType.VARIABLE, "missing", null)
        );

        String generatedCode = ReflectionTestUtils.invokeMethod(
                codeRuleUtils,
                "generate",
                "RULE",
                CodeRuleConstant.LevelCode.GLOBAL,
                CodeRuleConstant.LevelCode.GLOBAL,
                Collections.emptyMap(),
                detailList
        );

        assertThat(generatedCode).isEqualTo("PRE-");
    }

    /**
     * 验证未知字段类型不会抛异常，并保持现有拼接行为。
     */
    @Test
    void shouldKeepDefaultBranchBehaviorForUnknownFieldType() {
        List<SysCodeRuleDetail> detailList = List.of(
                buildDetail(1, CodeRuleConstant.FieldType.CONSTANT, "PRE-", null),
                buildDetail(2, "UNKNOWN", "ignored", null)
        );

        String generatedCode = ReflectionTestUtils.invokeMethod(
                codeRuleUtils,
                "generate",
                "RULE",
                CodeRuleConstant.LevelCode.GLOBAL,
                CodeRuleConstant.LevelCode.GLOBAL,
                Collections.emptyMap(),
                detailList
        );

        assertThat(generatedCode).isEqualTo("PRE-null");
    }

    /**
     * 构造最小可用的规则明细对象。
     *
     * @param orderSeq 生成顺序
     * @param fieldType 字段类型
     * @param fieldValue 字段值
     * @param seqLength 序列长度
     * @return 规则明细对象
     */
    private SysCodeRuleDetail buildDetail(int orderSeq, String fieldType, String fieldValue, Long seqLength) {
        SysCodeRuleDetail codeRuleDetail = new SysCodeRuleDetail();
        codeRuleDetail.setOrderSeq(orderSeq);
        codeRuleDetail.setFieldType(fieldType);
        codeRuleDetail.setFieldValue(fieldValue);
        codeRuleDetail.setSeqLength(seqLength);
        return codeRuleDetail;
    }
}
