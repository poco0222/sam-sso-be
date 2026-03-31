# SSO Backend Comprehensive Best Practice Audit Remediation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于 2026-03-31 的 live audit（实时审计）结果，把 `sam-sso-be` 收敛到更符合 `JDK 17 + Spring Boot 2.7.18` 官方最佳实践（best practice，最佳实践）的状态，优先修复真实高风险安全边界与输入契约问题，而不是继续停留在抽象架构讨论。

**Architecture:** 当前多模块 Maven 基线、`SecurityFilterChain`（安全过滤链）、`Liquibase`（数据库变更管理）和全量测试都已经可以工作，但这并不等于“架构完全没问题”。本轮结论是：整体架构可继续演进，但仍有一处需要优先处理的架构级安全问题，即 `Redis`（缓存）反序列化配置不安全；除此之外，其余主要风险集中在 controller/service/mapper 的写入边界、`OAuth`（开放授权）state（状态参数）闭环、以及 DTO（数据传输对象）与 Bean Validation（参数校验）收口不足。

**Tech Stack:** Java 17, Spring Boot 2.7.18, Spring Security, Spring MVC, Spring Validation, Spring Data Redis, MyBatis-Plus, Liquibase, RocketMQ, Maven multi-module, JUnit 5, MockMvc, Mockito, AssertJ

---

## Scope

- 实际 backend root（后端根目录）：`/Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be`
- wrapper root（包装层根目录）：`/Users/PopoY/workingFiles/Projects/SAM/sso`
- 本计划只覆盖当前 live audit 已确认的 open findings（待整改问题），不重新打开已经在 2026-03-30 之前确认关闭的主题，例如：
  - `changeOrg` 旧 token 失效与组织归属校验
  - `resetPwd` / `changeStatus` 专用 DTO 与专用写路径
  - validation transport semantics（校验传输语义）`HTTP 400`
  - `DISTRIBUTION` after-commit（提交后回调）结果回写闭环
- 当前 working tree（工作树）已有非目标本地改动，执行本计划时不要顺手改写它们：
  - `README.md`
  - `docs/review_plans/2026-03-30-sso-backend-second-pass-gap-remediation-plan.md`
  - `docs/review_plans/2026-03-30-sso-backend-third-pass-fresh-review-remediation-plan.md`
- 只有在本计划最后一项“文档与 handoff（交接）收口”明确要求时，才触碰旧计划文档；在此之前只围绕当前任务文件集做最小改动。
- 开始执行前先记录一次非目标脏文件快照，避免最后收口时误覆盖既有改动：

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
git status --short
git diff -- README.md docs/review_plans/2026-03-30-sso-backend-second-pass-gap-remediation-plan.md docs/review_plans/2026-03-30-sso-backend-third-pass-fresh-review-remediation-plan.md
```

## Fresh Baseline

- 已验证：

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
command -v mvn
java -version
mvn test
```

- 当前结果：
  - `command -v mvn` -> `/Users/PopoY/workingFiles/maven-3.9.12/bin/mvn`
  - `java -version` -> `OpenJDK 17.0.18`
  - `mvn test` -> `BUILD SUCCESS`
  - 全量基线 -> `Tests run: 60, Failures: 0, Errors: 0, Skipped: 5`

## Current Findings

### P0 Architecture Security: `RedisConfig` 使用 unsafe default typing

- 证据：
  - `yr-framework/src/main/java/com/yr/framework/config/RedisConfig.java:32-38`
- 问题：
  - 当前 `ObjectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ...)` 允许宽泛多态反序列化
  - 这不符合 Jackson / Spring Boot 2.7 的安全最佳实践
- 影响：
  - 一旦 Redis 中出现可控类型信息，存在反序列化攻击面
  - 这是当前 audit 中唯一需要直接上升为“架构层不完全可接受”的问题

### P0 Security Boundary: `PUT /system/user` 仍把完整 `SysUser` 下沉到通用更新 SQL

- 证据：
  - `yr-admin/src/main/java/com/yr/web/controller/system/SysUserController.java:175-188`
  - `yr-system/src/main/java/com/yr/system/service/impl/SysUserServiceImpl.java:244-248`
  - `yr-system/src/main/resources/mapper/system/SysUserMapper.xml:292-310`
- 问题：
  - 普通编辑接口仍接收完整实体并调用通用更新路径
  - `updateUser` SQL 仍会写 `password/status/login_ip/login_date`
- 影响：
  - `system:user:edit` 权限与专用密码/状态写边界再次混叠
  - 请求体若带入明文 `password`，当前链路不会像专用重置入口那样再次加密

### P1 Security Boundary: `PUT /system/user/profile` 仍存在 over-posting

