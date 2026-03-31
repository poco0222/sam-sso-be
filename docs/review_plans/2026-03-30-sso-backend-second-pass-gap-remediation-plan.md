# SSO Backend Second-Pass Gap Remediation Plan

> Historical closure snapshot only: superseded on 2026-03-30 by `docs/review_plans/2026-03-30-sso-backend-third-pass-fresh-review-remediation-plan.md`.
>
> This document preserves the second-pass closure result. For any new execution turn, use the third-pass fresh review plan as the canonical entrypoint.

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 收口 `sam-sso-be` 在完成 2026-03-30 tail-closure（收尾闭环）整改后，经第二次复核仍然存在的剩余缺口，重点修正 validation（参数校验）只改了 body code（响应体业务码）但未改 transport-level HTTP status（传输层 HTTP 状态码）、`MethodArgumentNotValidException` 对 class-level validation（类级校验）缺少稳健兜底、以及 `DISTRIBUTION` after-commit（提交后回调）结果回写失败时仍然缺少可恢复的闭环标记。

**Architecture:** 当前 `JDK 17 + Spring Boot 2.7.18` 的后端底座已经可用，本轮问题集中在“最后一层语义对齐”而不是架构重构。整改顺序应坚持“先冻结二次复核暴露的回归 -> 再收口 transport 语义和 after-commit 失败补偿 -> 最后刷新 canonical docs（权威文档）与 handoff（交接）入口”，避免继续把“已完成”的文档当成下一轮执行入口。

**Tech Stack:** Java 17, Spring Boot 2.7.18, Spring MVC, Spring Security, Spring Transaction, MyBatis-Plus, RocketMQ, Maven multi-module, MockMvc, JUnit 5, Mockito, AssertJ

---

## Scope

本计划只处理 second-pass review（第二次复核）确认的剩余缺口，不重新打开已经验证通过的主题：

- 不再重做 `changeOrg` 目标组织启用态校验
- 不再重做 `DISTRIBUTION` 正常成功/部分失败路径的 task/item 最终态回库
- 不再重做 `README` 与 2026-03-27 历史总览的首轮入口切换

本计划只处理以下 4 个 remaining gaps（剩余缺口）：

1. validation error 现在仍然返回 `HTTP 200 OK + body.code=400`，没有真正对齐 transport-level `400 Bad Request`
2. `MethodArgumentNotValidException` 当前只取 `fieldError`，遇到 class-level validation 时仍可能抛 `NullPointerException`
3. `DISTRIBUTION` after-commit 最终态回写失败时，当前只打日志并重新抛出，缺少任务级 reconciliation-failed（对账失败）标记与后续恢复入口
4. 2026-03-30 tail plan 已经标记 `Task 1-5 completed`，但 handoff 仍指向“从 Task 1 开始执行”，canonical docs 入口已经过期

## Verified Baseline

- `cwd`: `/Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be`
- 当前 working tree（工作区）包含本轮 tail-closure 的未提交改动
- 已串行复核通过：

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin,yr-framework,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysLoginServiceChangeOrgSecurityTest,GlobalExceptionHandlerContractTest,SsoClientControllerContractTest,SsoIdentityDistributionServiceAfterCommitContractTest,SsoSyncTaskDistributionStateClosureTest test
```

- 当前结果：`BUILD SUCCESS`
- 已串行复核全量：

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn test
```

- 当前结果：`BUILD SUCCESS`
- 当前全量测试基线：
  - `Tests run: 60, Failures: 0, Errors: 0, Skipped: 5`

## Current Findings

### P1 validation 只修到 body code，没有修到真实 HTTP status

- 现状：
  - `yr-framework/src/main/java/com/yr/framework/web/exception/GlobalExceptionHandler.java`
  - `yr-admin/src/test/java/com/yr/web/controller/sso/SsoClientControllerContractTest.java`
- 问题：
  - `GlobalExceptionHandler` 已把 validation error（校验错误）的 `AjaxResult.code` 改成 `400`
  - 但控制器契约测试仍然断言 `status().isOk()`
- 结果：
  - transport-level 仍然是 `HTTP 200`
  - 当前“HTTP 语义已修复”的结论并不完整

### P1 `MethodArgumentNotValidException` 对 class-level validation 缺少兜底

