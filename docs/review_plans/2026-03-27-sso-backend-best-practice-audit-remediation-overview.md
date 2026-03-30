# SSO Backend Best Practice Audit Remediation Overview

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不偏离当前一期边界的前提下，把 `sam-sso-be` 收敛到更符合 `JDK 17 + Spring Boot 2.7.18` 官方最佳实践（best practice，最佳实践）的状态，优先修复安全边界、凭据处理、任务一致性与输入契约问题。

**Architecture:** 本轮审计结论是“架构底座基本可用，主要风险集中在代码实现和少量工程治理残留”。当前模块拆分、`Spring Security`（安全框架）、`Liquibase`（数据库变更管理）和一批 contract test（契约测试）已经具备继续演进的基础，因此整改顺序应采用“先冻结回归 -> 先修安全与凭据 -> 再修任务一致性 -> 最后清理模板残留与构建治理”。

**Tech Stack:** Java 17, Spring Boot 2.7.18, Spring Security, MyBatis-Plus, Liquibase, Redis, RocketMQ, Quartz, Maven multi-module, JUnit 5, Mockito, AssertJ

---

## 当前执行状态

- 更新时间：2026-03-30
- 审计模式：整改完成（remediation completed，整改完成）
- 当前阶段：`All phases completed`
- 当前仓库：
  - wrapper repo: `/Users/PopoY/workingFiles/Projects/SAM/sso`
  - backend repo: `/Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be`
- Git 状态：
  - `git -C sam-sso-be status --short` 当前包含 `docs/review_plans/2026-03-27-sso-backend-best-practice-audit-remediation-overview.md`
  - 已新增 `yr-admin/src/test/java/com/yr/framework/web/service/SysLoginServiceChangeOrgSecurityTest.java`
- 已完成验证：
  - `command -v mvn` -> `/Users/PopoY/workingFiles/maven-3.9.12/bin/mvn`
  - `java -version` -> `17.0.18`
  - 已执行：

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin,yr-framework,yr-system \
  -Dtest=SsoIdentityBackendBoundaryContractTest,SsoLiquibaseBootstrapContractTest,SsoSecuritySurfaceContractTest,SecurityConfigContractTest,SysLoginControllerContractTest,WxworkLoginControllerContractTest,SsoClientControllerContractTest,SsoSyncTaskControllerContractTest,YrSystemConstructorInjectionAuditTest,YrSystemPersistenceStyleContractTest,SysUserServiceImplTransactionTest \
  test