- 证据：
  - `yr-admin/src/main/java/com/yr/web/controller/system/SysProfileController.java:58-83`
  - `yr-system/src/main/java/com/yr/system/service/impl/SysUserServiceImpl.java:273-276`
  - `yr-system/src/main/resources/mapper/system/SysUserMapper.xml:295-307`
- 问题：
  - profile 仍接收 `SysUser`
  - 仅清掉 `password`，但仍复用通用 `updateUser` SQL
- 影响：
  - 自助资料修改仍可越界触达 `status/deptId/remark/loginIp/loginDate` 等字段

### P1 Auth Flow: 企业微信 `OAuth` 缺少 `state` 闭环且失败日志会泄露敏感 URL

- 证据：
  - `yr-admin/src/main/java/com/yr/framework/web/service/SysLoginService.java:285-301`
  - `yr-admin/src/main/java/com/yr/framework/web/service/SysLoginService.java:409-418`
  - `yr-admin/src/main/java/com/yr/web/controller/auth/WxworkAuthController.java:48-52`
  - `yr-common/src/main/java/com/yr/common/core/domain/model/LoginBody.java:35-52`
  - `yr-admin/src/test/java/com/yr/login/WxworkLoginControllerContractTest.java:55-64`
- 问题：
  - 生成了 `state`，但没有持久化、消费和校验
  - 失败日志直接打印完整请求 URL，可能带出 `corpsecret` / `access_token`
  - 现有测试只覆盖 happy path（成功路径），没有锁住 `state` 与日志脱敏契约
- 影响：
  - 登录链路缺少 login CSRF（登录跨站请求伪造）防护闭环
  - 敏感凭据可能进入日志

### P1 Input Contract: `SsoClient` 状态切换与写接口校验仍偏弱

- 证据：
  - `yr-admin/src/main/java/com/yr/web/controller/sso/SsoClientController.java:128-133`
  - `yr-admin/src/main/java/com/yr/web/controller/sso/dto/SsoClientCreateRequest.java:23-37`
  - `yr-admin/src/main/java/com/yr/web/controller/sso/dto/SsoClientUpdateRequest.java:28-42`
  - `yr-system/src/main/java/com/yr/system/service/impl/SsoClientServiceImpl.java:113-176`
  - `yr-admin/src/test/java/com/yr/web/controller/sso/SsoClientControllerContractTest.java:113-215`
- 问题：
  - `changeStatus` 仍直接绑定 `SsoClient entity（实体）`
  - `status/allowPasswordLogin/allowWxworkLogin/syncEnabled` 仍只有“非空”语义，没有 allowed value（允许值）约束
  - `redirectUris` 只检查 scheme（协议）是否存在，异常 URI 也没有统一转换成受控业务错误
  - 现有控制器契约测试未覆盖 `changeStatus` 的空值/非法值
- 影响：
  - 异常输入可能变成 `500`，或把无效值落到数据库

### P2 Input Contract: `SsoSyncTaskController` 仍直接绑定 `SsoSyncTask`

- 证据：
  - `yr-admin/src/main/java/com/yr/web/controller/sso/SsoSyncTaskController.java:58-89`
  - `yr-admin/src/test/java/com/yr/web/controller/sso/SsoSyncTaskControllerContractTest.java:114-152`
- 问题：
  - `initImport` / `distribution` 都接受完整实体
  - 客户端可以预填 `taskType/status/batchNo/payloadJson` 等本应由服务端生成的字段
  - 现有测试只覆盖 happy path，没有非法输入约束
- 影响：
  - controller 层没有把 invariant（不变量）收紧到 DTO 与 `400 Bad Request`

### P2 Governance Residue: 生产配置、日志与文档仍有残留

- 证据：
  - `yr-admin/src/main/resources/application-prod.yml:2-25`
  - `yr-admin/src/main/resources/application-prod.yml:120-164`
  - `yr-common/src/main/java/com/yr/common/core/domain/entity/SysUser.java:387-410`
  - `yr-system/src/main/java/com/yr/system/service/impl/SsoIdentityImportServiceImpl.java:145-153`
  - `docs/superpowers/plans/2026-03-27-sso-backend-best-practice-remediation.md:10-13`
  - `docs/superpowers/plans/2026-03-27-sso-backend-best-practice-remediation.md:45-52`
  - `docs/superpowers/plans/2026-03-27-sso-backend-best-practice-remediation.md:78-85`
- 问题：
  - `application-prod.yml` 仍保留 `demoEnabled=true`、模板路径、`devtools.restart.enabled=true`
  - `SysUser.toString()` 仍拼出 `password/salt`
  - `SsoIdentityImportServiceImpl` 失败日志没有带堆栈
  - `docs/superpowers/plans/2026-03-27-...` 仍把 `WebSecurityConfigurerAdapter` 当现状，还引用并不存在的 `sam-sso-be/AGENTS.md`
- 影响：
  - 增加生产噪音与误配风险
  - 敏感字段有进入日志的可能
  - 后续新对话很容易被旧计划误导

