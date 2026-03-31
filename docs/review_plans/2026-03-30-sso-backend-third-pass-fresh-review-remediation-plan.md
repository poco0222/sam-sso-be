# SSO Backend Third-Pass Fresh Review Remediation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于 2026-03-30 的 fresh review（重新复核）结果，收口 `sam-sso-be` 在前两轮整改后仍未覆盖的真实安全边界与输入契约缺口，优先修复用户写接口越权/over-posting（过量绑定）、企业微信 `OAuth`（授权登录）state（状态参数）校验缺失、以及客户端配置输入校验过弱的问题。

**Architecture:** 当前 `JDK 17 + Spring Boot 2.7.18` 底座、`SecurityFilterChain`（安全过滤链）、`Liquibase`（数据库变更管理）分层与 `mvn test` 基线已经基本稳定，本轮不需要再做大规模架构重构。整改应继续遵循“先冻结红灯 -> 再修最高风险代码边界 -> 最后清理治理残留并刷新 handoff（交接）入口”的顺序，避免再次出现“测试全绿，但关键权限边界仍能被绕过”的情况。

**Tech Stack:** Java 17, Spring Boot 2.7.18, Spring Security, Spring MVC, MyBatis-Plus, Liquibase, Redis, RocketMQ, Maven multi-module, JUnit 5, MockMvc, Mockito, AssertJ

---

## Scope

本计划只处理 fresh review 新识别出的 remaining gaps（剩余缺口），不重复打开已经确认收敛的主题：

- 不再重做 `changeOrg` 旧 token 失效、目标组织启用态校验与 MQ after-commit（提交后回调）闭环
- 不再重做 `clientSecret`（客户端密钥）hash-at-rest（哈希存储）与 one-time reveal（一次性展示）主体改造
- 不再重做 validation transport semantics（校验传输层语义）`HTTP 400` 联动整改

本计划只处理以下 5 个高信号问题：

1. `PUT /system/user` 仍可通过通用编辑入口绕过专用密码重置边界，并把 `password` 直接下沉到通用 `updateUser` SQL
2. `PUT /system/user/profile` 仍直接接收 `SysUser entity（实体）` 并复用通用 `updateUser` SQL，存在自助修改 `userName/deptId/status/remark` 等字段的 over-posting 风险
3. 企业微信登录链路虽然生成了 `state`，但没有持久化和校验；同时失败日志会把带 `corpsecret/access_token` 的请求 URL 直接写入日志
4. `SsoClient` 写接口输入契约仍偏弱：状态切换入口使用原始 entity，`status`/`allow*`/`syncEnabled` 只做非空校验，`redirectUris` 只检查 scheme（协议）存在且未把非法 URI 映射成受控业务错误
5. 仍有少量治理残留：`SysUser.toString()` 会拼出 `password/salt`，`application-prod.yml` 仍保留 `demoEnabled=true`、`devtools.restart.enabled=true` 以及模板化默认路径

## Verified Baseline

