/**
 * @file 验证 SysAttachCategoryServiceImpl 对叶子节点编码唯一性的契约
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yr.common.core.domain.entity.SysAttachCategory;
import com.yr.common.exception.CustomException;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SysAttachCategoryServiceImpl 叶子节点编码契约测试。
 */
class SysAttachCategoryServiceImplTest {

    /**
     * 验证新增叶子节点时，服务层会拒绝已存在的 leafCode。
     */
    @Test
    void shouldRejectDuplicatedLeafCodeOnCreate() {
        SysAttachCategoryServiceImpl service = new DuplicateLeafCodeService();
        SysAttachCategory command = new SysAttachCategory();
        command.setParentId(1L);
        command.setCategoryName("合同附件");
        command.setLeafFlag(SysAttachCategory.LEAF_CATEGORY);
        command.setLeafCode("DOC_CODE");
        command.setAllowedFileType(".pdf");
        command.setAllowedFileSize(1024L);

        assertThatThrownBy(() -> service.updateOrInsert(command))
                .isInstanceOf(CustomException.class)
                .hasMessage("叶子节点编码已存在");
    }

    /**
     * 验证更新已有目录时会把目标字段汇总到 updateById 的实体载荷中。
     */
    @Test
    void shouldUpdateExistingCategoryViaUpdateByIdPayload() {
        RecordingUpdateService service = new RecordingUpdateService();
        SysAttachCategory command = new SysAttachCategory();
        command.setId(99L);
        command.setCategoryName("项目附件");
        command.setDescription("更新说明");
        command.setLeafFlag(SysAttachCategory.LEAF_CATEGORY);
        command.setLeafCode("DOC_CODE");
        command.setLeafDictCode("DICT_CODE");
        command.setAllowedFileType(".pdf,.docx");
        command.setAllowedFileSize(2048L);

        service.updateOrInsert(command);

        SysAttachCategory updatedEntity = service.getUpdatedEntity();
        assertThat(updatedEntity).isNotNull();
        assertThat(updatedEntity.getId()).isEqualTo(99L);
        assertThat(updatedEntity.getCategoryName()).isEqualTo("项目附件");
        assertThat(updatedEntity.getDescription()).isEqualTo("更新说明");
        assertThat(updatedEntity.getLeafDictCode()).isEqualTo("DICT_CODE");
        assertThat(updatedEntity.getAllowedFileType()).isEqualTo(".pdf,.docx");
        assertThat(updatedEntity.getAllowedFileSize()).isEqualTo(2048L);
    }

    /**
     * 通过覆写最小持久化交互，验证服务层在命中重复编码时直接失败。
     */
    private static final class DuplicateLeafCodeService extends SysAttachCategoryServiceImpl {

        /**
         * 模拟存在的父节点，避免测试依赖登录上下文。
         *
         * @param id 父节点主键
         * @return 父节点
         */
        @Override
        public SysAttachCategory getById(Serializable id) {
            SysAttachCategory parent = new SysAttachCategory();
            parent.setId((Long) id);
            parent.setLeafFlag("0");
            parent.setAncestors("0");
            parent.setCategoryLevel(1);
            parent.setOrgId(10L);
            return parent;
        }

        /**
         * 模拟数据库中已经存在相同 leafCode。
         *
         * @param queryWrapper 查询条件
         * @return 重复条数
         */
        @Override
        public long count(Wrapper<SysAttachCategory> queryWrapper) {
            return 1L;
        }

        /**
         * 当前用例不会执行到保存分支，这里提供最小桩避免意外访问真实基础设施。
         *
         * @param entity 待保存实体
         * @return 保存结果
         */
        @Override
        public boolean save(SysAttachCategory entity) {
            return true;
        }
    }

    /**
     * 记录更新分支传入实体的最小桩服务。
     */
    private static final class RecordingUpdateService extends SysAttachCategoryServiceImpl {

        /** 记录 updateById 接收到的实体。 */
        private SysAttachCategory updatedEntity;

        @Override
        public long count(Wrapper<SysAttachCategory> queryWrapper) {
            return 0L;
        }

        @Override
        public boolean updateById(SysAttachCategory entity) {
            this.updatedEntity = entity;
            return true;
        }

        /**
         * @return 最近一次更新实体
         */
        private SysAttachCategory getUpdatedEntity() {
            return updatedEntity;
        }
    }
}