```

  - 当前结果：`BUILD SUCCESS`
- 最新 checkpoint：
  - `mvn -pl yr-admin -Dtest=SysLoginServiceChangeOrgSecurityTest test` -> `BUILD FAILURE`（符合 TDD 红灯预期）
  - 已确认两个真实缺陷：
    - `changeOrg` 当前删除的是 `login_tokens:null`，没有删除当前登录态旧 token key
    - 目标组织用户上下文无法解析时，当前抛出的是 `NullPointerException`，不是受控拒绝
  - `mvn -pl yr-admin -Dtest=SysUserControllerPasswordResetContractTest test` -> `BUILD FAILURE`（符合 TDD 红灯预期）
  - 已确认密码重置链路当前会把 `status` 透传到 service，over-posting 风险真实存在
  - `mvn -pl yr-admin -Dtest=SsoClientControllerContractTest test` -> `BUILD FAILURE`（符合 TDD 红灯预期）
  - 已确认客户端列表接口当前会直接返回 `clientSecret`
  - `mvn -pl yr-system -Dtest=SsoSyncTaskServiceImplFailFastTest test` -> `BUILD FAILURE`（符合 TDD 红灯预期）
  - 已确认同步任务当前在 DISTRIBUTION 执行器缺失时不会 fail-fast，而是静默返回
  - `mvn -pl yr-system -Dtest=SsoIdentityDistributionServiceAfterCommitContractTest test` -> `BUILD FAILURE`（符合 TDD 红灯预期）
  - 已确认外层事务回滚时，MQ 当前已经在事务内发出
  - `mvn -pl yr-admin,yr-system -Dtest=... test` -> `BUILD FAILURE`
  - `yr-system` 先按预期红灯，Maven reactor 因首模块失败跳过了 `yr-admin`
  - `mvn -pl yr-admin -Dtest=SysLoginServiceChangeOrgSecurityTest,SysUserControllerPasswordResetContractTest,SsoClientControllerContractTest test` -> `BUILD FAILURE`
  - `yr-admin` 3 组红灯与 `yr-system` 2 组红灯均已成组复现，`Phase 1` 回归冻结完成
  - `mvn -pl yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysLoginServiceChangeOrgSecurityTest,SysUserControllerPasswordResetContractTest,SysUserControllerChangeStatusContractTest test` -> `BUILD SUCCESS`
  - `mvn -pl yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysUserServiceImplWriteBoundaryContractTest,SysUserServiceImplTransactionTest test` -> `BUILD SUCCESS`
  - `mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysLoginServiceChangeOrgSecurityTest,SysUserControllerPasswordResetContractTest,SysUserControllerChangeStatusContractTest,SysUserServiceImplWriteBoundaryContractTest,SysUserServiceImplTransactionTest test` -> `BUILD SUCCESS`
  - 已完成 `Phase 2` 实施：
    - `changeOrg` 已改为基于当前登录态校验 `userId + orgId` 归属，并使用当前 token 删除旧登录态
    - 密码重置入口已切到 `ResetUserPasswordRequest`，service 已改走专用 `resetUserPwd` SQL
    - 状态修改入口已切到 `ChangeUserStatusRequest`，service 已改走专用 `updateUserStatus` SQL
    - `SysProfileController.updatePwd` 已同步切到专用密码写入链路，避免继续依赖旧签名
  - `rg -n "clientSecret|client_secret|..."` 与只读兼容性检查确认：仓库内没有真实 `clientSecret` 认证消费方，只有后台 CRUD/轮换泄露路径
  - `mvn -pl yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoClientControllerContractTest test` -> `BUILD SUCCESS`
  - `mvn -pl yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoClientServiceImplSecretStorageContractTest test` -> `BUILD SUCCESS`
  - `mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoClientControllerContractTest,SsoClientServiceImplSecretStorageContractTest test` -> `BUILD SUCCESS`
  - 已完成 `Phase 3` 实施：
    - `SsoClient` 创建/更新入口已切到专用 DTO，并补齐基础输入校验
    - 列表查询已改为返回 `SsoClientView`，常规响应不再包含 `clientSecret`
    - 创建与轮换均改为 one-time reveal，存储层改为 `BCrypt` hash-at-rest
    - 兼容性检查未发现真实明文 secret 消费方，因此本阶段直接落地哈希存储
  - `mvn -pl yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoIdentityDistributionServiceAfterCommitContractTest test` -> `BUILD FAILURE`
  - 已确认第一次 `Phase 4` 尝试里，`DISTRIBUTION` 执行器自带 `@Transactional` 会让 `afterCommit` 绑定到内层事务，导致 outer rollback 前仍可能发送 MQ
  - `mvn -pl yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoSyncTaskServiceImplFailFastTest,SsoIdentityDistributionServiceAfterCommitContractTest test` -> `BUILD SUCCESS`
  - `mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoSyncTaskServiceImplFailFastTest,SsoIdentityDistributionServiceAfterCommitContractTest,SsoSyncTaskControllerContractTest,SsoDistributionMqContractTest test` -> `BUILD SUCCESS`
  - 已完成 `Phase 4` 实施：
    - `SsoSyncTaskServiceImpl` 已在执行器缺失或 `ssoSyncTaskItemService` 缺失时 fail-fast，避免“接单但不执行”
    - `SsoIdentityDistributionServiceImpl.execute` 已移除内层 `@Transactional`，事务边界改由外层 `SsoSyncTaskService` 持有
    - `DISTRIBUTION` 消息已改为“事务内预构建明细，事务提交后再发送 MQ”，outer rollback 时不会提前出队
    - 任务返回状态已对齐为 `PENDING`（待提交后发送）或真实执行结果，不再保留 `RUNNING/PENDING` 漂移
    - 本阶段继续复用 `mq_message_log` 的 `PENDING -> SUCCESS/FAILED` 发送履历语义，将其作为 outbound log（出站日志）配合 after-commit 发送使用
  - `mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ResourcesConfigCorsTest,SsoSecuritySurfaceContractTest,SysLoginControllerContractTest,SsoClientControllerContractTest,SsoSyncTaskControllerContractTest,YrSystemPersistenceStyleContractTest test` -> `BUILD SUCCESS`
  - `mvn test` 第一次执行 -> `BUILD FAILURE`
  - 已确认失败原因不是 `Phase 5` 新改动本身，而是 `SsoIdentitySkeletonContractTest` / `SsoSyncTaskPayloadJsonContractTest` 仍锁定旧的 skeleton 宽松语义，没有跟上 `Phase 4` “执行器缺失必须 fail-fast”的新契约
  - `mvn -pl yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoIdentitySkeletonContractTest,SsoSyncTaskPayloadJsonContractTest test` -> `BUILD SUCCESS`
  - `mvn test` -> `BUILD SUCCESS`
  - 已完成 `Phase 5` 实施：
    - parent `pom.xml` 已移除明文 HTTP 私服仓库，避免继续在仓库内传播不安全 Maven 仓库入口
    - `ResourcesConfig` 自定义跨域过滤器已改为具名 Bean，`application-*.yml` 已回收 `allow-bean-definition-overriding=true`
    - `yr-system` 已移除未使用的 `spring-boot-starter-websocket`，并同步清理 `SecurityConfig` 放行残留与两处 smoke test 的 `ServerEndpointExporter` mock
    - `application-local/dev/prod.yml` 上传阈值已收敛为显式业务值，`FileUploadUtils.DEFAULT_MAX_SIZE` 已与之对齐到 `50MB`
    - 生产代码中的 `printStackTrace/System.out` 已统一替换为 `slf4j` 日志或删除无意义调试输出
    - `README.md` 已改为反映 `sam-sso-be` 当前真实构建模块与整改主文档入口

## 审计结论

### 结论摘要

- 当前 backend 的 **架构层** 不是主要阻塞点：
  - parent `pom.xml` 已锁定 `Java 17`、`Spring Boot 2.7.18` 与核心 Maven plugin 版本。
  - `SecurityConfig` 已迁移到 `SecurityFilterChain` 模式，且关键安全表面已有 contract test。
  - `Liquibase` master 已收敛到 `sso/changelog1.0 -> 1.1 -> 1.2`，没有继续沿用全量平台 baseline。
  - `yr-system` 已有 constructor injection（构造器注入）与 persistence style（持久化风格）约束测试，说明治理方向是对的。

- 当前 backend 的 **代码实现层** 仍有数个高风险问题，优先级明显高于再做一轮高层架构讨论：
  - 组织切换 token 逻辑存在横向越权和旧 token 未失效的问题。
  - 密码重置链路用了通用用户更新 SQL，存在 over-posting（过量绑定）风险。
  - `SsoClient` 凭据在列表/更新模型里直接暴露，凭据边界不符合官方最佳实践。
  - 同步任务与 MQ 分发链路还没有做到 transaction-safe messaging（事务安全消息投递）。
  - 还有一批 template residue（模板残留）和工程治理残留，会持续放大后续整改成本。

### 审计判断

本轮执行应默认采用：

1. 先补测试把问题锁住。
2. 先修安全和凭据处理。
3. 再修任务一致性。
4. 最后再清理配置、构建和日志残留。

不要反过来先做大规模“架构重构”，否则很容易在高风险缺陷还存在时把精力耗在低价值整理上。

## Findings 摘要

| 优先级 | 类别 | 问题 | 关键文件 |
| --- | --- | --- | --- |
| P0 | Security（安全） | `changeOrg` 可切到任意 `orgId`，且旧 token 未正确失效 | `yr-admin/.../SysLoginService.java`、`yr-system/.../SysUserServiceImpl.java`、`yr-common/.../LoginUser.java`、`yr-framework/.../TokenService.java` |
| P0 | Security（安全） | `resetPwd` 走了通用 `updateUser`，具备 over-posting 风险 | `yr-admin/.../SysUserController.java`、`yr-system/.../SysUserServiceImpl.java`、`yr-system/.../SysUserMapper.xml` |
| P1 | Credential Hygiene（凭据治理） | `SsoClient` 把 `clientSecret` 当普通字段持久化和返回，列表接口天然暴露密钥 | `yr-common/.../SsoClient.java`、`yr-admin/.../SsoClientController.java`、`yr-system/.../SsoClientServiceImpl.java`、`yr-admin/.../db/liquibase/.../changelog1.1-client-sync.xml` |
| P1 | Consistency（一致性） | MQ 分发在数据库事务内直接发送，存在 DB rollback / MQ already sent 不一致风险 | `yr-system/.../SsoIdentityDistributionServiceImpl.java`、`yr-system/.../MqProducerService.java` |
| P1 | Runtime Safety（运行时安全） | 同步任务执行器缺失时会“接单但不真正执行”，并可能留下状态漂移 | `yr-system/.../SsoSyncTaskServiceImpl.java`、`yr-system/.../SsoSyncTaskFailureRecorder.java`、`yr-admin/.../SsoSyncTaskController.java` |
| P2 | Governance（工程治理） | 仍有 HTTP Maven repo、`allow-bean-definition-overriding`、模板残留依赖与 `printStackTrace/System.out` | `pom.xml`、`application-*.yml`、`yr-system/pom.xml`、`LogAspect.java`、`FileUploadUtils.java` 等 |

## Findings 详情

### P0-1 `changeOrg` 横向越权且旧 token 未失效

**现象**

- `changeOrg` 直接接受前端传入的 `orgId` 并重新签发 token：
  - `yr-admin/src/main/java/com/yr/framework/web/service/SysLoginService.java`
- `selectUserByUserName(username, orgId)` 并不会校验当前用户是否真的属于该组织，它只是查出用户后把传入的组织信息覆写到用户对象上：
  - `yr-system/src/main/java/com/yr/system/service/impl/SysUserServiceImpl.java`
- 重新签发 token 前，代码尝试删除旧 token，但新建的 `LoginUser` 此时尚未拥有旧 token 值，因此实际删除的是 `login_tokens:null`：
  - `yr-common/src/main/java/com/yr/common/core/domain/model/LoginUser.java`
  - `yr-framework/src/main/java/com/yr/framework/web/service/TokenService.java`

**证据**

- `yr-admin/src/main/java/com/yr/framework/web/service/SysLoginService.java:223-237`
- `yr-system/src/main/java/com/yr/system/service/impl/SysUserServiceImpl.java:132-150`
- `yr-common/src/main/java/com/yr/common/core/domain/model/LoginUser.java:24,91-97`
- `yr-framework/src/main/java/com/yr/framework/web/service/TokenService.java:112-120,148-156`

**影响**

- 只要知道目标 `orgId`，当前用户就能切换到非本人组织上下文。
- 旧 token 未正确失效，导致切组织后旧组织上下文和新组织上下文可能同时有效。
- 这是明显的 authorization bypass（授权绕过）和 session inconsistency（会话不一致）问题。

**整改原则**

- 切组织前必须校验 `user_id + org_id` 在 `sys_user_org` 中存在且启用。
- 切组织必须基于当前 `LoginUser` 删除旧 token，而不是基于新建对象删除。
- 建议新增专门的 `changeOrg` service test（服务测试）与 security contract test（安全契约测试）。

### P0-2 密码重置链路错误复用通用更新 SQL

**现象**

- `resetPwd` 控制器接收整个 `SysUser` 请求体：
  - `yr-admin/src/main/java/com/yr/web/controller/system/SysUserController.java:202-209`
- service 层 `resetPwd` 没走专用 `resetUserPwd`，而是直接调用 `userMapper.updateUser(user)`：
  - `yr-system/src/main/java/com/yr/system/service/impl/SysUserServiceImpl.java:290-304`
- `updateUser` SQL 会更新 `dept_id / user_name / nick_name / email / phone / sex / avatar / password / status / remark` 等多个字段：
  - `yr-system/src/main/resources/mapper/system/SysUserMapper.xml:292-311`
- 仓库里其实已经有专门的 `resetUserPwd` mapper，但没有被该链路使用：
  - `yr-system/src/main/java/com/yr/system/mapper/SysUserMapper.java:81-88`
  - `yr-system/src/main/resources/mapper/system/SysUserMapper.xml:325-329`

**影响**

- 拥有 `system:user:resetPwd` 权限的调用方，理论上可以在一次密码重置请求里顺带修改更多用户字段。
- 这是典型的 over-posting（过量绑定）/mass assignment（批量属性注入）风险。
- 也让“重置密码”和“修改用户资料”两个权限边界发生混叠。

**整改原则**

- 为密码重置创建独立 request DTO（请求 DTO）和独立 service method（服务方法）。
- 底层只调用专用 SQL，且只允许修改 `password` 与 `first_login` 等必要字段。
- 为该权限单独补 contract test，锁定“附带字段不会被更新”。

### P1-1 `SsoClient` 凭据治理不达标

**现象**

- `SsoClient` 直接把 `clientSecret` 作为普通可序列化字段暴露：
  - `yr-common/src/main/java/com/yr/common/core/domain/entity/SsoClient.java:27-49`
- 列表接口直接返回 `TableDataInfo<SsoClient>`：
  - `yr-admin/src/main/java/com/yr/web/controller/sso/SsoClientController.java:50-56`
- service 查询使用 `this.list()`，不会自动脱敏：
  - `yr-system/src/main/java/com/yr/system/service/impl/SsoClientServiceImpl.java:31-44`
- 数据库表也以明文 `client_secret` 存储：
  - `yr-admin/src/main/resources/db/liquibase/changelog/sso/changelog1.1-client-sync.xml:14-31`

**影响**

- 任何拥有客户端列表权限的后台用户都可能直接看到 `clientSecret`。
- 如果未来 `clientSecret` 用于 client authentication（客户端认证），那当前实现等于把凭据暴露给常规后台读接口。
- 这不符合 credential minimization（凭据最小暴露）和 secret at rest hygiene（静态凭据治理）的最佳实践。

**整改原则**

- 立即把 `clientSecret` 从列表/普通查询响应中移除。
- 新增/轮换时只做 one-time reveal（一次性展示）语义。
- 若一期客户端认证确实依赖 `clientSecret`，应把 end-state（目标态）定义为 hashed secret at rest（密文存储）+ verify on compare（比较时校验），不要长期明文存储。
- 同时补测试，明确“列表中不得返回密钥”。

### P1-2 MQ 分发在事务内直接发送

**现象**

- `SsoIdentityDistributionServiceImpl.execute` 标记了 `@Transactional`：
  - `yr-system/src/main/java/com/yr/system/service/impl/SsoIdentityDistributionServiceImpl.java:100-134`
- 循环内 `sendItem` 会直接调用 `mqProducerService.send(...)`：
  - `yr-system/src/main/java/com/yr/system/service/impl/SsoIdentityDistributionServiceImpl.java:205-221`

**影响**

- 如果后续任务明细写库或任务状态更新发生异常并回滚，MQ 侧可能已经成功发出消息。
- 这会产生典型的 DB / MQ inconsistency（数据库与消息系统不一致）。
- 当前项目已经有 `mq_message_log`，但还没有真正形成 transaction-safe outbox（事务安全 outbox）模式。

**整改原则**

- 不要在外层业务事务里直接做对外 MQ send。
- 建议收敛到以下两种模式之一：
  - `Outbox Pattern`（事务消息外盒模式）：事务内只写 `mq_message_log/outbox`，提交后再异步发送。
  - `TransactionSynchronization.afterCommit`：事务提交后再触发发送。
- 补充“DB rollback 时不会先把消息发出去”的回归测试。

### P1-3 同步任务会“接单但不执行”

**现象**

- `initImportTask / distributionTask / compensateTask` 在真正执行前先以 `RUNNING` 状态落库：
  - `yr-system/src/main/java/com/yr/system/service/impl/SsoSyncTaskServiceImpl.java:98-150,179-205`
- `executeTask` 发现执行器缺失或 `ssoSyncTaskItemService` 缺失时，只把内存中的 task 改成 `PENDING` 后直接返回：
  - `yr-system/src/main/java/com/yr/system/service/impl/SsoSyncTaskServiceImpl.java:246-250`
- 这条 early return（提前返回）没有回写数据库。

**影响**

- API 看起来创建成功，但真实执行链可能根本没有启用。
- 数据库里仍可能保留 `RUNNING`，而调用栈里的对象已经变成 `PENDING`，形成状态漂移。
- 这是典型的 fail-open（失败时继续放行）实现，不适合正式业务任务链路。

**整改原则**

- 生产运行态下，关键执行器缺失必须 fail-fast（快速失败）并明确报错。
- 如果测试或本地 skeleton 场景确实要允许“只建任务不执行”，必须加显式 feature flag（功能开关）并把状态、摘要、控制台展示全部对齐。
- 补测试锁定“执行器缺失时不能伪装成创建成功”。

### P2-1 模板残留与工程治理问题

**高信号残留**

- parent `pom.xml` 仍保留 HTTP 内网仓库：
  - `pom.xml:259-267`
- `application-local.yml / application-dev.yml / application-prod.yml` 仍统一开启：
  - `spring.main.allow-bean-definition-overriding=true`
  - 极大的 multipart limit（`20000MB`）
  - 模板路径与 Windows 默认值
- `yr-system/pom.xml` 还保留 `spring-boot-starter-websocket`，但仓库里已经没有真实 websocket 业务实现：
  - `yr-system/pom.xml:27-39`
- 生产代码里仍有 `printStackTrace` / `System.out.println`：
  - `yr-admin/src/main/java/com/yr/framework/aspectj/LogAspect.java:114-118`
  - `yr-common/src/main/java/com/yr/common/utils/file/FileUploadUtils.java:104-111,184-185`
  - `yr-admin/src/main/java/com/yr/YrApplication.java:18-21`
  - `yr-quartz/src/main/java/com/yr/quartz/task/RyTask.java:13-24`

**影响**

- 会持续制造配置漂移、误导后续实现、降低可观测性，并在构建/排障时增加噪声。

**整改原则**

- 这类问题不需要先于 P0/P1 处理，但必须在本轮计划尾部收口。
- 建议在最后一阶段统一做一次 configuration hygiene（配置卫生）和 logging hygiene（日志卫生）清理。

## 分阶段整改计划

## Phase 1: Freeze Regressions

目标：先把已经确认的问题写进测试，避免后续整改时回归。

**Files**

- Create: `yr-admin/src/test/java/com/yr/framework/web/service/SysLoginServiceChangeOrgSecurityTest.java`
- Create: `yr-admin/src/test/java/com/yr/web/controller/system/SysUserControllerPasswordResetContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/sso/SsoClientControllerContractTest.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SsoSyncTaskServiceImplFailFastTest.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SsoIdentityDistributionServiceAfterCommitContractTest.java`

- [x] 为 `changeOrg` 增加两个失败用例：
  - 非归属 `orgId` 必须拒绝签发 token
  - 切组织后旧 token cache key 必须被删除
- [x] 为密码重置增加契约测试：
  - 请求体附带 `status/email/deptId` 等字段时不得被更新
  - 只能修改密码与首次登录标志
- [x] 为 `SsoClient` 增加响应测试：
  - 列表接口返回中不得包含 `clientSecret`
  - 轮换密钥仅允许 one-time reveal
- [x] 为同步任务增加 fail-fast 测试：
  - 执行器缺失时接口/服务必须抛出明确异常，而不是返回伪成功
- [x] 为 MQ 分发增加事务语义测试：
  - 模拟任务明细写库失败时，不得先完成对外发送

**验证命令**

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin,yr-system -Dtest=SysLoginServiceChangeOrgSecurityTest,SysUserControllerPasswordResetContractTest,SsoClientControllerContractTest,SsoSyncTaskServiceImplFailFastTest,SsoIdentityDistributionServiceAfterCommitContractTest test
```