- 现状：
  - `yr-framework/src/main/java/com/yr/framework/web/exception/GlobalExceptionHandler.java`
  - `yr-framework/src/test/java/com/yr/framework/web/exception/GlobalExceptionHandlerContractTest.java`
- 问题：
  - 当前实现直接读取 `e.getBindingResult().getFieldError().getDefaultMessage()`
  - 如果未来出现只有 `ObjectError`（对象级错误）没有 `FieldError`（字段级错误）的校验场景，仍会空指针
- 结果：
  - 本轮 validation 修复只覆盖了字段级错误，缺少稳健收口

### P1 after-commit 结果回写失败仍缺少可恢复闭环

- 现状：
  - `yr-system/src/main/java/com/yr/system/service/impl/SsoIdentityDistributionServiceImpl.java`
  - `yr-system/src/main/java/com/yr/system/service/support/SsoDistributionDispatchResultRecorder.java`
- 问题：
  - after-commit 阶段如果 MQ 已经成功发送，但 `recordDispatchResult(...)` 回写失败，当前逻辑只记录日志并重新抛出
  - 没有额外的 task-level fallback marker（任务级兜底标记）或恢复入口
- 结果：
  - 极端情况下，系统可能重新回到“消息已发出，但任务侧没有可信终态”的语义缺口

### P2 canonical docs handoff 已过期

- 现状：
  - `docs/review_plans/2026-03-30-sso-backend-tail-closure-remediation-plan.md`
  - `README.md`
- 问题：
  - 当前 tail plan 已经把所有任务标为完成
  - 但 handoff 仍写着“请从 Task 1 开始执行”
- 结果：
  - 新对话会被误导回已经完成的执行入口，而不是这轮 second-pass gap plan

## Planned File Structure

### Task 1: Freeze Second-Pass Regressions

**Files:**
- Modify: `yr-framework/src/test/java/com/yr/framework/web/exception/GlobalExceptionHandlerContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/sso/SsoClientControllerContractTest.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SsoIdentityDistributionRecorderFailureContractTest.java`
- Modify: `docs/review_plans/2026-03-30-sso-backend-second-pass-gap-remediation-plan.md`

- [x] **Step 1: 先把 validation transport-level 语义缺口钉成失败测试**

目标：
- validation request（校验失败请求）不再返回 `HTTP 200`
- `MockMvc` 契约改为断言 `status().isBadRequest()`
- body 仍保持 `{"code":400,"msg":"..."}` 兼容形态

- [x] **Step 2: 给 class-level validation 增加无 `fieldError` 的红灯测试**

目标：
- `MethodArgumentNotValidException` 只有 `ObjectError` 时，异常处理仍返回受控 `400 + msg`
- 不允许因为 `getFieldError()` 为 `null` 再退化成 `500`

- [x] **Step 3: 给 after-commit recorder 失败路径增加红灯测试**

目标：
- 模拟 `SsoDistributionDispatchResultRecorder.recordDispatchResult(...)` 失败
- 明确要求系统至少留下一个 task-level reconciliation failure（任务级对账失败）标记，而不是只打日志

