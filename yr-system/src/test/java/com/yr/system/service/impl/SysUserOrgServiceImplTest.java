/**
 * @file 锁定 SysUserOrgServiceImpl 的重复关联校验行为
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.exception.CustomException;
import com.yr.system.domain.entity.SysUserOrg;
import com.yr.system.mapper.SysOrgMapper;
import com.yr.system.mapper.SysUserOrgMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * SysUserOrgServiceImpl 行为测试。
 */
class SysUserOrgServiceImplTest {

    /**
     * 验证新增用户组织关联时，重复关系会被拒绝且不会继续保存。
     */
    @Test
    void shouldRejectDuplicatedUserOrgRelation() {
        SysUserOrgServiceImpl service = spy(new SysUserOrgServiceImpl(mock(SysUserOrgMapper.class), mock(SysOrgMapper.class)));
        SysUserOrg existingRelation = buildRelation(10L, 20L);
        SysUserOrg command = buildRelation(10L, 20L);
        doReturn(existingRelation).when(service).getOne(any());
        doReturn(true).when(service).save(any(SysUserOrg.class));

        assertThatThrownBy(() -> service.addSysUserOrg(command))
                .isInstanceOf(CustomException.class)
                .hasMessage("该用户已经在组织下");

        verify(service, never()).save(any(SysUserOrg.class));
    }

    /**
     * 验证新增用户组织关联时，服务端会重建最小写入对象并兜底 enabled/isDefault。
     */
    @Test
    void shouldPersistServerControlledDefaultsWhenAddingUserOrg() {
        SysUserOrgServiceImpl service = spy(new SysUserOrgServiceImpl(mock(SysUserOrgMapper.class), mock(SysOrgMapper.class)));
        SysUserOrg command = buildRelation(10L, 20L);
        ArgumentCaptor<SysUserOrg> relationCaptor = ArgumentCaptor.forClass(SysUserOrg.class);
        command.setIsDefault(1);
        command.setEnabled(0);
        command.setCreateBy(99L);
        command.setObjectVersionNumber(7L);
        doReturn(null).when(service).getOne(any());
        doReturn(true).when(service).save(any(SysUserOrg.class));

        service.addSysUserOrg(command);

        verify(service).save(relationCaptor.capture());
        assertThat(relationCaptor.getValue().getUserId()).isEqualTo(10L);
        assertThat(relationCaptor.getValue().getOrgId()).isEqualTo(20L);
        assertThat(relationCaptor.getValue().getEnabled()).isEqualTo(1);
        assertThat(relationCaptor.getValue().getIsDefault()).isEqualTo(0);
        assertThat(relationCaptor.getValue().getCreateBy()).as("新增入口不应透传 createBy").isNull();
        assertThat(relationCaptor.getValue().getObjectVersionNumber()).as("新增入口不应透传版本字段").isNull();
    }

    /**
     * 验证 changeEnabled 不允许写入非法 enabled 值。
     */
    @Test
    void shouldRejectInvalidEnabledValueWhenChangingUserOrgEnabled() {
        SysUserOrgServiceImpl service = new SysUserOrgServiceImpl(mock(SysUserOrgMapper.class), mock(SysOrgMapper.class));

        assertThatThrownBy(() -> service.changeEnabledById(7L, 2))
                .isInstanceOf(CustomException.class)
                .hasMessage("enabled只允许为0或1");
    }

    /**
     * 构造最小用户组织关联对象。
     *
     * @param userId 用户 ID
     * @param orgId 组织 ID
     * @return 用户组织关联
     */
    private SysUserOrg buildRelation(Long userId, Long orgId) {
        SysUserOrg relation = new SysUserOrg();
        relation.setUserId(userId);
        relation.setOrgId(orgId);
        return relation;
    }
}
