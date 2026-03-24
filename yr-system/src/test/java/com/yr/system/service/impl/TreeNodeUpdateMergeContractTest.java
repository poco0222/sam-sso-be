/**
 * @file 验证树形节点更新会合并旧实体字段，避免 partial payload 覆盖已有数据
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yr.common.constant.UserConstants;
import com.yr.common.mybatisplus.service.impl.CustomServiceImpl;
import com.yr.system.domain.entity.SysDuty;
import com.yr.system.domain.entity.SysRank;
import com.yr.system.mapper.SysDutyMapper;
import com.yr.system.mapper.SysRankMapper;
import com.yr.system.service.ISysUserDutyService;
import com.yr.system.service.ISysUserRankService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 树形节点更新合并契约测试。
 */
class TreeNodeUpdateMergeContractTest {

    /**
     * 验证职级更新会保留旧实体里未在命令中提供的字段。
     */
    @Test
    void shouldPreserveExistingRankFieldsWhenUpdatingWithPartialPayload() {
        SysRankMapper mapper = mock(SysRankMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.selectById(1L)).thenReturn(parentRank());
        when(mapper.selectById(101L)).thenReturn(existingRank());
        when(mapper.selectDescendants(101L)).thenReturn(Collections.emptyList());
        when(mapper.updateById(any(SysRank.class))).thenReturn(1);

        SysRankServiceImpl service = new SysRankServiceImpl(mock(ISysUserRankService.class));
        injectCustomMapper(service, mapper);

        SysRank command = new SysRank();
        command.setId(101L);
        command.setParentId(1L);
        command.setOrgId(1L);
        command.setRankCode("R-001");
        command.setRankName("新的职级名称");

        service.updateRank(command);

        ArgumentCaptor<SysRank> rankCaptor = ArgumentCaptor.forClass(SysRank.class);
        verify(mapper).updateById(rankCaptor.capture());
        SysRank persistedRank = rankCaptor.getValue();
        assertThat(persistedRank.getRankName()).isEqualTo("新的职级名称");
        assertThat(persistedRank.getRankType()).isEqualTo(UserConstants.RANK_TYPE_RANK);
        assertThat(persistedRank.getOrderNum()).isEqualTo(9);
        assertThat(persistedRank.getAncestors()).isEqualTo("0,1");
    }

    /**
     * 验证职务更新会保留旧实体里未在命令中提供的字段。
     */
    @Test
    void shouldPreserveExistingDutyFieldsWhenUpdatingWithPartialPayload() {
        SysDutyMapper mapper = mock(SysDutyMapper.class);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.selectById(1L)).thenReturn(parentDuty());
        when(mapper.selectById(101L)).thenReturn(existingDuty());
        when(mapper.selectDescendants(101L)).thenReturn(Collections.emptyList());
        when(mapper.updateById(any(SysDuty.class))).thenReturn(1);

        SysDutyServiceImpl service = new SysDutyServiceImpl(mock(ISysUserDutyService.class));
        injectCustomMapper(service, mapper);

        SysDuty command = new SysDuty();
        command.setId(101L);
        command.setParentId(1L);
        command.setOrgId(1L);
        command.setDutyCode("D-001");
        command.setDutyName("新的职务名称");

        service.updateDuty(command);

        ArgumentCaptor<SysDuty> dutyCaptor = ArgumentCaptor.forClass(SysDuty.class);
        verify(mapper).updateById(dutyCaptor.capture());
        SysDuty persistedDuty = dutyCaptor.getValue();
        assertThat(persistedDuty.getDutyName()).isEqualTo("新的职务名称");
        assertThat(persistedDuty.getOrderNum()).isEqualTo(6);
        assertThat(persistedDuty.getAncestors()).isEqualTo("0,1");
    }

    /**
     * 验证删除职务时会返回真实删除行数，而不是固定返回占位值。
     */
    @Test
    void shouldReturnAffectedRowsWhenDeletingDuty() {
        SysDutyMapper mapper = mock(SysDutyMapper.class);
        ISysUserDutyService userDutyService = mock(ISysUserDutyService.class);
        when(mapper.selectById(101L)).thenReturn(existingDuty());
        when(mapper.selectCount(any())).thenReturn(0L);
        when(userDutyService.count(any())).thenReturn(0L);
        when(mapper.deleteById(101L)).thenReturn(1);

        SysDutyServiceImpl service = new SysDutyServiceImpl(userDutyService);
        injectCustomMapper(service, mapper);

        int rows = service.deleteDuty(101L);

        assertThat(rows).isEqualTo(1);
        verify(mapper).deleteById(101L);
    }

    /**
     * @return 父职级
     */
    private SysRank parentRank() {
        SysRank rank = new SysRank();
        rank.setId(1L);
        rank.setAncestors("0");
        rank.setRankType(UserConstants.RANK_TYPE_CATALOG);
        return rank;
    }

    /**
     * @return 已存在的职级实体
     */
    private SysRank existingRank() {
        SysRank rank = new SysRank();
        rank.setId(101L);
        rank.setParentId(1L);
        rank.setAncestors("0,1");
        rank.setRankCode("R-001");
        rank.setRankName("旧职级名称");
        rank.setRankType(UserConstants.RANK_TYPE_RANK);
        rank.setOrderNum(9);
        rank.setOrgId(1L);
        return rank;
    }

    /**
     * @return 父职务
     */
    private SysDuty parentDuty() {
        SysDuty duty = new SysDuty();
        duty.setId(1L);
        duty.setAncestors("0");
        return duty;
    }

    /**
     * @return 已存在的职务实体
     */
    private SysDuty existingDuty() {
        SysDuty duty = new SysDuty();
        duty.setId(101L);
        duty.setParentId(1L);
        duty.setAncestors("0,1");
        duty.setDutyCode("D-001");
        duty.setDutyName("旧职务名称");
        duty.setOrderNum(6);
        duty.setOrgId(1L);
        return duty;
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
     * 通过反射注入字段，保证测试可以复用真实继承链逻辑。
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
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("无法注入字段: " + fieldName, exception);
        }
    }
}