- [x] **Step 4: 跑本任务测试，确认先红灯**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin,yr-framework,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=GlobalExceptionHandlerContractTest,SsoClientControllerContractTest,SsoIdentityDistributionRecorderFailureContractTest test
```

Expected:
- 新增断言先失败，明确暴露 second-pass remaining gaps（第二次复核剩余缺口）

Progress (2026-03-30):
- 已补齐 second-pass Task 1 的三类红灯测试：
  - `GlobalExceptionHandlerContractTest` 新增 class-level validation（类级校验）只有 `ObjectError` 时的兜底断言
  - `SsoClientControllerContractTest` 把无效输入的 transport-level 断言收紧到 `status().isBadRequest()`
  - `SsoIdentityDistributionRecorderFailureContractTest` 新增 after-commit recorder 回写失败时必须留下 task-level reconciliation failure marker（任务级对账失败标记）的契约
- 已执行主命令：
  - `mvn -pl yr-admin,yr-framework,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=GlobalExceptionHandlerContractTest,SsoClientControllerContractTest,SsoIdentityDistributionRecorderFailureContractTest test`
- 由于 Maven reactor（构建反应器）先停在 `yr-framework`，补充执行了两条定向命令以拿齐剩余红灯证据：
  - `mvn -pl yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoClientControllerContractTest test`
  - `mvn -pl yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoIdentityDistributionRecorderFailureContractTest test`
- 当前红灯证据已覆盖三类 remaining gaps（剩余缺口）：
  - class-level validation：`shouldFallbackToObjectErrorMessageWhenFieldErrorIsMissing` 断言“不抛异常并返回 400”，实际因 `getFieldError()` 为 `null` 触发 `NullPointerException`
  - validation transport-level：`shouldRejectBlankClientCodeWhenCreatingClient` 断言 `HTTP 400`，实际响应仍是 `HTTP 200`，body 为 `{"msg":"clientCode不能为空","code":400}`
  - after-commit recorder failure：`shouldLeaveTaskLevelReconciliationMarkerWhenRecorderFailsAfterCommit` 断言任务侧应留下 `FAILED` / `PARTIAL_SUCCESS` 的对账失败标记，实际 `task.status` 仍是 `SUCCESS`

### Task 2: Align Validation Transport Semantics And Error Fallback

**Files:**
- Modify: `yr-framework/src/main/java/com/yr/framework/web/exception/GlobalExceptionHandler.java`
- Modify: `yr-framework/src/test/java/com/yr/framework/web/exception/GlobalExceptionHandlerContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/sso/SsoClientControllerContractTest.java`
- Optionally Modify: `yr-admin/src/test/java/com/yr/web/controller/system/SysUserControllerPasswordResetContractTest.java`
- Optionally Modify: `yr-admin/src/test/java/com/yr/web/controller/system/SysUserControllerChangeStatusContractTest.java`

- [x] **Step 1: 先做 compatibility checkpoint（兼容性检查点）**

检查要求：
- 确认前端/调用方是否依赖 validation error 永远返回 `HTTP 200`
- 若没有明确依赖，默认按 official best practice（官方最佳实践）切到真实 `400 Bad Request`

- [x] **Step 2: 把 validation exception 改为真实 `400 Bad Request`**

实现要求：
- 优先使用 `ResponseEntity<AjaxResult>` 或等价方式返回真实 `HTTP 400`
- 保留当前 `AjaxResult.code = 400` 与 message 结构，避免 body 契约回退

- [x] **Step 3: 为 `MethodArgumentNotValidException` 增加全量兜底**

实现要求：
- 先取 `FieldError`
- 如果 `FieldError` 为空，再回退到 `getAllErrors().get(0)` 或等价逻辑
- 保证 class-level validation 不会打成 `500`

- [x] **Step 4: 跑定向测试验证**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-admin,yr-framework -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=GlobalExceptionHandlerContractTest,SsoClientControllerContractTest,SysUserControllerPasswordResetContractTest,SysUserControllerChangeStatusContractTest test
```

Expected:
- PASS

Progress (2026-03-30):
- compatibility checkpoint（兼容性检查点）已完成：
  - `sam-sso-fe/src/utils/request.js` 当前并没有业务上“强依赖 validation 永远返回 HTTP 200”的硬编码契约
  - 但如果 backend 直接切到真实 `HTTP 400`，frontend 的 transport error 分支会把提示文案退化成通用 `"系统接口400异常"`
  - 因此本轮按用户确认走 FE/BE 联动收口：backend 修真实 transport semantics，frontend 同步补 `HTTP 400` 受控消息透传
- backend 已完成：
  - `GlobalExceptionHandler.validatedBindException(...)` 与 `validExceptionHandler(...)` 已改为返回 `ResponseEntity<AjaxResult>`，真实返回 `HTTP 400`
  - 新增 `resolveValidationMessage(...)`，先取 `FieldError`，为空时再回退到 `ObjectError` / `getAllErrors().get(0)`，保证 class-level validation 不会再打成 `500`
- frontend 已完成：
  - `sam-sso-fe/src/utils/request.js` 已新增 `extractBadRequestMessage(...)`，让 transport-level `HTTP 400` 优先展示 backend 返回的 `msg/message`
  - `sam-sso-fe/tests/review/startup/requestHttp400MessagePropagation.audit.test.js` 已新增并注册到 `tests/review/index.js` phase1 审计入口
