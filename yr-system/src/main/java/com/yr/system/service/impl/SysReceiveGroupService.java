/**
 * @file 接收组服务实现
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yr.common.core.domain.ObjectTree;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.enums.ModeType;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.common.utils.StringUtils;
import com.yr.system.domain.entity.SysReceiveGroup;
import com.yr.system.domain.entity.SysReceiveGroupObject;
import com.yr.system.domain.vo.SysObjectTreeVo;
import com.yr.system.domain.vo.SysReceiveGroupVo;
import com.yr.system.mapper.SysReceiveGroupMapper;
import com.yr.system.service.ISysReceiveGroupService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author carl
 * @since 2022-01-04
 */
@Service
public class SysReceiveGroupService extends CustomServiceImpl<SysReceiveGroupMapper, SysReceiveGroup> implements ISysReceiveGroupService {

    /**
     * 用户服务
     */
    private final SysUserQueryService userQueryService;

    public SysReceiveGroupService(SysUserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @Override
    public Page<SysReceiveGroup> pageList(SysReceiveGroupVo sysReceiveGroupVo) {
        return getBaseMapper().pageList(sysReceiveGroupVo);
    }

    @Override
    public SysReceiveGroup saveReceiveGroup(SysReceiveGroup sysReceiveGroup) {
        validateUniqueCode(sysReceiveGroup);
//        Long id = getBaseMapper().getMaxId();
//        if (null != id && id.longValue() > 0) {
//            sysReceiveGroup.setId(id);
//        } else {
//            id = 1L;
//        }
        saveOrUpdate(sysReceiveGroup);
        return sysReceiveGroup;
    }

    @Override
    public Integer del(Long id) {
        Optional.ofNullable(getById(id)).orElseThrow(() -> new CustomException("接收分组不存在，删除失败"));
        removeById(id);
        return 1;
    }

    /**
     * 校验接收编码是否已被其他分组占用。
     *
     * @param sysReceiveGroup 当前待保存分组
     */
    private void validateUniqueCode(SysReceiveGroup sysReceiveGroup) {
        SysReceiveGroup existingGroup = get(sysReceiveGroup.getReCode());
        // 更新时允许保留自身编码，但不允许占用其他分组已经使用的编码。
        if (existingGroup != null && !Objects.equals(existingGroup.getId(), sysReceiveGroup.getId())) {
            throw new CustomException("接收编码已经存在，添加失败");
        }
    }

    @Override
    public SysReceiveGroup get(String code) {
        return getOne(new LambdaQueryWrapper<SysReceiveGroup>().eq(SysReceiveGroup::getReCode, code));
    }

    @Override
    public SysObjectTreeVo getSpecificObjects(Long reGroupId) {
        SysReceiveGroup summaryGroup = Optional.ofNullable(getById(reGroupId))
                .orElseThrow(() -> new CustomException("分组模组Id错误"));
        SysReceiveGroup detailGroup = Optional.ofNullable(getReceiveGroupList(summaryGroup.getReCode())).orElse(summaryGroup);
        String receiveMode = detailGroup.getReMode();

        if (!StringUtils.isNotBlank(receiveMode)) {
            throw new CustomException("暂不支持接收模式: " + receiveMode);
        }

        ModeType modeType;
        try {
            modeType = ModeType.valueOf(receiveMode.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new CustomException("暂不支持接收模式: " + receiveMode);
        }

        if (modeType != ModeType.USER_GROUP) {
            throw new CustomException("暂不支持接收模式: " + receiveMode);
        }

        return buildUserGroupObjectTree(detailGroup);
    }

    /**
     * 构造用户组模式的对象树返回值。
     *
     * @param receiveGroup 接收组详情
     * @return 对象树返回结果
     */
    private SysObjectTreeVo buildUserGroupObjectTree(SysReceiveGroup receiveGroup) {
        SysObjectTreeVo sysObjectTreeVo = new SysObjectTreeVo();
        List<SysReceiveGroupObject> groupObjects = Optional.ofNullable(receiveGroup.getGroupObjectList())
                .orElse(Collections.emptyList());
        List<Long> checkedIds = groupObjects.stream()
                .map(SysReceiveGroupObject::getReObjectId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (checkedIds.isEmpty()) {
            sysObjectTreeVo.setCheckIds(Collections.emptyList());
            sysObjectTreeVo.setTreeList(Collections.emptyList());
            return sysObjectTreeVo;
        }
        List<ObjectTree> treeList = Optional.ofNullable(userQueryService.listUsersByIds(checkedIds))
                .orElse(Collections.emptyList())
                .stream()
                .map(this::buildUserTreeNode)
                .toList();

        sysObjectTreeVo.setCheckIds(checkedIds);
        sysObjectTreeVo.setTreeList(treeList);
        return sysObjectTreeVo;
    }

    /**
     * 把用户对象转换成前端所需的扁平树节点。
     *
     * @param sysUser 用户对象
     * @return 树节点
     */
    private ObjectTree buildUserTreeNode(SysUser sysUser) {
        ObjectTree objectTree = new ObjectTree();
        objectTree.setId(sysUser.getUserId());
        // 用户昵称优先展示，缺失时回退到登录名，避免返回空标签。
        objectTree.setLabel(StringUtils.isNotBlank(sysUser.getNickName()) ? sysUser.getNickName() : sysUser.getUserName());
        objectTree.setChildren(Collections.emptyList());
        return objectTree;
    }

    @Override
    public SysReceiveGroup getReceiveGroupList(String code) {
        return getBaseMapper().getReceiveGroupList(code);
    }
}
