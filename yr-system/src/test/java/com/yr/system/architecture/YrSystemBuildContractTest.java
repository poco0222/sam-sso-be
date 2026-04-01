/**
 * @file 锁定 yr-system 构建契约的 architecture-test
 * @author PopoY
 * @date 2026-03-24
 */
package com.yr.system.architecture;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * yr-system 构建依赖契约测试，确保直接依赖不会被隐式继承。
 */
class YrSystemBuildContractTest {

    // 需要显式声明的 groupId，artifactId 组合，以防止传递依赖被误判为直接依赖。
    private static final String GROUP_ID = "com.baomidou";
    private static final String ARTIFACT_ID = "mybatis-plus-boot-starter";
    // RocketMQ starter 必须显式排除 Tomcat 6 annotations-api，避免运行时覆盖 JSR-250 新接口。
    private static final String ROCKETMQ_GROUP_ID = "org.apache.rocketmq";
    private static final String ROCKETMQ_ARTIFACT_ID = "rocketmq-spring-boot-starter";
    private static final String EXCLUDED_GROUP_ID = "org.apache.tomcat";
    private static final String EXCLUDED_ARTIFACT_ID = "annotations-api";

    /**
     * 确认 yr-system/pom.xml 经过结构化解析后包含指定的 groupId/artifactId 依赖，并在报错中揭示 pom 路径。
     *
     * @throws Exception 解析 pom 或找不到依赖时的异常
     */
    @Test
    void shouldDeclareMybatisPlusBootStarterDependency() throws Exception {
        Path pomPath = resolveModulePom();
        Document document = parsePom(pomPath);

        assertThat(hasDeclaredDependency(document))
                .as("%s 必须显式声明在 %s", ARTIFACT_ID, pomPath)
                .isTrue();
    }

    /**
     * 确认 RocketMQ starter 已显式排除 Tomcat 6 annotations-api，避免启动时把旧版 `javax.annotation.Resource` 放到前面。
     *
     * @throws Exception 解析 pom 或查找 exclusion 失败时抛出
     */
    @Test
    void shouldExcludeTomcatAnnotationsApiFromRocketMqStarter() throws Exception {
        Path pomPath = resolveModulePom();
        Document document = parsePom(pomPath);

        assertThat(hasDeclaredDependencyExclusion(
                document,
                ROCKETMQ_GROUP_ID,
                ROCKETMQ_ARTIFACT_ID,
                EXCLUDED_GROUP_ID,
                EXCLUDED_ARTIFACT_ID
        )).as("%s:%s 必须在 %s 中排除 %s:%s",
                ROCKETMQ_GROUP_ID,
                ROCKETMQ_ARTIFACT_ID,
                pomPath,
                EXCLUDED_GROUP_ID,
                EXCLUDED_ARTIFACT_ID
        ).isTrue();
    }

    /**
     * 确认一期已经彻底移除 duty/rank 历史树服务，避免以“先留着以后再删”的方式重新回流。
     */
    @Test
    void shouldRemoveLegacyDutyAndRankServicesFromPhaseOneBoundary() {
        Path repositoryRoot = resolveRepositoryRoot();

        assertThat(repositoryRoot.resolve("yr-system/src/main/java/com/yr/system/service/impl/SysDutyServiceImpl.java"))
                .as("一期不应继续保留 SysDutyServiceImpl.java")
                .doesNotExist();
        assertThat(repositoryRoot.resolve("yr-system/src/main/java/com/yr/system/service/impl/SysRankServiceImpl.java"))
                .as("一期不应继续保留 SysRankServiceImpl.java")
                .doesNotExist();
    }

    /**
     * 确认 main source set 下不再保留整文件注释掉的 legacy Java 源码。
     */
    @Test
    void shouldRemoveCommentedOutLegacySourceFiles() {
        Path repositoryRoot = resolveRepositoryRoot();

        assertThat(repositoryRoot.resolve("yr-system/src/main/java/com/yr/system/domain/TreeSelect.java"))
                .as("TreeSelect.java 不应继续以整文件注释形式留在 src/main/java")
                .doesNotExist();
        assertThat(repositoryRoot.resolve("yr-system/src/main/java/com/yr/system/domain/entity/SysAttachCategory.java"))
                .as("SysAttachCategory.java 不应继续以整文件注释形式留在 src/main/java")
                .doesNotExist();
    }