- 已执行 backend 定向验证：
  - `mvn -pl yr-admin,yr-framework -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=GlobalExceptionHandlerContractTest,SsoClientControllerContractTest,SysUserControllerPasswordResetContractTest,SysUserControllerChangeStatusContractTest test`
- backend 当前结果：`BUILD SUCCESS`
- 已执行 frontend 定向/聚合验证：
  - `node tests/review/startup/requestHttp400MessagePropagation.audit.test.js`
  - `node tests/review/index.js phase1`
- frontend 当前结果：
  - 新增 `HTTP 400` 消息透传审计通过
  - `phase1` 聚合共通过 `21` 个脚本

### Task 3: Close After-Commit Recorder Failure Fallback

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SsoIdentityDistributionServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/support/SsoDistributionDispatchResultRecorder.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/support/SsoSyncTaskFailureRecorder.java`
- Optionally Create: `yr-system/src/main/java/com/yr/system/service/support/SsoDistributionDispatchFailureMarker.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SsoIdentityDistributionRecorderFailureContractTest.java`
- Optionally Modify: `yr-system/src/test/java/com/yr/system/service/impl/SsoIdentityDistributionServiceAfterCommitContractTest.java`

- [x] **Step 1: 明确 recorder failure 的目标语义**

目标态建议：
- MQ 已成功发送但状态回写失败时，任务不能只停留在隐式日志里
- 至少要留下可查询的 `FAILED` / `PARTIAL_SUCCESS` + “dispatch state reconciliation failed” 摘要

- [x] **Step 2: 为 recorder failure 增加独立 fallback path（兜底路径）**

实现要求：
- 不要继续只 `LOGGER.error(...); throw exception`
- 在可用情况下用新的 `REQUIRES_NEW` 兜底记录器回写任务级失败摘要
- 明确区分“MQ send failed”和“MQ sent but state reconciliation failed”

- [x] **Step 3: 保持正常 after-commit 成功路径和 rollback 保护不回退**

实现要求：
- 成功/部分失败路径继续走现有最终态回库
- rollback 前不发 MQ 的保护必须保持不变

- [x] **Step 4: 跑定向测试验证**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn -pl yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoIdentityDistributionServiceAfterCommitContractTest,SsoSyncTaskDistributionStateClosureTest,SsoIdentityDistributionRecorderFailureContractTest,SsoSyncTaskServiceImplFailFastTest test
```

Expected:
- PASS

Progress (2026-03-30):
- 已明确 recorder failure（结果回写失败）的目标语义：
  - 当 MQ 已发送完成但最终状态回写失败时，任务侧不能继续停留在纯 `SUCCESS`
  - 至少要留下可查询的 `PARTIAL_SUCCESS` / `FAILED` 与 `dispatch state reconciliation failed` 摘要
- 已完成生产改动：
  - `SsoIdentityDistributionServiceImpl` 已注入 `SsoSyncTaskFailureRecorder`，在 `recordDispatchResult(...)` 失败时不再只 `throw exception`
  - `SsoSyncTaskFailureRecorder` 已新增 `recordDispatchReconciliationFailure(...)`，用 `REQUIRES_NEW` 事务回写任务级 fallback marker（兜底标记）
  - fallback 语义已区分：
    - MQ send 失败：仍按既有 item/task 最终态闭环
    - MQ send 成功但 state reconciliation 失败：任务改落 `PARTIAL_SUCCESS` / `FAILED`，摘要追加 `dispatch state reconciliation failed`
- fresh review（重新复核）后追加收口：
  - 已补齐 recorder failure fallback（结果回写失败兜底）只更新 task、不更新 item 的残余缺口
  - `SsoSyncTaskFailureRecorder.recordDispatchReconciliationFailure(...)` 现在会在写入 task-level fallback marker（任务级兜底标记）后，继续按 after-commit 的真实发送结果补回 item 最终态，避免数据库长期保留第一次落库的 `PENDING` 快照
  - `SsoSyncTaskItemServiceImpl.updateDispatchResult(...)` 已切到 `REQUIRES_NEW`，保证 fallback item 回写失败时不会反向吞掉 task-level reconciliation marker
