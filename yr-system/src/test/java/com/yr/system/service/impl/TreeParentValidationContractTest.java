/**
 * @file 验证树形服务在父节点缺失时的输入校验契约
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yr.common.constant.UserConstants;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.exception.CustomException;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.system.domain.SysPost;
import com.yr.system.domain.entity.SysDuty;
import com.yr.system.domain.entity.SysRank;
import com.yr.system.mapper.SysDutyMapper;
import com.yr.system.mapper.SysDeptMapper;
import com.yr.system.mapper.SysPostMapper;
import com.yr.system.mapper.SysRankMapper;
import com.yr.system.mapper.SysUserPostMapper;
import com.yr.system.mapper.SysRoleMapper;
import com.yr.system.mapper.SysUserDeptMapper;
import com.yr.system.service.ISysDeptService;
import com.yr.system.service.ISysUserDutyService;
import com.yr.system.service.ISysUserRankService;
import com.yr.system.service.ISysUserService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 树形父节点校验契约测试。
 */
class TreeParentValidationContractTest {

    /**
     * 验证新增职级时会拒绝不存在的父节点。
     */
    @Test
    void shouldRejectMissingParentWhenAddingRank() {
        SysRankServiceImpl service = buildRankServiceForMissingParent();
        SysRank command = buildRankCommand();

        assertThatThrownBy(() -> service.addRank(command))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("上级职级");
    }

    /**
     * 验证更新职级时会拒绝不存在的父节点。
     */
    @Test
    void shouldRejectMissingParentWhenUpdatingRank() {
        SysRankServiceImpl service = buildRankServiceForMissingParent();
        SysRank command = buildRankCommand();
        command.setId(101L);

        assertThatThrownBy(() -> service.updateRank(command))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("上级职级");
    }

    /**
     * 验证新增职务时会拒绝不存在的父节点。
     */
    @Test
    void shouldRejectMissingParentWhenAddingDuty() {
        SysDutyServiceImpl service = buildDutyServiceForMissingParent();
        SysDuty command = buildDutyCommand();

        assertThatThrownBy(() -> service.addDuty(command))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("上级职务");
    }

    /**
     * 验证更新职务时会拒绝不存在的父节点。
     */
    @Test
    void shouldRejectMissingParentWhenUpdatingDuty() {
        SysDutyServiceImpl service = buildDutyServiceForMissingParent();
        SysDuty command = buildDutyCommand();
        command.setId(101L);

        assertThatThrownBy(() -> service.updateDuty(command))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("上级职务");
    }

    /**
     * 验证新增岗位时会拒绝不存在的父节点。
     */
    @Test
    void shouldRejectMissingParentWhenInsertingPost() {
        SysPostServiceImpl service = buildPostServiceForMissingParent();
        SysPost command = buildPostCommand();

        assertThatThrownBy(() -> service.insertPost(command))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("上级岗位");
    }

    /**
     * 验证更新岗位时会拒绝不存在的父节点。
     */
    @Test
    void shouldRejectMissingParentWhenUpdatingPost() {
        SysPostServiceImpl service = buildPostServiceForMissingParent();
        SysPost command = buildPostCommand();
        command.setPostId(101L);
        command.setStatus(UserConstants.DEPT_DISABLE);

        assertThatThrownBy(() -> service.updatePost(command))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("上级岗位");
    }

    /**
     * 验证新增部门时会拒绝不存在的父节点，避免 SQL hygiene 调整后重新退化成静默错误结果。
     */
    @Test
    void shouldRejectMissingParentWhenInsertingDept() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        when(deptMapper.selectDeptById(999L)).thenReturn(null);

        SysDeptServiceImpl service = new SysDeptServiceImpl(
                deptMapper,
                mock(SysRoleMapper.class),
                mock(SysUserDeptMapper.class),
                mock(ISysDeptService.class)
        );
        SysDept command = new SysDept();
        command.setParentId(999L);

