/**
 * @file 岗位服务实现
 * @author Codex
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.yr.common.constant.UserConstants;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.text.Convert;
import com.yr.common.exception.CustomException;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.domain.SysPost;
import com.yr.system.mapper.SysPostMapper;
import com.yr.system.mapper.SysUserPostMapper;
import com.yr.system.service.ISysPostService;
import com.yr.system.service.ISysUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 岗位信息 服务层处理
 *
 * @author Youngron
 */
@Service
public class SysPostServiceImpl implements ISysPostService {
    private final SysPostMapper postMapper;

    private final SysUserPostMapper userPostMapper;

    private final ISysUserService userService;

    public SysPostServiceImpl(SysPostMapper postMapper,
                              SysUserPostMapper userPostMapper,
                              ISysUserService userService) {
        this.postMapper = postMapper;
        this.userPostMapper = userPostMapper;
        this.userService = userService;
    }

    /**
     * 查询岗位信息集合
     *
     * @param post 岗位信息
     * @return 岗位信息集合
     */
    @Override
    public List<SysPost> selectPostList(SysPost post) {
        if (post.getOrgId() == null) {
            post.setOrgId(SecurityUtils.getOrgId());
        }
        return postMapper.selectPostList(post);
    }

    /**
     * 查询所有岗位
     *
     * @return 岗位列表
     */
    @Override
    public List<SysPost> selectPostAll() {
        return postMapper.selectPostAll();
    }

    /**
     * 通过岗位ID查询岗位信息
     *
     * @param postId 岗位ID
     * @return 角色对象信息
     */
    @Override
    public SysPost selectPostById(Long postId) {
        return postMapper.selectPostById(postId);
    }

    /**
     * 根据用户ID获取岗位选择框列表
     *
     * @param userId 用户ID
     * @return 选中岗位ID列表
     */
    @Override
    public List<Integer> selectPostListByUserId(Long userId) {
        return postMapper.selectPostListByUserId(userId);
    }

    /**
     * 校验岗位编码是否唯一
     *
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public String checkPostCodeUnique(SysPost post) {
        Long postId = StringUtils.isNull(post.getPostId()) ? -1L : post.getPostId();
        SysPost info = postMapper.checkPostCodeUnique(post.getPostCode(), post.getOrgId());
        if (StringUtils.isNotNull(info) && info.getPostId().longValue() != postId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 通过岗位ID查询岗位使用数量
     *
     * @param postId 岗位ID
     * @return 结果
     */
    @Override
    public int countUserPostById(Long postId) {
        return userPostMapper.countUserPostById(postId);
    }

    /**
     * 删除岗位信息
     *
     * @param postId 岗位ID
     * @return 结果
     */
    @Override
    public int deletePostById(Long postId) {
        return postMapper.deletePostById(postId);
    }

    /**
     * 批量删除岗位信息
     *
     * @param postIds 需要删除的岗位ID
     * @return 结果
     * @throws Exception 异常
     */
    @Override
    public int deletePostByIds(Long[] postIds) {
        for (Long postId : postIds) {
            SysPost post = selectPostById(postId);
            if (countUserPostById(postId) > 0) {
                throw new CustomException(String.format("%1$s已分配,不能删除", post.getPostName()));
            }
        }
        return postMapper.deletePostByIds(postIds);
    }