    /**
     * 确认 README、执行计划与构建契约都稳定声明 JDK 17 基线，不绑定机器专属路径或本地 AGENTS 文件。
     *
     * @throws IOException 读取文档失败
     */
    @Test
    void shouldDeclareStableJdk17BaselineAcrossReadmePlanAndBuildContract() throws IOException {
        Path repositoryRoot = resolveRepositoryRoot();
        Path readmePath = repositoryRoot.resolve("README.md");
        Path remediationPlanPath = repositoryRoot.resolve("docs/superpowers/plans/2026-03-24-yr-system-official-best-practice-convergence.md");
        Path buildContractPath = repositoryRoot.resolve("pom.xml");

        assertSourceContains(readmePath, "当前构建基线为 `JDK 17 + Spring Boot 2.7.18`");
        assertSourceContains(remediationPlanPath, "**Tech Stack:** `JDK 17`, `Spring Boot 2.7.18`");
        assertSourceContains(buildContractPath, "<java.version>17</java.version>");
        assertSourceContains(buildContractPath, "<release>${java.version}</release>");
    }

    /**
     * 解析模块根目录下的 pom 文件，先尝试基于 Maven 提供的 basedir，再依次向上搜索 CodeSource 与当前路径。
     *
     * @return 指向 yr-system/pom.xml 的 Path
     */
    private Path resolveModulePom() {
        Path basedirCandidate = resolvePomViaBasedir();
        if (basedirCandidate != null) {
            return basedirCandidate;
        }

        Path codeSourceRoot = resolveCodeSourceRoot();
        if (codeSourceRoot != null) {
            Path pomFromCodeSource = findPomUpwards(codeSourceRoot);
            if (pomFromCodeSource != null) {
                return pomFromCodeSource;
            }
        }

        Path currentDir = Path.of(".").toAbsolutePath().normalize();
        Path pomFromCwd = findPomUpwards(currentDir);
        if (pomFromCwd != null) {
            return pomFromCwd;
        }

        throw new IllegalStateException("无法定位 yr-system/pom.xml；basedir="
                + System.getProperty("basedir")
                + ", codeSourceRoot=" + codeSourceRoot
                + ", cwd=" + currentDir);
    }

    /**
     * 根据 yr-system/pom.xml 反推仓库根目录。
     *
     * @return 仓库根目录
     */
    private Path resolveRepositoryRoot() {
        Path moduleRoot = resolveModulePom().getParent();
        if (moduleRoot == null || moduleRoot.getParent() == null) {
            throw new IllegalStateException("无法根据 yr-system/pom.xml 推断仓库根目录");
        }
        return moduleRoot.getParent();
    }

