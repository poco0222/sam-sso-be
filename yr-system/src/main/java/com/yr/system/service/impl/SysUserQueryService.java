/**
 * @file 用户查询服务，承接复杂查询与批量查询优化
 * @author PopoY
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysUser;
import com.yr.system.domain.entity.SysUserDept;
import com.yr.system.mapper.SysUserDeptMapper;
import com.yr.system.mapper.SysUserMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户查询服务。
 */
@Service
public class SysUserQueryService {

    private final SysUserMapper userMapper;
    private final SysUserDeptMapper sysUserDeptMapper;

    /**
     * 构造用户查询服务。
     *
     * @param userMapper 用户 Mapper
     * @param sysUserDeptMapper 用户部门 Mapper
     */
    public SysUserQueryService(SysUserMapper userMapper,
                               SysUserDeptMapper sysUserDeptMapper) {
        this.userMapper = userMapper;
        this.sysUserDeptMapper = sysUserDeptMapper;
    }

    /**
     * 根据用户 ID 查询用户及当前职级。
     *
     * @param userId 用户 ID
     * @return 用户信息；未查询到时返回 null
     */
    @Nullable
    public SysUser getUserById(Long userId) {
        // 一期详情页不再拼接职级扩展信息，直接返回用户基础数据。
        return userMapper.selectUserByUserId(userId);
    }

    /**
     * 通过部门编码批量查询激活用户，避免逐个用户查询导致的 N+1。
     *
     * @param deptCode 部门编码
     * @return 用户列表
     */
    public List<SysUser> selectSysUserById(String deptCode) {
        return userMapper.selectSysUsersByDeptCode(deptCode);
    }

    /**
     * 通过部门编码数组查询用户。
     *
     * @param deptId 部门编码数组
     * @return 用户列表
     */
    public List<SysUser> selectUserByDeptCode(String[] deptId) {
        return userMapper.selectUserByDeptCode(deptId);
    }

    /**
     * 按用户 ID 列表批量查询用户，并按输入顺序返回去重后的结果。
     *
     * @param userIds 用户 ID 列表
     * @return 用户列表
     */
    public List<SysUser> listUsersByIds(List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        List<Long> orderedUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollectionUtils.isEmpty(orderedUserIds)) {
            return Collections.emptyList();
        }

        Map<Long, SysUser> userMap = userMapper.selectSysUserByUserIds(orderedUserIds.toArray(Long[]::new)).stream()
                .collect(Collectors.toMap(SysUser::getUserId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        return orderedUserIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 通过部门及其子部门查询用户。
     *
     * @param deptId 部门 ID
     * @return 用户列表
     */
    public List<SysUser> selectUserByDeptId(Long deptId) {
        List<SysUserDept> userDeptList = sysUserDeptMapper.selectAllUserByDeptId(deptId);
        return selectUsersByUserDeptRelations(userDeptList);
    }

    /**
     * 批量查询多个部门及其子部门下的用户。
     *
     * @param deptIds 部门 ID 数组
     * @return 以部门 ID 为 key 的用户列表
     */
    public Map<Long, List<SysUser>> batchSelectUserByDeptId(Long[] deptIds) {
        List<SysUserDept> userDeptRelations = sysUserDeptMapper.batchSelectUserByDeptId(deptIds);
        if (CollectionUtils.isEmpty(userDeptRelations)) {
            return Collections.emptyMap();
        }

        Map<Long, List<Long>> userIdMap = userDeptRelations.stream()
                .collect(Collectors.groupingBy(
                        SysUserDept::getDeptId,
                        LinkedHashMap::new,
                        Collectors.mapping(SysUserDept::getUserId, Collectors.toList())
                ));

        Long[] uniqueUserIds = userDeptRelations.stream()
                .map(SysUserDept::getUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .toArray(Long[]::new);
        if (uniqueUserIds.length == 0) {
            return Collections.emptyMap();
        }

        Map<Long, SysUser> userMap = userMapper.selectSysUserByUserIds(uniqueUserIds).stream()
                .collect(Collectors.toMap(SysUser::getUserId, Function.identity()));

        return userIdMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(userMap::get)
                                .filter(java.util.Objects::nonNull)
                                .collect(Collectors.toList()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    /**
     * 通过用户部门关系批量查询用户，并在无关联时直接返回空列表。
     *
     * @param userDeptList 用户部门关系列表
     * @return 用户列表
     */
    private List<SysUser> selectUsersByUserDeptRelations(List<SysUserDept> userDeptList) {
        if (CollectionUtils.isEmpty(userDeptList)) {
            return Collections.emptyList();
        }
        Long[] userIds = userDeptList.stream()
                .map(SysUserDept::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toArray(Long[]::new);
        if (userIds.length == 0) {
            return Collections.emptyList();
        }
        return userMapper.selectSysUserByUserIds(userIds);
    }
}