## Phase 2: Fix Security Boundaries

目标：先处理真正的越权和权限边界混叠。

**Files**

- Modify: `yr-admin/src/main/java/com/yr/framework/web/service/SysLoginService.java`
- Modify: `yr-framework/src/main/java/com/yr/framework/web/service/TokenService.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysUserServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/ISysUserService.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysUserOrgServiceImpl.java`
- Modify: `yr-system/src/main/resources/mapper/system/SysUserMapper.xml`
- Modify or Create: `yr-admin/src/main/java/com/yr/web/controller/system/dto/ResetUserPasswordRequest.java`
- Modify or Create: `yr-admin/src/main/java/com/yr/web/controller/system/dto/ChangeUserStatusRequest.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysUserController.java`

- [x] `changeOrg` 改为基于当前登录态获取旧 token 和当前用户 ID
- [x] 在签发新 token 前校验 `user_id + org_id` 归属关系存在且启用
- [x] 删除旧 token 时使用当前 `LoginUser.token`，不要使用新建对象
- [x] 将密码重置入口切到独立 DTO 和独立 service method
- [x] service 层使用专用 `resetUserPwd` SQL，而不是通用 `updateUser`
- [x] 明确禁止密码重置链路更新其他字段
- [x] 同时把 `changeStatus` 收口到专用状态更新 DTO + 专用 SQL，避免继续复用通用 `updateUser`

