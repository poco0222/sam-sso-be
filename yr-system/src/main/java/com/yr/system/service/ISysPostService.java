package com.yr.system.service;

import com.yr.common.core.domain.entity.SysUser;
import com.yr.system.domain.SysPost;

import java.util.List;

/**
 * 岗位信息 服务层
 *
 * @author Youngron
 */
public interface ISysPostService {
    /**
     * 查询岗位信息集合
     *
     * @param post 岗位信息
     * @return 岗位列表
     */
    public List<SysPost> selectPostList(SysPost post);

    /**
     * 查询所有岗位
     *
     * @return 岗位列表
     */
    public List<SysPost> selectPostAll();

    /**
     * 通过岗位ID查询岗位信息
     *
     * @param postId 岗位ID
     * @return 角色对象信息
     */
    public SysPost selectPostById(Long postId);

    /**
     * 根据用户ID获取岗位选择框列表
     *
     * @param userId 用户ID
     * @return 选中岗位ID列表
     */
    public List<Integer> selectPostListByUserId(Long userId);

    /**
     * 校验岗位编码
     *
     * @param post 岗位信息
     * @return 结果
     */
    public String checkPostCodeUnique(SysPost post);

    /**
     * 通过岗位ID查询岗位使用数量
     *
     * @param postId 岗位ID
     * @return 结果
     */
    public int countUserPostById(Long postId);

    /**
     * 删除岗位信息
     *
     * @param postId 岗位ID
     * @return 结果
     */
    public int deletePostById(Long postId);

    /**
     * 批量删除岗位信息
     *
     * @param postIds 需要删除的岗位ID
     * @return 结果
     * @throws Exception 异常
     */
    public int deletePostByIds(Long[] postIds);

    /**
     * 新增保存岗位信息
     *
     * @param post 岗位信息
     * @return 结果
     */
    public int insertPost(SysPost post);

    /**
     * 修改保存岗位信息
     *
     * @param post 岗位信息
     * @return 结果
     */
    public int updatePost(SysPost post);

    /**
     * 查询岗位已分配的用户列表
     *
     * @param postId
     * @param sysUser
     * @return
     */
    List<SysUser> listAssignUserByPostId(Long postId, SysUser sysUser);

    /**
     * 将用户从岗位中移除
     *
     * @param postId
     * @param userIds
     */
    void removeAssignUser(Long postId, Long[] userIds);

    /**
     * 查询岗位未分配的用户列表
     *
     * @param postId
     * @param sysUser
     * @return
     */
    List<SysUser> listUnAssignUserByPostId(Long postId, SysUser sysUser);

    /**
     * 分配岗位
     *
     * @param postId
     * @param userIds
     */
    void addAssignUser(Long postId, Long[] userIds);

    /**
     * 根据ID查询所有子岗位（正常状态）
     *
     * @param postId
     * @return
     */
    int selectNormalChildrenPostById(Long postId);

    /**
     * 岗位是否有子岗位
     *
     * @param postId
     * @return
     */
    boolean hasChildByPostId(Long postId);

    /**
     * 岗位是否关联了用户
     *
     * @param postId
     * @return
     */
    boolean checkPostExistUser(Long postId);

    /**
     * 分页查询用户已分配的岗位
     *
     * @param userId
     * @return
     */
    List<SysPost> listUserAssignPostUserId(Long userId);

    /**
     * 给用户分配岗位
     *
     * @param userId
     * @param postId
     */
    void addPostForUser(Long userId, Long postId);

    /**
     * 移除分配给用户的岗位
     *
     * @param userId
     * @param postId
     */
    void removePostFromUser(Long userId, Long postId);
}