    /**
     * 读取 pom 文件并通过 XML 解析器获取 Document，避免依赖平台默认编码。
     *
     * @param pomPath 要读取的路径
     * @return 解析后的 Document
     * @throws IOException 如果读流失败
     * @throws SAXException 如果 XML 语法不合法
     * @throws ParserConfigurationException XML 解析器配置失败
     */
    private Document parsePom(Path pomPath) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilder builder = createSecureDocumentBuilder();
        try (InputStream stream = Files.newInputStream(pomPath)) {
            Document document = builder.parse(stream);
            document.getDocumentElement().normalize();
            return document;
        }
    }

    /**
     * 断言给定源码文件包含目标文本。
     *
     * @param filePath 文件路径
     * @param expectedText 目标文本
     * @throws IOException 读取文件失败
     */
    private void assertSourceContains(Path filePath, String expectedText) throws IOException {
        String source = Files.readString(filePath, StandardCharsets.UTF_8);

        assertThat(source)
                .as("%s 应包含 %s", filePath, expectedText)
                .contains(expectedText);
    }

    /**
     * 断言给定源码文件不包含目标文本。
     *
     * @param filePath 文件路径
     * @param unexpectedText 不应出现的文本
     * @throws IOException 读取文件失败
     */
    private void assertSourceDoesNotContain(Path filePath, String unexpectedText) throws IOException {
        String source = Files.readString(filePath, StandardCharsets.UTF_8);

        assertThat(source)
                .as("%s 不应包含 %s", filePath, unexpectedText)
                .doesNotContain(unexpectedText);
    }

    /**
     * 遍历解析后的 pom 文档，确认所需的 groupId + artifactId 组合存在。
     *
     * @param document pom Document
     * @return 如果找到了预期依赖则返回 true
     */
    private boolean hasDeclaredDependency(Document document) {
        return findDependencyElement(document, GROUP_ID, ARTIFACT_ID) != null;
    }

    /**
     * 遍历解析后的 pom 文档，确认目标依赖下是否存在指定 exclusion。
     *
     * @param document pom Document
     * @param dependencyGroupId 目标依赖 groupId
     * @param dependencyArtifactId 目标依赖 artifactId
     * @param exclusionGroupId 期望排除的 groupId
     * @param exclusionArtifactId 期望排除的 artifactId
     * @return 如果目标 exclusion 已声明则返回 true
     */
    private boolean hasDeclaredDependencyExclusion(
            Document document,
            String dependencyGroupId,
            String dependencyArtifactId,
            String exclusionGroupId,
            String exclusionArtifactId
    ) {
        Element dependency = findDependencyElement(document, dependencyGroupId, dependencyArtifactId);
        if (dependency == null) {
            return false;
        }
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList exclusionNodes = (NodeList) xpath.evaluate(
                    "exclusions/exclusion",
                    dependency,
                    XPathConstants.NODESET
            );
            for (int i = 0; i < exclusionNodes.getLength(); i++) {
                Node node = exclusionNodes.item(i);
                if (!(node instanceof Element exclusion)) {
                    continue;
                }
                String actualGroupId = xpath.evaluate("groupId", exclusion).trim();
                String actualArtifactId = xpath.evaluate("artifactId", exclusion).trim();
                if (exclusionGroupId.equals(actualGroupId) && exclusionArtifactId.equals(actualArtifactId)) {
                    return true;
                }
            }
            return false;
        } catch (XPathExpressionException ex) {
            throw new IllegalStateException("无法在 pom 中定位 exclusion 定义", ex);
        }
    }

    /**
     * 遍历解析后的 pom 文档，定位指定的直接依赖节点。
     *
     * @param document pom Document
     * @param expectedGroupId 依赖 groupId
     * @param expectedArtifactId 依赖 artifactId
     * @return 命中的 dependency 元素；未找到时返回 null
     */
    private Element findDependencyElement(Document document, String expectedGroupId, String expectedArtifactId) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList dependencyNodes = (NodeList) xpath.evaluate(
                    "/project/dependencies/dependency",
                    document,
                    XPathConstants.NODESET
            );
            for (int i = 0; i < dependencyNodes.getLength(); i++) {
                Node node = dependencyNodes.item(i);
                if (!(node instanceof Element dependency)) {
                    continue;
                }
                String actualGroupId = xpath.evaluate("groupId", dependency).trim();
                String actualArtifactId = xpath.evaluate("artifactId", dependency).trim();
                if (expectedGroupId.equals(actualGroupId) && expectedArtifactId.equals(actualArtifactId)) {
                    return dependency;
                }
            }
            return null;
        } catch (XPathExpressionException ex) {
            throw new IllegalStateException("无法在 pom 中定位直接依赖定义", ex);
        }
    }

    /**
     * 使用 basedir 属性定位 pom，如果属性存在且文件可用则直接返回。
     *
     * @return 基于 basedir 的 pom 路径或 null
     */
    private Path resolvePomViaBasedir() {
        String basedir = System.getProperty("basedir");
        if (basedir == null || basedir.isBlank()) {
            return null;
        }
        Path basePath = Path.of(basedir).toAbsolutePath().normalize();
        Path pomFile = basePath.resolve("pom.xml");
        if (Files.exists(pomFile) && Files.isRegularFile(pomFile) && isYrSystemPom(pomFile)) {
            return pomFile;
        }
        return null;
    }

    /**
     * 尝试通过当前测试类的 CodeSource 定位工作目录，用于向上查找 pom。
     *
     * @return classpath 根目录或 null
     */
    private Path resolveCodeSourceRoot() {
        CodeSource codeSource = YrSystemBuildContractTest.class.getProtectionDomain().getCodeSource();
        if (codeSource == null) {
            return null;
        }
        try {
            Path location = Path.of(codeSource.getLocation().toURI());
            Path directory = Files.isRegularFile(location) ? location.getParent() : location;
            return directory == null ? null : directory.toAbsolutePath().normalize();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("无法解析 CodeSource 路径", e);
        }
    }

    /**
     * 从起始目录向上递归查找 pom 文件。
     *
     * @param start 起始目录
     * @return 找到的 pom 路径或 null
     */
    private Path findPomUpwards(Path start) {
        if (start == null) {
            return null;
        }
        Path current = start.toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve("pom.xml");
            if (Files.exists(candidate) && Files.isRegularFile(candidate) && isYrSystemPom(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }

    /**
     * 判断给定 pom 是否真正属于 yr-system 模块，避免误命中聚合根 pom。
     *
     * @param pomPath 候选 pom 路径
     * @return true 表示该 pom 的 artifactId 为 yr-system
     */
    private boolean isYrSystemPom(Path pomPath) {
        try {
            Document document = parsePom(pomPath);
            XPath xpath = XPathFactory.newInstance().newXPath();
            String artifactId = xpath.evaluate("/project/artifactId", document).trim();
            return "yr-system".equals(artifactId);
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException ex) {
            throw new IllegalStateException("无法校验 pom 是否属于 yr-system: " + pomPath, ex);
        }
    }

    /**
     * 创建开启安全配置的 XML 解析器，避免 XXE 等外部实体风险。
     *
     * @return 安全的 DocumentBuilder
     * @throws ParserConfigurationException XML 解析器配置失败
     */
    private DocumentBuilder createSecureDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setExpandEntityReferences(false);
        factory.setXIncludeAware(false);
        return factory.newDocumentBuilder();
    }
}
