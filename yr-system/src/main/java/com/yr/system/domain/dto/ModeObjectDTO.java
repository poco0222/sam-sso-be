/**
 * @file ModeObjectDTO 模式对象传输对象
 * @author Codex
 * @date 2026-03-11
 */
package com.yr.system.domain.dto;

import java.io.Serializable;

/**
 * 模式对象基础信息。
 *
 * @param id 对象 ID
 * @param name 对象名称
 */
public record ModeObjectDTO(Long id, String name) implements Serializable {

    /**
     * 兼容既有 Java Bean 读取方式。
     *
     * @return 对象 ID
     */
    public Long getId() {
        return id;
    }

    /**
     * 兼容既有 Java Bean 读取方式。
     *
     * @return 对象名称
     */
    public String getName() {
        return name;
    }

    /**
     * 保持既有日志输出格式。
     *
     * @return 字符串表示
     */
    @Override
    public String toString() {
        return "ModeObjectDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
