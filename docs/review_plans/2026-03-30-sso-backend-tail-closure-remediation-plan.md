# SSO Backend Tail Closure Remediation Plan

> First-pass closure snapshot only: superseded on 2026-03-30 by `docs/review_plans/2026-03-30-sso-backend-second-pass-gap-remediation-plan.md`.
>
> This document preserves the first tail-closure execution result. For any new execution turn, use the second-pass gap plan as the canonical entrypoint.

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 收口 `sam-sso-be` 在完成前一轮最佳实践整改后仍遗留的真实高信号问题，重点修复 `DISTRIBUTION`（分发）after-commit（提交后回调）状态不闭环、`changeOrg`（切组织）对停用组织校验缺失、以及 validation error（参数校验错误）HTTP 语义不准确的问题。

**Architecture:** 当前后端的 `JDK 17 + Spring Boot 2.7.18` 架构底座已经基本稳定，剩余问题都属于 tail closure（收尾闭环）层面的实现偏差，而不是需要再做一轮大规模架构重构。整改顺序应坚持“先冻结回归 -> 再修最危险的状态/权限语义 -> 最后同步文档与验证口径”，避免继续出现“文档已完成，但真实行为还没闭环”的情况。

**Tech Stack:** Java 17, Spring Boot 2.7.18, Spring Security, Spring Transaction, MyBatis-Plus, RocketMQ, Quartz, Maven multi-module, JUnit 5, Mockito, AssertJ

---

## Scope

本计划只处理本轮复查确认的剩余缺口，不再重新打开已经确认收敛的主题：

- 不再重做 `clientSecret`（客户端密钥）基础脱敏和 hash-at-rest（哈希存储）主体改造
- 不再重做 `resetPwd/changeStatus`（重置密码/状态修改）DTO 边界收口
- 不再重做 HTTP Maven repo（仓库）与 websocket（WebSocket，长连接）依赖清理

本计划只处理以下 4 个 remaining gaps（剩余缺口）：

1. `DISTRIBUTION` after-commit 发送后没有把 task/item 最终状态回写数据库
2. `changeOrg` 只校验 membership（归属关系）启用态，没有校验目标组织本身是否停用
3. `BindException/MethodArgumentNotValidException` 仍返回 `500` 风格错误码，而不是 `400 Bad Request`
4. 旧总览文档已把 `Phase 4/5` 标成完成，但当前仓库真实状态仍需以本计划为准

## Verified Baseline

- `cwd`: `/Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be`
- `git status --short`: 干净
- `mvn test`: `BUILD SUCCESS`
- 当前全量测试结果：
  - `Tests run: 59, Failures: 0, Errors: 0, Skipped: 5`
- 当前仍被 `@EnabledIfSystemProperty`（系统属性开关）跳过的 smoke test（烟雾测试）：
  - `yr-admin/src/test/java/com/yr/sso/SsoDistributionMqSmokeTest.java`
  - `yr-admin/src/test/java/com/yr/sso/SsoInitImportRehearsalSmokeTest.java`
  - `yr-admin/src/test/java/com/yr/mq/MqProducerServiceIntegrationTest.java`

## Current Findings

### P1 `DISTRIBUTION` 状态闭环未完成

- 现状：
  - `yr-system/src/main/java/com/yr/system/service/impl/SsoIdentityDistributionServiceImpl.java`
  - `yr-system/src/main/java/com/yr/system/service/impl/SsoSyncTaskServiceImpl.java`
- 问题：
  - 事务内先把 `item` 构造成 `PENDING`
  - `afterCommit` 只负责真实发送 MQ，但不会把发送结果写回 `task/item`
  - 发送失败只打日志，不会把任务切成 `FAILED/PARTIAL_SUCCESS`
- 结果：
  - 控制台可能长期看到 `PENDING`
  - retry/compensate（重试/补偿）链路缺少可信状态输入
  - 当前文档把“禁止 `RUNNING/PENDING` 漂移”标成已完成，但代码还没做到

### P1 `changeOrg` 未校验目标组织停用态