## Execution Order

1. `Task 1` 冻结并修复 `RedisConfig` 安全序列化与 prod profile 安全基线
2. `Task 2` 冻结用户编辑 / 个人资料写边界回归
3. `Task 3` 拆分用户编辑 / 个人资料 DTO 与专用写路径
4. `Task 4` 补齐企业微信 `OAuth state` 与日志脱敏闭环
5. `Task 5` 收紧 `SsoClient` 与 `SsoSyncTask` 输入契约
6. `Task 6` 清理残余日志 / `toString` / 文档漂移，并做全量验证与 handoff 收口

## Task 1: Freeze And Fix Redis / Prod Safety Baseline

**Files:**
- Create: `yr-framework/src/test/java/com/yr/framework/config/RedisConfigSecurityContractTest.java`
- Create: `yr-admin/src/test/java/com/yr/config/ProdProfileSafetyContractTest.java`
- Modify: `yr-framework/src/main/java/com/yr/framework/config/RedisConfig.java`
- Modify: `yr-admin/src/main/resources/application-prod.yml`

- [x] **Step 1: 先补 RedisConfig 安全红灯**

目标：
- 不允许继续使用 `LaissezFaireSubTypeValidator`
- 锁定 `LoginUser` / token cache（令牌缓存）当前所需的兼容序列化行为

- [x] **Step 2: 再补 prod profile 安全红灯**

目标：
- `demoEnabled` 必须默认关闭
- `devtools.restart.enabled` 不能在 prod profile 开启
- 模板化默认上传路径 / 日志路径不能继续作为 production default（生产默认值）

- [x] **Step 3: 落地实现**

实现要求：
- Redis 序列化改为受限 `PolymorphicTypeValidator` 或等价安全实现
- 保持现有登录缓存链路可工作，不做额外协议漂移
- prod profile 用环境变量驱动运维值，不再保留模板默认值

- [x] **Step 4: 跑定向测试**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-framework,yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RedisConfigSecurityContractTest,ProdProfileSafetyContractTest,TokenServiceContractTest test
```

Expected:
- PASS

**Execution Status (2026-03-31 17:59 CST)**

- Status: `Task 1` completed
- Scope note:
  - 本轮按当前 finding（问题）边界执行，仅收敛 `demoEnabled`、`devtools.restart.enabled`、上传路径 / 日志路径默认值，以及 `Redis` 登录缓存安全与兼容性；未扩散去清理本任务未点名的其它 `prod profile` 模板默认值
- Red-phase evidence:
  - `mvn -pl yr-framework,yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RedisConfigSecurityContractTest,ProdProfileSafetyContractTest,TokenServiceContractTest test`
  - 首轮失败点：
    - `RedisConfig` 仍使用 `LaissezFaireSubTypeValidator`
    - `LoginUser` 缓存样本的 `authorities` / legacy payload（历史载荷）兼容性未被锁住
  - `mvn -pl yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ProdProfileSafetyContractTest test`
  - 首轮失败点：
    - `demoEnabled=true`
    - `spring.devtools.restart.enabled=true`
    - `yr.profile` / `logging.file.path` / `file.constantPath` 仍保留模板默认值
    - `token.secret` 一度出现 fallback（回退默认值），已补契约并修回 fail-fast（快速失败）语义
- Green-phase implementation summary:
  - `RedisConfig` 改为受限 `BasicPolymorphicTypeValidator` 白名单
  - `Redis ObjectMapper` 改为 field-only（仅字段）可见性，并容忍 legacy payload 里的派生 `admin` 字段
  - 为 `SimpleGrantedAuthority` 增加 Redis 反序列化 `mixin`，保证 `authorities` 链路可 round-trip（往返）
  - `application-prod.yml` 改为关闭 `demoEnabled`、固定关闭 `devtools.restart.enabled`，并把上传路径 / 日志路径收口为环境变量驱动
  - `token.secret` 保持 `${TOKEN_SECRET:}`，不引入硬编码默认密钥
- Final verification:
  - `mvn -pl yr-framework,yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RedisConfigSecurityContractTest,ProdProfileSafetyContractTest,TokenServiceContractTest test`
  - Result: `BUILD SUCCESS`
  - Summary: `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`
  - Review gates:
    - `spec compliance` review: PASS
    - `code quality` review: PASS

## Task 2: Freeze User Edit / Profile Write-Boundary Regressions

**Files:**
- Create: `yr-admin/src/test/java/com/yr/web/controller/system/SysUserControllerEditWriteBoundaryContractTest.java`
- Create: `yr-admin/src/test/java/com/yr/web/controller/system/SysProfileControllerWriteBoundaryContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/system/SysUserControllerPasswordResetContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/system/SysUserControllerChangeStatusContractTest.java`

- [x] **Step 1: 先锁住 edit 入口不能继续承担 resetPwd 语义**

目标：
- `PUT /system/user` 带 `password/status/loginIp/loginDate` 时必须被拒绝或忽略
- 只能保留普通资料编辑字段

- [x] **Step 2: 再锁住 profile 入口只能修改个人资料字段**

目标：
- `profile` 只允许 `nickName/phonenumber/email/sex`
- `userName/deptId/status/remark/loginIp/loginDate/password` 不得下沉

- [x] **Step 3: 跑定向红灯测试**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysUserControllerEditWriteBoundaryContractTest,SysProfileControllerWriteBoundaryContractTest,SysUserControllerPasswordResetContractTest,SysUserControllerChangeStatusContractTest test
```

