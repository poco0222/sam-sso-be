# Yr-System Best Practice Follow-up Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 根据 2026-03-24 的 review 结论，修复 `yr-system` 当前仍存在的 `null-safety`（空值安全）、`transaction consistency`（事务一致性）、导入语义和 `layering`（分层）耦合问题，并把对应回归测试固化下来。

**Architecture:** 先收口 `P1 correctness`（正确性）问题：角色菜单/数据权限数组空值、防止配置批量删除出现“半成功”、统一用户导入的部分成功语义。再处理 `P2 layering`（分层）问题：让 `warmup runner`（预热运行器）依赖 service interface（服务接口）而不是实现类。最后处理 `P3 query hygiene`（查询卫生）：接收组对象树只按选中 ID 拉用户，避免全量用户查询。所有行为变更都先补 failing test（失败测试），再做最小实现，最后用 targeted test（定向测试）和 `mvn -pl yr-system test` 收口。

**Tech Stack:** `JDK 17`, `Spring Boot 2.7.18`, `JUnit 5`, `Mockito`, `Spring Test`, `MyBatis-Plus`, `Maven`

---

## Scope Guardrails

- 本计划只覆盖当前 review 已确认的问题，不顺手扩散到未验证的 repository-wide 重构。
- 除非任务明确要求，否则不修改 `yr-admin` controller（控制器）层的 API 形态；优先在 `yr-system` service/domain/test 范围内解决问题。
- 所有行为改动先补或改 failing test，再做最小实现；不要先写实现再回头补测试。
- 批量写操作优先采用 “先校验、后写入” 的两阶段结构；仅靠 `@Transactional` 不足以表达调用语义时，要把前置校验显式写进 service。
- 只有 `P1` 到 `P3` 主线全部通过后，才考虑清理 `LoginParams` / `SysGroupObjectDTO` / `MessageBody` 一类低优先级卫生项。

## Priority Summary

- `P1` 正确性：`SysRoleServiceImpl` 空值防御、`SysConfigServiceImpl` 批量删除原子性、`SysUserImportService` 部分成功语义
- `P2` 分层治理：`YrSystemWarmupRunner` 改为依赖接口并把 warmup contract（预热契约）上移到 service interface
- `P3` 查询卫生：`SysReceiveGroupService` 不再全量拉用户构建对象树
- `Backlog` 代码卫生：`LoginParams` 注释/字段语义、`SysGroupObjectDTO` 注释残留、`MessageBody`/`insertSysMessageBody` 死代码审计

## File Map

- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysRoleServiceImpl.java`
  责任：把 `menuIds` / `deptIds` 归一化为空集合，避免角色写操作因空数组直接 `NullPointerException`。
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysRoleServiceImplNullSafetyTest.java`
  责任：锁定角色菜单/数据权限数组缺失时的行为。
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysConfigServiceImpl.java`
  责任：把配置批量删除改成“先校验、后删除”，并补事务边界。
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysConfigServiceImplDeleteConfigTransactionTest.java`
  责任：锁定配置批量删除不会出现“前半段已删、后半段报错”的半成功状态。
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysUserImportService.java`
  责任：保留逐条导入策略，但把“部分成功”变成显式可返回结果，而不是统一抛业务异常。
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SysUserImportServiceExceptionHandlingTest.java`
  责任：把现有“业务异常继续处理”测试对齐到新的结果契约。
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysUserImportServiceResultContractTest.java`
  责任：锁定全部成功、部分成功、全部失败三种导入结果。
- Modify: `yr-system/src/main/java/com/yr/system/service/ISysCodeRuleService.java`
  责任：暴露编码规则缓存预热 contract。
- Modify: `yr-system/src/main/java/com/yr/system/service/ISysConfigService.java`
  责任：暴露参数缓存预热 contract。
- Modify: `yr-system/src/main/java/com/yr/system/service/ISysDictTypeService.java`
  责任：暴露字典缓存预热 contract。
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysCodeRuleServiceImpl.java`
  责任：实现并显式 `@Override` 预热 contract。
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysConfigServiceImpl.java`
  责任：实现并显式 `@Override` 预热 contract。
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysDictTypeServiceImpl.java`
  责任：实现并显式 `@Override` 预热 contract。
