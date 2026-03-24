package com.yr.system.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.mybatisplus.custommapper.CustomMapper;
import com.yr.system.domain.entity.SysFile;
import org.apache.ibatis.annotations.Param;

/**
 * 系统文件表(SysFile)表数据库访问层
 *
 * @author Youngron
 * @since 2021-12-30 10:48:54
 */
public interface SysFileMapper extends CustomMapper<SysFile> {

    /**
     * 分页查询附件信息
     *
     * @param page
     * @param sysFile
     * @return
     */
    IPage<SysFile> pageByCondition(IPage<SysFile> page, @Param("params") SysFile sysFile);
}
