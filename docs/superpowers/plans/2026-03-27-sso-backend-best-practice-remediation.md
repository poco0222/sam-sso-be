# Sam-Sso-Be Backend Best Practice Remediation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `sam-sso-be` 收敛到当前仓库一期边界下可接受的 `Spring Boot 2.7.18 + JDK 17` 官方最佳实践基线，先修 `security`（安全）与 `correctness`（正确性）问题，再收口 `controller/service layering`（控制层/服务层分层）、`transaction semantics`（事务语义）、`configuration hygiene`（配置卫生）和 `module boundary`（模块边界）债务。

**Architecture:** 采用 `Phase 0 gate repair -> Phase 1 security closure -> Phase 2 correctness closure -> Phase 3 controller/auth hardening -> Phase 4 framework convergence -> Phase 5 dependency and module governance` 的顺序串行推进。前两阶段只做最小必要改动，确保仓库先从“可泄露、可误用、可回滚丢状态”恢复到“行为可信”；后两阶段才处理 `WebSecurityConfigurerAdapter` 迁移、字段注入收敛、`yr-common` 瘦身和 `yr-framework -> yr-system` 反向依赖。

**Tech Stack:** `JDK 17`, `Spring Boot 2.7.18`, `Spring Security`, `Spring MVC`, `Spring Test`, `JUnit 5`, `Mockito`, `AssertJ`, `MyBatis`, `MyBatis-Plus`, `Liquibase`, `Maven`

---

## Working Context

- **Execution cwd:** `/Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be`
- **Verified JDK 17 path on this machine:** `/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home`
- **Primary scope:** `yr-admin`, `yr-framework`, `yr-system`, `yr-common`
- **Boundary guardrail:** 当前仓库仍按 `SSO + MQ 组织架构消息推送 + user/org/dept` 一期范围收敛；整改过程中不要顺手把 `role/menu/workflow/monitor` 一类 legacy 平台能力重新扩回主路径。

## Plan Positioning

- 本计划是对本轮后端只读 audit 的执行化落库，不是泛泛的 review 摘要。
- 本计划优先解决当前已经确认的 `P0/P1` 问题，不把执行重心转移到“看起来更现代”的大规模重写。
- 本计划默认在现有多模块 Maven 结构上做 staged remediation（分阶段整改）；只有在前置阶段验证通过后，才推进 `module split`（模块拆分）或大规模依赖替换。

## Scope Guardrails

- 所有功能性修复必须先补 failing test（失败测试）或 failing contract check（失败契约检查），再做最小实现。
- 所有新增/修改 Java 代码注释必须满足仓库规则，作者统一使用 `PopoY`。
- 任何会改变 API shape（接口形态）的整改，都要在任务内明确“兼容保留”还是“有意调整”；不要隐式改前后端契约。
- 任何会影响生产访问面的整改，都必须优先做 `local profile`（本地环境）与 `prod profile`（生产环境）区分，避免为了方便开发把生产安全面继续放开。
- `Phase 0` 未完成前，不要宣称仓库“测试已恢复可信”；因为当前 `mvn test` 已经被 stale tests（过时测试）污染。

## Execution Order

1. `Task 0` 修复现有失真的 regression gate（回归门禁）
2. `Task 1` 关闭最高风险安全暴露面
3. `Task 2` 修复文件接口与权限边界
4. `Task 3` 修复同步任务事务与 payload 契约
5. `Task 4` 收紧异常语义、登录链路和控制层依赖注入
6. `Task 5` 迁移安全基础设施到 Spring Boot 2.7 推荐写法
7. `Task 6` 处理模块边界、公共模块瘦身与依赖升级
8. `Task 7` 全量验证、文档收口与执行 handoff（交接）

## Current Baseline

- 已验证 `mvn -v` 正常，当前 shell 使用 `Java version: 17.0.18`。
- 已验证根 POM 已统一 `java.version=17` 和 `spring-boot.version=2.7.18`。
- 已验证 `mvn test` 当前失败，并且失败同时包含真实治理问题与测试漂移：
  - `YrSystemBuildContractTest` 依赖不存在的 `sam-sso-be/AGENTS.md`
  - `YrSystemPhase2ArchitectureTest` 把静态 `Logger` 也误判成 legacy 依赖
  - `YrSystemSqlContractTest` 仍要求已不存在的 `SysFileMapper.xml`
- 已确认当前最高风险问题包括：
  - 已登录用户可读取数据库与 Redis 凭据的系统信息接口
  - Swagger/Druid 匿名开放且保留弱默认口令
  - `GET /common/fileDelete` + 未做路径归一化校验
  - 多个 controller 方法缺少 `@PreAuthorize`
  - `SsoSyncTaskServiceImpl` 失败状态在事务回滚后无法持久化
  - 全局异常与登录链路直接把内部异常信息返回给前端

## Findings Summary

### P0: Must Fix Immediately

- `SystemInfoController` 直接返回带 `username/password` 的配置对象。
- `SecurityConfig` 匿名放行 `/swagger-ui/**`、`/v3/api-docs/**`、`/druid/**`。
- `application-local.yml` / `application-prod.yml` 仍保留可预测默认值或显式弱口令。
- `CommonController.fileDelete` 使用 `GET` 删除文件，并直接拼接本地路径。