Expected:
- 新增断言先失败，明确暴露当前剩余写边界缺口

**Execution Status (2026-03-31 18:07 CST)**

- Status: `Task 2` completed as planned red phase
- Test additions:
  - 新增 `SysUserControllerEditWriteBoundaryContractTest`
  - 新增 `SysProfileControllerWriteBoundaryContractTest`
  - 补强 `SysUserControllerPasswordResetContractTest`
  - 补强 `SysUserControllerChangeStatusContractTest`
- Verification:
  - `mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysUserControllerEditWriteBoundaryContractTest,SysProfileControllerWriteBoundaryContractTest,SysUserControllerPasswordResetContractTest,SysUserControllerChangeStatusContractTest test`
  - Result: expected red
  - Failure summary:
    - `SysUserControllerEditWriteBoundaryContractTest`
      - `PUT /system/user` 仍透传 `password`
    - `SysProfileControllerWriteBoundaryContractTest`
      - `PUT /system/user/profile` 仍透传 `userName`
  - Confirmed still green:
    - `SysUserControllerPasswordResetContractTest`
    - `SysUserControllerChangeStatusContractTest`
- Outcome:
  - 当前剩余缺口已被稳定冻结，可直接进入 `Task 3` 做 DTO / 专用写路径整改

## Task 3: Split User DTOs And Dedicated Write Paths

**Files:**
- Create: `yr-admin/src/main/java/com/yr/web/controller/system/dto/UpdateUserRequest.java`
- Create: `yr-admin/src/main/java/com/yr/web/controller/system/dto/UpdateProfileRequest.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysUserController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysProfileController.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/ISysUserService.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysUserServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysUserWriteService.java`
- Modify: `yr-system/src/main/resources/mapper/system/SysUserMapper.xml`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/system/SysUserControllerEditWriteBoundaryContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/system/SysProfileControllerWriteBoundaryContractTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SysUserServiceImplWriteBoundaryContractTest.java`

- [x] **Step 1: 为 admin edit 拆白名单 DTO**

实现要求：
- 不再接收完整 `SysUser`
- 明确只允许普通编辑字段
- 禁止 `password/status/loginIp/loginDate` 从 controller 进入 service

- [x] **Step 2: 为 profile 拆专用 DTO 与专用 SQL**

实现要求：
- 不再复用通用 `updateUser`
- profile 只更新自助资料字段

- [x] **Step 3: 复跑已有 resetPwd / changeStatus 契约**

目标：
- 新改动不能回归已关闭的专用写边界

- [x] **Step 4: 跑定向验证**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysUserControllerEditWriteBoundaryContractTest,SysProfileControllerWriteBoundaryContractTest,SysUserControllerPasswordResetContractTest,SysUserControllerChangeStatusContractTest,SysUserServiceImplWriteBoundaryContractTest test
```

Expected:
- PASS

**Execution Status (2026-03-31 18:26 CST)**

- Status: `Task 3` completed
- Implementation summary:
  - 新增 `UpdateUserRequest` / `UpdateProfileRequest`，`edit/profile` 入口不再接收完整 `SysUser`
  - `SysUserController.edit` 改为仅构造普通编辑字段
  - `SysProfileController.updateProfile` 改为仅构造 `nickName/phonenumber/email/sex`
  - `updateUserProfile` 改为专用 SQL，不再复用通用 `updateUser`
  - 为避免 `SysLoginService.recordLoginInfo` 回归，专用 SQL 受控保留 `loginIp/loginDate` 更新能力，但 `profile` 请求体本身仍无法传入这些字段
- Verification:
  - 首轮 `spec compliance` review 发现 `loginIp/loginDate` 回归风险，已按最小范围补回专用 SQL 并复验
  - `mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysUserControllerEditWriteBoundaryContractTest,SysProfileControllerWriteBoundaryContractTest,SysUserControllerPasswordResetContractTest,SysUserControllerChangeStatusContractTest,SysUserServiceImplWriteBoundaryContractTest test`
  - Result: `BUILD SUCCESS`
  - Summary: `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`
  - Review gates:
    - `spec compliance` review: PASS
    - `code quality` review: PASS
