# AGENTS

## 项目概览
- YR 后端，多模块 Maven 工程，当前基线为 `JDK 17 + Spring Boot 2.7.18`。
- 入口模块是 `yr-admin`，启动类为 `yr-admin/src/main/java/com/yr/YrApplication.java`。
- 当前仓库是从既有平台工程中裁剪后的后端模板，保留系统底座能力，便于后续项目复用。
- 当前阶段重点模块是 `yr-system`，用于系统管理、权限、字典、消息等基础能力。

## 强制构建基线
- 执行 Maven 命令前先运行 `mvn -v`，确认当前 Java 版本不是 `1.8.x`。
- `yr-system` 的标准验证入口如下：

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system test
```

- 预期输出为 `BUILD SUCCESS`。
- 如果出现 `无效的标记: --release`，说明当前 shell 仍在使用 JDK 8，需要先切到 JDK 17 再重试。

## 模块结构
- `yr-admin`: Web 入口，聚合系统依赖并提供启动能力。
- `yr-framework`: 核心框架与基础配置。
- `yr-system`: 系统管理、消息、权限、字典等基础功能。
- `yr-common`: 通用工具与公共实体。
- `yr-quartz`: 定时任务支持。
- `yr-activiti7`: 工作流引擎集成。

## 常用命令
- 构建全部模块: `mvn package`
- 仅构建入口模块及依赖: `mvn -pl yr-admin -am package`
- 本地启动入口模块: `mvn -pl yr-admin -am spring-boot:run`
- 验证 `yr-system`: `mvn -pl yr-system test`
- 运行打包产物: `java -jar yr-admin/target/yr-admin.jar`
- 脚本管理进程: `cd yr-admin && ../ry.sh start|stop|restart|status`
- Windows 脚本: `ry.bat`

## 配置与环境
- 主配置: `yr-admin/src/main/resources/application.yml`
- 环境配置: `yr-admin/src/main/resources/application-local.yml`、`application-dev.yml`、`application-prod.yml`
- 常见环境变量覆盖:
  - `SPRING_PROFILES_ACTIVE`, `SERVER_PORT`
  - `SPRING_REDIS_HOST`, `SPRING_REDIS_PORT`, `SPRING_REDIS_PASSWORD`
  - `TOKEN_SECRET`, `TOKEN_EXPIRE_TIME`
  - `SPRING_LIQUIBASE_ENABLED`
- 数据源基于 Druid，默认 MySQL，SQL Server 从库可选，配置在 `spring.datasource.druid.*`
- 日志配置: `yr-admin/src/main/resources/logback-spring.xml`
- Swagger 开关与前缀: `swagger.enabled`, `swagger.pathMapping`
- 文件上传/存储配置: `yr.profile`, `file.*`
- Liquibase 变更入口: `yr-admin/src/main/resources/db/liquibase/master.xml`

## 代码组织与约定
- MyBatis-Plus Mapper XML 放在 `resources/mapper/**/*Mapper.xml`
- Mapper 扫描包为 `com.yr.**.mapper`
- `yr-system` 单元测试位于 `yr-system/src/test/java/com/yr/system/**`

## 相关文档与数据
- 企业微信登录方案: `backend-wxwork-api.md`