- Modify: `yr-system/src/main/java/com/yr/system/config/YrSystemWarmupRunner.java`
  责任：改成依赖 `ISys*Service` 接口。
- Modify: `yr-system/src/test/java/com/yr/system/config/YrSystemWarmupRunnerTest.java`
  责任：用 interface mock（接口 mock）锁定 runner 行为。
- Modify: `yr-system/src/test/java/com/yr/system/architecture/YrSystemPhase2ArchitectureTest.java`
  责任：补 runner 依赖接口的 architecture contract（架构契约）。
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysReceiveGroupService.java`
  责任：对象树构建改为按选中用户 ID 定向查询，避免 `selectUserList(new SysUser())` 全量扫描。
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysUserQueryService.java`
  责任：暴露按用户 ID 列表批量查询的公共 helper（辅助查询）方法。
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SysReceiveGroupServiceObjectTreeTest.java`
  责任：锁定对象树只按选中用户 ID 查询用户。
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SysUserQueryServiceTest.java`
  责任：覆盖新增的按 ID 批量查询 helper。

### Task 1: 修复角色菜单与数据权限的空值安全

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysRoleServiceImpl.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysRoleServiceImplNullSafetyTest.java`
- Test: `yr-system/src/test/java/com/yr/system/service/impl/SysRoleServiceImplDeleteRoleCleanupTest.java`

- [x] **Step 1: 先写失败测试**

在 `SysRoleServiceImplNullSafetyTest` 中新增两个用例：

```java
@Test
void shouldTreatMissingMenuIdsAsEmptyWhenInsertingRoleMenu() { }

@Test
void shouldTreatMissingDeptIdsAsEmptyWhenAuthorizingDataScope() { }
```

断言点：
- `role.setMenuIds(null)` 时，`insertRoleMenu(role)` 不抛异常，并且不会调用 `batchRoleMenu`
- `role.setDeptIds(null)` 时，`authDataScope(role)` 不抛异常，并且不会调用 `batchRoleDept`

- [x] **Step 2: 跑测试确认现在失败**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=SysRoleServiceImplNullSafetyTest test
```

Expected: FAIL，出现 `NullPointerException`，堆栈指向 `insertRoleMenu` 或 `insertRoleDept` 的 `for (...)` 遍历。

- [x] **Step 3: 做最小实现**

实现要求：
- 在 `insertRoleMenu` / `insertRoleDept` 内先把 `role.getMenuIds()` / `role.getDeptIds()` 归一化成 `Collections.emptyList()`
- 保持当前“空集合时返回 `rows = 1`”的兼容语义，避免影响调用方
- 不额外改 `roleMapper` / `roleMenuMapper` / `roleDeptMapper` 签名

- [x] **Step 4: 跑定向验证**

Run:

```bash
mvn -pl yr-system -Dtest=SysRoleServiceImplNullSafetyTest,SysRoleServiceImplDeleteRoleCleanupTest test
```

Expected: PASS。

- [x] **Step 5: 提交本任务**

```bash
git add yr-system/src/main/java/com/yr/system/service/impl/SysRoleServiceImpl.java yr-system/src/test/java/com/yr/system/service/impl/SysRoleServiceImplNullSafetyTest.java
git commit -m "fix: 补齐角色菜单与数据权限空值防御"
```

### Task 2: 让配置批量删除具备原子性与清晰语义

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysConfigServiceImpl.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysConfigServiceImplDeleteConfigTransactionTest.java`

- [x] **Step 1: 先写失败测试**

在 `SysConfigServiceImplDeleteConfigTransactionTest` 中补两个用例：

```java
@Test
void shouldRejectWholeBatchBeforeDeletingWhenBuiltInConfigExists() { }