- Outcome:
  - `edit/profile` 写边界已经转绿，且 `resetPwd/changeStatus` 无回归，可继续进入 `Task 4`

## Task 4: Close Wxwork OAuth State And Secret-Redaction Gap

**Files:**
- Modify: `yr-common/src/main/java/com/yr/common/core/domain/model/LoginBody.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/auth/WxworkAuthController.java`
- Modify: `yr-admin/src/main/java/com/yr/framework/web/service/SysLoginService.java`
- Create: `yr-admin/src/test/java/com/yr/framework/web/service/SysLoginServiceWxworkStateContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/login/WxworkLoginControllerContractTest.java`

- [x] **Step 1: 先补 `state` 契约红灯**

目标：
- `/auth/wxwork/pre-login` 生成 `state` 后必须落库或落缓存
- `/auth/wxwork/login` 必须要求并校验 `state`
- 缺失或不匹配必须返回受控错误

- [x] **Step 2: 再补日志脱敏红灯**

目标：
- 日志中不得出现 `corpsecret`
- 日志中不得出现 `access_token`
- 请求失败时仍要保留足够定位信息，但只能记录脱敏后的 URL / scene（场景）

- [x] **Step 3: 落地实现**

实现要求：
- 使用短 TTL（有效期）存放 `state`
- 登录成功或失败后都要消费 `state`
- 控制器与 service 契约同步收紧，不允许“只生成不验证”

- [x] **Step 4: 跑定向验证**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysLoginServiceWxworkStateContractTest,WxworkLoginControllerContractTest,SysLoginControllerContractTest test
```

Expected:
- PASS

**Execution Status (2026-03-31 19:04 CST)**

- Status: `Task 4` completed
- Implementation summary:
  - `LoginBody` 明确补齐企业微信 `state` 字段注释，`WxworkAuthController.login` 改为强制下沉 `code + state`
  - `SysLoginService.buildWxworkPreLoginUrl` 改为先生成 `state`，再通过 `StringRedisTemplate` 以 `300s TTL` 写入 `wxwork:oauth_state:*`
  - `SysLoginService.loginByWxworkCode` 改为先校验并原子消费 `state`，缺失或不匹配统一返回受控错误
  - 企业微信失败日志继续保留 scene（场景）与 URL 结构，但对 `corpsecret/access_token/code` 做脱敏
- Verification:
  - 首轮 `spec compliance` review 发现 `state mismatch` 契约未冻结，已在 `SysLoginServiceWxworkStateContractTest` 补齐负向用例并复验
  - 首轮 `code quality` review 发现 `state` 消费非原子与 `code` 仍可能泄露，已改为 `StringRedisTemplate.opsForValue().getAndDelete(...)` 原子消费，并补齐 `code` 脱敏断言后复验
  - `mvn -pl yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysLoginServiceWxworkStateContractTest,WxworkLoginControllerContractTest,SysLoginControllerContractTest test`
  - Result: `BUILD SUCCESS`
  - Summary: `Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`
  - Review gates:
    - `spec compliance` review: PASS
    - `code quality` review: PASS
- Outcome:
  - 企业微信登录形成 `state` 签发 -> 原子校验/消费 -> 失败日志脱敏 的完整闭环，可继续进入 `Task 5`

## Task 5: Harden SsoClient And SsoSyncTask Input Contracts

**Files:**
- Create: `yr-admin/src/main/java/com/yr/web/controller/sso/dto/SsoClientStatusUpdateRequest.java`
- Create: `yr-admin/src/main/java/com/yr/web/controller/sso/dto/SsoSyncTaskInitImportRequest.java`
- Create: `yr-admin/src/main/java/com/yr/web/controller/sso/dto/SsoSyncTaskDistributionRequest.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/sso/SsoClientController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/sso/SsoSyncTaskController.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SsoClientServiceImpl.java`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/sso/SsoClientControllerContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/sso/SsoSyncTaskControllerContractTest.java`
- Optionally Create: `yr-system/src/test/java/com/yr/system/service/impl/SsoClientServiceImplValidationContractTest.java`

- [x] **Step 1: 先冻结 `changeStatus` 空值/非法值输入**

目标：
- `status` 缺失时必须返回 `400`
- `status` 非允许值时必须返回受控业务错误

- [x] **Step 2: 再冻结 `initImport` / `distribution` 的 DTO 边界**

目标：
- controller 只接收必要字段
- `taskType/status/batchNo/payloadJson` 不能由客户端传入
- `distribution` 缺少 `targetClientCode` 时必须在 controller 层返回 `400`

- [x] **Step 3: 落地 service 层 allowed value 校验**

实现要求：
- `status` 收窄到明确允许值
- `allowPasswordLogin/allowWxworkLogin/syncEnabled` 收窄到明确允许值
- `redirectUris` 的异常 URI 要统一映射到受控业务错误

