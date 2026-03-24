package com.yr.system.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yr.common.core.domain.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户表 数据层
 *
 * @author Youngron
 */
@Mapper
public interface SysUserDprMapper {
    List<SysUser> listUserByDeptId(@Param("deptId") Long deptId);

    List<SysUser> listUserByPostId(@Param("postId") Long postId);

    List<SysUser> listUserByRoleId(@Param("roleId") Long roleId);


    List<SysUser> listUserByDeptCode(@Param("deptCode") String deptCode);

    List<SysUser> listUserByPostCode(@Param("postCode") String postCode);

    List<SysUser> listUserByRoleKey(@Param("roleKey") String roleKey);

    List<SysUser> listUserByDeptIdWithAncestors(@Param("deptId") Long deptId);

    List<SysUser> listUserByDeptIdWithDescenders(@Param("deptId") Long deptId);

    List<SysUser> listUserByDeptCodeWithAncestors(@Param("deptCode") String deptCode);

    List<SysUser> listUserByDeptCodeWithDescenders(@Param("deptCode") String deptCode);
}