**阶段验收**

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin,yr-system -Dtest=SysLoginServiceChangeOrgSecurityTest,SysUserControllerPasswordResetContractTest,SysUserServiceImplTransactionTest test
```

## Phase 3: Fix Credential Hygiene

目标：把 `SsoClient` 从“普通后台实体”收敛为“带凭据边界的客户端模型”。

**Files**

- Modify or Create: `yr-admin/src/main/java/com/yr/web/controller/sso/dto/SsoClientCreateRequest.java`
- Modify or Create: `yr-admin/src/main/java/com/yr/web/controller/sso/dto/SsoClientUpdateRequest.java`
- Modify or Create: `yr-admin/src/main/java/com/yr/web/controller/sso/dto/SsoClientView.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/sso/SsoClientController.java`
- Modify: `yr-common/src/main/java/com/yr/common/core/domain/entity/SsoClient.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SsoClientServiceImpl.java`
- Modify: `yr-system/src/main/resources/mapper/system/SsoClientMapper.xml`
- Optional schema follow-up: `yr-admin/src/main/resources/db/liquibase/changelog/sso/changelog1.x-client-secret-hardening.xml`

- [x] 列表/普通查询改为返回脱敏 view DTO，不再直接返回 entity
- [x] `clientSecret` 不得出现在常规列表响应
- [x] create / rotate 只允许 one-time reveal
- [x] 评估是否在本阶段直接落地 `BCrypt` hash at rest；若影响现有 client bootstrap，则先完成响应脱敏，再在下一小阶段补存储迁移
- [x] 补齐输入校验：`clientCode/clientName/status/redirectUris` 基本合法性

**阶段验收**

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin,yr-system -Dtest=SsoClientControllerContractTest test
```

