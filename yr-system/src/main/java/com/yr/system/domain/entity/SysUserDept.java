/**
 * @file 用户部门关联实体，描述用户与部门的归属关系
 * @author Codex
 * @date 2026-03-16
 */
package com.yr.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yr.common.mybatisplus.entity.CustomEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author Youngron
 * @version V1.0
 * @Date 2021-9-18 10:41
 * @description
 */

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_user_dept")
public class SysUserDept extends CustomEntity {

    /**
     * 表id，主键
     */
    @TableId
    private Long id;

    /**
     * 用户id，sys_user.user_id
     */
    private Long userId;

    /**
     * 部门id，sys_sept.dept_id
     */
    private Long deptId;

    /**
     * 1表示用户的默认部门
     */
    private Integer isDefault;

    /**
     * 是否激活，0:否；1:是
     */
    private Integer enabled;

    /**
     * 用户名
     */
    @TableField(exist = false)
    private String userName;

    /**
     * 昵称
     */
    @TableField(exist = false)
    private String nickName;

    /**
     * 部门名
     */
    @TableField(exist = false)
    private String deptName;
}