- 现状：
  - `yr-admin/src/main/java/com/yr/framework/web/service/SysLoginService.java`
  - `yr-system/src/main/java/com/yr/system/service/impl/SysUserOrgServiceImpl.java`
- 问题：
  - 当前只校验 `sys_user_org.enabled = 1`
  - 没有校验 `sys_org.status = 0`
- 结果：
  - 用户仍可能切进已经停用的组织上下文

### P2 validation error HTTP 语义不准确

- 现状：
  - `yr-framework/src/main/java/com/yr/framework/web/exception/GlobalExceptionHandler.java`
  - `yr-common/src/main/java/com/yr/common/core/domain/AjaxResult.java`
  - `yr-admin/src/test/java/com/yr/web/controller/sso/SsoClientControllerContractTest.java`
- 问题：
  - DTO 校验已经接上，但 `BindException` / `MethodArgumentNotValidException` 仍走 `AjaxResult.error(message)`
  - 这会返回 `HttpStatus.ERROR`（500），而不是 `HttpStatus.BAD_REQUEST`（400）
  - 现有 controller contract test 还把这个错误语义固化成了正确行为

### P3 文档入口未切换

- 现状：
  - `docs/review_plans/2026-03-27-sso-backend-best-practice-audit-remediation-overview.md`
- 问题：
  - 该文档包含大量“所有阶段已完成”的状态描述
  - 对下个对话来说，这会误导执行入口和真实剩余范围

## Planned File Structure

### Task 1: Freeze Remaining Regressions

**Files:**
- Modify: `yr-admin/src/test/java/com/yr/framework/web/service/SysLoginServiceChangeOrgSecurityTest.java`
- Modify: `yr-framework/src/test/java/com/yr/framework/web/exception/GlobalExceptionHandlerContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/sso/SsoClientControllerContractTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SsoIdentityDistributionServiceAfterCommitContractTest.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SsoSyncTaskDistributionStateClosureTest.java`

- [x] **Step 1: 先给 `changeOrg` 增加“停用组织不可切入”的失败测试**

目标：
- 关系表仍启用，但目标 `org.status = 1` 时必须拒绝签发 token

- [x] **Step 2: 给 `GlobalExceptionHandler` 增加 400 语义测试**

目标：
- `BindException` 返回 `HttpStatus.BAD_REQUEST`
- `MethodArgumentNotValidException` 返回 `HttpStatus.BAD_REQUEST`

- [x] **Step 3: 更新 `SsoClientControllerContractTest` 的无效输入断言**

目标：
- 空 `clientCode` 不再断言 `500`
- 改为断言 `400` 与受控 message（消息）

- [x] **Step 4: 先把 `DISTRIBUTION` 的状态闭环问题用失败测试钉死**

目标：
- commit（提交）成功后，after-commit 发送成功必须把 task/item 从 `PENDING` 推进到 `SUCCESS`
- after-commit 发送失败必须把 task/item 推进到 `FAILED` 或 `PARTIAL_SUCCESS`