## Phase 4: Fix Task And MQ Consistency

目标：让同步任务、DB 状态和 MQ 发送具备清晰一致的运行语义。

**Files**

- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SsoSyncTaskServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/support/SsoSyncTaskFailureRecorder.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SsoIdentityDistributionServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/common/service/MqProducerService.java`
- Optional Create: `yr-system/src/main/java/com/yr/system/service/support/SsoDistributionOutboxDispatcher.java`
- Optional Modify: `yr-quartz/src/main/java/com/yr/quartz/task/MqRetryTask.java`

- [x] 明确区分“任务创建成功”“任务已执行”“任务排队待执行”
- [x] 执行器缺失时在生产运行态 fail-fast，不再默默返回 skeleton 结果
- [x] 统一数据库状态与返回状态，禁止 `RUNNING/PENDING` 漂移
- [x] 将 MQ 外发改为 after-commit 或 outbox 驱动
- [x] 若继续复用 `mq_message_log`，就把它正式定义为 outbox/outbound log，而不是“发送时顺手写一下”

**阶段验收**

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin,yr-system -Dtest=SsoSyncTaskServiceImplFailFastTest,SsoIdentityDistributionServiceAfterCommitContractTest,SsoSyncTaskControllerContractTest,SsoDistributionMqContractTest test
```

