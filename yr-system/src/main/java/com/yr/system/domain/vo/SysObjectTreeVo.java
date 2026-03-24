package com.yr.system.domain.vo;

import com.yr.common.core.domain.ObjectTree;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 树形结果返回
 * </p>
 *
 * @author carl 2022-01-06 16:10
 * @version V1.0
 */
public class SysObjectTreeVo implements Serializable {
    /**
     * 选中的ID
     */
    private List<Long> checkIds;

    private List<? extends ObjectTree> treeList;

    public List<Long> getCheckIds() {
        return checkIds;
    }

    public void setCheckIds(List<Long> checkIds) {
        this.checkIds = checkIds;
    }

    public List<? extends ObjectTree> getTreeList() {
        return treeList;
    }

    public void setTreeList(List<? extends ObjectTree> treeList) {
        this.treeList = treeList;
    }
}