- [x] **Step 4: 跑定向验证**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoClientControllerContractTest,SsoSyncTaskControllerContractTest,SsoClientServiceImplSecretStorageContractTest,SsoClientServiceImplValidationContractTest test
```

Expected:
- PASS

**Execution Status (2026-03-31 19:36 CST)**

- Status: `Task 5` completed
- Implementation summary:
  - 新增 `SsoClientStatusUpdateRequest`、`SsoSyncTaskInitImportRequest`、`SsoSyncTaskDistributionRequest`，`changeStatus/initImport/distribution` 不再直接绑定完整 entity
  - `SsoClientController.changeStatus` 改为 controller 层 `@Validated` 校验 `status` 必填；`SsoSyncTaskController.distribution` 改为 controller 层 `@Validated` 校验 `targetClientCode` 必填
  - `SsoSyncTaskController` 只构造必要字段下沉，`taskType/status/batchNo/payloadJson` 等服务端管理字段不再允许从客户端进入
  - `SsoClientServiceImpl` 补齐 `status=0/1`、`allowPasswordLogin/allowWxworkLogin/syncEnabled=Y/N` allowed value 校验，并把 `redirectUris` 收紧到受支持的 `http/https` 回调地址且统一映射为受控业务错误
  - `rotateClientSecret` 现在会检查持久化结果，避免返回“数据库未生效”的新密钥；同步任务创建入口也改为和客户端控制器一致的操作人 fail-fast
- Verification:
  - 首轮 red baseline：
    - `mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoClientControllerContractTest,SsoSyncTaskControllerContractTest,SsoClientServiceImplSecretStorageContractTest,SsoClientServiceImplValidationContractTest test`
    - Result: `BUILD FAILURE`
    - 关键红灯：`SsoClientServiceImplValidationContractTest` 暴露缺少 allowed value 校验与 `redirectUris` 未受控映射；`SsoClientControllerContractTest` / `SsoSyncTaskControllerContractTest` 暴露 `changeStatus` 仍返回 `200`、`initImport/distribution` 仍透传服务端字段且 `distribution` 缺少 `400`
  - 首轮 `spec compliance` review: PASS
  - 首轮 `code quality` review 发现 `rotateClientSecret` 未检查持久化结果、同步任务控制器吞掉安全上下文异常、密钥存储测试过于脆弱、`init-import` 成功用例表达自相矛盾；已按最小范围修复
  - 二轮 `code quality` review 继续指出 `redirectUris` 仅检查 scheme 非空仍不足；已结合 2026-03-30 旧计划中的同类要求，把协议收紧到 `http/https` 并补负向契约后复验
  - 最终验证命令：
    - `mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoClientControllerContractTest,SsoSyncTaskControllerContractTest,SsoClientServiceImplSecretStorageContractTest,SsoClientServiceImplValidationContractTest test`
  - Result: `BUILD SUCCESS`
  - Summary: `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`
  - Review gates:
    - `spec compliance` review: PASS
    - `code quality` review: PASS
- Outcome:
  - `SsoClient` 与 `SsoSyncTask` 的输入契约已经从 entity 绑定收口到 DTO + Bean Validation + service allowed value 校验，可继续进入 `Task 6`

## Task 6: Clean Logging / toString / Docs Drift And Close The Handoff

**Files:**
- Modify: `yr-common/src/main/java/com/yr/common/core/domain/entity/SysUser.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SsoIdentityImportServiceImpl.java`
- Modify: `docs/superpowers/plans/2026-03-27-sso-backend-best-practice-remediation.md`
- Optionally Modify: `docs/review_plans/2026-03-30-sso-backend-third-pass-fresh-review-remediation-plan.md`
- Optionally Modify: `README.md`

- [x] **Step 1: 清掉 `SysUser.toString()` 的敏感字段**

目标：
- `password/salt` 不得进入 `toString`

- [x] **Step 2: 补齐导入失败日志堆栈**

目标：
- `SsoIdentityImportServiceImpl` 失败日志要保留异常堆栈
- 不改变当前 item 摘要对前端的受控输出

- [x] **Step 3: 收口文档漂移**

实现要求：
- 把 `docs/superpowers/plans/2026-03-27-...` 标记为历史文档或更新到当前真实架构
- 若要更新 `README.md` 或 2026-03-30 旧文档，先确认不会覆盖当前已有本地改动
- 当前计划完成后，需要明确新的 canonical handoff（权威交接入口）

- [x] **Step 4: 跑全量验证**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn test
```

Expected:
- `BUILD SUCCESS`
- `Tests run` 数量允许增加，但 `Failures: 0, Errors: 0`

- [x] **Step 5: 收口执行状态与 handoff**

目标：
- 在本计划文件中标记已完成任务
- 补全最终验证结果
- 给下一次对话留下唯一执行入口

