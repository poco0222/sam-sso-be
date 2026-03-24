package com.yr.system.service;

import com.yr.common.core.domain.TreeSelect;
import com.yr.common.core.domain.entity.SysAttachCategory;
import com.yr.common.mybatisplus.service.ICustomService;
import java.util.List;

/**
 * 附件目录表(SysAttachCategory)表服务接口
 *
 * @author Youngron
 * @since 2021-12-30 10:41:35
 */
public interface ISysAttachCategoryService extends ICustomService<SysAttachCategory> {

    /**
     * 新增/更新附件目录
     *
     * @param sysAttachCategory
     */
    void updateOrInsert(SysAttachCategory sysAttachCategory);

    /**
     * 获取附件目录树
     *
     * @param sysAttachCategory
     * @return
     */
    List<TreeSelect> buildTreeList(SysAttachCategory sysAttachCategory);
}
