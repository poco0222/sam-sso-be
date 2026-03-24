/**
 * @file 锁定 SysUserDutyServiceImpl 的单用户分配行为
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.system.domain.entity.SysUserDuty;
import com.yr.common.exception.CustomException;
import com.yr.system.mapper.SysUserDutyMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * SysUserDutyServiceImpl 叶子节点编码契约测试。
 */
class SysUserDutyServiceImplDuplicateAssignmentTest {

    /**
     * 验证单用户场景下重复分配会被拒绝并抛出 CustomException。
     */
    @Test
    void shouldRejectDuplicateDutyAssignmentForSingleUser() {
        DuplicateAssignmentService service = new DuplicateAssignmentService();
        assertThatThrownBy(() -> service.addAssignUserDuty(5L, new Long[]{88L}))
                .isInstanceOf(CustomException.class)
                .hasMessage("用户已经关联了该职务");
        assertThat(service.isSaveBatchInvoked()).isFalse();
    }

    /**
     * 用于模拟重复关系并检测保存分支是否被触发。
     */
    private static final class DuplicateAssignmentService extends SysUserDutyServiceImpl {

        /** 记录是否调用了批量保存。 */
        private boolean saveBatchInvoked;

        private DuplicateAssignmentService() {
            super(mock(SysUserDutyMapper.class));
        }

        /**
         * 返回固定的重复数量，模拟数据库已存在关联。
         *
         * @param wrapper 查询条件
         * @return 始终返回已存在
         */
        @Override
        public long count(Wrapper<SysUserDuty> wrapper) {
            return 1L;
        }

        /**
         * 记录是否触发了保存操作。
         *
         * @param entityList 待保存列表
         * @return true
         */
        @Override
        public boolean saveBatch(Collection<SysUserDuty> entityList) {
            saveBatchInvoked = true;
            return true;
        }

        /**
         * @return 是否触发保存操作
         */
        private boolean isSaveBatchInvoked() {
            return saveBatchInvoked;
        }
    }
}
