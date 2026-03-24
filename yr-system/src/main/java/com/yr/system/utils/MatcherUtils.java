/**
 * @file 模板占位符匹配工具，负责替换与提取 `${...}` 变量
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.utils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * description
 * </p>
 *
 * @author carl 2022-01-18 9:32
 * @version V1.0
 */
public class MatcherUtils {

    /** 占位符匹配模式，仅识别 `${...}`。 */
    private static final Pattern ESCAPE_PLACEHOLDER = Pattern.compile("\\$\\{([^}]*)\\}");

    /**
     * 解析模板中的占位符并替换为给定参数值。
     *
     * @param content 模板内容
     * @param kvs 占位符映射
     * @return 替换后的文本
     */
    public static String parse(String content, Map<String, String> kvs) {
        if (content == null) {
            throw new IllegalArgumentException("content 不能为空");
        }
        if (kvs == null) {
            throw new IllegalArgumentException("kvs 不能为空");
        }
        Matcher m = ESCAPE_PLACEHOLDER.matcher(content);
        StringBuilder resolved = new StringBuilder();
        while (m.find()) {
            String group = m.group();
            String replacement = kvs.get(group);
            if (replacement == null) {
                throw new IllegalArgumentException("未找到占位符映射: " + group);
            }
            m.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(resolved);
        return resolved.toString();
    }

    /**
     * 获取表达式中${}中的值
     *
     * @param content
     * @return
     */
    public static String resolve(String content) {
        Matcher matcher = ESCAPE_PLACEHOLDER.matcher(content);
        StringBuilder sql = new StringBuilder();
        while (matcher.find()) {
            sql.append("${").append(matcher.group(1)).append("},");
        }
        if (sql.length() > 0) {
            sql.deleteCharAt(sql.length() - 1);
        }
        return sql.toString();
    }
}