- `cwd`: `/Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be`
- `git status --short`: 审计开始前干净
- `command -v mvn`: `/Users/PopoY/workingFiles/maven-3.9.12/bin/mvn`
- `java -version`: `OpenJDK 17.0.18`
- 已执行：

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn test
```

- 当前结果：`BUILD SUCCESS`
- 当前全量测试基线：
  - `Tests run: 60, Failures: 0, Errors: 0, Skipped: 5`
- 当前 canonical docs（权威文档）：
  - latest closure snapshot（上一轮最新收口快照）：`docs/review_plans/2026-03-30-sso-backend-second-pass-gap-remediation-plan.md`
  - previous closure snapshot（上一轮收口快照）：`docs/review_plans/2026-03-30-sso-backend-tail-closure-remediation-plan.md`

## Current Findings

### P0 `SysUserController.edit` 仍可绕过专用密码重置边界

- 现状：
  - `yr-admin/src/main/java/com/yr/web/controller/system/SysUserController.java:175-188`
  - `yr-system/src/main/resources/mapper/system/SysUserMapper.xml:292-310`
  - `yr-admin/src/test/java/com/yr/web/controller/system/SysUserControllerPasswordResetContractTest.java:42-69`
- 问题：
  - `PUT /system/user` 只要求 `system:user:edit` 权限，却直接把整个 `SysUser` 传入 `userService.updateUser(...)`
  - `updateUser` SQL 仍会写 `password`
  - 这使得“普通编辑用户”和“重置密码”两条本应隔离的写边界重新混叠
- 影响：
  - 拥有 `system:user:edit` 但没有 `system:user:resetPwd` 的调用方，理论上仍能通过编辑接口修改密码
  - 当前 edit 链路不会像 `resetPwd` 那样显式走 `SecurityUtils.encryptPassword(...)`，若请求体带入明文密码，会直接写库

### P1 `SysProfileController.updateProfile` 仍存在自助 over-posting 风险

- 现状：
  - `yr-admin/src/main/java/com/yr/web/controller/system/SysProfileController.java:58-83`
  - `yr-system/src/main/java/com/yr/system/service/impl/SysUserServiceImpl.java:273-276`
  - `yr-system/src/main/resources/mapper/system/SysUserMapper.xml:295-307`
- 问题：
  - 个人资料修改入口仍使用 `@RequestBody SysUser`
  - controller 只显式清空了 `password`，但没有收窄 `userName/deptId/status/remark/avatar` 等字段
  - service 直接复用通用 `updateUser` SQL，因此这些字段只要非空就会被更新
- 影响：
  - 当前登录用户可以通过 profile 接口越界修改不属于“个人资料”范畴的字段
  - 这会污染身份语义、部门归属和状态语义，并继续扩大 `SysUser entity` 作为写 DTO（数据传输对象）的攻击面

### P1 企业微信登录缺少 `state` 校验且失败日志会泄露敏感凭据

- 现状：
  - `yr-admin/src/main/java/com/yr/framework/web/service/SysLoginService.java:285-301`
  - `yr-common/src/main/java/com/yr/common/core/domain/model/LoginBody.java:35-52`
  - `yr-admin/src/main/java/com/yr/web/controller/auth/WxworkAuthController.java:48-52`
  - `yr-admin/src/main/java/com/yr/framework/web/service/SysLoginService.java:351-358`
  - `yr-admin/src/main/java/com/yr/framework/web/service/SysLoginService.java:385-418`
- 问题：
  - 预登录地址会生成随机 `state`，但当前实现没有把它落到 `Redis`（缓存）或 session，也没有在 `/auth/wxwork/login` 校验 `LoginBody.state`
  - 企业微信请求失败时，日志直接输出完整 `requestUrl`；这里可能包含 `corpsecret` 或 `access_token`
- 影响：
  - 我据当前代码推断，这条 `OAuth` 登录链路仍缺少最基本的 login CSRF（登录跨站请求伪造）防护闭环
  - 一旦上游接口失败，企业微信敏感凭据可能进入应用日志

### P2 `SsoClient` 输入契约仍偏弱

- 现状：
  - `yr-admin/src/main/java/com/yr/web/controller/sso/SsoClientController.java:128-133`
  - `yr-admin/src/main/java/com/yr/web/controller/sso/dto/SsoClientCreateRequest.java:23-37`
  - `yr-admin/src/main/java/com/yr/web/controller/sso/dto/SsoClientUpdateRequest.java:28-42`
  - `yr-system/src/main/java/com/yr/system/service/impl/SsoClientServiceImpl.java:113-121`
  - `yr-system/src/main/java/com/yr/system/service/impl/SsoClientServiceImpl.java:139-176`
- 问题：
  - `changeStatus` 入口仍使用原始 `SsoClient entity`
  - `status`/`allowPasswordLogin`/`allowWxworkLogin`/`syncEnabled` 只有“非空”语义，没有被收窄到允许值
  - `redirectUris` 只要求 URI 能解析出 scheme；对非法 URI 的 `IllegalArgumentException` 也没有转换成受控业务错误
- 影响：
  - 客户端表仍可能被写入无效状态值或异常 URI
  - 这些值会污染后续鉴权、回调地址配置与同步配置的可信度

### P3 仍有治理残留会放大后续风险

- 现状：
  - `yr-common/src/main/java/com/yr/common/core/domain/entity/SysUser.java:387-410`
  - `yr-admin/src/main/resources/application-prod.yml:9-12`
  - `yr-admin/src/main/resources/application-prod.yml:161-164`
- 问题：
  - `SysUser.toString()` 仍会拼出 `password` 与 `salt`
  - `application-prod.yml` 仍保留 `demoEnabled=true`、`devtools.restart.enabled=true` 和模板化上传路径
- 影响：
  - 一旦对象被日志框架或异常上下文打印，敏感字段会进入日志
  - 生产 profile 继续保留模板默认值，会让后续部署与排障持续混入低价值噪音

## Planned File Structure

### Task 1: Freeze User Write-Boundary Regressions

**Files:**
- Create: `yr-admin/src/test/java/com/yr/web/controller/system/SysUserControllerEditWriteBoundaryContractTest.java`
- Create: `yr-admin/src/test/java/com/yr/web/controller/system/SysProfileControllerWriteBoundaryContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/system/SysUserControllerPasswordResetContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/system/SysUserControllerChangeStatusContractTest.java`

- [ ] **Step 1: 先为 edit 入口补密码写边界红灯**

目标：
- `PUT /system/user` 不允许透传 `password`
- `system:user:edit` 不应承担 `resetPwd` 语义

- [ ] **Step 2: 再为 profile 入口补最小写字段红灯**

目标：
- profile 只允许修改 `nickName/phonenumber/email/sex`
- `userName/deptId/status/remark/avatar/password` 不得继续下沉

- [ ] **Step 3: 跑定向测试，确认先红灯**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysUserControllerEditWriteBoundaryContractTest,SysProfileControllerWriteBoundaryContractTest,SysUserControllerPasswordResetContractTest,SysUserControllerChangeStatusContractTest test
```

