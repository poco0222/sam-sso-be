/**
 * @file 附件目录服务实现，负责目录树维护与叶子节点校验
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yr.common.core.domain.TreeSelect;
import com.yr.common.core.domain.entity.SysAttachCategory;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.mapper.SysAttachCategoryMapper;
import com.yr.system.service.ISysAttachCategoryService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 附件目录表(SysAttachCategory)表服务实现类
 *
 * @author Youngron
 * @since 2021-12-30 10:41:35
 */
@Service
public class SysAttachCategoryServiceImpl extends CustomServiceImpl<SysAttachCategoryMapper, SysAttachCategory> implements ISysAttachCategoryService {

    @Override
    public void updateOrInsert(SysAttachCategory sysAttachCategory) {
        if (sysAttachCategory.getId() == null) {
            if (sysAttachCategory.getParentId() != null) {
                SysAttachCategory parent = this.getById(sysAttachCategory.getParentId());
                if (parent == null) {
                    throw new CustomException("未找到上级节点数据");
                }
                if (SysAttachCategory.LEAF_CATEGORY.equals(parent.getLeafFlag())) {
                    throw new CustomException("叶子节点不能新增下级节点");
                }
                sysAttachCategory.setAncestors(parent.getAncestors() + "," + parent.getId());
                sysAttachCategory.setCategoryLevel(parent.getCategoryLevel() + 1);
                sysAttachCategory.setOrgId(parent.getOrgId());
            } else {
                sysAttachCategory.setParentId(0L);
                sysAttachCategory.setAncestors("0");
                sysAttachCategory.setCategoryLevel(1);
                sysAttachCategory.setOrgId(SecurityUtils.getOrgId());
            }
        }
        // 叶子节点
        if (SysAttachCategory.LEAF_CATEGORY.equals(sysAttachCategory.getLeafFlag())) {
            if (StringUtils.isBlank(sysAttachCategory.getLeafCode())) {
                throw new CustomException("叶子节点编码不能为空");
            }
            // 叶子节点编码唯一校验
            if (sysAttachCategory.getId() == null) {
                LambdaQueryWrapper<SysAttachCategory> queryWrapper = new LambdaQueryWrapper<SysAttachCategory>()
                        .eq(SysAttachCategory::getLeafCode, sysAttachCategory.getLeafCode());
                long count = this.count(queryWrapper);
                if (count > 0) {
                    throw new CustomException("叶子节点编码已存在");
                }
            }
            if (StringUtils.isBlank(sysAttachCategory.getAllowedFileType())) {
                throw new CustomException("附件类型不能为空");
            }
            if (sysAttachCategory.getAllowedFileSize() == null) {
                sysAttachCategory.setAllowedFileSize(0L);
            }
        }
        boolean result;
        if (sysAttachCategory.getId() == null) {
            result = this.save(sysAttachCategory);
        } else {
            SysAttachCategory updateEntity = new SysAttachCategory();
            updateEntity.setId(sysAttachCategory.getId());
            updateEntity.setCategoryName(sysAttachCategory.getCategoryName());
            updateEntity.setDescription(sysAttachCategory.getDescription());
            updateEntity.setLeafDictCode(sysAttachCategory.getLeafDictCode());
            updateEntity.setAllowedFileType(sysAttachCategory.getAllowedFileType());
            updateEntity.setAllowedFileSize(sysAttachCategory.getAllowedFileSize());
            result = this.updateById(updateEntity);
        }
        if (!result) {
            throw new CustomException("新增/更新失败");
        }
    }

    @Override
    public List<TreeSelect> buildTreeList(SysAttachCategory sysAttachCategory) {
        List<SysAttachCategory> list = this.list();
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        List<SysAttachCategory> treeList = this.buildTree(list);
        // 组织树转为 id + label树
        return treeList.stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    private List<SysAttachCategory> buildTree(List<SysAttachCategory> list) {
        List<SysAttachCategory> returnList = new ArrayList<>();
        List<Long> allIdList = list.stream().map(SysAttachCategory::getId).collect(Collectors.toList());
        for (SysAttachCategory attachCategory : list) {
            // 如果是顶级节点, 遍历该父节点的所有子节点，这里不能用getParentId() == 0来判断，因为list里的数据的顶级节点的父级ID不一定是0
            if (!allIdList.contains(attachCategory.getParentId())) {
                recursionFn(list, attachCategory);
                returnList.add(attachCategory);
            }
        }
        if (returnList.isEmpty()) {
            returnList = list;
        }
        return returnList;
    }

    private void recursionFn(List<SysAttachCategory> list, SysAttachCategory t) {
        // 得到子节点列表
        List<SysAttachCategory> childList = getChildList(list, t);
        t.setChildren(childList);
        // 递归获取子列表的子列表
        for (SysAttachCategory child : childList) {
            if (hasChild(list, child)) {
                recursionFn(list, child);
            }
        }
    }

    private List<SysAttachCategory> getChildList(List<SysAttachCategory> list, SysAttachCategory t) {
        List<SysAttachCategory> childList = new ArrayList<>();
        for (SysAttachCategory n : list) {
            if (n.getParentId() != null && n.getParentId().equals(t.getId())) {
                childList.add(n);
            }
        }
        return childList;
    }

    private boolean hasChild(List<SysAttachCategory> list, SysAttachCategory t) {
        return getChildList(list, t).size() > 0;
    }

}