### P1: High Risk Functional Debt

- `SsoSyncTaskServiceImpl.executeTask` 在事务内 `updateById(task)` 后继续 `throw`，失败状态会回滚丢失。
- `buildCompensationPayload` 手工拼接 JSON，缺乏转义与结构化序列化。
- `GlobalExceptionHandler`、`CommonController`、`CaptchaController`、`SysLoginService`、`SysMobileLoginService` 多处把 `exception.getMessage()` 直接返回给前端。
- `SsoClientController.resolveOperator` 吞掉所有异常返回 `null`，破坏审计字段可信度。
- `SysUserController`、`SysDeptController`、`SwaggerController`、`TestController` 等仍保留无权限或弱权限的可达入口。

### P2: Best Practice Convergence Debt

- `SecurityConfig` 仍基于 `WebSecurityConfigurerAdapter`，不符合 Spring Security 5.7+ 推荐模式。
- `ResourcesConfig` 在 `allowCredentials=true` 时仍允许 `*` origin pattern，缺少 environment-driven allowlist（环境驱动白名单）。
- `SysMobileLoginService` 大量 `@Autowired(required = false)` 与未发现调用方的移动端遗留逻辑混在主代码中。
- `yr-framework` 直接依赖 `yr-system`，存在典型分层反转。
- `yr-common` 过度膨胀，带入 `spring-web`、`Spring Security`、`Redis`、`MyBatis-Plus`、`RocketMQ` 等重量依赖。
- `fastjson 1.2.76`、`jjwt 0.9.1`、`okhttp 3.14.9` 等依赖版本偏旧。

## File Map

### Runtime Surface

- Modify: `yr-admin/src/main/java/com/yr/web/controller/common/SystemInfoController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/common/CommonController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/common/CaptchaController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/tool/SwaggerController.java`
- Modify or Delete: `yr-admin/src/main/java/com/yr/web/controller/tool/TestController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysUserController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysDeptController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/sso/SsoClientController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/domain/DatabaseInfo.java`
- Modify: `yr-admin/src/main/java/com/yr/web/domain/RedisInfo.java`
- Create: `yr-admin/src/main/java/com/yr/web/domain/SystemInfoView.java`

### Security / Framework

- Modify: `yr-framework/src/main/java/com/yr/framework/config/SecurityConfig.java`
- Modify: `yr-framework/src/main/java/com/yr/framework/config/ResourcesConfig.java`
- Modify: `yr-framework/src/main/java/com/yr/framework/web/exception/GlobalExceptionHandler.java`
- Modify: `yr-framework/src/main/java/com/yr/framework/web/service/SysLoginService.java`
- Modify or Remove: `yr-framework/src/main/java/com/yr/framework/web/service/SysMobileLoginService.java`
- Modify: `yr-framework/src/main/java/com/yr/framework/web/service/UserDetailsServiceImpl.java`
- Modify: `yr-framework/src/main/java/com/yr/framework/web/service/PermissionService.java`
- Modify: `yr-framework/src/main/java/com/yr/framework/security/filter/JwtAuthenticationTokenFilter.java`

### Domain / System

- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SsoSyncTaskServiceImpl.java`
- Create: `yr-system/src/main/java/com/yr/system/service/support/SsoSyncTaskFailureRecorder.java`
- Modify: `yr-system/src/main/java/com/yr/system/domain/dto/SsoDistributionMessagePayload.java`

### Common / Utility / Build

- Modify: `yr-common/src/main/java/com/yr/common/utils/file/FileUtils.java`
- Modify: `yr-common/src/main/java/com/yr/common/utils/file/FileUploadUtils.java`
- Modify: `yr-common/pom.xml`
- Modify: `yr-framework/pom.xml`
- Modify: `pom.xml`

### Config / Profiles

- Modify: `yr-admin/src/main/resources/application-local.yml`
- Modify: `yr-admin/src/main/resources/application-prod.yml`
- Modify: `yr-admin/src/main/resources/application-dev.yml`

### Tests

- Modify: `yr-system/src/test/java/com/yr/system/architecture/YrSystemBuildContractTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/architecture/YrSystemPhase2ArchitectureTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/sso/SsoClientControllerContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/system/SysSystemControllerPermissionContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/login/SysLoginControllerContractTest.java`
- Create: `yr-admin/src/test/java/com/yr/config/SsoSecuritySurfaceContractTest.java`
- Create: `yr-admin/src/test/java/com/yr/web/controller/common/SystemInfoControllerContractTest.java`
- Create: `yr-admin/src/test/java/com/yr/web/controller/common/CommonControllerSecurityContractTest.java`
- Create: `yr-framework/src/test/java/com/yr/framework/config/SecurityConfigContractTest.java`
- Create: `yr-framework/src/test/java/com/yr/framework/web/exception/GlobalExceptionHandlerContractTest.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SsoSyncTaskFailurePersistenceTest.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SsoSyncTaskPayloadJsonContractTest.java`
- Create: `yr-framework/src/test/java/com/yr/framework/web/service/SysMobileLoginServiceContractTest.java`
- Create: `yr-framework/src/test/java/com/yr/framework/architecture/YrFrameworkBuildBoundaryContractTest.java`

## Decision Checkpoints

- **Demo surface default path:** 默认移除 `TestController`；如果必须保留，只允许在 `local profile` 下启用，并补显式说明。
- **Swagger/Druid default path:** 默认只在 `local profile` 或显式 `ops.enabled=true` 条件下暴露，不再对匿名用户开放。
- **SystemInfo default path:** 默认保留 endpoint，但只返回脱敏后的运行态摘要，并加显式权限；不要再直接返回 `ConfigurationProperties` bean。
- **SysMobileLoginService default path:** 先用 `rg` 验证调用方；如果仓库内无 controller/router/测试入口消费它，默认从主执行路径移出，再决定是否彻底删除。
- **Module split default path:** 先消除 `yr-framework -> yr-system` 的反向依赖，再做 `yr-common` 拆分；不要一开始就同时做两类大手术。

### Task 0: 修复当前不可信的 regression gate

**Files:**
- Modify: `yr-system/src/test/java/com/yr/system/architecture/YrSystemBuildContractTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/architecture/YrSystemPhase2ArchitectureTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlContractTest.java`

- [x] **Step 1: 先冻结当前失败基线**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=YrSystemBuildContractTest,YrSystemPhase2ArchitectureTest,YrSystemSqlContractTest test
```