@Test
void shouldEvictCacheOnlyAfterConfigDeleteSucceeds() { }
```

断言点：
- 当 `configIds = [1, 2]` 且 `2` 对应内置参数时，不应对 `1` 发生任何删除
- 只有数据库删除执行后，才调用 `redisCache.deleteObject(...)`

- [x] **Step 2: 跑测试确认现在失败**

Run:

```bash
mvn -pl yr-system -Dtest=SysConfigServiceImplDeleteConfigTransactionTest test
```

Expected: FAIL，能观察到当前实现在遇到内置参数前已经执行了前面配置的删除。

- [x] **Step 3: 做最小实现**

实现要求：
- 给 `deleteConfigByIds` 增加 `@Transactional(rollbackFor = Exception.class)`
- 改成“两阶段处理”：
  - 第一阶段：把待删配置全部查出并校验，任何一条缺失/内置都立即失败
  - 第二阶段：只有全部通过后，才循环执行 `configMapper.deleteConfigById` 和缓存删除
- 缺失配置要抛明确的 `CustomException`，不要让 `config.getConfigType()` 触发空指针

- [x] **Step 4: 跑定向验证**

Run:

```bash
mvn -pl yr-system -Dtest=SysConfigServiceImplDeleteConfigTransactionTest test
```

Expected: PASS。

- [x] **Step 5: 提交本任务**

```bash
git add yr-system/src/main/java/com/yr/system/service/impl/SysConfigServiceImpl.java yr-system/src/test/java/com/yr/system/service/impl/SysConfigServiceImplDeleteConfigTransactionTest.java
git commit -m "fix: 收紧配置批量删除事务与前置校验"
```

### Task 3: 统一用户导入的部分成功结果契约

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysUserImportService.java`
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SysUserImportServiceExceptionHandlingTest.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysUserImportServiceResultContractTest.java`

- [x] **Step 1: 先写失败测试**

在 `SysUserImportServiceResultContractTest` 中新增三个用例：

```java
@Test
void shouldReturnSuccessSummaryWhenAllUsersImportSuccessfully() { }

@Test
void shouldReturnMixedSummaryWhenSomeUsersSucceedAndSomeFail() { }

@Test
void shouldThrowBusinessExceptionWhenAllUsersFailValidation() { }
```

同时把 `SysUserImportServiceExceptionHandlingTest.shouldCollectBusinessFailureAndContinueProcessingLaterUsers` 改成断言“继续处理后返回 mixed summary（混合结果摘要）”，不再期望统一抛 `CustomException`。

- [x] **Step 2: 跑测试确认现在失败**

Run:

```bash
mvn -pl yr-system -Dtest=SysUserImportServiceExceptionHandlingTest,SysUserImportServiceResultContractTest test
```

Expected: FAIL，当前实现会在 `failureNum > 0` 时统一抛 `CustomException`。

- [x] **Step 3: 做最小实现**

实现要求：
- 保留“逐条事务处理”的导入策略
- 保留 `RuntimeException` 立即中止并原样抛出的语义
- 调整结果契约：
  - `successNum > 0 && failureNum == 0`：返回成功摘要
  - `successNum > 0 && failureNum > 0`：返回混合摘要，明确列出成功和失败条目
  - `successNum == 0 && failureNum > 0`：抛 `CustomException`
- 不新增 controller 层 DTO；先保持方法签名 `String importUser(...)` 不变

- [x] **Step 4: 跑定向验证**

Run:

```bash
mvn -pl yr-system -Dtest=SysUserImportServiceExceptionHandlingTest,SysUserImportServiceResultContractTest,SysUserWriteServiceSpringWiringIntegrationTest test
```

Expected: PASS。

- [x] **Step 5: 提交本任务**

```bash
git add yr-system/src/main/java/com/yr/system/service/impl/SysUserImportService.java yr-system/src/test/java/com/yr/system/service/impl/SysUserImportServiceExceptionHandlingTest.java yr-system/src/test/java/com/yr/system/service/impl/SysUserImportServiceResultContractTest.java
git commit -m "fix: 明确用户导入部分成功结果契约"
```

### Task 4: 让 Warmup Runner 依赖接口而不是实现类

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/service/ISysCodeRuleService.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/ISysConfigService.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/ISysDictTypeService.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysCodeRuleServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysConfigServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysDictTypeServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/config/YrSystemWarmupRunner.java`
- Modify: `yr-system/src/test/java/com/yr/system/config/YrSystemWarmupRunnerTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/architecture/YrSystemPhase2ArchitectureTest.java`