    /**
     * 新增保存岗位信息
     *
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    public int insertPost(SysPost post) {
        SysPost parentInfo = postMapper.selectPostById(post.getParentId());
        if (parentInfo == null) {
            throw new CustomException("上级岗位不存在");
        }
        // 如果父节点不为正常状态,则不允许新增子节点
        if (!UserConstants.DEPT_NORMAL.equals(parentInfo.getStatus())) {
            throw new CustomException("岗位停用，不允许新增");
        }
        if (UserConstants.POST_TYPE_POST.equals(parentInfo.getPostType())) {
            throw new CustomException("只有分类为目录的才能新增下级节点");
        }
        post.setAncestors(parentInfo.getAncestors() + "," + post.getParentId());

        // 如果组织ID为空，取父节点的组织ID
        if (post.getOrgId() == null) {
            post.setOrgId(parentInfo.getOrgId());
        }
        return postMapper.insertPost(post);
    }

    /**
     * 修改保存岗位信息
     *
     * @param post 岗位信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updatePost(SysPost post) {
        // return postMapper.updatePost(post);
        SysPost newParentPost = postMapper.selectPostById(post.getParentId());
        if (newParentPost == null) {
            throw new CustomException("上级岗位不存在");
        }
        SysPost oldPost = postMapper.selectPostById(post.getPostId());
        if (StringUtils.isNotNull(newParentPost) && StringUtils.isNotNull(oldPost)) {
            String newAncestors = newParentPost.getAncestors() + "," + newParentPost.getPostId();
            String oldAncestors = oldPost.getAncestors();
            post.setAncestors(newAncestors);
            updatePostChildren(post.getPostId(), newAncestors, oldAncestors);
        }
        int result = postMapper.updatePost(post);
        if (UserConstants.DEPT_NORMAL.equals(post.getStatus()) && UserConstants.DEPT_DISABLE.equals(oldPost.getStatus())) {
            // 如果该岗位是启用状态，则启用该岗位的所有上级岗位
            updateParentPostStatusNormal(post);
        }
        return result;
    }

    @Override
    public List<SysUser> listAssignUserByPostId(Long postId, SysUser sysUser) {
        return userService.listPostAssignUserByPostId(postId, sysUser);
    }

    @Override
    public void removeAssignUser(Long postId, Long[] userIds) {
        userPostMapper.removeAssignUser(postId, userIds);
    }

    @Override
    public List<SysUser> listUnAssignUserByPostId(Long postId, SysUser sysUser) {
        return userService.listUnAssignUserByPostId(postId, sysUser);
    }

    @Override
    public void addAssignUser(Long postId, Long[] userIds) {
        userPostMapper.addAssignUser(postId, userIds);
    }

    @Override
    public int selectNormalChildrenPostById(Long postId) {
        return postMapper.selectNormalChildrenPostById(postId);
    }

    @Override
    public boolean hasChildByPostId(Long postId) {
        int count = postMapper.hasChildByPostId(postId);
        return count > 0;
    }

    @Override
    public boolean checkPostExistUser(Long postId) {
        int count = this.countUserPostById(postId);
        return count > 0;
    }

    @Override
    public List<SysPost> listUserAssignPostUserId(Long userId) {
        return postMapper.listUserAssignPostUserId(userId, SecurityUtils.getOrgId());
    }

    @Override
    public void addPostForUser(Long userId, Long postId) {
        if (userId == null || postId == null) {
            throw new CustomException("userId or postId can't be null");
        }
        int count = userPostMapper.countUserPost(userId, postId);
        if (count > 0) {
            throw new CustomException("用户已经关联了该岗位");
        }
        SysPost sysPost = postMapper.selectPostById(postId);
        if (UserConstants.POST_TYPE_CATALOG.equals(sysPost.getPostType())) {
            throw new CustomException("目录不能分配用户");
        }
        userPostMapper.addAssignUser(postId, new Long[]{userId});
    }

    @Override
    public void removePostFromUser(Long userId, Long postId) {
        if (userId == null || postId == null) {
            throw new CustomException("userId or postId can't be null");
        }
        userPostMapper.removeAssignUser(postId, new Long[]{userId});
    }

    /**
     * 修改子元素关系
     *
     * @param postId       被修改的岗位ID
     * @param newAncestors 新的父ID集合
     * @param oldAncestors 旧的父ID集合
     */
    private void updatePostChildren(Long postId, String newAncestors, String oldAncestors) {
        List<SysPost> children = postMapper.selectChildrenPostById(postId);
        for (SysPost child : children) {
            child.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
        }
        if (children.size() > 0) {
            postMapper.updatePostChildren(children);
        }
    }

    /**
     * 修改该岗位的父级岗位状态
     *
     * @param post 当前岗位
     */
    private void updateParentPostStatusNormal(SysPost post) {
        String ancestors = post.getAncestors();
        Long[] postIds = Convert.toLongArray(ancestors);
        postMapper.updatePostStatusNormal(postIds);
    }
}
