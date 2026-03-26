/**
 * @file 用户导入服务，负责导入链路的逐条处理与结果汇总
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.exception.CustomException;
import com.yr.common.utils.SecurityUtils;
import com.yr.common.utils.StringUtils;
import com.yr.system.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户导入服务。
 */
@Service
public class SysUserImportService {

    private static final Logger log = LoggerFactory.getLogger(SysUserImportService.class);

    private final String initPassword;
    private final SysUserMapper userMapper;
    private final SysUserWriteService sysUserWriteService;

    /**
     * 构造导入服务。
     *
     * @param initPassword 一期默认初始化密码
     * @param userMapper 用户 Mapper
     * @param sysUserWriteService 用户写入服务
     */
    public SysUserImportService(@Value("${yr.user.initPassword:Init@123}") String initPassword,
                                SysUserMapper userMapper,
                                SysUserWriteService sysUserWriteService) {
        this.initPassword = initPassword;
        this.userMapper = userMapper;
        this.sysUserWriteService = sysUserWriteService;
    }

    /**
     * 导入用户数据，并保持逐条事务策略不变。
     *
     * @param userList 用户列表
     * @param isUpdateSupport 是否允许更新
     * @param operName 操作人
     * @return 导入结果说明
     */
    public String importUser(List<SysUser> userList, Boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(userList) || userList.isEmpty()) {
            throw new CustomException("导入用户数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();

        // 导入策略固定为“逐条事务处理”，单条失败不会回滚此前已经成功的数据。
        for (SysUser user : userList) {
            try {
                SysUser existedUser = userMapper.selectUserByUserName(user.getUserName());
                if (StringUtils.isNull(existedUser)) {
                    user.setPassword(SecurityUtils.encryptPassword(initPassword));
                    user.setCreateBy(operName);
                    sysUserWriteService.insertUser(user);
                    successNum++;
                    successMsg.append("<br/>").append(successNum).append("、账号 ").append(user.getUserName()).append(" 导入成功");
                } else if (Boolean.TRUE.equals(isUpdateSupport)) {
                    user.setUpdateBy(operName);
                    sysUserWriteService.updateUser(user);
                    successNum++;
                    successMsg.append("<br/>").append(successNum).append("、账号 ").append(user.getUserName()).append(" 更新成功");
                } else {
                    failureNum++;
                    failureMsg.append("<br/>").append(failureNum).append("、账号 ").append(user.getUserName()).append(" 已存在");
                }
            } catch (CustomException exception) {
                failureNum++;
                String msg = "<br/>" + failureNum + "、账号 " + user.getUserName() + " 导入失败：";
                failureMsg.append(msg).append(exception.getMessage());
                log.error(msg, exception);
            } catch (RuntimeException exception) {
                // 系统异常需要保留原始语义并立即中止，避免被误判成数据格式问题。
                log.error("账号 {} 导入出现系统异常，中止后续导入", user.getUserName(), exception);
                throw exception;
            }
        }
        if (failureNum > 0) {
            // 部分成功时返回混合摘要，保留成功与失败明细，便于调用方展示完整结果。
            if (successNum > 0) {
                StringBuilder mixedMsg = new StringBuilder();
                mixedMsg.append("导入完成！成功 ")
                        .append(successNum)
                        .append(" 条，失败 ")
                        .append(failureNum)
                        .append(" 条，详情如下：");
                mixedMsg.append(successMsg);
                mixedMsg.append(failureMsg);
                return mixedMsg.toString();
            }
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new CustomException(failureMsg.toString());
        }
        successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        return successMsg.toString();
    }
}