## Phase 5: Clean Governance Residue

目标：把模板残留、配置绕行和日志噪声一起收口，降低后续维护成本。

**Files**

- Modify: `pom.xml`
- Modify: `README.md`
- Modify: `yr-admin/src/main/resources/application-local.yml`
- Modify: `yr-admin/src/main/resources/application-dev.yml`
- Modify: `yr-admin/src/main/resources/application-prod.yml`
- Modify: `yr-system/pom.xml`
- Modify: `yr-admin/src/main/java/com/yr/framework/aspectj/LogAspect.java`
- Modify: `yr-common/src/main/java/com/yr/common/utils/file/FileUploadUtils.java`
- Modify: `yr-admin/src/main/java/com/yr/YrApplication.java`
- Modify: `yr-quartz/src/main/java/com/yr/quartz/task/RyTask.java`

- [x] 将 HTTP 私服仓库迁移到 `settings.xml` 或 HTTPS 入口
- [x] 复核并尽量移除 `allow-bean-definition-overriding=true`
- [x] 重新评估 `multipart` 大小上限，至少改为显式业务值而不是模板极值
- [x] 清理未使用的 websocket starter 和相关安全放行残留
- [x] 把 `printStackTrace/System.out` 全部替换为 `slf4j`
- [x] 更新 `README.md`，确保模块清单与当前 backend 真相一致