Expected:
- `YrSystemBuildContractTest` 因不存在的 `sam-sso-be/AGENTS.md` 失败
- `YrSystemPhase2ArchitectureTest` 因把静态 `Logger` 误判为依赖失败
- `YrSystemSqlContractTest` 因引用不存在的 `SysFileMapper.xml` 失败

- [x] **Step 2: 让测试表达“真实仓库契约”而不是“环境偶然状态”**

实现要求：
- `YrSystemBuildContractTest` 不再要求仓库内存在机器专属路径或本地 `AGENTS.md` 文件；改为校验 README / plan doc / build contract 中对 `JDK 17` 的稳定描述。
- `YrSystemPhase2ArchitectureTest` 只审计实例字段，不把静态 `Logger` 计入 legacy 依赖。
- `YrSystemSqlContractTest` 若一期边界内确实移除了 `SysFileMapper.xml`，则把断言改成“文件 mapper 不再存在”；不要继续期待不存在的文件内容。

- [x] **Step 3: 跑定向测试确认 gate 已恢复可信**

Run:

```bash
mvn -pl yr-system -Dtest=YrSystemBuildContractTest,YrSystemPhase2ArchitectureTest,YrSystemSqlContractTest test
```

Expected: PASS。

- [x] **Step 4: 提交本任务**

```bash
git add yr-system/src/test/java/com/yr/system/architecture/YrSystemBuildContractTest.java \
        yr-system/src/test/java/com/yr/system/architecture/YrSystemPhase2ArchitectureTest.java \
        yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlContractTest.java
git commit -m "test: 修正sso后端当前失真的架构门禁"
```

**Execution Result**

- 已完成，提交链为 `6dd7da2 -> d8ef972 -> bca7c71`
- 定向验证命令：`mvn -pl yr-system -Dtest=YrSystemBuildContractTest,YrSystemPhase2ArchitectureTest,YrSystemSqlContractTest test`
- 验证结果：`BUILD SUCCESS`
- 遗留风险：无阻塞项
- 是否需要开新 task：否

### Task 1: 关闭最高风险安全暴露面

