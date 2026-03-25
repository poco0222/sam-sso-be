/**
 * @file INIT_IMPORT local profile 数据源契约测试
 * @author PopoY
 * @date 2026-03-25
 */
package com.yr.sso;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 锁定 local profile 下 `local_sam -> local_sam_empty` 的导入拓扑，避免 source/target 再漂回模板库。
 */
class SsoInitImportLocalProfileDatasourceContractTest {

    /** `application-local.yml` 源文件路径。 */
    private static final Path APPLICATION_LOCAL_PATH = Path.of("src/main/resources/application-local.yml");

    /**
     * 验证 local profile 默认把 `local_sam` 作为只读 source，把 `local_sam_empty` 作为 target。
     *
     * @throws IOException 读取配置文件失败时抛出
     */
    @Test
    void shouldUseLocalSamAsReadonlySourceAndLocalSamEmptyAsTarget() throws IOException {
        String applicationLocal = Files.readString(APPLICATION_LOCAL_PATH);

        assertThat(applicationLocal).contains("SPRING_DATASOURCE_DRUID_MASTER_URL:jdbc:mysql://127.0.0.1:3306/local_sam_empty");
        assertThat(applicationLocal).contains("SPRING_DATASOURCE_DRUID_MASTER_USERNAME:root");
        assertThat(applicationLocal).contains("SPRING_DATASOURCE_DRUID_MASTER_PASSWORD:Popo0222");
        assertThat(applicationLocal).contains("enabled: true");
        assertThat(applicationLocal).contains("SPRING_DATASOURCE_DRUID_SLAVE_URL:jdbc:mysql://127.0.0.1:3306/local_sam");
        assertThat(applicationLocal).contains("SPRING_DATASOURCE_DRUID_SLAVE_USERNAME:root");
        assertThat(applicationLocal).contains("SPRING_DATASOURCE_DRUID_SLAVE_PASSWORD:Popo0222");
        assertThat(applicationLocal).contains("legacy-source-datasource: ${SSO_INIT_IMPORT_LEGACY_SOURCE_DATASOURCE:SLAVE}");
    }
}