- [x] **Step 1: 先写失败测试/契约**

改动测试：

```java
// YrSystemWarmupRunnerTest
ISysCodeRuleService codeRuleService = mock(ISysCodeRuleService.class);
ISysConfigService configService = mock(ISysConfigService.class);
ISysDictTypeService dictTypeService = mock(ISysDictTypeService.class);
```

并在 `YrSystemPhase2ArchitectureTest` 增加一个断言：

```java
assertThat(Arrays.stream(YrSystemWarmupRunner.class.getDeclaredFields()).map(Field::getType).toList())
        .contains(ISysCodeRuleService.class, ISysConfigService.class, ISysDictTypeService.class)
        .doesNotContain(SysCodeRuleServiceImpl.class, SysConfigServiceImpl.class, SysDictTypeServiceImpl.class);
```

- [x] **Step 2: 跑测试确认现在失败**

Run:

```bash
mvn -pl yr-system -Dtest=YrSystemWarmupRunnerTest,YrSystemPhase2ArchitectureTest test
```

Expected: FAIL，当前 runner 构造器仍依赖具体实现类，且 interface 上没有对应 warmup 方法。

- [x] **Step 3: 做最小实现**

实现要求：
- 在三个 `ISys*Service` 接口上新增 warmup 方法：
  - `int warmUpCodeRuleCache();`
  - `int warmUpConfigCache();`
  - `int warmUpDictCache();`
- 在对应实现类上显式 `@Override`
- `YrSystemWarmupRunner` 构造器与字段改成接口类型
- 不改变现有日志格式与“单个 warmup 失败不阻断启动”的语义

- [x] **Step 4: 跑定向验证**

Run:

```bash
mvn -pl yr-system -Dtest=YrSystemWarmupRunnerTest,YrSystemPhase2ArchitectureTest test
```

Expected: PASS。

- [x] **Step 5: 提交本任务**

```bash
git add yr-system/src/main/java/com/yr/system/service/ISysCodeRuleService.java yr-system/src/main/java/com/yr/system/service/ISysConfigService.java yr-system/src/main/java/com/yr/system/service/ISysDictTypeService.java yr-system/src/main/java/com/yr/system/service/impl/SysCodeRuleServiceImpl.java yr-system/src/main/java/com/yr/system/service/impl/SysConfigServiceImpl.java yr-system/src/main/java/com/yr/system/service/impl/SysDictTypeServiceImpl.java yr-system/src/main/java/com/yr/system/config/YrSystemWarmupRunner.java yr-system/src/test/java/com/yr/system/config/YrSystemWarmupRunnerTest.java yr-system/src/test/java/com/yr/system/architecture/YrSystemPhase2ArchitectureTest.java
git commit -m "refactor: 让系统预热runner依赖服务接口"
```

### Task 5: 接收组对象树改为定向查询用户

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysReceiveGroupService.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysUserQueryService.java`
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SysReceiveGroupServiceObjectTreeTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SysUserQueryServiceTest.java`

- [x] **Step 1: 先写失败测试**

在 `SysReceiveGroupServiceObjectTreeTest` 中增加一个用例：

```java
@Test
void shouldQueryOnlyCheckedUsersWhenBuildingUserGroupObjectTree() { }
```

