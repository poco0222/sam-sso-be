/**
 * @file Redis 配置安全契约测试
 * @author PopoY
 * @date 2026-03-31
 */
package com.yr.framework.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.yr.common.core.domain.entity.SysDept;
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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 锁定 Redis（缓存）序列化必须同时满足安全边界与登录缓存兼容性。
 */
class RedisConfigSecurityContractTest {

    /** RedisConfig 源码路径。 */
    private static final Path REDIS_CONFIG_SOURCE_PATH = Path.of("src/main/java/com/yr/framework/config/RedisConfig.java");

    /**
     * 验证 Redis 配置源码已从宽泛 package 前缀白名单收紧为具体 subtype（子类型）白名单。
     *
     * @throws IOException 读取源码失败时抛出
     */
    @Test
    void shouldUseConcreteSubtypeAllowListInRedisConfig() throws IOException {
        String redisConfigSource = Files.readString(REDIS_CONFIG_SOURCE_PATH, StandardCharsets.UTF_8);

        assertThat(redisConfigSource).doesNotContain("LaissezFaireSubTypeValidator");
        assertThat(redisConfigSource).doesNotContain(".allowIfSubType(\"java.lang.\")");
        assertThat(redisConfigSource).doesNotContain(".allowIfSubType(\"java.util.\")");
        assertThat(redisConfigSource).doesNotContain(".allowIfSubType(\"java.time.\")");
        assertThat(redisConfigSource).contains("DefaultTyping.NON_FINAL");
        assertThat(redisConfigSource).contains(".allowIfSubType(LoginUser.class)");
        assertThat(redisConfigSource).contains(".allowIfSubType(SysUser.class)");
        assertThat(redisConfigSource).contains(".allowIfSubType(SysDept.class)");
        assertThat(redisConfigSource).contains(".allowIfSubType(LinkedHashSet.class)");
        assertThat(redisConfigSource).contains(".allowIfSubType(ArrayList.class)");
        assertThat(redisConfigSource).contains(".allowIfSubType(HashMap.class)");
        assertThat(redisConfigSource).contains(".allowIfSubType(Date.class)");
        assertThat(redisConfigSource).contains(".allowIfSubType(Long.class)");
    }

