package com.yr.system.mapper;

import com.yr.common.mybatisplus.custommapper.CustomMapper;
import com.yr.system.domain.entity.SysCodeRuleDetail;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 编码规则明细(SysCodeRuleDetail)表数据库访问层
 *
 * @author Youngron
 * @since 2021-10-29 19:53:14
 */
public interface SysCodeRuleDetailMapper extends CustomMapper<SysCodeRuleDetail> {

    /**
     * 根据规则编码、层级code和层级值查询规则明细
     *
     * @param ruleCode   规则编码
     * @param levelCode  层级code
     * @param levelValue 层级值
     * @return
     */
    List<SysCodeRuleDetail> listRuleCodeDetail(@Param("ruleCode") String ruleCode, @Param("levelCode") String levelCode, @Param("levelValue") String levelValue);

    /**
     * 更新序列信息
     *
     * @param codeRuleDetail
     * @return
     */
    int updateSeqNumber(SysCodeRuleDetail codeRuleDetail);
}
