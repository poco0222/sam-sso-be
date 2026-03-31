/**
 * @file Redis 配置安全契约测试
 * @author PopoY
 * @date 2026-03-31
 */
package com.yr.framework.config;

import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.core.domain.model.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 锁定 Redis（缓存）序列化必须同时满足安全边界与登录缓存兼容性。
 */
class RedisConfigSecurityContractTest {

    /** RedisConfig 源码路径。 */
    private static final Path REDIS_CONFIG_SOURCE_PATH = Path.of("src/main/java/com/yr/framework/config/RedisConfig.java");

    /**
     * 验证 Redis 配置源码不再继续使用 Jackson（JSON 库）的宽泛默认多态校验器。
     *
     * @throws IOException 读取源码失败时抛出
     */
    @Test
    void shouldNotUseLaissezFaireSubtypeValidatorInRedisConfig() throws IOException {
        String redisConfigSource = Files.readString(REDIS_CONFIG_SOURCE_PATH, StandardCharsets.UTF_8);

        assertThat(redisConfigSource).doesNotContain("LaissezFaireSubTypeValidator");
        assertThat(redisConfigSource).contains("PolymorphicTypeValidator");
    }

    /**
     * 验证登录缓存对象仍保留当前 `@class` 兼容序列化形态，避免 token cache（令牌缓存）协议漂移。
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldRoundTripLoginUserWithoutBreakingTokenCachePayloadShape() {
        RedisTemplate<Object, Object> redisTemplate = new RedisConfig().redisTemplate(mock(RedisConnectionFactory.class));
        FastJson2JsonRedisSerializer<Object> serializer =
                (FastJson2JsonRedisSerializer<Object>) redisTemplate.getValueSerializer();
        LoginUser expectedLoginUser = createLoginUser();

        byte[] serializedBytes = serializer.serialize(expectedLoginUser);
        String serializedJson = new String(serializedBytes, StandardCharsets.UTF_8);
        Object deserializedObject = serializer.deserialize(serializedBytes);

        assertThat(serializedJson).contains("\"@class\":\"com.yr.common.core.domain.model.LoginUser\"");
        assertThat(serializedJson).contains("\"@class\":\"com.yr.common.core.domain.entity.SysUser\"");
        assertThat(deserializedObject).isInstanceOf(LoginUser.class);

        LoginUser actualLoginUser = (LoginUser) deserializedObject;
        assertThat(actualLoginUser.getToken()).isEqualTo("token-123");
        assertThat(actualLoginUser.getUserId()).isEqualTo(1L);
        assertThat(actualLoginUser.getUsername()).isEqualTo("admin");
        assertThat(actualLoginUser.getUser().getNickName()).isEqualTo("管理员");
        assertThat(actualLoginUser.getPermissions()).containsExactlyInAnyOrder("system:user:list");
        assertThat(actualLoginUser.getRoles()).containsExactlyInAnyOrder("admin");
        assertThat(actualLoginUser.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ACTIVITI_USER");
    }

    /**
     * 验证新配置仍能吃回旧缓存里带有 `admin` 派生字段的 LoginUser 载荷。
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldDeserializeLegacyLoginUserPayloadWithDerivedAdminField() {
        RedisTemplate<Object, Object> redisTemplate = new RedisConfig().redisTemplate(mock(RedisConnectionFactory.class));
        FastJson2JsonRedisSerializer<Object> serializer =
                (FastJson2JsonRedisSerializer<Object>) redisTemplate.getValueSerializer();

        String legacyPayloadJson = "{\"@class\":\"com.yr.common.core.domain.model.LoginUser\","
                + "\"token\":\"token-legacy\","
                + "\"loginTime\":1000,"
                + "\"expireTime\":2000,"
                + "\"permissions\":[\"java.util.LinkedHashSet\",[\"system:user:list\"]],"
                + "\"roles\":[\"java.util.LinkedHashSet\",[\"admin\"]],"
                + "\"authorities\":[\"java.util.ArrayList\",[{\"role\":\"ROLE_ACTIVITI_USER\"}]],"
                + "\"user\":{\"@class\":\"com.yr.common.core.domain.entity.SysUser\","
                + "\"userId\":1,"
                + "\"deptId\":10,"
                + "\"orgId\":20,"
                + "\"userName\":\"admin\","
                + "\"nickName\":\"管理员\","
                + "\"status\":\"0\","
                + "\"admin\":true}}";

        Object deserializedObject = serializer.deserialize(legacyPayloadJson.getBytes(StandardCharsets.UTF_8));

        assertThat(deserializedObject).isInstanceOf(LoginUser.class);
        LoginUser actualLoginUser = (LoginUser) deserializedObject;
        assertThat(actualLoginUser.getToken()).isEqualTo("token-legacy");
        assertThat(actualLoginUser.getUserId()).isEqualTo(1L);
        assertThat(actualLoginUser.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ACTIVITI_USER");
    }

    /**
     * 构造与当前登录缓存链路一致的最小 LoginUser（登录用户）样本。
     *
     * @return 登录缓存样本
     */
    private LoginUser createLoginUser() {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(1L);
        sysUser.setDeptId(10L);
        sysUser.setOrgId(20L);
        sysUser.setUserName("admin");
        sysUser.setNickName("管理员");
        sysUser.setStatus("0");

        LinkedHashSet<String> permissions = new LinkedHashSet<>(java.util.List.of("system:user:list"));
        LinkedHashSet<String> roles = new LinkedHashSet<>(java.util.List.of("admin"));
        ArrayList<SimpleGrantedAuthority> authorities =
                new ArrayList<>(java.util.List.of(new SimpleGrantedAuthority("ROLE_ACTIVITI_USER")));
        LoginUser loginUser = new LoginUser(sysUser, permissions, roles, authorities);
        loginUser.setToken("token-123");
        loginUser.setLoginTime(1_000L);
        loginUser.setExpireTime(2_000L);

        // 使用 LinkedHashSet 锁定当前缓存中常见的集合载荷形态，避免引入 JDK 不可变集合额外噪音。
        return loginUser;
    }
}
