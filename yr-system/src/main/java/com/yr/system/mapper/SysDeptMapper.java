/**
 * @file 部门数据访问接口
 * @author Codex
 * @date 2026-03-11
 */
package com.yr.system.mapper;

import com.yr.common.core.domain.entity.SysDept;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 部门管理 数据层
 *
 * @author Youngron
 */
public interface SysDeptMapper {
    /**
     * 查询部门管理数据
     *
     * @param dept 部门信息
     * @return 部门信息集合
     */
    public List<SysDept> selectDeptList(SysDept dept);

    /**
     * 根据部门ID查询信息
     *
     * @param deptId 部门ID
     * @return 部门信息
     */
    public SysDept selectDeptById(Long deptId);

    /**
     * 根据ID查询所有子部门
     *
     * @param deptId 部门ID
     * @return 部门列表
     */
    public List<SysDept> selectChildrenDeptById(Long deptId);

    /**
     * 根据ID查询所有子部门（正常状态）
     *
     * @param deptId 部门ID
     * @return 子部门数
     */
    public int selectNormalChildrenDeptById(Long deptId);

    /**
     * 是否存在子节点
     *
     * @param deptId 部门ID
     * @return 结果
     */
    public int hasChildByDeptId(Long deptId);

    /**
     * 查询部门是否存在用户
     *
     * @param deptId 部门ID
     * @return 结果
     */
    public int checkDeptExistUser(Long deptId);

    /**
     * 校验部门编码是否唯一
     *
     * @param deptCode 部门编码
     * @param parentId 父部门ID
     * @param orgId    组织ID
     * @return 结果
     */
    public SysDept checkDeptCodeUnique(@Param("deptCode") String deptCode, @Param("parentId") Long parentId, @Param("orgId") Long orgId);

    /**
     * 新增部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    public int insertDept(SysDept dept);

    /**
     * 修改部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    public int updateDept(SysDept dept);

    /**
     * 修改所在部门正常状态
     *
     * @param deptIds 部门ID组
     */
    public void updateDeptStatusNormal(Long[] deptIds);

    /**
     * 修改子元素关系
     *
     * @param depts 子元素
     * @return 结果
     */
    public int updateDeptChildren(@Param("depts") List<SysDept> depts);

    /**
     * 删除部门管理信息
     *
     * @param deptId 部门ID
     * @return 结果
     */
    public int deleteDeptById(Long deptId);

    /*
    根据部门id获取 对应的部门
     */
    SysDept selectSysDeptByDeptId(Long deptId);

    /*
     根据deptCode 查找对应的deptId
     */
    public SysDept selectByDeptCode(String deptCode);

    /*
    获取启动的部门编号和名称
     */
    List<SysDept> selectSysDept();

    /*
    根据部门编号 获取部门id
     */
    SysDept selectSysDeptByDeptCode(String deptCode);

    SysDept queryByDeptCodeList(SysDept sysDept);

    /**
     * @param deptCodes 部门代码
     * @CodingBy PopoY
     * @DateTime 2024/7/23 下午2:50
     * @Description 组装标准C产值分配动态表头
     * @Return java.util.List<com.yr.common.core.domain.entity.SysDept>
     */
    List<SysDept> selectSysDeptByDeptCodeBatch(String[] deptCodes);

    /**
     * @CodingBy PopoY
     * @DateTime 2024/9/2 19:10
     * @Description 查询父级部门id
     * @Param deptId 部门id
     * @Return com.yr.common.core.domain.entity.SysDept
     */
    @Select(" select parent_id from sys_dept where dept_id = #{deptId} and parent_id <> 1 ")
    SysDept selectParentDept(Long deptId);

    /**
     * @CodingBy PopoY
     * @DateTime 2024/9/12 15:03
     * @Description 部门转字典表
     * @Return java.util.List<com.yr.common.core.domain.entity.SysDept>
     */
    @Select(" select dept_id as dict_value, dept_name as dict_label from sys_dept ")
    List<SysDept> getAllSysDeptForOptions();

    /**
     * @CodingBy PopoY
     * @DateTime 2025/1/15 15:21
     * @Description 查询所有部门
     * @Return java.util.List<com.yr.common.core.domain.entity.SysDept>
     */
    @Select(" select * from sys_dept ")
    List<SysDept> getAllDeptInfo();

    /*
    hwl 通过userName 找的其默认部门信息
     */
    @Select("""
            SELECT *
            FROM sys_dept
            WHERE dept_id = (
                SELECT dept_id
                FROM sys_user_dept
                WHERE user_id = (
                    SELECT user_id
                    FROM sys_user
                    WHERE user_name = #{userName}
                )
                AND is_default = '1'
            )
            """)
    SysDept selectSysDeptByUserName(String userName);

    @Select(" select * from sys_dept ")
    List<SysDept> selectDeptListForExcel();

    @Select("""
            SELECT *
            FROM sys_dept
            WHERE dept_id IN (
                SELECT dept_id
                FROM sys_user_dept
                WHERE user_id IN (
                    SELECT user_id
                    FROM sys_user
                )
                AND is_default = '1'
            )
            """)
    List<SysDept> selectUserDefaultDeptList();

    @Select(" select dept_id, dept_code, dept_name from sys_dept where parent_id = #{deptId} ")
    List<SysDept> getChildrenDept(String deptId);

    SysDept selectByUserNameAndIsDefault(@Param("userName") String userName);

    @Select(" select * from sys_dept where del_flag='0' and accounte_unit='0' ")
    List<SysDept> getCheckDeptList();
}
