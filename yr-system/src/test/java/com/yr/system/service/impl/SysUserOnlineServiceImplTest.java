/**
 * @file 锁定 SysUserOnlineServiceImpl 的空值保护与映射行为
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysDept;
import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import com.yr.system.domain.SysUserOnline;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SysUserOnlineServiceImpl 行为测试。
 */
class SysUserOnlineServiceImplTest {

    /**
     * 验证传入空 LoginUser 时不会抛异常，而是直接返回 null。
     */
    @Test
    void shouldReturnNullWhenUserIsNull() {
        SysUserOnlineServiceImpl service = new SysUserOnlineServiceImpl();

        assertThat(service.selectOnlineByIpaddr("127.0.0.1", null)).isNull();
        assertThat(service.selectOnlineByUserName("demo", null)).isNull();
        assertThat(service.selectOnlineByInfo("127.0.0.1", "demo", null)).isNull();
    }

    /**
     * 验证匹配成功时会映射出在线用户信息与部门名称。
     */
    @Test
    void shouldConvertMatchedLoginUserToOnlineUser() {
        SysUserOnlineServiceImpl service = new SysUserOnlineServiceImpl();
        LoginUser loginUser = buildLoginUser();

        SysUserOnline online = service.selectOnlineByInfo("127.0.0.1", "demo", loginUser);

        assertThat(online).isNotNull();
        assertThat(online.getUserName()).isEqualTo("demo");
        assertThat(online.getIpaddr()).isEqualTo("127.0.0.1");
        assertThat(online.getDeptName()).isEqualTo("研发部");
        assertThat(online.getTokenId()).isEqualTo("token-1");
    }

    /**
     * 构造最小登录用户。
     *
     * @return 登录用户
     */
    private LoginUser buildLoginUser() {
        SysDept dept = new SysDept();
        dept.setDeptName("研发部");

        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUserName("demo");
        user.setDept(dept);

        LoginUser loginUser = new LoginUser(user, Collections.emptySet());
        loginUser.setToken("token-1");
        loginUser.setIpaddr("127.0.0.1");
        loginUser.setLoginLocation("上海");
        loginUser.setBrowser("Chrome");
        loginUser.setOs("macOS");
        loginUser.setLoginTime(123456789L);
        return loginUser;
    }
}