实现要求：
- 若本轮没有显式改写 `README.md` 与旧计划文档，则当前 canonical handoff（权威交接入口）默认就是本文件：
  - `docs/review_plans/2026-03-31-sso-backend-comprehensive-best-practice-audit-remediation-plan.md`
- 若本轮显式改写了 `README.md` 或历史计划文档，必须把它们同步更新到与本文件一致的入口说明
- 在 handoff 里必须写明：
  - 最新 `git status --short`
  - 本轮新增 / 修改的目标文件
  - 最后一条通过的验证命令与结果

**Execution Status (2026-03-31 19:50 CST)**

- Status: `Task 6` completed
- Implementation summary:
  - `SysUser.toString()` 已移除 `password/salt` 输出，避免敏感字段通过对象日志或调试文本外泄
  - `SsoIdentityImportServiceImpl` 的单条导入失败 warning（告警）日志现在会保留异常堆栈，但写回 `item.errorMessage` 的仍然是受控摘要，不改变前端可见输出契约
  - `docs/superpowers/plans/2026-03-27-sso-backend-best-practice-remediation.md` 已明确标记为历史文档，并把当前 canonical handoff（权威交接入口）收口到本计划文件
  - `SsoIdentityImportServiceImplTest` 的日志捕获清理已改为 `try/finally`，避免断言失败时泄漏 `ListAppender` 污染后续测试
  - 由于 `README.md`、`docs/review_plans/2026-03-30-sso-backend-second-pass-gap-remediation-plan.md`、`docs/review_plans/2026-03-30-sso-backend-third-pass-fresh-review-remediation-plan.md` 存在非目标本地改动，本轮未覆盖这些文件，也未扩散到额外重构
- Verification:
  - 首轮 red baseline：
    - `mvn -pl yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysUserToStringContractTest,SsoIdentityImportServiceImplTest test`
    - Result: `BUILD FAILURE`
    - 关键红灯：
      - `SysUser.toString()` 仍输出 `password=cipher-text` 与 `salt=salt-value`
      - `SsoIdentityImportServiceImpl` 的 `INIT_IMPORT item failed` warning 日志未携带异常堆栈
  - 定向复验命令：
    - `mvn -pl yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysUserToStringContractTest,SsoIdentityImportServiceImplTest test`
    - Result: `BUILD SUCCESS`
    - Summary: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`
  - 全量验证命令：
    - `mvn test`
    - Result: `BUILD SUCCESS`
    - Summary: `Tests run: 78, Failures: 0, Errors: 0, Skipped: 5`
  - Review gates:
    - `spec compliance` review: PASS
    - `code quality` review: PASS
  - Reviewer note:
    - reviewer 首轮提示的 `Step 5` 文档收口缺口来自并发快照；当前计划文件已补齐勾选状态、handoff 与最新验证结果
    - reviewer 提出的 `ListAppender` 清理健壮性问题已按最小范围修复，并通过定向验证与全量复验
- Handoff:
  - canonical handoff（权威交接入口）：
    - `docs/review_plans/2026-03-31-sso-backend-comprehensive-best-practice-audit-remediation-plan.md`
  - 本轮新增 / 修改的目标文件：
    - `docs/review_plans/2026-03-31-sso-backend-comprehensive-best-practice-audit-remediation-plan.md`
    - `yr-common/src/main/java/com/yr/common/core/domain/entity/SysUser.java`
    - `yr-system/src/main/java/com/yr/system/service/impl/SsoIdentityImportServiceImpl.java`
    - `docs/superpowers/plans/2026-03-27-sso-backend-best-practice-remediation.md`
    - `yr-system/src/test/java/com/yr/system/domain/entity/SysUserToStringContractTest.java`
    - `yr-system/src/test/java/com/yr/system/service/impl/SsoIdentityImportServiceImplTest.java`
  - 最新 `git status --short`：

```bash
 M README.md
 M docs/review_plans/2026-03-30-sso-backend-second-pass-gap-remediation-plan.md
 M docs/superpowers/plans/2026-03-27-sso-backend-best-practice-remediation.md
 M yr-admin/src/main/java/com/yr/framework/web/service/SysLoginService.java
 M yr-admin/src/main/java/com/yr/web/controller/auth/WxworkAuthController.java
 M yr-admin/src/main/java/com/yr/web/controller/sso/SsoClientController.java
 M yr-admin/src/main/java/com/yr/web/controller/sso/SsoSyncTaskController.java
 M yr-admin/src/main/java/com/yr/web/controller/system/SysProfileController.java
 M yr-admin/src/main/java/com/yr/web/controller/system/SysUserController.java
 M yr-admin/src/main/resources/application-dev.yml
 M yr-admin/src/main/resources/application-local.yml
 M yr-admin/src/main/resources/application-prod.yml
 M yr-admin/src/test/java/com/yr/login/WxworkLoginControllerContractTest.java
 M yr-admin/src/test/java/com/yr/web/controller/sso/SsoClientControllerContractTest.java
 M yr-admin/src/test/java/com/yr/web/controller/sso/SsoSyncTaskControllerContractTest.java
 M yr-admin/src/test/java/com/yr/web/controller/system/SysUserControllerChangeStatusContractTest.java
 M yr-admin/src/test/java/com/yr/web/controller/system/SysUserControllerPasswordResetContractTest.java
 M yr-common/src/main/java/com/yr/common/core/domain/entity/SysUser.java
 M yr-common/src/main/java/com/yr/common/core/domain/model/LoginBody.java
 M yr-framework/src/main/java/com/yr/framework/config/RedisConfig.java
 M yr-system/src/main/java/com/yr/system/mapper/SysUserMapper.java
 M yr-system/src/main/java/com/yr/system/service/ISysUserService.java
 M yr-system/src/main/java/com/yr/system/service/impl/SsoClientServiceImpl.java
 M yr-system/src/main/java/com/yr/system/service/impl/SsoIdentityImportServiceImpl.java
 M yr-system/src/main/java/com/yr/system/service/impl/SysUserServiceImpl.java
 M yr-system/src/main/java/com/yr/system/service/impl/SysUserWriteService.java
 M yr-system/src/main/resources/mapper/system/SysUserMapper.xml
 M yr-system/src/test/java/com/yr/system/service/impl/SsoClientServiceImplSecretStorageContractTest.java
 M yr-system/src/test/java/com/yr/system/service/impl/SsoIdentityImportServiceImplTest.java
 M yr-system/src/test/java/com/yr/system/service/impl/SysUserServiceImplWriteBoundaryContractTest.java