        assertThatThrownBy(() -> service.insertDept(command))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("上级部门不存在");
    }

    /**
     * 验证部门换父节点时会重写子树祖级链路，避免 SQL hygiene 调整后树重排语义退化。
     */
    @Test
    void shouldRewriteDeptChildAncestorsWhenReparentingDept() {
        SysDeptMapper deptMapper = mock(SysDeptMapper.class);
        SysDept newParentDept = new SysDept();
        newParentDept.setDeptId(2L);
        newParentDept.setAncestors("0");
        SysDept oldDept = new SysDept();
        oldDept.setDeptId(101L);
        oldDept.setAncestors("0,1");
        SysDept childDept = new SysDept();
        childDept.setDeptId(1001L);
        childDept.setAncestors("0,1,101");

        when(deptMapper.selectDeptById(2L)).thenReturn(newParentDept);
        when(deptMapper.selectDeptById(101L)).thenReturn(oldDept);
        when(deptMapper.selectChildrenDeptById(101L)).thenReturn(List.of(childDept));
        when(deptMapper.updateDept(any(SysDept.class))).thenReturn(1);
        when(deptMapper.updateDeptChildren(any())).thenReturn(1);

        SysDeptServiceImpl service = new SysDeptServiceImpl(
                deptMapper,
                mock(SysRoleMapper.class),
                mock(SysUserDeptMapper.class),
                mock(ISysDeptService.class)
        );
        SysDept command = new SysDept();
        command.setDeptId(101L);
        command.setParentId(2L);

        service.updateDept(command);

        verify(deptMapper).updateDeptChildren(argThat(children ->
                children.size() == 1 && "0,2,101".equals(children.get(0).getAncestors())));
    }

    /**
     * 验证岗位换父节点时会同步重写子树祖级链路，避免树形岗位重排出现静默错误结果。
     */
    @Test
    void shouldRewritePostChildAncestorsWhenReparentingPost() {
        SysPostMapper postMapper = mock(SysPostMapper.class);
        SysPost newParentPost = new SysPost();
        newParentPost.setPostId(2L);
        newParentPost.setAncestors("0");
        SysPost oldPost = new SysPost();
        oldPost.setPostId(101L);
        oldPost.setAncestors("0,1");
        oldPost.setStatus(UserConstants.DEPT_NORMAL);
        SysPost childPost = new SysPost();
        childPost.setPostId(1001L);
        childPost.setAncestors("0,1,101");

        when(postMapper.selectPostById(2L)).thenReturn(newParentPost);
        when(postMapper.selectPostById(101L)).thenReturn(oldPost);
        when(postMapper.selectChildrenPostById(101L)).thenReturn(List.of(childPost));
        when(postMapper.updatePost(any(SysPost.class))).thenReturn(1);
        when(postMapper.updatePostChildren(any())).thenReturn(1);

        SysPostServiceImpl service = new SysPostServiceImpl(
                postMapper,
                mock(SysUserPostMapper.class),
                mock(ISysUserService.class)
        );
        SysPost command = new SysPost();
        command.setPostId(101L);
        command.setParentId(2L);

        service.updatePost(command);

        verify(postMapper).updatePostChildren(argThat(children ->
                children.size() == 1 && "0,2,101".equals(children.get(0).getAncestors())));
    }

    /**
     * 构造父节点缺失的职级服务。
     *
     * @return 职级服务
     */
    private SysRankServiceImpl buildRankServiceForMissingParent() {
        SysRankMapper mapper = mock(SysRankMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.selectById(999L)).thenReturn(null);
        when(mapper.selectById(101L)).thenReturn(existingRank());
        when(mapper.updateById(any(SysRank.class))).thenReturn(1);

        SysRankServiceImpl service = new SysRankServiceImpl(mock(ISysUserRankService.class));
        injectCustomMapper(service, mapper);
        return service;
    }

    /**
     * 构造父节点缺失的职务服务。
     *
     * @return 职务服务
     */
    private SysDutyServiceImpl buildDutyServiceForMissingParent() {
        SysDutyMapper mapper = mock(SysDutyMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.selectById(999L)).thenReturn(null);
        when(mapper.selectById(101L)).thenReturn(existingDuty());
        when(mapper.updateById(any(SysDuty.class))).thenReturn(1);

        SysDutyServiceImpl service = new SysDutyServiceImpl(mock(ISysUserDutyService.class));
        injectCustomMapper(service, mapper);
        return service;
    }

    /**
     * 构造父节点缺失的岗位服务。
     *
     * @return 岗位服务
     */
    private SysPostServiceImpl buildPostServiceForMissingParent() {
        SysPostMapper postMapper = mock(SysPostMapper.class);
        when(postMapper.selectPostById(999L)).thenReturn(null);
        when(postMapper.selectPostById(101L)).thenReturn(existingPost());
        when(postMapper.updatePost(any(SysPost.class))).thenReturn(1);
        return new SysPostServiceImpl(postMapper, mock(SysUserPostMapper.class), mock(ISysUserService.class));
    }

    /**
     * 构造最小新增/更新职级命令。
     *
     * @return 职级命令
     */
    private SysRank buildRankCommand() {
        SysRank command = new SysRank();
        command.setParentId(999L);
        command.setOrgId(1L);
        command.setRankCode("R-001");
        command.setRankName("测试职级");
        command.setRankType(UserConstants.RANK_TYPE_RANK);
        return command;
    }

    /**
     * 构造最小新增/更新职务命令。
     *
     * @return 职务命令
     */
    private SysDuty buildDutyCommand() {
        SysDuty command = new SysDuty();
        command.setParentId(999L);
        command.setOrgId(1L);
        command.setDutyCode("D-001");
        command.setDutyName("测试职务");
        return command;
    }

    /**
     * 构造最小新增/更新岗位命令。
     *
     * @return 岗位命令
     */
    private SysPost buildPostCommand() {
        SysPost command = new SysPost();
        command.setParentId(999L);
        command.setPostCode("P-001");
        command.setPostName("测试岗位");
        command.setPostSort("1");
        return command;
    }

    /**
     * @return 已存在的旧职级
     */
    private SysRank existingRank() {
        SysRank rank = new SysRank();
        rank.setId(101L);
        rank.setParentId(1L);
        rank.setAncestors("0,1");
        rank.setRankType(UserConstants.RANK_TYPE_RANK);
        return rank;
    }

    /**
     * @return 已存在的旧职务
     */
    private SysDuty existingDuty() {
        SysDuty duty = new SysDuty();
        duty.setId(101L);
        duty.setParentId(1L);
        duty.setAncestors("0,1");
        return duty;
    }

    /**
     * @return 已存在的旧岗位
     */
    private SysPost existingPost() {
        SysPost post = new SysPost();
        post.setPostId(101L);
        post.setParentId(1L);
        post.setAncestors("0,1");
        post.setStatus(UserConstants.DEPT_DISABLE);
        return post;
    }

    /**
     * 为继承 MyBatis-Plus 基类的服务补齐 mapper 注入。
     *
     * @param target 服务对象
     * @param mapper mapper 桩对象
     */
    private void injectCustomMapper(Object target, Object mapper) {
        injectField(ServiceImpl.class, target, "baseMapper", mapper);
        injectField(CustomServiceImpl.class, target, "baseMapper", mapper);
    }

    /**
     * 通过反射注入字段，保证测试能复用真实继承链逻辑。
     *
     * @param owner 声明字段的类
     * @param target 目标对象
     * @param fieldName 字段名
     * @param value 字段值
     */
    private void injectField(Class<?> owner, Object target, String fieldName, Object value) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            throw new IllegalStateException("注入字段失败: " + fieldName, exception);
        }
    }
}
