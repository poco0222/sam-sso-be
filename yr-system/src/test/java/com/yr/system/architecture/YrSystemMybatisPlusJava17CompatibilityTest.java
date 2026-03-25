/**
 * @file 锁定 MyBatis-Plus 在 Java 17 下的 LambdaQueryWrapper 兼容性
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.system.architecture;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.yr.common.core.domain.entity.SsoClient;
import com.yr.system.service.impl.SsoClientServiceImpl;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * MyBatis-Plus 与 Java 17 的兼容性回归测试。
 */
class YrSystemMybatisPlusJava17CompatibilityTest {

    /**
     * 验证 LambdaQueryWrapper 在 Java 17 下解析 SsoClient lambda 字段时不会触发反射初始化异常。
     */
    @Test
    void shouldResolveSsoClientLambdaColumnsOnJava17() {
        initializeSsoClientTableInfo();

        // 使用与客户端管理列表一致的 LambdaQueryWrapper 访问路径，直接覆盖 sqlSegment 构造逻辑。
        LambdaQueryWrapper<SsoClient> queryWrapper = new LambdaQueryWrapper<SsoClient>()
                .like(SsoClient::getClientCode, "demo")
                .like(SsoClient::getClientName, "Demo Client")
                .eq(SsoClient::getStatus, "0")
                .orderByAsc(SsoClient::getClientId);

        assertThatCode(queryWrapper::getSqlSegment)
                .as("LambdaQueryWrapper 在 Java 17 下应能正常解析 SsoClient lambda 字段")
                .doesNotThrowAnyException();

        // 额外断言生成的 SQL segment（SQL 片段）中包含稳定的排序列，防止测试只验证“不抛异常”。
        assertThat(queryWrapper.getSqlSegment())
                .contains("client_id");
    }

    /**
     * 验证客户端列表服务在带查询条件时，能够完整走到 list(queryWrapper) 而不会因 lambda 字段解析失败。
     */
    @Test
    void shouldBuildSsoClientListQueryThroughServicePathOnJava17() {
        initializeSsoClientTableInfo();

        AtomicReference<String> sqlSegmentRef = new AtomicReference<>();
        SsoClientServiceImpl service = createProbeService(sqlSegmentRef);
        SsoClient query = new SsoClient();
        query.setClientCode("portal");
        query.setClientName("Portal");
        query.setStatus("0");

        List<SsoClient> result = service.selectSsoClientList(query);

        assertThat(result).isEmpty();
        assertThat(sqlSegmentRef.get())
                .contains("client_code")
                .contains("client_name")
                .contains("status")
                .contains("client_id");
    }

    /**
     * 验证客户端列表服务在空查询条件下，仍能稳定生成默认排序片段。
     */
    @Test
    void shouldBuildDefaultOrderSegmentForNullSsoClientQueryOnJava17() {
        initializeSsoClientTableInfo();

        AtomicReference<String> sqlSegmentRef = new AtomicReference<>();
        SsoClientServiceImpl service = createProbeService(sqlSegmentRef);

        List<SsoClient> result = service.selectSsoClientList(null);

        assertThat(result).isEmpty();
        assertThat(sqlSegmentRef.get())
                .contains("client_id")
                .doesNotContain("client_code")
                .doesNotContain("client_name")
                .doesNotContain("status");
    }

    /**
     * 初始化 SsoClient 的 TableInfo（表元数据）缓存，贴近真实 mapper 启动后的运行前置条件。
     */
    private void initializeSsoClientTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), SsoClient.class);
    }

    /**
     * 创建仅用于测试的 service 探针，在 list(queryWrapper) 入口捕获 SQL segment（SQL 片段）。
     *
     * @param sqlSegmentRef SQL 片段接收器
     * @return 测试用服务实例
     */
    private SsoClientServiceImpl createProbeService(AtomicReference<String> sqlSegmentRef) {
        return new SsoClientServiceImpl() {
            @Override
            public List<SsoClient> list(Wrapper<SsoClient> queryWrapper) {
                sqlSegmentRef.set(queryWrapper.getSqlSegment());
                return Collections.emptyList();
            }
        };
    }
}
