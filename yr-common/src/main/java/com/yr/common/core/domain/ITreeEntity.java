package com.yr.common.core.domain;

/**
 * <p>
 * description
 * </p>
 *
 * @author carl 2022-01-06 14:12
 * @version V1.0
 */
public interface ITreeEntity {
    /**
     * 获取id
     *
     * @return
     */
    Long getId();

    /**
     * 获取父类Id
     *
     * @return
     */
    Long getParentId();

    /**
     * 获取名称
     *
     * @return
     */
    String getLabel();
}
