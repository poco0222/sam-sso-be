package com.yr.common.core.domain;

import org.apache.commons.compress.utils.Lists;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 任何对象转tree
 * </p>
 *
 * @author carl 2022-01-06 13:48
 * @version V1.0
 */
public class ObjectTree implements Serializable {
    /**
     * id名称
     */
    private Long id;

    /**
     * 节点名称
     */
    private String label;

    /**
     * 子节点
     */
    private List<ObjectTree> children;

    private boolean checked = true;

    public ObjectTree() {
    }

    /**
     * 单个没有子类的情况Tree
     *
     * @param entity
     */
    public ObjectTree(ITreeEntity entity) {
        this.id = entity.getId();
        this.label = entity.getLabel();
        children = Lists.newArrayList();
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<ObjectTree> getChildren() {
        if (null == children) {
            children = Lists.newArrayList();
        }
        return children;
    }

    public void setChildren(List<ObjectTree> children) {
        this.children = children;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