- rollback 保护保持不变：
  - `shouldNotSendMqBeforeOuterTransactionCommits` 仍通过，证明没有把 MQ 发送提前回事务内
- 已执行定向验证：
  - `mvn -pl yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoIdentityDistributionServiceAfterCommitContractTest,SsoSyncTaskDistributionStateClosureTest,SsoIdentityDistributionRecorderFailureContractTest,SsoSyncTaskServiceImplFailFastTest test`
- 当前结果：`BUILD SUCCESS`

### Task 4: Refresh Canonical Docs And Handoff

**Files:**
- Modify: `README.md`
- Modify: `docs/review_plans/2026-03-30-sso-backend-tail-closure-remediation-plan.md`
- Modify: `docs/review_plans/2026-03-30-sso-backend-second-pass-gap-remediation-plan.md`

- [x] **Step 1: 把 2026-03-30 tail plan 标为 first-pass closure snapshot（第一轮收口快照）**

实现要求：
- 顶部加 superseded note（后续计划接管提示）
- 不再把它作为新执行入口

- [x] **Step 2: 把 README 指向 second-pass gap plan**

实现要求：
- 仓库级入口直接指向本计划
- 避免新对话继续按照“从 Task 1 开始执行旧 plan”进入

- [x] **Step 3: 全量回归后更新本计划执行状态与 handoff**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
mvn test
```

Expected:
- `BUILD SUCCESS`

Progress (2026-03-30):
- `docs/review_plans/2026-03-30-sso-backend-tail-closure-remediation-plan.md` 已维持为 first-pass closure snapshot（第一轮收口快照）口径，本轮确认无需再额外改写执行入口
- `README.md` 已改为把 second-pass plan 标记为“最新收口快照”，并明确说明 `Task 1-4` 已完成；后续若有新问题，应基于当前结果先做 fresh review，而不是重新从 `Task 1` 重跑
- 已执行全量回归：
  - `mvn test`
- 当前结果：`BUILD SUCCESS`
- 当前全量测试基线：
  - `Tests run: 60, Failures: 0, Errors: 0, Skipped: 5`
- 当前计划状态：
  - `Task 1` 已完成：second-pass 红灯冻结完成
  - `Task 2` 已完成：validation transport semantics 与 class-level fallback 闭环完成
  - `Task 3` 已完成：after-commit recorder failure fallback 闭环完成
  - `Task 4` 已完成：canonical docs、handoff 与全量回归完成

## Recommended Execution Order

1. `Task 1: Freeze Second-Pass Regressions`
2. `Task 2: Align Validation Transport Semantics And Error Fallback`
3. `Task 3: Close After-Commit Recorder Failure Fallback`
4. `Task 4: Refresh Canonical Docs And Handoff`

原因：

- validation transport semantics（校验传输层语义）影响面更广，但实现相对局部，先锁红灯能避免后续“以为修了其实只改了 body code”。
- after-commit recorder failure 是第二层一致性问题，先定义明确 fallback contract（兜底契约）再实现，能避免继续出现“日志有了但状态没了”的半闭环。
- 文档入口必须最后刷新，避免执行中多次切换 canonical plan。

## Next-Turn Handoff

如需新开对话继续处理后续问题，直接提供以下四项：

1. `cwd`

```text
/Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
```

2. latest closure snapshot（最新收口快照）

```text
docs/review_plans/2026-03-30-sso-backend-second-pass-gap-remediation-plan.md
```

3. previous closure snapshot（上一轮收口快照）

```text
docs/review_plans/2026-03-30-sso-backend-tail-closure-remediation-plan.md
```

4. 执行要求

```text
2026-03-30 second-pass gap plan 已完成；若发现新缺口，请先基于当前 README、latest closure snapshot 与 previous closure snapshot 做 fresh review，再决定是否需要新一轮 remediation plan，不要重新从 Task 1 开始执行。
```

## Suggested Prompt

```text
请以 /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be 为 cwd，先基于 README、docs/review_plans/2026-03-30-sso-backend-second-pass-gap-remediation-plan.md 和 docs/review_plans/2026-03-30-sso-backend-tail-closure-remediation-plan.md 做 fresh review；如果确认又出现新的 backend gap，再整理新的 remediation plan 并给出执行入口，不要重复从已完成的 Task 1 开始执行。
```
