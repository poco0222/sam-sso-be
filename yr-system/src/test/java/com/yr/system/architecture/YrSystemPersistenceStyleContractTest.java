/**
 * @file 锁定 yr-system 服务层风格约束，避免回退到静态代理和字符串查询写法
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 服务层代码风格契约测试。
 */
class YrSystemPersistenceStyleContractTest {

    /** yr-system 模块根目录（module root），用于稳定解析源码路径，避免依赖 working directory。 */
    private static final Path MODULE_ROOT = locateModuleRoot();

    /**
     * 验证角色与部门服务不再依赖 SpringUtils 自取 AOP 代理。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldAvoidSpringUtilsSelfProxyInRoleAndDeptServices() throws IOException {
        assertSourceDoesNotContain("service/impl/SysRoleServiceImpl.java", "SpringUtils.getAopProxy(this)");
        assertSourceDoesNotContain("service/impl/SysDeptServiceImpl.java", "SpringUtils.getAopProxy(this)");
    }

    /**
     * 验证树形服务不再在 service 层拼接 raw SQL descendants 查询。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldMoveDescendantLookupOutOfServiceApplyClauses() throws IOException {
        assertSourceDoesNotContain("service/impl/SysRankServiceImpl.java", ".apply(\"find_in_set");
        assertSourceDoesNotContain("service/impl/SysDutyServiceImpl.java", ".apply(\"find_in_set");
    }

    /**
     * 验证核心服务不再使用字符串化 QueryWrapper 构造器。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldAvoidStringlyTypedQueryWrappersInRefactoredServices() throws IOException {
        assertSourceDoesNotContain("service/impl/SysRankServiceImpl.java", "new QueryWrapper<");
        assertSourceDoesNotContain("service/impl/SysDutyServiceImpl.java", "new QueryWrapper<");
        assertSourceDoesNotContain("service/impl/SysFileServiceImpl.java", "new QueryWrapper<");
        assertSourceDoesNotContain("service/impl/SysUserDutyServiceImpl.java", "new QueryWrapper<");
    }

    /**
     * 验证树形服务不再依赖 updateAllColumnById 做全字段覆盖更新。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldAvoidFullColumnUpdatesInTreeServices() throws IOException {
        assertSourceDoesNotContain("service/impl/SysRankServiceImpl.java", "updateAllColumnById");
        assertSourceDoesNotContain("service/impl/SysDutyServiceImpl.java", "updateAllColumnById");
    }

    /**
     * 验证编码规则头服务不再依赖全字段更新与字符串化头表查询。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldAvoidFullColumnUpdatesAndStringHeaderQueriesInCodeRuleService() throws IOException {
        assertSourceDoesNotContain("service/impl/SysCodeRuleServiceImpl.java", "updateAllColumnById");
        assertSourceDoesNotContain("service/impl/SysCodeRuleServiceImpl.java", "new QueryWrapper<");
    }

    /**
     * 验证用户写服务不再使用字符串化 QueryWrapper 判断默认组织。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldAvoidStringlyTypedQueryWrapperInUserWriteService() throws IOException {
        assertSourceDoesNotContain("service/impl/SysUserWriteService.java", "new QueryWrapper<");
    }

    /**
     * 验证编码规则行与明细服务不再使用字符串化 QueryWrapper / UpdateWrapper。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldAvoidStringlyTypedWrappersInCodeRuleLineAndDetailServices() throws IOException {
        assertSourceDoesNotContain("service/impl/SysCodeRuleLineServiceImpl.java", "new QueryWrapper<");
        assertSourceDoesNotContain("service/impl/SysCodeRuleLineServiceImpl.java", "new UpdateWrapper<");
        assertSourceDoesNotContain("service/impl/SysCodeRuleDetailServiceImpl.java", "new QueryWrapper<");
        assertSourceDoesNotContain("service/impl/SysCodeRuleValueServiceImpl.java", "new QueryWrapper<");
        assertSourceDoesNotContain("service/impl/SysUserOrgServiceImpl.java", "new QueryWrapper<");
        assertSourceDoesNotContain("service/impl/SysUserOrgServiceImpl.java", "new UpdateWrapper<");
        assertSourceDoesNotContain("service/impl/SysRegionServiceImpl.java", "new QueryWrapper<");
        assertSourceDoesNotContain("service/impl/SysUserDeptServiceImpl.java", "new QueryWrapper<");
        assertSourceDoesNotContain("service/impl/SysUserDeptServiceImpl.java", "new UpdateWrapper<");
        assertSourceDoesNotContain("service/impl/SysAttachCategoryServiceImpl.java", "new QueryWrapper<");
        assertSourceDoesNotContain("service/impl/SysAttachCategoryServiceImpl.java", "new UpdateWrapper<");
    }

    /**
     * 验证消息链路不再依赖 ObjectUtils 宽泛判空，也不再使用单线程 StringBuffer。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldAvoidObjectUtilsAndLegacyStringBufferInMessagingFlow() throws IOException {
        assertSourceDoesNotContain("component/message/impl/WebMessageServiceImpl.java", "ObjectUtils");
        assertSourceDoesNotContain("component/message/impl/WebMessageServiceImpl.java", "StringBuffer");
        assertSourceDoesNotContain("component/message/impl/DefaultMessageListenerImpl.java", "ObjectUtils");
        assertSourceDoesNotContain("service/impl/SysMessageBodyReceiverService.java", "ObjectUtils");
        assertSourceDoesNotContain("component/message/impl/WebSocketServerImpl.java", "ObjectUtils");
        assertSourceDoesNotContain("service/impl/SysMsgTemplateService.java", "StringBuffer");
        assertSourceDoesNotContain("service/impl/SysUserServiceImpl.java", "StringBuffer");
        assertSourceDoesNotContain("utils/MatcherUtils.java", "StringBuffer");
        assertRepoSourceDoesNotContain(Path.of("..", "yr-framework", "src", "main", "java",
                "com", "yr", "framework", "aspectj", "AutoMessageAspect.java"), "ObjectUtils");
        assertPathDoesNotExist(Path.of("src", "main", "java", "com", "yr", "system", "utils", "ObjectUtils.java"));
    }

    /**
     * 验证框架层基础配置不再使用单线程场景下的 StringBuffer。
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldAvoidLegacyStringBufferInFrameworkServerConfig() throws IOException {
        assertRepoSourceDoesNotContain(Path.of("..", "yr-framework", "src", "main", "java",
                "com", "yr", "framework", "config", "ServerConfig.java"), "StringBuffer");
    }

    /**
     * 验证 yr-system 消息链路不再依赖 fastjson（JSON 序列化库：Fastjson）。
     *
     * <p>注意：仓库其他模块（例如 yr-common / yr-framework / yr-activiti7）仍真实使用 fastjson，
     * 本契约仅收敛到 yr-system 消息链路的关键类，避免在消息链路中继续引入 fastjson 依赖。</p>
     *
     * @throws IOException 读取源码失败
     */
    @Test
    void shouldAvoidFastjsonInYrSystemMessagingFlow() throws IOException {
        assertSourceDoesNotContain("component/message/AbstractMessageListener.java", "com.alibaba.fastjson");
        assertSourceDoesNotContain("component/message/AbstractMessageListener.java", "JSON.toJSONString");

        assertSourceDoesNotContain("component/message/impl/WebMessageServiceImpl.java", "com.alibaba.fastjson");
        assertSourceDoesNotContain("component/message/impl/WebMessageServiceImpl.java", "JSON.toJSONString");

        assertSourceDoesNotContain("component/message/impl/WebSocketServerImpl.java", "com.alibaba.fastjson");
        assertSourceDoesNotContain("component/message/impl/WebSocketServerImpl.java", "JSON.toJSONString");
    }

