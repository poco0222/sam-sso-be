/**
 * @file SysUser.toString 敏感字段契约测试
 * @author PopoY
 * @date 2026-03-31
 */
package com.yr.system.domain.entity;

import com.yr.common.core.domain.entity.SysUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定 SysUser.toString 不得输出敏感字段。
 */
class SysUserToStringContractTest {

    /**
     * 验证 password/salt 不会进入 toString 文本。
     */
    @Test
    void shouldNotExposePasswordOrSaltInToString() {
        SysUser user = new SysUser();
        user.setUserId(7L);
        user.setUserName("admin");
        user.setPassword("cipher-text");
        user.setSalt("salt-value");

        String toStringResult = user.toString();

        assertThat(toStringResult).doesNotContain("cipher-text");
        assertThat(toStringResult).doesNotContain("salt-value");
        assertThat(toStringResult).doesNotContain("password=");
        assertThat(toStringResult).doesNotContain("salt=");
    }
}