    /**
     * 验证登录缓存对象 round-trip（往返）后仍保持兼容。
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
        assertThat(deserializedObject).isInstanceOf(LoginUser.class);

        LoginUser actualLoginUser = (LoginUser) deserializedObject;
        assertThat(actualLoginUser.getToken()).isEqualTo("token-123");
        assertThat(actualLoginUser.getUserId()).isEqualTo(1L);
        assertThat(actualLoginUser.getUsername()).isEqualTo("admin");
        assertThat(actualLoginUser.getUser().getNickName()).isEqualTo("管理员");
        assertThat(actualLoginUser.getUser().getLoginDate()).isEqualTo(new Date(1_710_000_000_000L));
        assertThat(actualLoginUser.getUser().getCreateTime()).isEqualTo(new Date(1_709_999_000_000L));
        assertThat(actualLoginUser.getUser().getUpdateTime()).isEqualTo(new Date(1_710_001_000_000L));
        assertThat(actualLoginUser.getUser().getDept()).isNotNull();
        assertThat(actualLoginUser.getUser().getDept().getDeptName()).isEqualTo("研发部");
        assertThat(actualLoginUser.getUser().getParams()).containsEntry("traceId", "trace-001");
        assertThat(actualLoginUser.getPermissions()).containsExactlyInAnyOrder("system:user:list");
        assertThat(actualLoginUser.getRoles()).containsExactlyInAnyOrder("admin");
        assertThat(actualLoginUser.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ACTIVITI_USER");
    }

    /**
     * 验证新配置仍能吃回旧的 `NON_FINAL` LoginUser 载荷，包括 typed Date（带类型日期）与 `admin` 派生字段。
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldDeserializeLegacyNonFinalLoginUserPayloadWithDatesAndDerivedAdminField() throws IOException {
        RedisTemplate<Object, Object> redisTemplate = new RedisConfig().redisTemplate(mock(RedisConnectionFactory.class));
        FastJson2JsonRedisSerializer<Object> serializer =
                (FastJson2JsonRedisSerializer<Object>) redisTemplate.getValueSerializer();
        String legacyPayloadJson = createLegacyNonFinalPayloadWithDerivedAdminField();

        Object deserializedObject = serializer.deserialize(legacyPayloadJson.getBytes(StandardCharsets.UTF_8));

        assertThat(deserializedObject).isInstanceOf(LoginUser.class);
        LoginUser actualLoginUser = (LoginUser) deserializedObject;
        assertThat(actualLoginUser.getToken()).isEqualTo("token-legacy");
        assertThat(actualLoginUser.getUserId()).isEqualTo(1L);
        assertThat(actualLoginUser.getUser().getLoginDate()).isEqualTo(new Date(1_710_000_000_000L));
        assertThat(actualLoginUser.getUser().getDept()).isNotNull();
        assertThat(actualLoginUser.getUser().getDept().getDeptName()).isEqualTo("研发部");
        assertThat(actualLoginUser.getUser().getParams()).containsEntry("traceId", "trace-legacy");
        assertThat(actualLoginUser.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ACTIVITI_USER");
    }

    /**
     * 验证 `SameUrlDataInterceptor` 的嵌套 `HashMap` 载荷在新配置下仍可 round-trip。
     */
    @Test
    @SuppressWarnings("unchecked")
    void shouldRoundTripRepeatSubmitHashMapPayload() {
        RedisTemplate<Object, Object> redisTemplate = new RedisConfig().redisTemplate(mock(RedisConnectionFactory.class));
        FastJson2JsonRedisSerializer<Object> serializer =
                (FastJson2JsonRedisSerializer<Object>) redisTemplate.getValueSerializer();
        Map<String, Object> previousData = new HashMap<>();
        previousData.put("repeatParams", "{\"userId\":1}");
        previousData.put("repeatTime", 1_710_000_123_000L);
        Map<String, Object> repeatSubmitCache = new HashMap<>();
        repeatSubmitCache.put("/system/user/list", previousData);

        byte[] serializedBytes = serializer.serialize(repeatSubmitCache);
        Object deserializedObject = serializer.deserialize(serializedBytes);

        assertThat(deserializedObject).isInstanceOf(Map.class);
        Map<String, Object> actualCache = (Map<String, Object>) deserializedObject;
        assertThat(actualCache).containsKey("/system/user/list");
        assertThat(actualCache.get("/system/user/list")).isInstanceOf(Map.class);
        assertThat((Map<String, Object>) actualCache.get("/system/user/list"))
                .containsEntry("repeatParams", "{\"userId\":1}")
                .containsEntry("repeatTime", 1_710_000_123_000L);
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
        sysUser.setLoginDate(new Date(1_710_000_000_000L));
        sysUser.setCreateTime(new Date(1_709_999_000_000L));
        sysUser.setUpdateTime(new Date(1_710_001_000_000L));
        sysUser.setParams(new HashMap<>(Map.of("traceId", "trace-001")));
        SysDept dept = new SysDept();
        dept.setDeptId(10L);
        dept.setDeptName("研发部");
        sysUser.setDept(dept);

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

    /**
     * 按历史 `NON_FINAL` Redis 配置生成 legacy payload，覆盖 typed Date（带类型日期）兼容面。
     *
     * @return 旧缓存 JSON 载荷
     * @throws IOException 序列化失败时抛出
     */
    private String createLegacyNonFinalPayloadWithDerivedAdminField() throws IOException {
        LoginUser loginUser = createLoginUser();
        loginUser.setToken("token-legacy");
        loginUser.getUser().getParams().put("traceId", "trace-legacy");

        ObjectMapper legacyMapper = new ObjectMapper();
        legacyMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        legacyMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        legacyMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        legacyMapper.addMixIn(SimpleGrantedAuthority.class, SimpleGrantedAuthorityRedisMixin.class);
        legacyMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.yr.common.core.domain.")
                        .allowIfSubType("java.lang.")
                        .allowIfSubType("java.util.")
                        .allowIfSubType("java.time.")
                        .allowIfSubType("org.springframework.security.core.authority.")
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        String legacyPayloadJson = legacyMapper.writeValueAsString(loginUser);
        return legacyPayloadJson.replace("\"status\":\"0\"}", "\"status\":\"0\",\"admin\":true}");
    }

    /**
     * 为测试中的 `SimpleGrantedAuthority` 构造兼容旧 Redis payload 的反序列化签名。
     */
    private abstract static class SimpleGrantedAuthorityRedisMixin {

        /**
         * @param role Spring Security 角色编码
         */
        @JsonCreator
        SimpleGrantedAuthorityRedisMixin(@JsonProperty("role") String role) {
        }
    }
}