    /**
     * 断言源码不包含指定文本。
     *
     * @param relativePath 相对 com/yr/system 的源码路径
     * @param forbiddenText 禁止文本
     * @throws IOException 读取源码失败
     */
    private void assertSourceDoesNotContain(String relativePath, String forbiddenText) throws IOException {
        Path sourcePath = resolveUnderModuleRoot("src/main/java/com/yr/system").resolve(relativePath);
        String sourceText = Files.readString(sourcePath);

        assertThat(sourceText)
                .as("%s 不应再包含 %s", relativePath, forbiddenText)
                .doesNotContain(forbiddenText);
    }

    /**
     * 断言仓库内任意源码不包含指定文本。
     *
     * @param sourcePath 源码路径
     * @param forbiddenText 禁止文本
     * @throws IOException 读取源码失败
     */
    private void assertRepoSourceDoesNotContain(Path sourcePath, String forbiddenText) throws IOException {
        Path resolvedSourcePath = resolveUnderModuleRoot(sourcePath);
        String sourceText = Files.readString(resolvedSourcePath);

        assertThat(sourceText)
                .as("%s 不应再包含 %s", resolvedSourcePath, forbiddenText)
                .doesNotContain(forbiddenText);
    }

    /**
     * 断言指定路径文件已经不存在。
     *
     * @param sourcePath 文件路径
     */
    private void assertPathDoesNotExist(Path sourcePath) {
        Path resolvedSourcePath = resolveUnderModuleRoot(sourcePath);
        assertThat(Files.exists(resolvedSourcePath))
                .as("%s 应当已经移除", resolvedSourcePath)
                .isFalse();
    }

    /**
     * 定位当前测试所属的 Maven module root（模块根目录）。
     *
     * <p>做法：从测试 class 的 CodeSource（通常是 target/test-classes）向上回溯，
     * 找到包含 pom.xml 的目录即认为是 module root。</p>
     *
     * @return 模块根目录
     */
    private static Path locateModuleRoot() {
        try {
            Path codeSourcePath = Paths.get(YrSystemPersistenceStyleContractTest.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI())
                    .toAbsolutePath()
                    .normalize();

            Path current = codeSourcePath;
            while (current != null && !Files.exists(current.resolve("pom.xml"))) {
                current = current.getParent();
            }
            if (current == null) {
                throw new IllegalStateException("无法定位 yr-system module root（未找到 pom.xml）");
            }
            return current;
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("无法解析测试 CodeSource 路径", ex);
        }
    }

    /**
     * 将路径解析为基于 module root 的绝对路径。
     *
     * @param path 路径（可为 absolute 或相对于 module root 的 relative）
     * @return 解析后的绝对路径
     */
    private static Path resolveUnderModuleRoot(Path path) {
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return MODULE_ROOT.resolve(path).normalize();
    }

    /**
     * 将字符串路径解析为基于 module root 的绝对路径。
     *
     * @param relativePath 相对 module root 的路径
     * @return 解析后的绝对路径
     */
    private static Path resolveUnderModuleRoot(String relativePath) {
        return resolveUnderModuleRoot(Path.of(relativePath));
    }
}
