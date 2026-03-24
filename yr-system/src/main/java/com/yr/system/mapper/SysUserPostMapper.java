package com.yr.system.mapper;

import com.yr.system.domain.SysUserPost;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户与岗位关联表 数据层
 *
 * @author Youngron
 */
public interface SysUserPostMapper {
    /**
     * 通过用户ID删除用户和岗位关联
     *
     * @param userId 用户ID
     * @return 结果
     */
    public int deleteUserPostByUserId(Long userId);

    /**
     * 通过岗位ID查询岗位使用数量
     *
     * @param postId 岗位ID
     * @return 结果
     */
    public int countUserPostById(Long postId);

    /**
     * 批量删除用户和岗位关联
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteUserPost(Long[] ids);

    /**
     * 批量新增用户岗位信息
     *
     * @param userPostList 用户角色列表
     * @return 结果
     */
    public int batchUserPost(List<SysUserPost> userPostList);

    /**
     * 将用户从岗位中移除
     *
     * @param postId
     * @param userIds
     */
    void removeAssignUser(@Param("postId") Long postId, @Param("userIds") Long[] userIds);

    /**
     * 分配岗位
     *
     * @param postId
     * @param userIds
     */
    void addAssignUser(@Param("postId") Long postId, @Param("userIds") Long[] userIds);

    /**
     * 用户岗位是否已经被关联
     *
     * @param userId
     * @param postId
     * @return
     */
    int countUserPost(@Param("userId") Long userId, @Param("postId") Long postId);

    /*
    根据user_id查询post_id
     */
    List<SysUserPost> selectSysUserPostByPostId(Long userId);

    @Select(" select * from sys_user_post where post_id = #{postId} ")
    List<SysUserPost> selectUserPostListByPostId(Long postId);
}
