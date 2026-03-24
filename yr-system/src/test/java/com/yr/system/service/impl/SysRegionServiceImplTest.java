/**
 * @file 验证 SysRegionServiceImpl 初始化区域数据时的层级与事务契约
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.yr.common.exception.CustomException;
import com.yr.system.domain.dto.SysRegionDTO;
import com.yr.system.domain.entity.SysRegion;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SysRegionServiceImpl 初始化行为测试。
 */
class SysRegionServiceImplTest {

    /**
     * 验证初始化逻辑会基于持久化后的根节点 ID 构建省级祖级链路。
     */
    @Test
    void shouldUsePersistedRootIdWhenBuildingProvinceAncestors() {
        RecordingSysRegionService service = new RecordingSysRegionService(99L, null);
        List<SysRegionDTO> input = List.of(
                dto("上海市", "310000", "上海市", "310100", "黄浦区", "310101")
        );

        service.initData(input);

        assertThat(service.getSavedRoot()).isNotNull();
        assertThat(service.getProvinceBatch()).singleElement().satisfies(province -> {
            assertThat(province.getParentId()).isEqualTo(99L);
            assertThat(province.getAncestors()).isEqualTo("0,99");
        });
    }

    /**
     * 验证初始化方法声明事务边界，避免中途异常留下脏数据。
     *
     * @throws NoSuchMethodException 当方法签名变化时抛出
     */
    @Test
    void shouldDeclareTransactionalOnInitData() throws NoSuchMethodException {
        Method method = SysRegionServiceImpl.class.getMethod("initData", List.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
    }

    /**
     * 验证重复初始化会被快速拒绝，而不是静默追加同一套根节点。
     */
    @Test
    void shouldFailFastWhenChinaRootAlreadyExists() {
        SysRegion existingRoot = new SysRegion();
        existingRoot.setId(1L);
        existingRoot.setRegionCode("CHINA");
        RecordingSysRegionService service = new RecordingSysRegionService(99L, existingRoot);

        assertThatThrownBy(() -> service.initData(List.of(
                dto("上海市", "310000", "上海市", "310100", "黄浦区", "310101")
        )))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("区域数据已初始化");
    }

    /**
     * 验证区域编码未命中现有记录时会被视为唯一。
     */
    @Test
    void shouldTreatRegionCodeAsUniqueWhenNoExistingRecordMatches() {
        RecordingSysRegionService service = new RecordingSysRegionService(99L, null);
        SysRegion command = new SysRegion();
        command.setRegionCode("310000");

        boolean duplicated = service.checkCodeUnique(command);

        assertThat(duplicated).isFalse();
    }

    /**
     * 构造最小区域初始化 DTO。
     *
     * @param provinceName 省名称
     * @param provinceCode 省编码
     * @param cityName 市名称
     * @param cityCode 市编码
     * @param countyName 区县名称
     * @param countyCode 区县编码
     * @return DTO
     */
    private SysRegionDTO dto(String provinceName,
                             String provinceCode,
                             String cityName,
                             String cityCode,
                             String countyName,
                             String countyCode) {
        return new SysRegionDTO(provinceName, provinceCode, cityName, cityCode, countyName, countyCode);
    }

    /**
     * 通过覆写持久化接口记录初始化过程，避免测试依赖数据库。
     */
    private static final class RecordingSysRegionService extends SysRegionServiceImpl {

        /** 模拟主键序列。 */
        private long nextId;

        /** 模拟已存在的根节点。 */
        private final SysRegion existingRoot;

        /** 记录单条保存的根节点。 */
        private SysRegion savedRoot;

        /** 记录每次批量保存的快照。 */
        private final List<List<SysRegion>> savedBatches = new ArrayList<>();

        /**
         * @param nextId 第一个要分配的主键
         * @param existingRoot 模拟已存在的根节点
         */
        private RecordingSysRegionService(long nextId, SysRegion existingRoot) {
            this.nextId = nextId;
            this.existingRoot = existingRoot;
        }

        @Override
        public boolean save(SysRegion entity) {
            entity.setId(nextId++);
            savedRoot = copy(entity);
            return true;
        }

        @Override
        public boolean saveBatch(Collection<SysRegion> entityList) {
            List<SysRegion> snapshot = new ArrayList<>();
            for (SysRegion region : entityList) {
                region.setId(nextId++);
                snapshot.add(copy(region));
            }
            savedBatches.add(snapshot);
            return true;
        }

        @Override
        public SysRegion getOne(Wrapper<SysRegion> queryWrapper) {
            return existingRoot;
        }

        /**
         * @return 初始化时保存的根节点
         */
        private SysRegion getSavedRoot() {
            return savedRoot;
        }

        /**
         * @return 第一批保存的省级节点
         */
        private List<SysRegion> getProvinceBatch() {
            return savedBatches.get(0);
        }

        /**
         * 复制区域节点，避免后续批量处理继续修改同一对象影响断言。
         *
         * @param source 原对象
         * @return 浅拷贝结果
         */
        private SysRegion copy(SysRegion source) {
            SysRegion target = new SysRegion();
            target.setId(source.getId());
            target.setParentId(source.getParentId());
            target.setAncestors(source.getAncestors());
            target.setRegionCode(source.getRegionCode());
            target.setRegionName(source.getRegionName());
            target.setRegionLevel(source.getRegionLevel());
            target.setHasChildren(source.getHasChildren());
            return target;
        }
    }
}