- [x] **Step 5: 跑本任务测试，确认先红灯**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin,yr-framework,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysLoginServiceChangeOrgSecurityTest,GlobalExceptionHandlerContractTest,SsoClientControllerContractTest,SsoIdentityDistributionServiceAfterCommitContractTest,SsoSyncTaskDistributionStateClosureTest test
```

Expected:
- 新增断言先失败，明确暴露当前 remaining gaps（剩余缺口）

Progress (2026-03-30):
- 已完成 Task 1 五个步骤的红灯测试落地与执行，生产代码保持未改动。
- 主命令已红灯：`mvn -pl yr-admin,yr-framework,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysLoginServiceChangeOrgSecurityTest,GlobalExceptionHandlerContractTest,SsoClientControllerContractTest,SsoIdentityDistributionServiceAfterCommitContractTest,SsoSyncTaskDistributionStateClosureTest test`
- 由于 Maven reactor（构建反应器）会先停在 `yr-framework`，补充执行了两条定向命令以拿齐剩余红灯证据：
  - `mvn -pl yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysLoginServiceChangeOrgSecurityTest,SsoClientControllerContractTest test`
  - `mvn -pl yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoIdentityDistributionServiceAfterCommitContractTest,SsoSyncTaskDistributionStateClosureTest test`
- 已完成一轮 spec compliance review（规格符合性审查）与一轮 code quality review（代码质量审查），并据此收紧了 after-commit 发送断言、持久化终态断言、以及停用组织输入信号建模。
- 关键失败已覆盖四个缺口：
  - `changeOrg`：`shouldRejectDisabledTargetOrgEvenWhenMembershipIsEnabled` 期望抛异常但未抛（当前仍可切入 `org.status = 1` 的组织上下文）
  - validation：`GlobalExceptionHandlerContractTest.shouldReturnBadRequestForBindException` 与 `shouldReturnBadRequestForMethodArgumentNotValidException` 断言 `400` 实际 `500`
  - controller：`SsoClientControllerContractTest.shouldRejectBlankClientCodeWhenCreatingClient` 断言 `$.code = 400`，实际响应 `{"msg":"clientCode不能为空","code":500}`
  - DISTRIBUTION：
    - `SsoIdentityDistributionServiceAfterCommitContractTest.shouldClosePendingResultToSuccessAfterCommitDispatch` 断言结果状态应为 `SUCCESS`，实际仍是 `PENDING`
    - `SsoSyncTaskDistributionStateClosureTest` 断言任务/明细最终应从 `PENDING` 推进到 `SUCCESS` 或 `PARTIAL_SUCCESS`，实际持久化快照仍只有 `["PENDING"]`

### Task 2: Close `changeOrg` Active-Org Validation

**Files:**
- Modify: `yr-admin/src/main/java/com/yr/framework/web/service/SysLoginService.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/ISysUserOrgService.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysUserOrgServiceImpl.java`
- Optionally Modify: `yr-system/src/main/java/com/yr/system/service/ISysOrgService.java`
- Modify: `yr-admin/src/test/java/com/yr/framework/web/service/SysLoginServiceChangeOrgSecurityTest.java`

- [x] **Step 1: 给组织归属校验补“组织启用态”语义**

实现要求：
- 不仅检查 `sys_user_org.enabled = 1`
- 还要检查目标 `sys_org.status = 0`

- [x] **Step 2: 保持现有旧 token 删除逻辑不回退**

实现要求：
- 仍然使用当前登录态 token 删除旧登录缓存
- 不要重新引入 `login_tokens:null` 问题

- [x] **Step 3: 跑定向测试验证**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysLoginServiceChangeOrgSecurityTest test
```

Expected:
- PASS

Progress (2026-03-30):
- 已新增 `ISysUserOrgService.hasActiveOrgMembership(...)`，并在 `SysUserOrgServiceImpl` 中把“关系启用 + 组织启用”语义合并到同一校验入口。
- `SysLoginService.changeOrg(...)` 已切换为使用 active-org membership（启用组织归属）校验，但旧 token 删除逻辑仍保持 `currentLoginUser.getToken()` 删除链路不变。
- 已执行：
  - `mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysLoginServiceChangeOrgSecurityTest test`
- 当前结果：`BUILD SUCCESS`

### Task 3: Fix Validation Error HTTP Semantics

**Files:**
- Modify: `yr-framework/src/main/java/com/yr/framework/web/exception/GlobalExceptionHandler.java`
- Modify: `yr-framework/src/test/java/com/yr/framework/web/exception/GlobalExceptionHandlerContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/sso/SsoClientControllerContractTest.java`
- Optionally Modify: `yr-admin/src/test/java/com/yr/web/controller/system/SysUserControllerPasswordResetContractTest.java`
- Optionally Modify: `yr-admin/src/test/java/com/yr/web/controller/system/SysUserControllerChangeStatusContractTest.java`

- [x] **Step 1: 把参数校验异常改为 `400 Bad Request`**

实现要求：
- `BindException` -> `AjaxResult.error(HttpStatus.BAD_REQUEST, message)`
- `MethodArgumentNotValidException` -> `AjaxResult.error(HttpStatus.BAD_REQUEST, message)`

