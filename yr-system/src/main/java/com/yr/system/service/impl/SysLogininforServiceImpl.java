/**
 * @file 登录日志服务实现，负责以 fail-open 方式持久化访问审计
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.service.impl;

import com.yr.system.domain.SysLogininfor;
import com.yr.system.mapper.SysLogininforMapper;
import com.yr.system.service.ISysLogininforService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统访问日志情况信息服务层处理。
 */
@Service
public class SysLogininforServiceImpl implements ISysLogininforService {

    /** 登录日志服务日志。 */
    private static final Logger LOGGER = LoggerFactory.getLogger(SysLogininforServiceImpl.class);

    /** 登录日志持久层。 */
    private final SysLogininforMapper logininforMapper;

    /**
     * 构造登录日志服务。
     *
     * @param logininforMapper 登录日志持久层
     */
    public SysLogininforServiceImpl(SysLogininforMapper logininforMapper) {
        this.logininforMapper = logininforMapper;
    }

    /**
     * 新增系统登录日志。
     *
     * @param logininfor 访问日志对象
     */
    @Override
    public void insertLogininfor(SysLogininfor logininfor) {
        try {
            logininforMapper.insertLogininfor(logininfor);
        } catch (DataAccessException exception) {
            // 登录审计属于旁路能力，底层表暂缺时记录告警即可，不能回灌影响登录主流程。
            LOGGER.warn(
                    "插入登录日志失败，已按 fail-open 处理。userName={}, msg={}",
                    logininfor == null ? null : logininfor.getUserName(),
                    logininfor == null ? null : logininfor.getMsg(),
                    exception
            );
        }
    }

    /**
     * 查询系统登录日志集合。
     *
     * @param logininfor 访问日志对象
     * @return 登录记录集合
     */
    @Override
    public List<SysLogininfor> selectLogininforList(SysLogininfor logininfor) {
        return logininforMapper.selectLogininforList(logininfor);
    }

    /**
     * 批量删除系统登录日志。
     *
     * @param infoIds 需要删除的登录日志ID
     * @return 删除条数
     */
    @Override
    public int deleteLogininforByIds(Long[] infoIds) {
        return logininforMapper.deleteLogininforByIds(infoIds);
    }

    /**
     * 清空系统登录日志。
     */
    @Override
    public void cleanLogininfor() {
        logininforMapper.cleanLogininfor();
    }
}
