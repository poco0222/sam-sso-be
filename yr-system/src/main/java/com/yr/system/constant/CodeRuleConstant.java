package com.yr.system.constant;

import com.yr.common.exception.CustomException;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-10-28 11:05
 * @description 编码规则常量
 */
public class CodeRuleConstant {

    /**
     * 生成缓存编码规则数据到redis的key
     *
     * @param ruleCode   编码规则CODE
     * @param levelCode  层级CODE
     * @param levelValue 层级值，这个值除了levelCode为CUSTOM时是会变的，剩下的都和levelCode一样
     * @return
     */
    public static String generateCacheKey(String ruleCode, String levelCode, String levelValue) {
        if (LevelCode.CUSTOM.equals(levelCode)) {
            return "sys_code_rule:" + ruleCode + "." + levelCode + "." + levelValue;
        } else {
            return "sys_code_rule:" + ruleCode + "." + levelCode + "." + levelCode;
        }
    }

    /**
     * 生成失败的key，从redis和数据库都没获取到编码规则信息时，存入redis。redis的value固定为1
     *
     * @param ruleCode   编码规则CODE
     * @param levelCode  层级CODE
     * @param levelValue 层级值，这个值除了levelCode为CUSTOM时是会变的，剩下的都和levelCode一样
     * @return
     */
    public static String generateFailFastKey(String ruleCode, String levelCode, String levelValue) {
        if (LevelCode.CUSTOM.equals(levelCode)) {
            return "sys_code_rule:fail-fast:" + ruleCode + "." + levelCode + "." + levelValue;
        } else {
            return "sys_code_rule:fail-fast:" + ruleCode + "." + levelCode + "." + levelCode;
        }
    }

    /**
     * 生成序列的key，redis的value为当前的序列值
     *
     * @param ruleCode   编码规则CODE
     * @param levelCode  层级CODE
     * @param levelValue 层级值
     * @return
     */
    public static String generateSequenceKey(String ruleCode, String levelCode, String levelValue) {
        return "sys_code_rule:sequence:" + ruleCode + "." + levelCode + "." + levelValue;
    }

    /**
     * 生成已使用的key，主要是判断编码行是否已经在使用当中，redis的value固定为1
     *
     * @param ruleCode   编码规则CODE
     * @param levelCode  层级CODE
     * @param levelValue 层级值，这个值除了levelCode为CUSTOM时是会变的，剩下的都和levelCode一样
     * @return
     */
    public static String generateUsedKey(String ruleCode, String levelCode, String levelValue) {
        if (LevelCode.CUSTOM.equals(levelCode)) {
            return "sys_code_rule:used:" + ruleCode + "." + levelCode + "." + levelValue;
        } else {
            return "sys_code_rule:used:" + ruleCode + "." + levelCode + "." + levelCode;
        }
    }

    /**
     * 编码规则应用层级
     */
    public static class LevelCode {
        /**
         * 全局级
         */
        public static final String GLOBAL = "GLOBAL";
        /**
         * 组织
         */
        public static final String ORGANIZATION = "ORG";
        /**
         * 部门
         */
        public static final String DEPARTMENT = "DEPARTMENT";
        /**
         * 自定义
         */
        public static final String CUSTOM = "CUSTOM";

        private LevelCode() {
        }

        /**
         * 判断字符串是不是合法的层级
         *
         * @param levelCode 层级
         */
        public static void contains(String levelCode) {
            if (GLOBAL.equals(levelCode) || ORGANIZATION.equals(levelCode) || DEPARTMENT.equals(levelCode) || CUSTOM.equals(levelCode)) {
                return;
            }
            throw new CustomException("levelCode : " + levelCode + " , must be in [GLOBAL, ORG, DEPARTMENT, CUSTOM]");
        }
    }

    /**
     * 重置频率
     */
    public static class ResetFrequency {
        /**
         * 从不
         */
        public static final String NEVER = "NEVER";
        /**
         * 每年
         */
        public static final String YEAR = "YEAR";
        /**
         * 每季
         */
        public static final String QUARTER = "QUARTER";
        /**
         * 每月
         */
        public static final String MONTH = "MONTH";
        /**
         * 每天
         */
        public static final String DAY = "DAY";

        private ResetFrequency() {
        }
    }

    /**
     * 编码规则段类型
     */
    public static class FieldType {
        /**
         * 序列
         */
        public static final String SEQUENCE = "SEQUENCE";
        /**
         * 常量
         */
        public static final String CONSTANT = "CONSTANT";
        /**
         * 日期
         */
        public static final String DATE = "DATE";
        /**
         * 变量
         */
        public static final String VARIABLE = "VARIABLE";
        /**
         * uuid
         */
        public static final String UUID = "UUID";

        private FieldType() {
        }
    }

    public static class UsedFlag {
        public static final String YES = "1";
        public static final String NO = "0";
    }

    public static class EnabledFlag {
        public static final String ENABLED = "1";
        public static final String DISABLED = "0";
    }

}