?? docs/review_plans/2026-03-30-sso-backend-third-pass-fresh-review-remediation-plan.md
?? docs/review_plans/2026-03-31-sso-backend-comprehensive-best-practice-audit-remediation-plan.md
?? yr-admin/src/main/java/com/yr/web/controller/sso/dto/SsoClientStatusUpdateRequest.java
?? yr-admin/src/main/java/com/yr/web/controller/sso/dto/SsoSyncTaskDistributionRequest.java
?? yr-admin/src/main/java/com/yr/web/controller/sso/dto/SsoSyncTaskInitImportRequest.java
?? yr-admin/src/main/java/com/yr/web/controller/system/dto/UpdateProfileRequest.java
?? yr-admin/src/main/java/com/yr/web/controller/system/dto/UpdateUserRequest.java
?? yr-admin/src/test/java/com/yr/config/ProdProfileSafetyContractTest.java
?? yr-admin/src/test/java/com/yr/framework/web/service/SysLoginServiceWxworkStateContractTest.java
?? yr-admin/src/test/java/com/yr/web/controller/system/SysProfileControllerWriteBoundaryContractTest.java
?? yr-admin/src/test/java/com/yr/web/controller/system/SysUserControllerEditWriteBoundaryContractTest.java
?? yr-framework/src/test/java/com/yr/framework/config/RedisConfigSecurityContractTest.java
?? yr-system/src/test/java/com/yr/system/domain/entity/
?? yr-system/src/test/java/com/yr/system/service/impl/SsoClientServiceImplValidationContractTest.java
```

  - 最后一条通过的验证命令与结果：
    - `mvn test`
    - Result: `BUILD SUCCESS`
    - Summary: `Tests run: 78, Failures: 0, Errors: 0, Skipped: 5`

## Completion Criteria

- `RedisConfig` 不再使用 unsafe default typing
- `application-prod.yml` 不再保留明显的开发 / 模板默认危险值
- `PUT /system/user` 与 `PUT /system/user/profile` 不再接收完整 `SysUser`
- 企业微信登录形成 `state` 签发 -> 校验 -> 消费 的闭环
- `SsoClient` 与 `SsoSyncTask` controller 输入改为白名单 DTO + Bean Validation
- `SysUser.toString()` 不再输出敏感字段
- `SsoIdentityImportServiceImpl` 失败日志保留堆栈
- 全量 `mvn test` 通过

## Recommended Continuation Prompt

```text
请以 /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be 为 cwd，按照 docs/review_plans/2026-03-31-sso-backend-comprehensive-best-practice-audit-remediation-plan.md 从 Task 1 开始执行，逐任务汇报检查点，不跳步，并同步更新文档状态和验证结果。注意当前本地已有非目标改动：README.md、docs/review_plans/2026-03-30-sso-backend-second-pass-gap-remediation-plan.md、docs/review_plans/2026-03-30-sso-backend-third-pass-fresh-review-remediation-plan.md；除计划内明确要求外，不要覆盖这些文件，也不要扩散到额外重构。
```
