package com.yr.system.mapper;

import com.yr.system.domain.SysPost;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 岗位信息 数据层
 *
 * @author Youngron
 */
public interface SysPostMapper {
    /**
     * 查询岗位数据集合
     *
     * @param post 岗位信息
     * @return 岗位数据集合
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
     * 查询用户所属岗位组
     *
     * @param userName 用户名
     * @return 结果
     */
    public List<SysPost> selectPostsByUserName(String userName);

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
     */
    public int deletePostByIds(Long[] postIds);

    /**
     * 修改岗位信息
     *
     * @param post 岗位信息
     * @return 结果
     */
    public int updatePost(SysPost post);

    /**
     * 新增岗位信息
     *
     * @param post 岗位信息
     * @return 结果
     */
    public int insertPost(SysPost post);

    /**
     * 校验岗位编码
     *
     * @param postCode 岗位编码
     * @param orgId    组织ID
     * @return 结果
     */
    public SysPost checkPostCodeUnique(@Param("postCode") String postCode, @Param("orgId") Long orgId);

    /**
     * 根据ID查询所有子岗位（正常状态）
     *
     * @param postId
     * @return
     */
    int selectNormalChildrenPostById(@Param("postId") Long postId);

    /**
     * 根据岗位ID查询所有子岗位
     *
     * @param postId
     * @return
     */
    List<SysPost> selectChildrenPostById(@Param("postId") Long postId);

    /**
     * 修改子元素关系
     *
     * @param posts
     * @return
     */
    int updatePostChildren(@Param("posts") List<SysPost> posts);

    /**
     * 修改所在岗位正常状态
     *
     * @param postIds
     * @return
     */
    int updatePostStatusNormal(@Param("postIds") Long[] postIds);

    /**
     * 查询子岗位数量
     *
     * @param postId
     * @return
     */
    int hasChildByPostId(@Param("postId") Long postId);

    /**
     * 分页查询用户已分配的岗位
     *
     * @param userId
     * @param orgId
     * @return
     */
    List<SysPost> listUserAssignPostUserId(@Param("userId") Long userId, @Param("orgId") Long orgId);

    /**
     * 根据用户ID和租户ID查询用户岗位
     *
     * @param userId
     * @param orgId
     * @return
     */
    List<SysPost> selectPostsByUserId(@Param("userId") Long userId, @Param("orgId") Long orgId);

    /*
    根据postId查询postName
     */
    SysPost selectSysPostByPostName(Long postId);

    @Select(" select * from sys_post where post_code = #{postKey} ")
    SysPost selectPostByPostKey(String postKey);
}