Expected:
- 新增断言先失败，明确暴露剩余的用户写边界缺口

### Task 2: Split User Edit/Profile DTOs And Dedicated Write Paths

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

- [ ] **Step 1: 为 admin edit 拆专用 DTO（请求 DTO）与 service 写入口**

实现要求：
- 普通编辑链路不能再接收完整 `SysUser`
- 明确排除 `password` 字段
- 如果业务上仍需改状态，必须继续走 `changeStatus`

- [ ] **Step 2: 为 profile 拆专用 DTO 与 profile-only SQL（仅资料更新 SQL）**

实现要求：
- 不再复用通用 `updateUser`
- profile 只允许改基础资料字段

- [ ] **Step 3: 跑定向测试验证**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysUserControllerEditWriteBoundaryContractTest,SysProfileControllerWriteBoundaryContractTest,SysUserControllerPasswordResetContractTest,SysUserControllerChangeStatusContractTest,SysUserServiceImplWriteBoundaryContractTest test
```

Expected:
- PASS

### Task 3: Close Wxwork `OAuth` State And Secret-Redaction Gap

**Files:**
- Modify: `yr-common/src/main/java/com/yr/common/core/domain/model/LoginBody.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/auth/WxworkAuthController.java`
- Modify: `yr-admin/src/main/java/com/yr/framework/web/service/SysLoginService.java`
- Create: `yr-admin/src/test/java/com/yr/framework/web/service/SysLoginServiceWxworkStateContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/login/WxworkLoginControllerContractTest.java`

- [ ] **Step 1: 冻结 `state` 必须先签发、再校验的红灯**

目标：
- `/pre-login` 生成 `state` 后必须持久化到 `Redis`
- `/login` 必须校验 `state`，缺失/不匹配必须拒绝

- [ ] **Step 2: 同步冻结敏感 URL 不得进日志的红灯**

目标：
- `corpsecret`、`access_token` 不得出现在 error log（错误日志）消息中

- [ ] **Step 3: 落地实现**

实现要求：
- 使用短 TTL（有效期）缓存 `state`
- 登录成功或失败后都要消费/失效 `state`
- 日志只保留场景与脱敏后的 endpoint（接口），不要记录完整 query string（查询串）

- [ ] **Step 4: 跑定向测试验证**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysLoginServiceWxworkStateContractTest,WxworkLoginControllerContractTest test
```

Expected:
- PASS

### Task 4: Harden `SsoClient` Input Contracts

