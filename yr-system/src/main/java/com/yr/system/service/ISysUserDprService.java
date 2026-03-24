package com.yr.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.page.PageDomain;
import com.yr.system.domain.SysPost;
import com.yr.system.domain.entity.SysDuty;
import com.yr.system.domain.entity.SysRank;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 业务层
 *
 * @author Youngron
 */
public interface ISysUserDprService {

    List<SysUser> listUserByDeptId(Long deptId);

    List<SysUser> listUserByPostId(Long postId);

    List<SysUser> listUserByRoleId(Long roleId);

    List<SysUser> listUserByDeptCode(String deptCode);

    List<SysUser> listUserByPostCode(String postCode);

    List<SysUser> listUserByRoleKey(String roleKey);

    List<SysUser> listUserByDeptIdWithAncestors(Long deptId);

    List<SysUser> listUserByDeptIdWithDescenders(Long deptId);

    List<SysUser> listUserByDeptCodeWithAncestors(String deptCode);

    List<SysUser> listUserByDeptCodeWithDescenders(String deptCode);
}