**阶段验收**

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn test
```

## 推荐执行顺序

1. 先执行 `Phase 1`
2. 再执行 `Phase 2`
3. 然后执行 `Phase 3`
4. 再执行 `Phase 4`
5. 最后执行 `Phase 5`

原因：

- `Phase 2` 直接处理越权和权限边界混叠，是最不能后放的部分。
- `Phase 3` 和 `Phase 4` 都重要，但 `clientSecret` 暴露属于更确定的安全面，应先于任务链一致性细化。
- `Phase 5` 都是必要工作，但不应抢在 P0/P1 前面。

## 执行注意事项

- 每一阶段开始前都先跑对应的 failing test（失败测试），不要先改代码再补测试。
- 对 `clientSecret` 是否直接进入 hash at rest，需要在执行 `Phase 3` 开始时做一次 compatibility checkpoint（兼容性检查点）：
  - 如果当前没有任何真实 client authentication 消费方依赖明文 secret，则直接收敛到 hash at rest。
  - 如果已有依赖，则先完成“响应不暴露 + one-time reveal”，再做存储迁移。
- 对 MQ 分发，优先选 outbox，不建议继续在业务事务内直接调用 MQ SDK。

## 新对话续接方式

新开对话时，直接提供以下四项即可无缝续接：

1. `cwd`：

```text
/Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
```

2. canonical doc（权威文档）：

```text
docs/review_plans/2026-03-27-sso-backend-best-practice-audit-remediation-overview.md
```

3. 当前状态：

```text
只读审计已完成；已确认“架构底座基本可用，优先修代码实现”；请从 Phase 1 开始执行，逐任务汇报检查点，不跳步，并同步更新文档状态和进度。
```

4. 当前已验证命令：

```text
mvn -pl yr-admin,yr-framework,yr-system -Dtest=SsoIdentityBackendBoundaryContractTest,SsoLiquibaseBootstrapContractTest,SsoSecuritySurfaceContractTest,SecurityConfigContractTest,SysLoginControllerContractTest,WxworkLoginControllerContractTest,SsoClientControllerContractTest,SsoSyncTaskControllerContractTest,YrSystemConstructorInjectionAuditTest,YrSystemPersistenceStyleContractTest,SysUserServiceImplTransactionTest test
```

## 建议的新对话首句

```text
请以 /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be 为 cwd，按照 docs/review_plans/2026-03-27-sso-backend-best-practice-audit-remediation-overview.md 从 Phase 1 开始执行，逐任务汇报检查点，不跳步，并同步更新文档状态和进度。
```