**Files:**
- Modify: `yr-admin/src/main/java/com/yr/web/controller/common/SystemInfoController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/domain/DatabaseInfo.java`
- Modify: `yr-admin/src/main/java/com/yr/web/domain/RedisInfo.java`
- Create: `yr-admin/src/main/java/com/yr/web/domain/SystemInfoView.java`
- Modify: `yr-framework/src/main/java/com/yr/framework/config/SecurityConfig.java`
- Modify: `yr-framework/src/main/java/com/yr/framework/config/ResourcesConfig.java`
- Modify: `yr-admin/src/main/resources/application-local.yml`
- Modify: `yr-admin/src/main/resources/application-prod.yml`
- Modify: `yr-admin/src/main/resources/application-dev.yml`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/tool/SwaggerController.java`
- Modify or Delete: `yr-admin/src/main/java/com/yr/web/controller/tool/TestController.java`
- Create: `yr-admin/src/test/java/com/yr/config/SsoSecuritySurfaceContractTest.java`
- Create: `yr-admin/src/test/java/com/yr/web/controller/common/SystemInfoControllerContractTest.java`

- [x] **Step 1: 先写失败测试，锁定安全收口目标**

新增或扩展断言：

```java
assertThat(systemInfoJson).doesNotContain("password");
assertThat(systemInfoJson).doesNotContain("secret");
assertThat(securityConfigSource).doesNotContain(".antMatchers(\"/druid/**\").anonymous()");
assertThat(securityConfigSource).doesNotContain(".antMatchers(\"/swagger-ui/**\").anonymous()");
assertThat(localYaml).doesNotContain("login-password: 123456");
assertThat(localYaml).doesNotContain("password: Popo0222");
assertThat(prodYaml).doesNotContain("TOKEN_SECRET:abcdefghijklmnopqrstuvwxyz");
```

- [x] **Step 2: 跑定向测试确认当前失败**

Run:

```bash
mvn -pl yr-admin -am -Dtest=SsoSecuritySurfaceContractTest,SystemInfoControllerContractTest test
```

Expected: FAIL，指出匿名暴露面、弱默认值和系统信息脱敏缺失。

- [x] **Step 3: 做最小实现**

实现要求：
- `SystemInfoController` 不再直接返回 `DatabaseInfo` / `RedisInfo` 原始 bean；改为返回显式脱敏视图对象。
- `DatabaseInfo` / `RedisInfo` 不再承担“可直接序列化给前端”的职责。
- `SwaggerController` 与 `/swagger-ui`、`/v3/api-docs`、`/druid/**` 默认不再匿名开放；只允许在 `local profile` 或显式配置下启用。
- 移除 `application-*.yml` 中的硬编码弱默认凭据；改为环境变量占位符，生产配置不提供可用兜底值。
- `ResourcesConfig` 的 CORS 改为 property-driven allowlist，不再使用 `allowCredentials=true + *` 组合。
- `TestController` 默认从主 runtime surface（运行时暴露面）移除；若必须保留，则限定在 `local profile`。

- [x] **Step 4: 跑定向验证**

Run:

```bash
mvn -pl yr-admin -am -Dtest=SsoSecuritySurfaceContractTest,SystemInfoControllerContractTest,ResourcesConfigCorsTest test
```

Expected: PASS。

- [x] **Step 5: 提交本任务**

```bash
git add yr-admin/src/main/java/com/yr/web/controller/common/SystemInfoController.java \
        yr-admin/src/main/java/com/yr/web/domain/DatabaseInfo.java \
        yr-admin/src/main/java/com/yr/web/domain/RedisInfo.java \
        yr-admin/src/main/java/com/yr/web/domain/SystemInfoView.java \
        yr-framework/src/main/java/com/yr/framework/config/SecurityConfig.java \
        yr-framework/src/main/java/com/yr/framework/config/ResourcesConfig.java \
        yr-admin/src/main/resources/application-local.yml \
        yr-admin/src/main/resources/application-prod.yml \
        yr-admin/src/main/resources/application-dev.yml \
        yr-admin/src/main/java/com/yr/web/controller/tool/SwaggerController.java \
        yr-admin/src/main/java/com/yr/web/controller/tool/TestController.java \
        yr-admin/src/test/java/com/yr/config/SsoSecuritySurfaceContractTest.java \
        yr-admin/src/test/java/com/yr/web/controller/common/SystemInfoControllerContractTest.java
git commit -m "fix: 收紧sso后端高风险安全暴露面"
```

**Execution Result**

- 已完成，提交链为 `f88d974 -> 044a574 -> 6117e3c -> 55896bb`
- 定向验证命令：`mvn -pl yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoSecuritySurfaceContractTest,SystemInfoControllerContractTest,ResourcesConfigCorsTest test`
- 验证结果：`BUILD SUCCESS`，`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`
- 遗留风险：无阻塞项
- 是否需要开新 task：否

### Task 2: 修复文件接口与权限边界

**Files:**
- Modify: `yr-admin/src/main/java/com/yr/web/controller/common/CommonController.java`
- Modify: `yr-common/src/main/java/com/yr/common/utils/file/FileUtils.java`
- Modify: `yr-common/src/main/java/com/yr/common/utils/file/FileUploadUtils.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysUserController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysDeptController.java`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/system/SysSystemControllerPermissionContractTest.java`
- Create: `yr-admin/src/test/java/com/yr/web/controller/common/CommonControllerSecurityContractTest.java`

- [x] **Step 1: 先补失败的路径安全与权限契约测试**

测试目标：
- `GET /common/fileDelete` 不再存在，或已改为 `DELETE` 且要求显式权限。
- `fileDelete` / `download` / `download/resource` 对 `..`、绝对路径、非白名单文件名返回拒绝结果。
- `SysUserController.listV2`、`alllist`、`getAllUserForOptions`、`importTemplate` 等入口必须显式声明权限，或被移出主路径。
- `SysDeptController.getCheckDeptList`、`selectSysDept` 等入口必须有明确权限或用途说明。

- [x] **Step 2: 跑定向测试确认当前失败**

Run:

```bash
mvn -pl yr-admin -am -Dtest=CommonControllerSecurityContractTest,SysSystemControllerPermissionContractTest test
```

Expected: FAIL，指出删除接口语义错误、路径校验缺失和 controller 权限缺口。

- [x] **Step 3: 做最小实现**

实现要求：
- `fileDelete` 改为 `DELETE` 或 `POST`，并补显式权限控制。
- 所有文件路径都用 `Path.resolve(...).normalize()` + 根目录前缀校验，不能继续用字符串拼接。
- 下载/删除失败返回受控错误，不再静默吞异常或把底层异常直接返回前端。
- 对当前确实需要保留的 `SysUserController` / `SysDeptController` 辅助接口补齐 `@PreAuthorize`；对不再需要的入口直接删除或下线。

- [x] **Step 4: 跑定向验证**

Run:

```bash
mvn -pl yr-admin -am -Dtest=CommonControllerSecurityContractTest,SysSystemControllerPermissionContractTest,SysUserControllerInfoContractTest test
```

Expected: PASS。

- [x] **Step 5: 提交本任务**

```bash
git add yr-admin/src/main/java/com/yr/web/controller/common/CommonController.java \
        yr-common/src/main/java/com/yr/common/utils/file/FileUtils.java \
        yr-common/src/main/java/com/yr/common/utils/file/FileUploadUtils.java \
        yr-admin/src/main/java/com/yr/web/controller/system/SysUserController.java \
        yr-admin/src/main/java/com/yr/web/controller/system/SysDeptController.java \
        yr-admin/src/test/java/com/yr/web/controller/system/SysSystemControllerPermissionContractTest.java \
        yr-admin/src/test/java/com/yr/web/controller/common/CommonControllerSecurityContractTest.java
git commit -m "fix: 修复文件接口与权限边界"
```

**Execution Result**

- 已完成，提交链为 `856be3a -> 81b5bcb`
- 定向验证命令：`mvn -pl yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoSecuritySurfaceContractTest,SystemInfoControllerContractTest,ResourcesConfigCorsTest,CommonControllerSecurityContractTest,SysSystemControllerPermissionContractTest,SysUserControllerInfoContractTest test`
- 验证结果：`BUILD SUCCESS`，`Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`
- 遗留风险：无阻塞项
- 是否需要开新 task：否

### Task 3: 修复同步任务事务与 payload 契约

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SsoSyncTaskServiceImpl.java`
- Create: `yr-system/src/main/java/com/yr/system/service/support/SsoSyncTaskFailureRecorder.java`
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SsoSyncTaskServiceImplTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SsoSyncTaskPersistenceContractTest.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SsoSyncTaskFailurePersistenceTest.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SsoSyncTaskPayloadJsonContractTest.java`

- [x] **Step 1: 先写失败测试，锁定两个核心回归**

断言目标：
- 当执行器抛 `RuntimeException` 时，数据库中的 `task.status` 仍然会被持久化成 `FAILED`。
- `buildCompensationPayload` 输出的是合法 JSON，并能正确转义带双引号或特殊字符的 `entityType/sourceId`。

- [x] **Step 2: 跑定向测试确认当前失败**

Run:

```bash
mvn -pl yr-system -Dtest=SsoSyncTaskServiceImplTest,SsoSyncTaskPersistenceContractTest,SsoSyncTaskFailurePersistenceTest,SsoSyncTaskPayloadJsonContractTest test
```

Expected:
- 失败状态持久化相关测试 FAIL
- 手工拼 JSON 的契约测试 FAIL

- [x] **Step 3: 做最小实现**

实现要求：
- 把失败状态写入迁移到独立事务，或使用显式 `TransactionTemplate`/专用 recorder，确保 `FAILED` 状态不会随外层异常一起回滚。
- `buildInitImportPayload`、`buildDistributionPayload`、`buildCompensationPayload` 统一改为结构化序列化，不再手工 `String.format` 拼 JSON。
- 保持现有 task API shape 不变，不在这一任务里引入 controller 层大改。

- [x] **Step 4: 跑定向验证**

Run:

```bash
mvn -pl yr-system -Dtest=SsoSyncTaskServiceImplTest,SsoSyncTaskPersistenceContractTest,SsoSyncTaskFailurePersistenceTest,SsoSyncTaskPayloadJsonContractTest,SsoDistributionMqContractTest test
```

Expected: PASS。

- [x] **Step 5: 提交本任务**

```bash
git add yr-system/src/main/java/com/yr/system/service/impl/SsoSyncTaskServiceImpl.java \
        yr-system/src/main/java/com/yr/system/service/support/SsoSyncTaskFailureRecorder.java \
        yr-system/src/test/java/com/yr/system/service/impl/SsoSyncTaskServiceImplTest.java \
        yr-system/src/test/java/com/yr/system/service/impl/SsoSyncTaskPersistenceContractTest.java \
        yr-system/src/test/java/com/yr/system/service/impl/SsoSyncTaskFailurePersistenceTest.java \
        yr-system/src/test/java/com/yr/system/service/impl/SsoSyncTaskPayloadJsonContractTest.java
git commit -m "fix: 修复同步任务失败状态与payload契约"
```

**Execution Result**

- 已完成，提交链为 `6e650c8 -> 29c1882`
- 定向验证命令：`mvn -pl yr-system -Dtest=SsoSyncTaskServiceImplTest,SsoSyncTaskPersistenceContractTest,SsoSyncTaskFailurePersistenceTest,SsoSyncTaskPayloadJsonContractTest,SsoDistributionMqContractTest test`
- 验证结果：`BUILD SUCCESS`，`Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`
- 遗留风险：无阻塞项
- 是否需要开新 task：否

### Task 4: 收紧异常语义、登录链路与控制层依赖注入

**Files:**
- Modify: `yr-framework/src/main/java/com/yr/framework/web/exception/GlobalExceptionHandler.java`
- Modify: `yr-framework/src/main/java/com/yr/framework/web/service/SysLoginService.java`
- Modify or Remove: `yr-framework/src/main/java/com/yr/framework/web/service/SysMobileLoginService.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/sso/SsoClientController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/common/CaptchaController.java`
- Modify: `yr-framework/src/main/java/com/yr/framework/web/service/UserDetailsServiceImpl.java`
- Modify: `yr-framework/src/main/java/com/yr/framework/web/service/PermissionService.java`
- Create: `yr-framework/src/test/java/com/yr/framework/web/exception/GlobalExceptionHandlerContractTest.java`
- Create: `yr-framework/src/test/java/com/yr/framework/web/service/SysMobileLoginServiceContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/sso/SsoClientControllerContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/login/SysLoginControllerContractTest.java`

- [x] **Step 1: 先补失败的错误语义与依赖注入契约测试**

断言目标：
- 未知异常对外只返回受控消息，不泄露底层 `exception.getMessage()`。
- `SsoClientController` 使用构造器注入，`resolveOperator` 不再吞异常返回 `null`。
- `SysLoginService` 对认证失败、上游接口失败、配置缺失分别走不同错误语义。
- `SysMobileLoginService` 若无仓库内调用方，则不再处于主执行路径；若有调用方，则必须去掉 `required = false` 字段注入和宽泛异常吞吐。

- [x] **Step 2: 跑定向测试确认当前失败**

Run:

```bash
mvn -pl yr-admin -am -Dtest=SsoClientControllerContractTest,SysLoginControllerContractTest,GlobalExceptionHandlerContractTest,SysMobileLoginServiceContractTest test
```

Expected: FAIL，指出原样透传异常文本、字段注入和可空依赖问题。

- [x] **Step 3: 做最小实现**

实现要求：
- `GlobalExceptionHandler` 只对已知业务异常保留业务消息，对未知异常返回统一受控文案并保留日志。
- `SysLoginService` 不再把未知异常原样包装给前端；对认证、解密、远端 HTTP、配置缺失做显式分类。
- `SsoClientController` 改为构造器注入；创建/更新操作人获取失败时应该 fail-fast，而不是静默写 `null`。
- `SysMobileLoginService` 先跑引用确认；如果无调用方，优先下线/隔离；如果保留，则补齐注入、校验与异常映射。

- [x] **Step 4: 跑定向验证**

Run:

```bash
mvn -pl yr-admin -am -Dtest=SsoClientControllerContractTest,SysLoginControllerContractTest,WxworkLoginControllerContractTest,GlobalExceptionHandlerContractTest,SysMobileLoginServiceContractTest test
```

Expected: PASS。

- [x] **Step 5: 提交本任务**

```bash
git add yr-framework/src/main/java/com/yr/framework/web/exception/GlobalExceptionHandler.java \
        yr-framework/src/main/java/com/yr/framework/web/service/SysLoginService.java \
        yr-framework/src/main/java/com/yr/framework/web/service/SysMobileLoginService.java \
        yr-admin/src/main/java/com/yr/web/controller/sso/SsoClientController.java \
        yr-admin/src/main/java/com/yr/web/controller/common/CaptchaController.java \
        yr-framework/src/main/java/com/yr/framework/web/service/UserDetailsServiceImpl.java \
        yr-framework/src/main/java/com/yr/framework/web/service/PermissionService.java \
        yr-framework/src/test/java/com/yr/framework/web/exception/GlobalExceptionHandlerContractTest.java \
        yr-framework/src/test/java/com/yr/framework/web/service/SysMobileLoginServiceContractTest.java \
        yr-admin/src/test/java/com/yr/web/controller/sso/SsoClientControllerContractTest.java \
        yr-admin/src/test/java/com/yr/login/SysLoginControllerContractTest.java
git commit -m "fix: 收紧登录异常语义与控制层依赖注入"
```

**Execution Result**

- 已完成，提交为 `7b60248`
- 定向验证命令：`mvn -pl yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoClientControllerContractTest,SysLoginControllerContractTest,WxworkLoginControllerContractTest,GlobalExceptionHandlerContractTest,SysMobileLoginServiceContractTest,CaptchaControllerContractTest test`
- 验证结果：`BUILD SUCCESS`，`Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`
- 遗留风险：无阻塞项
- 是否需要开新 task：否

### Task 5: 迁移安全基础设施到 Spring Boot 2.7 推荐写法

**Files:**
- Modify: `yr-framework/src/main/java/com/yr/framework/config/SecurityConfig.java`
- Modify: `yr-framework/src/main/java/com/yr/framework/security/filter/JwtAuthenticationTokenFilter.java`
- Modify: `yr-framework/src/main/java/com/yr/framework/web/service/UserDetailsServiceImpl.java`
- Modify: `yr-framework/src/main/java/com/yr/framework/web/service/PermissionService.java`
- Create: `yr-framework/src/test/java/com/yr/framework/config/SecurityConfigContractTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/architecture/YrSystemConstructorInjectionAuditTest.java`

- [x] **Step 1: 先写失败的 framework contract tests**

断言目标：
- 不再使用 `WebSecurityConfigurerAdapter`
- 使用 `SecurityFilterChain`、`AuthenticationManager` bean 和构造器注入
- 关键框架 Bean 不再保留字段级 `@Autowired`

- [x] **Step 2: 跑定向测试确认当前失败**

Run:

```bash
mvn -pl yr-framework -am -Dtest=SecurityConfigContractTest,YrSystemConstructorInjectionAuditTest test
```

Expected: FAIL，指出旧式安全配置和字段注入残留。

- [x] **Step 3: 做最小实现**

实现要求：
- `SecurityConfig` 迁移到 `SecurityFilterChain` + `PasswordEncoder` + `AuthenticationManager` bean 模式。
- 用 `@EnableMethodSecurity` 替换旧式 `@EnableGlobalMethodSecurity`。
- 高信号框架 Bean 统一改为构造器注入。
- 保持现有 JWT 过滤顺序与登录 API shape 基本不变，避免与前端契约同时漂移。

- [x] **Step 4: 跑定向验证**

Run:

```bash
mvn -pl yr-framework -am -Dtest=SecurityConfigContractTest,ThreadPoolConfigAsyncIntegrationTest,YrSystemConstructorInjectionAuditTest test
```

Expected: PASS。

- [x] **Step 5: 提交本任务**

```bash
git add yr-framework/src/main/java/com/yr/framework/config/SecurityConfig.java \
        yr-framework/src/main/java/com/yr/framework/security/filter/JwtAuthenticationTokenFilter.java \
        yr-framework/src/main/java/com/yr/framework/web/service/UserDetailsServiceImpl.java \
        yr-framework/src/main/java/com/yr/framework/web/service/PermissionService.java \
        yr-framework/src/test/java/com/yr/framework/config/SecurityConfigContractTest.java \
        yr-system/src/test/java/com/yr/system/architecture/YrSystemConstructorInjectionAuditTest.java
git commit -m "refactor: 对齐spring security官方配置基线"
```

**Execution Result**

- 已完成，提交为 `60dcf3e`
- 定向验证命令：`mvn -pl yr-framework -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SecurityConfigContractTest,ThreadPoolConfigAsyncIntegrationTest,YrSystemConstructorInjectionAuditTest test`
- 验证结果：`BUILD SUCCESS`
- 遗留风险：无阻塞项
- 是否需要开新 task：否

### Task 6: 处理模块边界、公共模块瘦身与依赖升级

**Files:**
- Modify: `yr-framework/pom.xml`
- Modify: `yr-common/pom.xml`
- Modify: `pom.xml`
- Move: `yr-framework/src/main/java/com/yr/framework/web/service/SysLoginService.java`
- Move: `yr-framework/src/main/java/com/yr/framework/web/service/UserDetailsServiceImpl.java`
- Move: `yr-framework/src/main/java/com/yr/framework/manager/factory/AsyncFactory.java`
- Move: `yr-framework/src/main/java/com/yr/framework/aspectj/LogAspect.java`
- Move: `yr-framework/src/main/java/com/yr/framework/security/handle/LogoutSuccessHandlerImpl.java`
- Create: `yr-framework/src/main/java/com/yr/common/core/redis/RedisCache.java`
- Create: `yr-system/src/main/java/com/yr/common/service/MqProducerService.java`
- Create: `yr-framework/src/test/java/com/yr/framework/architecture/YrFrameworkBuildBoundaryContractTest.java`

- [x] **Step 1: 先补 build boundary tests**

断言目标：
- `yr-framework` 不再直接依赖 `yr-system`
- `yr-common` 不再直接声明 `spring-boot-starter-security`、`spring-boot-starter-data-redis`、`mybatis-plus-boot-starter` 等重量依赖
- `fastjson` / `jjwt 0.9.1` / `okhttp 3.14.9` 不再作为最终生产依赖保留

- [x] **Step 2: 跑定向测试确认当前失败**

Run:

```bash
mvn -pl yr-framework -am -Dtest=YrFrameworkBuildBoundaryContractTest test
```

Expected: FAIL，指出模块边界反转与旧依赖残留。

- [x] **Step 3: 先消除 `yr-framework -> yr-system` 反向依赖**

默认路径：
- 把带业务语义的认证/权限组装逻辑从 `yr-framework` 挪到 `yr-admin` 或新建 `auth` 子包。
- 保留 `yr-framework` 只负责 infra config（基础设施配置）、filter（过滤器）、handler（处理器）、exception mapping（异常映射）和纯框架组件。

- [x] **Step 4: 再瘦身 `yr-common`**

默认路径：
- `yr-common` 只保留纯领域对象、基础常量、无框架绑定的 util。
- `RedisCache`、MyBatis-Plus 基类、MQ 支撑、文件 Web 相关辅助类移动到更合适的 infra 模块。
- 依赖升级默认路径：
  - `fastjson` -> `Jackson`
  - `jjwt 0.9.1` -> `jjwt-api/jjwt-impl/jjwt-jackson 0.11.x`
  - `okhttp 3.14.9` -> `4.x`

- [x] **Step 5: 跑模块级验证**

Run:

```bash
mvn test
```

Expected: 全模块测试通过，且 `yr-framework`、`yr-common` 的构建边界测试为 PASS。

- [x] **Step 6: 提交本任务**

```bash
git add pom.xml yr-common/pom.xml yr-framework/pom.xml \
        yr-framework/src/main/java/com/yr/framework/web/service/SysLoginService.java \
        yr-framework/src/main/java/com/yr/framework/web/service/UserDetailsServiceImpl.java \
        yr-framework/src/main/java/com/yr/framework/web/service/PermissionService.java \
        yr-admin/src/main/java/com/yr/web/service/auth \
        yr-framework/src/test/java/com/yr/framework/architecture/YrFrameworkBuildBoundaryContractTest.java
git commit -m "refactor: 收敛模块边界并升级公共依赖基线"
```

**Execution Result**

- 已完成，提交为 `e27f62c`
- 定向边界验证命令：`mvn -pl yr-framework -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=YrFrameworkBuildBoundaryContractTest test`
- 模块级验证命令：`mvn test`
- 验证结果：两轮命令均为 `BUILD SUCCESS`
- 遗留风险：`com.yr.framework.*` 仍存在 split package（分包）于 `yr-framework/yr-admin` 两个模块；当前不阻塞合并，但后续若继续拆模块，建议再开独立治理 task
- 是否需要开新 task：当前不需要；后续如继续收敛模块物理包结构，可单独开 task

### Task 7: 全量验证、文档收口与新对话 handoff

**Files:**
- Modify: `docs/superpowers/plans/2026-03-27-sso-backend-best-practice-remediation.md`
- Modify: `README.md`
- Modify: `docs/review_plans/*`（仅当需要记录执行结果时）

- [x] **Step 1: 跑最终验证**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn test
```

Expected: `BUILD SUCCESS`。

- [x] **Step 2: 做安全回归 spot checks**

人工或自动化确认：
- 普通登录用户无法获取数据库/Redis 密码
- 匿名用户无法访问 `/druid/**`、`/swagger-ui/**`
- 非法 `../` 路径不能下载/删除文件
- 同步任务失败后数据库里能看到 `FAILED` 状态与摘要

- [x] **Step 3: 更新计划文档状态**

要求：
- 将已完成步骤打勾
- 在每个任务末尾补充执行结果、遗留风险和是否需要开新 task
- 如果 `Task 6` 因范围较大未完成，必须明确停留在哪个 checkpoint

- [x] **Step 4: 输出 handoff（交接）信息**

交接必须包含：
- exact cwd
- 当前 branch
- 本计划文档路径
- 已通过的验证命令
- 尚未完成的 task / step
- 非目标但存在的本地变更

- [x] **Step 5: 提交文档收口**

```bash
git add docs/superpowers/plans/2026-03-27-sso-backend-best-practice-remediation.md README.md docs/review_plans
git commit -m "docs: 收口sso后端最佳实践整改执行状态"
```

**Execution Result**

- 已完成，最终验证命令：`mvn test`
- 验证结果：`BUILD SUCCESS`
- 安全回归 spot checks 已由自动化覆盖：
  - `SsoSecuritySurfaceContractTest` / `SystemInfoControllerContractTest`：确认数据库与 Redis 密码不对外暴露
  - `CommonControllerSecurityContractTest`：确认非法 `../` 路径与删除接口边界受控
  - `SsoSyncTaskFailurePersistenceTest`：确认同步任务失败状态会持久化
  - `SsoInitImportLocalProfileDatasourceContractTest`：确认 local profile 仍是 `local_sam -> local_sam_empty` 拓扑且不再保留弱默认口令
- handoff（交接）信息：
  - exact cwd：`/Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be/.worktrees/sso-backend-best-practice-remediation`
  - 当前 branch：`popoy/sso-backend-best-practice-remediation`
  - 计划文档：`docs/superpowers/plans/2026-03-27-sso-backend-best-practice-remediation.md`
  - 已通过验证命令：
    - `mvn -pl yr-framework -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=YrFrameworkBuildBoundaryContractTest test`
    - `mvn test`
  - 尚未完成的 task / step：无
  - 非目标但存在的本地变更：文档收口前仅 `README.md` 与本计划文档；提交后应为干净工作树
- 遗留风险：无阻塞项
- 是否需要开新 task：否

## Recommended First Execution Slice

- 第一轮执行只做 `Task 0 -> Task 1 -> Task 2`。
- 只有当这三项都绿了，才进入 `Task 3` 之后的 correctness / convergence（正确性/收敛）阶段。
- 如果执行到 `Task 4` 时发现 `SysMobileLoginService` 有真实外部调用方，再单独开 decision checkpoint（决策检查点），不要边查边顺手重写。

## New Conversation Handoff Template

新开对话时直接贴下面这段即可：

```text
请从 /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be 继续执行
docs/superpowers/plans/2026-03-27-sso-backend-best-practice-remediation.md

按文档顺序严格执行，从 Task 0 开始，逐任务汇报检查点，不跳步。
先跑文档里的基线验证命令，确认当前停留点，再继续实现。
如果发现文档与仓库现状不一致，先用 repo evidence 修正文档，再继续执行。
```

Plan executed and closed on `2026-03-27`.