断言点：
- 只把 `groupObjectList` 中的 `reObjectId` 传给新的批量查询 helper
- 当 `checkedIds` 为空时，返回空 `treeList`
- 不再依赖 `selectUserList(new SysUser())` 全量拉用户

在 `SysUserQueryServiceTest` 中增加一个用例：

```java
@Test
void shouldReturnUsersByIdsInInputOrderAfterDeduplication() { }
```

- [x] **Step 2: 跑测试确认现在失败**

Run:

```bash
mvn -pl yr-system -Dtest=SysReceiveGroupServiceObjectTreeTest,SysUserQueryServiceTest test
```

Expected: FAIL，当前 `SysReceiveGroupService` 仍调用 `userService.selectUserList(new SysUser())`。

- [x] **Step 3: 做最小实现**

实现要求：
- 在 `SysUserQueryService` 增加公共方法，例如：

```java
public List<SysUser> listUsersByIds(List<Long> userIds) { }
```

- `SysReceiveGroupService` 改为依赖 `SysUserQueryService`
- 对 `checkedIds` 去重、过滤 `null` 后再查询
- 保持昵称优先、用户名回退的 label 规则不变

- [x] **Step 4: 跑定向验证**

Run:

```bash
mvn -pl yr-system -Dtest=SysReceiveGroupServiceTest,SysReceiveGroupServiceObjectTreeTest,SysUserQueryServiceTest test
```

Expected: PASS。

- [x] **Step 5: 提交本任务**

```bash
git add yr-system/src/main/java/com/yr/system/service/impl/SysReceiveGroupService.java yr-system/src/main/java/com/yr/system/service/impl/SysUserQueryService.java yr-system/src/test/java/com/yr/system/service/impl/SysReceiveGroupServiceObjectTreeTest.java yr-system/src/test/java/com/yr/system/service/impl/SysUserQueryServiceTest.java
git commit -m "refactor: 按选中用户定向构建接收组对象树"
```

### Task 6: 运行模块级回归并收口文档

**Files:**
- Modify: `docs/superpowers/plans/2026-03-24-yr-system-best-practice-followup-remediation.md`

- [x] **Step 1: 跑完整模块测试**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system test
```

Expected: `BUILD SUCCESS`，并且没有新增失败测试。

- [x] **Step 2: 更新 plan 复选框与验证记录**

把已经完成的步骤打勾，并在 plan 末尾补一段执行结果摘要：

```markdown
## Execution Notes
- Verified with: `mvn -pl yr-system test`
- Result: `BUILD SUCCESS`
```

- [x] **Step 3: 提交收口**

```bash
git add docs/superpowers/plans/2026-03-24-yr-system-best-practice-followup-remediation.md
git commit -m "docs: 更新yr-system后续整改计划执行状态"
```

## Deferred Hygiene Backlog

这些项本轮只做记录，不并入主执行路径；只有在 `Task 1` 到 `Task 6` 全部完成后，再决定是否继续：

- `LoginParams.java`
  风险：字段注释错位、`validateCode()` 永远返回 `true`，但它跨 `yr-admin` / `yr-framework` 使用，贸然改语义容易影响登录链路。
- `SysGroupObjectDTO.java`
  风险：保留了大段注释掉的旧实现，属于低风险卫生问题，可在单独 cleanup（清理）任务里删除。
- `MessageBody.java`
  风险：当前 repo-wide grep（全仓搜索）看不到使用方，适合后续做 dead code（死代码）移除，但应先补使用审计再删。
- `ISysMessageBodyService#insertSysMessageBody` / `SysMessageBodyService`
  风险：当前没有仓库内调用，但涉及公开 service contract，最好在单独任务中做“无引用审计 + 删除或废弃”。
- `RouterVo.java`
  风险：可以后续评估是否改成更现代的不可变 DTO，但这不是当前 review 里会引发 bug 的问题。

## Execution Notes

- Verified with: `mvn -pl yr-system test`
- Result: `BUILD SUCCESS`