- [x] **Step 2: 更新 controller contract test**

实现要求：
- 不再把 invalid request（非法请求）视为 `500`
- 统一断言 `400`

- [x] **Step 3: 跑定向测试验证**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin,yr-framework -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=GlobalExceptionHandlerContractTest,SsoClientControllerContractTest,SysUserControllerPasswordResetContractTest,SysUserControllerChangeStatusContractTest test
```

Expected:
- PASS

Progress (2026-03-30):
- `GlobalExceptionHandler.validatedBindException(...)` 与 `validExceptionHandler(...)` 已统一改为 `AjaxResult.error(HttpStatus.BAD_REQUEST, message)`。
- `GlobalExceptionHandlerContractTest` 与 `SsoClientControllerContractTest` 已保持 `400` 断言；`SysUserControllerPasswordResetContractTest` / `SysUserControllerChangeStatusContractTest` 复跑通过，证明本轮没有把此前 DTO 边界整改带回退。
- 已执行：
  - `mvn -pl yr-admin,yr-framework -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=GlobalExceptionHandlerContractTest,SsoClientControllerContractTest,SysUserControllerPasswordResetContractTest,SysUserControllerChangeStatusContractTest test`
- 当前结果：`BUILD SUCCESS`

### Task 4: Close `DISTRIBUTION` After-Commit State Loop

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SsoIdentityDistributionServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SsoSyncTaskServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/support/SsoSyncTaskFailureRecorder.java`
- Create: `yr-system/src/main/java/com/yr/system/service/support/SsoDistributionDispatchResultRecorder.java`
- Optionally Modify: `yr-system/src/main/java/com/yr/system/service/ISsoSyncTaskItemService.java`
- Optionally Modify: `yr-system/src/main/java/com/yr/system/service/impl/SsoSyncTaskItemServiceImpl.java`
- Optionally Modify: `yr-system/src/main/resources/mapper/system/SsoSyncTaskItemMapper.xml`
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SsoIdentityDistributionServiceAfterCommitContractTest.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SsoSyncTaskDistributionStateClosureTest.java`

- [x] **Step 1: 先明确 after-commit 成功/失败后的数据库目标态**

目标态建议：
- 全成功：`task = SUCCESS`，所有 item = `SUCCESS`
- 全失败：`task = FAILED`，所有 item = `FAILED`
- 部分失败：`task = PARTIAL_SUCCESS`

- [x] **Step 2: 新建一个 `REQUIRES_NEW`（新事务）结果回写器**

实现要求：
- after-commit 真实发送后，把 item/task 最终状态写回数据库
- 不能只留在内存对象里

- [x] **Step 3: 失败场景也要可观测**

实现要求：
- after-commit 单条发送失败时要落 item.errorMessage
- task.resultSummary 需要反映最终发送结果

- [x] **Step 4: 保持 “rollback 前不发消息” 这个已修复行为不回退**

实现要求：
- 保留现有 no-send-before-commit 保护
- 新增状态回写后，不能重新把 MQ 提前到事务内

- [x] **Step 5: 跑定向测试验证**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoIdentityDistributionServiceAfterCommitContractTest,SsoSyncTaskDistributionStateClosureTest,SsoSyncTaskServiceImplFailFastTest,SsoDistributionMqContractTest test
```

Expected:
- PASS

Progress (2026-03-30):
- `SsoIdentityDistributionServiceImpl` 已改为在 after-commit 阶段先闭环内存结果，再调用新的 `SsoDistributionDispatchResultRecorder` 用 `REQUIRES_NEW` 事务把 task/item 最终态回库。
- `SsoDistributionDispatchResultRecorder` 已负责回写任务状态摘要和明细最终状态；`SsoSyncTaskItemServiceImpl.replaceTaskItems(...)` 已显式清空旧 `itemId`，避免 after-commit 二次 replace 时带着历史主键重插。
- `SsoIdentityDistributionServiceAfterCommitContractTest` / `SsoSyncTaskDistributionStateClosureTest` 已升级为“两段式观测”：
  - 事务内先看到初始 `PENDING`
  - after-commit 再看到最终 `SUCCESS` 或 `PARTIAL_SUCCESS/FAILED`