**Files:**
- Create: `yr-admin/src/main/java/com/yr/web/controller/sso/dto/ChangeClientStatusRequest.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/sso/dto/SsoClientCreateRequest.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/sso/dto/SsoClientUpdateRequest.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/sso/SsoClientController.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SsoClientServiceImpl.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SsoClientServiceImplInputContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/sso/SsoClientControllerContractTest.java`

- [ ] **Step 1: 先把状态切换与 flag（标记）取值约束补成红灯**

目标：
- `status` 只能是 `0/1`
- `allowPasswordLogin/allowWxworkLogin/syncEnabled` 只能是 `Y/N`

- [ ] **Step 2: 收紧 `redirectUris` 合法性校验**

实现要求：
- 至少把非法 URI 统一映射为受控 `CustomException`
- 对 scheme 做更严格约束，不要只判断“非空”

- [ ] **Step 3: 跑定向测试验证**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoClientControllerContractTest,SsoClientServiceImplInputContractTest test
```

Expected:
- PASS

### Task 5: Clean Governance Residue And Refresh Docs

**Files:**
- Modify: `yr-common/src/main/java/com/yr/common/core/domain/entity/SysUser.java`
- Modify: `yr-admin/src/main/resources/application-prod.yml`
- Modify: `README.md`
- Modify: `docs/review_plans/2026-03-30-sso-backend-second-pass-gap-remediation-plan.md`
- Modify: `docs/review_plans/2026-03-30-sso-backend-third-pass-fresh-review-remediation-plan.md`
- Optionally Create: `yr-common/src/test/java/com/yr/common/core/domain/entity/SysUserSensitiveDataContractTest.java`

- [ ] **Step 1: 去掉 `SysUser.toString()` 中的敏感字段**

目标：
- `password/salt` 不再进入 `toString()`

- [ ] **Step 2: 收紧 prod profile**

实现要求：
- `demoEnabled=false`
- `devtools.restart.enabled=false`
- 把明显模板化的默认路径收口成环境变量占位，不再保留误导性默认值

- [ ] **Step 3: 刷新 canonical docs（权威文档）与 handoff**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn test
```

Expected:
- `BUILD SUCCESS`

## Recommended Execution Order

1. `Task 1: Freeze User Write-Boundary Regressions`
2. `Task 2: Split User Edit/Profile DTOs And Dedicated Write Paths`
3. `Task 3: Close Wxwork OAuth State And Secret-Redaction Gap`
4. `Task 4: Harden SsoClient Input Contracts`
5. `Task 5: Clean Governance Residue And Refresh Docs`

原因：

- 用户写边界是当前最高风险面，且直接关系权限语义与密码安全，必须先冻结红灯
- `OAuth state` 与敏感日志问题属于第二优先级安全面，修复成本相对可控
- `SsoClient` 输入契约属于配置可信度与 runtime safety（运行时安全）问题，适合在主安全边界收口后继续推进
- 治理残留放到最后处理，避免文档与配置收尾打断前面的高风险修复

## Next-Turn Handoff

如需新开对话继续执行，直接提供以下四项：

1. `cwd`

```text
/Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
```

2. latest execution entrypoint（最新执行入口）

```text
docs/review_plans/2026-03-30-sso-backend-third-pass-fresh-review-remediation-plan.md
```

3. previous closure snapshots（上一轮收口快照）

```text
docs/review_plans/2026-03-30-sso-backend-second-pass-gap-remediation-plan.md
docs/review_plans/2026-03-30-sso-backend-tail-closure-remediation-plan.md
```

4. 执行要求

```text
请从 Task 1 开始按顺序执行，逐任务汇报检查点，不跳步，并同步更新 docs/review_plans/2026-03-30-sso-backend-third-pass-fresh-review-remediation-plan.md 的进度与验证结果。除文档中列出的整改项外，不要顺手扩散到计划外重构。
```

## Suggested Prompt

```text
请以 /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be 为 cwd，按照 docs/review_plans/2026-03-30-sso-backend-third-pass-fresh-review-remediation-plan.md 从 Task 1 开始执行，逐任务汇报检查点，不跳步，并同步更新文档状态和验证结果。除计划内整改外，不要扩散到额外重构。
```