- 已执行：
  - `mvn -pl yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoIdentityDistributionServiceAfterCommitContractTest,SsoSyncTaskDistributionStateClosureTest,SsoSyncTaskServiceImplFailFastTest,SsoDistributionMqContractTest test`
- 当前结果：`BUILD SUCCESS`

### Task 5: Refresh Canonical Docs And Handoff

**Files:**
- Modify: `docs/review_plans/2026-03-27-sso-backend-best-practice-audit-remediation-overview.md`
- Modify: `README.md`
- Modify: `docs/review_plans/2026-03-30-sso-backend-tail-closure-remediation-plan.md`

- [x] **Step 1: 把 2026-03-27 文档明确标记为 historical overview（历史总览）**

实现要求：
- 顶部加 superseded note（后续文档接管提示）
- 不再把它作为当前执行入口

- [x] **Step 2: 保证 README 指向新的 canonical tail plan**

实现要求：
- 下个对话打开仓库时，先看到的是当前剩余缺口，而不是已完成总览

- [x] **Step 3: 全量回归后更新本计划执行状态**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn test
```

Expected:
- `BUILD SUCCESS`

Progress (2026-03-30):
- `docs/review_plans/2026-03-27-sso-backend-best-practice-audit-remediation-overview.md` 已明确改为历史总览口径，不再把 `All phases completed` 误导成当前执行状态。
- `README.md` 已把当前整改主文档切到 `docs/review_plans/2026-03-30-sso-backend-tail-closure-remediation-plan.md`，并显式说明 2026-03-27 文档仅保留历史快照。
- 已执行：
  - `mvn test`
- 当前结果：`BUILD SUCCESS`
- 全量测试基线：
  - `Tests run: 60, Failures: 0, Errors: 0, Skipped: 5`
- 当前计划状态：
  - `Task 1` 已完成：剩余缺口红灯冻结
  - `Task 2` 已完成：`changeOrg` 启用组织校验闭环
  - `Task 3` 已完成：validation error 统一回到 `400`
  - `Task 4` 已完成：`DISTRIBUTION` after-commit 最终态回库且不回退 rollback 保护
  - `Task 5` 已完成：文档入口切换与全量回归完成

## Recommended Execution Order

1. `Task 1: Freeze Remaining Regressions`
2. `Task 2: Close changeOrg Active-Org Validation`
3. `Task 3: Fix Validation Error HTTP Semantics`
4. `Task 4: Close DISTRIBUTION After-Commit State Loop`
5. `Task 5: Refresh Canonical Docs And Handoff`

原因：

- `Task 4` 是最复杂的行为闭环，必须先有红灯测试再动手。
- `Task 2` 和 `Task 3` 都是局部修复，先做能快速降低风险并减少并发改动面。
- 文档入口要最后再刷新，避免中途多次改 handoff（交接）口径。

## Next-Turn Handoff

新开对话时，直接提供以下四项：

1. `cwd`

```text
/Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
```

2. canonical plan（权威计划）

```text
docs/review_plans/2026-03-30-sso-backend-tail-closure-remediation-plan.md
```

3. historical overview（历史总览）

```text
docs/review_plans/2026-03-27-sso-backend-best-practice-audit-remediation-overview.md
```

4. 执行要求

```text
2026-03-30 tail-closure plan 已完成；请改为按照 second-pass gap plan 从 Task 1 开始执行，逐任务汇报检查点，不跳步，并同步更新 docs/review_plans/2026-03-30-sso-backend-second-pass-gap-remediation-plan.md 的状态。
```

## Suggested Prompt

```text
请以 /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be 为 cwd，按照 docs/review_plans/2026-03-30-sso-backend-second-pass-gap-remediation-plan.md 从 Task 1 开始执行，逐任务汇报检查点，不跳步，并同步更新文档状态；docs/review_plans/2026-03-30-sso-backend-tail-closure-remediation-plan.md 仅作为上一轮收口快照参考。
```
