# Yr-System Best Practice Remediation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 `yr-system` 在正确性、依赖治理、消息链路、持久层实现和代码卫生上达到可持续维护的 `best practice`（最佳实践）基线，并补齐对应验证。

**Architecture:** 先修 `P1` 正确性与边界安全问题，再收敛依赖和消息链路，最后处理持久层卫生、性能债务和仓库清理。优先复用现有 `architecture/contract tests`（架构/契约测试）风格锁定回归，再用面向服务的单元测试和 Maven 验证命令补齐行为证据。

**Tech Stack:** `JDK 17`, `Spring Boot 2.7.18`, `Spring Test`, `JUnit 5`, `Mockito`, `MyBatis/MyBatis-Plus`, `Maven`, `WebSocket`, `Redis`

---

## Scope Guardrails

- 当前工作树已经是 `dirty worktree`（有未提交改动），执行前先确认是否继续在现有改动上推进，或者切到独立 `worktree`。
- 这个计划只覆盖 `yr-system` 及其直接相关的 `yr-common` / `yr-framework` / 根 `pom.xml` 边界，不扩散到 `yr-admin` 控制器层。
- 任何行为改动必须先补 failing test（失败测试）或 failing contract check（失败契约检查），再做最小实现。
- 每完成一个任务就跑该任务的 targeted verification（定向验证），不要把所有风险堆到最后。

## Priority Summary

- `P1` 正确性与边界安全：菜单 SQL、字典空指针、service 空值防御
- `P2` 依赖与链路治理：`fastjson` 迁移、显式依赖声明、消息分发模型
- `P3` 持久层与卫生：`SELECT *`、`find_in_set`、deprecated API、dead source、构建文档

## File Map

- Modify: `yr-system/src/main/resources/mapper/system/SysMenuMapper.xml`
  责任：修复平台过滤 SQL precedence，避免错误放大结果集。
- Create: `yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlContractTest.java`
  责任：锁定高风险 XML SQL 约束，例如括号、禁止新增 `SELECT *`、关键 mapper 约束。
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysDictTypeServiceImpl.java`
  责任：补字典类型更新路径的存在性校验与错误建模。
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysDictDataServiceImpl.java`
  责任：补字典数据删除路径的空值安全与缓存刷新边界。
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysDictTypeServiceImplSafetyTest.java`
  责任：覆盖 `updateDictType` 的空值与缓存行为。
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysDictDataServiceImplSafetyTest.java`
  责任：覆盖 `deleteDictDataByIds` 的空值与缓存行为。
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysMenuServiceImpl.java`
  责任：统一菜单相关 service 的空值防御和错误语义。
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysDeptServiceImpl.java`
  责任：统一部门相关 service 的空值防御和错误语义。
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysMenuServiceImplSafetyTest.java`
  责任：覆盖角色或菜单缺失时的失败行为。
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysDeptServiceImplSafetyTest.java`
  责任：覆盖角色缺失时的失败行为。
- Modify: `pom.xml`
  责任：收敛 `fastjson` 版本策略，必要时移除根依赖定义。
- Modify: `yr-common/pom.xml`
  责任：移除或收敛 `fastjson` 的共享依赖暴露。
- Modify: `yr-system/pom.xml`
  责任：为 `yr-system` 显式声明直接使用的 `mybatis-plus-boot-starter` 依赖。
- Modify: `yr-system/src/main/java/com/yr/system/component/message/AbstractMessageListener.java`
  责任：移除 `fastjson`，统一消息响应序列化入口。
- Modify: `yr-system/src/main/java/com/yr/system/component/message/impl/WebMessageServiceImpl.java`
  责任：移除 `fastjson`，改为 `Jackson` 或统一序列化适配器。
- Modify: `yr-system/src/main/java/com/yr/system/component/message/impl/WebSocketServerImpl.java`
  责任：替换 JSON 序列化方式，并为后续异步发送模型调整做准备。
- Modify: `yr-system/src/test/java/com/yr/system/component/message/impl/WebSocketServerImplTest.java`
  责任：锁定消息发送、错误记录与序列化后的行为。
- Modify: `yr-system/src/test/java/com/yr/system/component/message/impl/WebMessageServiceImplTest.java`
  责任：锁定模板消息、通知消息和发送调用行为。
- Modify: `yr-system/src/main/java/com/yr/system/component/message/impl/AsyncWebMessageDispatchService.java`
  责任：引入更合理的异步发送模型或最小限度的发送隔离。
- Create: `yr-system/src/test/java/com/yr/system/component/message/impl/AsyncWebMessageDispatchServiceThreadingTest.java`
  责任：锁定异步 dispatch（分发）的线程池和异常行为。
- Modify: `yr-system/src/main/resources/mapper/system/SysUserMapper.xml`
  责任：逐步替换关键 `SELECT *`，保留稳定列映射。
- Modify: `yr-system/src/main/resources/mapper/system/SysDeptMapper.xml`
  责任：逐步替换关键 `SELECT *` 并标记树查询后续优化点。
- Modify: `yr-system/src/main/resources/mapper/system/SysRoleMapper.xml`
  责任：逐步替换关键 `SELECT *`。
- Modify: `yr-system/src/main/resources/mapper/system/SysDutyMapper.xml`
  责任：后续替换关键 `SELECT *`。
- Modify: `yr-system/src/main/resources/mapper/system/SysRankMapper.xml`
  责任：后续替换关键 `SELECT *`。
- Modify: `yr-system/src/main/resources/mapper/system/SysUserMapper.xml`
  责任：定位并逐步替换 `find_in_set` 祖级查询。
- Modify: `yr-system/src/main/resources/mapper/system/SysDeptMapper.xml`
  责任：定位并逐步替换 `find_in_set` 祖级查询。
- Modify: `yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlContractTest.java`
  责任：锁定 `find_in_set` 的整改边界与剩余允许点。
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysDutyServiceImpl.java`
  责任：消除 deprecated API，用一致的返回约定替换历史实现。
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysRankServiceImpl.java`
  责任：消除 deprecated API。
- Modify: `yr-system/src/main/java/com/yr/system/domain/TreeSelect.java`
  责任：删除或迁移 dead source。
- Modify: `yr-system/src/main/java/com/yr/system/domain/entity/SysAttachCategory.java`
  责任：删除注释掉的死文件或恢复为真实实现。
- Modify: `yr-system/src/main/java/com/yr/system/domain/entity/SysAttachCategory1.java`
  责任：如果保留附件目录实体，恢复一致命名。
- Modify: `AGENTS.md`
  责任：修正无效 `JAVA_HOME` 示例，更新当前可用的 `JDK 17` 路径说明。

### Task 1: 修复菜单平台过滤 SQL 正确性

**Files:**
- Modify: `yr-system/src/main/resources/mapper/system/SysMenuMapper.xml`
- Create: `yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlContractTest.java`

- [x] **Step 1: 先写失败的 SQL 契约测试**

在 `YrSystemSqlContractTest` 中增加两个断言：

```java
assertSourceContains("SysMenuMapper.xml", "(m.platform='mgmt' or m.platform is null)");
assertSourceContains("SysMenuMapper.xml", "(platform='mgmt' or platform is null)");
```

- [x] **Step 2: 运行测试确认当前失败**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=YrSystemSqlContractTest test
```

Expected: FAIL，指出 `SysMenuMapper.xml` 仍然是未加括号的 `and ... or ...` 写法。

- [x] **Step 3: 最小修改 SQL**

把两个平台分支都改成显式括号形式，例如：

```xml
<if test="platform == 'mgmt'.toString() ">
    and (m.platform = 'mgmt' or m.platform is null)
</if>
```

- [x] **Step 4: 跑定向测试与消息相关回归**

Run:

```bash
mvn -pl yr-system -Dtest=YrSystemSqlContractTest,com.yr.system.component.message.impl.WebMessageServiceImplTest test
```

Expected: PASS。

- [x] **Step 5: 提交这个单独修复**

```bash
git add yr-system/src/main/resources/mapper/system/SysMenuMapper.xml yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlContractTest.java
git commit -m "fix: 修正菜单平台过滤SQL优先级"
```

### Task 2: 补齐字典服务的空值安全与缓存回归

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysDictTypeServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysDictDataServiceImpl.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysDictTypeServiceImplSafetyTest.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysDictDataServiceImplSafetyTest.java`

- [x] **Step 1: 先补两个失败测试**

```java
@Test
void shouldThrowBusinessExceptionWhenUpdatingMissingDictType() { }

@Test
void shouldHandleMissingDictDataBeforeRefreshingCache() { }
```

断言点：
- `updateDictType` 在 `oldDict == null` 时抛 `CustomException`
- `deleteDictDataByIds` 遇到缺失记录时，不得抛 `NullPointerException`

- [x] **Step 2: 跑测试确认现在失败**

Run:

```bash
mvn -pl yr-system -Dtest=SysDictTypeServiceImplSafetyTest,SysDictDataServiceImplSafetyTest test
```

Expected: FAIL，出现 `NullPointerException` 或异常类型不匹配。

- [x] **Step 3: 做最小实现**

实现要求：
- `SysDictTypeServiceImpl.updateDictType` 在查不到旧记录时抛明确业务异常
- `SysDictDataServiceImpl.deleteDictDataByIds` 在记录缺失时选择“跳过并继续”或“抛业务异常并中止”，但语义必须显式且有测试锁定
- 缓存刷新逻辑不得依赖空对象解引用

- [x] **Step 4: 跑字典相关全套验证**

Run:

```bash
mvn -pl yr-system -Dtest=SysDictTypeServiceImplSafetyTest,SysDictDataServiceImplSafetyTest,SysDictTypeServiceImplSelectDictDataTest,SysDictTypeServiceImplTransactionTest test
```

Expected: PASS。

- [x] **Step 5: 提交字典安全修复**

```bash
git add yr-system/src/main/java/com/yr/system/service/impl/SysDictTypeServiceImpl.java yr-system/src/main/java/com/yr/system/service/impl/SysDictDataServiceImpl.java yr-system/src/test/java/com/yr/system/service/impl/SysDictTypeServiceImplSafetyTest.java yr-system/src/test/java/com/yr/system/service/impl/SysDictDataServiceImplSafetyTest.java
git commit -m "fix: 补齐字典服务空值安全与缓存边界"
```

### Task 3: 统一 service 层空值防御和错误语义

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysMenuServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysDeptServiceImpl.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysMenuServiceImplSafetyTest.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysDeptServiceImplSafetyTest.java`

- [x] **Step 1: 补 failing tests**

```java
@Test
void shouldFailFastWhenRoleIsMissingForMenuSelection() { }

@Test
void shouldFailFastWhenMenuIsMissingById() { }

@Test
void shouldFailFastWhenRoleIsMissingForDeptSelection() { }
```

- [x] **Step 2: 跑测试确认失败**

Run:

```bash
mvn -pl yr-system -Dtest=SysMenuServiceImplSafetyTest,SysDeptServiceImplSafetyTest test
```

Expected: FAIL，当前实现会解引用 `null`。

- [x] **Step 3: 最小实现**

实现要求：
- 所有 `select...ById` 的关键依赖对象在解引用前都要做存在性校验
- 对外统一抛 `CustomException`，不要让 `NullPointerException` 泄漏到 controller
- 尽量不改既有成功路径的返回模型

- [x] **Step 4: 跑定向回归**

Run:

```bash
mvn -pl yr-system -Dtest=SysMenuServiceImplSafetyTest,SysDeptServiceImplSafetyTest,SysRoleServiceImplDeleteRoleCleanupTest test
```

Expected: PASS。

- [x] **Step 5: 提交 service 边界修复**

```bash
git add yr-system/src/main/java/com/yr/system/service/impl/SysMenuServiceImpl.java yr-system/src/main/java/com/yr/system/service/impl/SysDeptServiceImpl.java yr-system/src/test/java/com/yr/system/service/impl/SysMenuServiceImplSafetyTest.java yr-system/src/test/java/com/yr/system/service/impl/SysDeptServiceImplSafetyTest.java
git commit -m "fix: 收敛菜单与部门服务空值防御"
```

### Task 4: 把消息链路从 Fastjson 迁到 Jackson

**Files:**
- Modify: `pom.xml`
- Modify: `yr-common/pom.xml`
- Modify: `yr-system/src/main/java/com/yr/system/component/message/AbstractMessageListener.java`
- Modify: `yr-system/src/main/java/com/yr/system/component/message/impl/WebMessageServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/component/message/impl/WebSocketServerImpl.java`
- Modify: `yr-system/src/test/java/com/yr/system/component/message/impl/WebMessageServiceImplTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/component/message/impl/WebSocketServerImplTest.java`

- [x] **Step 1: 先补失败测试或收紧现有测试断言**

测试目标：
- `WebSocketServerImpl` 仍能把 `AjaxResult` 正常序列化为文本
- `WebMessageServiceImpl` 的日志和发送路径不依赖 `fastjson`
- 编译期扫描不能再出现 `com.alibaba.fastjson` import

可以在架构测试里加：

```java
assertRepoSourceDoesNotContain(Path.of("src/main/java/com/yr/system/component/message/impl/WebSocketServerImpl.java"), "com.alibaba.fastjson");
```

- [x] **Step 2: 跑测试确认失败**

Run:

```bash
mvn -pl yr-system -Dtest=WebSocketServerImplTest,WebMessageServiceImplTest,YrSystemPersistenceStyleContractTest test
```

Expected: FAIL，如果 contract test 先加上禁用 `fastjson` 的断言。

- [x] **Step 3: 最小实现**

实现要求：
- 优先复用 `Spring Boot` 默认 `Jackson`
- 选择一种统一序列化方式：注入 `ObjectMapper` 或抽一个 message serializer（消息序列化器）
- 删除 `yr-system` 消息链路里的 `fastjson` import
- 如果 `yr-common` 里不再需要 `fastjson`，同步移除共享依赖；如果短期保留，至少不再让 `yr-system` 使用它

- [x] **Step 4: 跑消息链路验证**

Run:

```bash
mvn -pl yr-system -Dtest=WebSocketServerImplTest,WebMessageServiceImplTest,DefaultMessageListenerImplTest,AsyncWebMessageDispatchServiceTest test
```

Expected: PASS。

- [x] **Step 5: 提交序列化迁移**

```bash
git add pom.xml yr-common/pom.xml yr-system/src/main/java/com/yr/system/component/message/AbstractMessageListener.java yr-system/src/main/java/com/yr/system/component/message/impl/WebMessageServiceImpl.java yr-system/src/main/java/com/yr/system/component/message/impl/WebSocketServerImpl.java yr-system/src/test/java/com/yr/system/component/message/impl/WebMessageServiceImplTest.java yr-system/src/test/java/com/yr/system/component/message/impl/WebSocketServerImplTest.java
git commit -m "refactor: 迁移站内消息序列化到Jackson"
```

### Task 5: 为 Yr-System 显式声明直接依赖并锁定构建边界

**Files:**
- Modify: `yr-system/pom.xml`
- Create: `yr-system/src/test/java/com/yr/system/architecture/YrSystemBuildContractTest.java`

- [x] **Step 1: 先补构建契约检查**

```java
assertProjectSourceContains(Path.of("pom.xml"), "<artifactId>mybatis-plus-boot-starter</artifactId>");
```

目标不是测试 `Maven` 本身，而是防止以后继续依赖 `yr-common` 的传递依赖。

- [x] **Step 2: 跑测试确认失败**

Run:

```bash
mvn -pl yr-system -Dtest=YrSystemBuildContractTest test
```

Expected: FAIL，当前 `yr-system/pom.xml` 不含该依赖。

- [x] **Step 3: 最小修改 POM**

在 `yr-system/pom.xml` 中显式加入：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
</dependency>
```

- [x] **Step 4: 跑构建验证**

Run:

```bash
mvn -pl yr-system -DskipTests compile
mvn -pl yr-system -q dependency:tree -Dincludes=com.baomidou:mybatis-plus-boot-starter
```

Expected: compile PASS，且依赖树里能看到 `yr-system` 直接声明的 starter。

- [x] **Step 5: 提交依赖边界修复**

```bash
git add yr-system/pom.xml yr-system/src/test/java/com/yr/system/architecture/YrSystemBuildContractTest.java
git commit -m "build: 显式声明yr-system的MyBatis-Plus依赖"
```

### Task 6: 收敛高风险 Mapper SQL 卫生

**Files:**
- Modify: `yr-system/src/main/resources/mapper/system/SysUserMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysDeptMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysRoleMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysDutyMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysRankMapper.xml`
- Modify: `yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlContractTest.java`

- [x] **Step 1: 先补 SQL contract tests**

第一轮只锁定高风险查询，不做全仓一次性清理：

```java
assertSourceDoesNotContain("SysUserMapper.xml", "select *");
assertSourceDoesNotContain("SysDeptMapper.xml", "select *");
assertSourceDoesNotContain("SysRoleMapper.xml", "SELECT *");
```

如果一次性收不完，就按方法级别写 allowlist（允许列表）并逐个消灭。

- [x] **Step 2: 跑测试确认失败**

Run:

```bash
mvn -pl yr-system -Dtest=YrSystemSqlContractTest test
```

Expected: FAIL，指出当前 `SELECT *` 仍存在。

- [x] **Step 3: 最小替换关键 SQL**

实现要求：
- 优先替换被 service 高频调用的查询
- 复用已有 `resultMap` 或抽公共 `<sql id="...">` 字段片段
- 本任务只解决 `SELECT *`，`find_in_set` 在下一任务单独处理，避免把列映射改造和树查询改造混成一个大提交

- [x] **Step 4: 跑 mapper 与 service 回归**

Run:

```bash
mvn -pl yr-system -Dtest=YrSystemSqlContractTest,SysUserQueryServiceTest,SysRoleServiceImplDeleteRoleCleanupTest,TreeParentValidationContractTest test
```

Expected: PASS。

- [x] **Step 5: 提交第一轮 SQL 卫生改造**

```bash
git add yr-system/src/main/resources/mapper/system/SysUserMapper.xml yr-system/src/main/resources/mapper/system/SysDeptMapper.xml yr-system/src/main/resources/mapper/system/SysRoleMapper.xml yr-system/src/main/resources/mapper/system/SysDutyMapper.xml yr-system/src/main/resources/mapper/system/SysRankMapper.xml yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlContractTest.java
git commit -m "refactor: 收敛高风险Mapper的SELECT星号查询"
```

### Task 7: 整改 Find-In-Set 树查询债务

**Files:**
- Modify: `yr-system/src/main/resources/mapper/system/SysUserMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysDeptMapper.xml`
- Modify: `yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlContractTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SysUserQueryServiceTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/TreeParentValidationContractTest.java`

- [x] **Step 1: 先补 inventory（清单）和 contract tests**

第一轮先把当前 `find_in_set` 的出现点锁定下来，避免新增：

```java
assertFindInSetOnlyAppearsIn("SysUserMapper.xml", List.of("selectUserList", "selectAllUserList"));
assertFindInSetOnlyAppearsIn("SysDeptMapper.xml", List.of("selectChildrenDeptById", "selectNormalChildrenDeptById"));
```

再为目标替代路径补行为测试，至少覆盖：
- 部门及子部门查用户
- 查询子部门
- 正常状态子部门计数

- [x] **Step 2: 跑测试确认失败**

Run:

```bash
mvn -pl yr-system -Dtest=YrSystemSqlContractTest,SysUserQueryServiceTest,TreeParentValidationContractTest test
```

Expected: FAIL，当前 `find_in_set` 使用点超出新 contract 或行为尚未锁定。

- [x] **Step 3: 先做最小可落地替代**

替代策略按风险从低到高选：
- 优先复用现有 `ancestors` 字段做 `like '%,id,%'` 风格的封装 SQL，避免 service 层拼字符串
- 如果对应 mapper 已经有 `selectDescendants` 之类的专用方法，优先让调用方切到专用方法
- 不在这一轮直接做闭包表、路径枚举表或递归 CTE（`recursive CTE`）的大型重构，除非数据库方言和现有 schema 已经准备好

完成标准：
- 核心查询路径上不再新增新的 `find_in_set`
- 至少移除高频路径中的 `find_in_set`
- 不能改坏现有树结构业务语义

- [x] **Step 4: 跑树查询回归**

Run:

```bash
mvn -pl yr-system -Dtest=YrSystemSqlContractTest,SysUserQueryServiceTest,TreeParentValidationContractTest,SysRegionServiceImplTest test
```

Expected: PASS。

- [x] **Step 5: 提交树查询整改**

```bash
git add yr-system/src/main/resources/mapper/system/SysUserMapper.xml yr-system/src/main/resources/mapper/system/SysDeptMapper.xml yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlContractTest.java yr-system/src/test/java/com/yr/system/service/impl/SysUserQueryServiceTest.java yr-system/src/test/java/com/yr/system/service/impl/TreeParentValidationContractTest.java
git commit -m "refactor: 收敛树查询中的find_in_set依赖"
```

### Task 8: 优化 WebSocket 异步发送模型

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/component/message/impl/AsyncWebMessageDispatchService.java`
- Modify: `yr-system/src/main/java/com/yr/system/component/message/impl/WebSocketServerImpl.java`
- Create: `yr-system/src/test/java/com/yr/system/component/message/impl/AsyncWebMessageDispatchServiceThreadingTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/component/message/impl/WebSocketServerImplTest.java`

- [x] **Step 1: 先补行为测试**

测试目标：
- 发送异常不会把整个批次打断
- dispatch 仍通过独立线程池执行
- 发送实现不再强绑定 `BasicRemote` 的阻塞语义，或者至少隔离到更小粒度

- [x] **Step 2: 跑测试确认失败或缺失**

Run:

```bash
mvn -pl yr-system -Dtest=AsyncWebMessageDispatchServiceThreadingTest,WebSocketServerImplTest test
```

Expected: 至少有一个测试失败，证明当前模型没有被锁定。

- [x] **Step 3: 最小实现**

实现候选，二选一：
- 优先：改用 `session.getAsyncRemote().sendText(...)`
- 保守：保留 `BasicRemote`，但把单用户发送和批量重试隔离到更细粒度，并增加线程池/异常语义测试

选择原则：先保行为稳定，再争取吞吐提升。

- [x] **Step 4: 跑消息回归**

Run:

```bash
mvn -pl yr-system -Dtest=AsyncWebMessageDispatchServiceThreadingTest,AsyncWebMessageDispatchServiceTest,WebSocketServerImplTest,WebMessageServiceImplTest test
```

Expected: PASS。

- [x] **Step 5: 提交异步发送优化**

```bash
git add yr-system/src/main/java/com/yr/system/component/message/impl/AsyncWebMessageDispatchService.java yr-system/src/main/java/com/yr/system/component/message/impl/WebSocketServerImpl.java yr-system/src/test/java/com/yr/system/component/message/impl/AsyncWebMessageDispatchServiceThreadingTest.java yr-system/src/test/java/com/yr/system/component/message/impl/WebSocketServerImplTest.java
git commit -m "refactor: 优化WebSocket异步发送模型"
```

### Task 9: 清理 deprecated API、dead source 与构建文档

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysDutyServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysRankServiceImpl.java`
- Modify or Delete: `yr-system/src/main/java/com/yr/system/domain/TreeSelect.java`
- Modify or Delete: `yr-system/src/main/java/com/yr/system/domain/entity/SysAttachCategory.java`
- Modify: `yr-system/src/main/java/com/yr/system/domain/entity/SysAttachCategory1.java`
- Modify: `AGENTS.md`

- [x] **Step 1: 先把 warning 和 dead source 变成可验证目标**

增加检查项：
- 编译时不再出现 `orderByAsc(R,R...) 已过时`
- `src/main/java` 中不再保留整文件注释的 Java 源码

可以把 dead source 检查收进 `YrSystemBuildContractTest` 或新的 hygiene contract test。

- [x] **Step 2: 跑失败检查**

Run:

```bash
mvn -pl yr-system -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true -Dmaven.compiler.compilerArgs=-Xlint:deprecation,-Xlint:unchecked compile
rg -n "^//package |^//import |^//public class " yr-system/src/main/java
```

Expected: 能看到 deprecated warning 和被整段注释的源文件。

- [x] **Step 3: 做最小清理**

实现要求：
- 用非 deprecated 的 `orderByAsc` 替代写法
- `deleteDuty` 这类返回值不一致的方法，调整到统一语义并补测试
- 清理或恢复 `TreeSelect.java` / `SysAttachCategory.java` / `SysAttachCategory1.java` 的命名与存活关系
- 更新 `AGENTS.md` 中失效的 `JAVA_HOME` 示例为当前真实可用路径

- [x] **Step 4: 跑最终总验证**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system test
mvn -pl yr-system -DskipTests -Dmaven.compiler.showWarnings=true -Dmaven.compiler.showDeprecation=true -Dmaven.compiler.compilerArgs=-Xlint:deprecation,-Xlint:unchecked compile
git diff --check
```

Expected:
- `BUILD SUCCESS`
- 不再出现本轮目标内的 deprecated warning
- `git diff --check` 无格式错误

- [x] **Step 5: 提交收尾清理**

```bash
git add AGENTS.md yr-system/src/main/java/com/yr/system/service/impl/SysDutyServiceImpl.java yr-system/src/main/java/com/yr/system/service/impl/SysRankServiceImpl.java yr-system/src/main/java/com/yr/system/domain/TreeSelect.java yr-system/src/main/java/com/yr/system/domain/entity/SysAttachCategory.java yr-system/src/main/java/com/yr/system/domain/entity/SysAttachCategory1.java
git commit -m "chore: 清理yr-system历史卫生与构建基线"
```

## Execution Notes

- 如果只做第一轮高收益整改，推荐先执行 `Task 1` 到 `Task 5`。
- `Task 6` 到 `Task 9` 适合作为第二轮技术债治理，不建议和第一轮正确性修复混在一个超大提交里。
- 每个任务完成后都要更新本计划中的复选框，避免跨任务丢状态。
- 若执行过程中发现 `find_in_set` 树模型需要整体重构，应单独拆新计划，不要在本计划内扩 scope。

## Verification Matrix

- 正确性：`YrSystemSqlContractTest`, `SysDictTypeServiceImplSafetyTest`, `SysDictDataServiceImplSafetyTest`, `SysMenuServiceImplSafetyTest`, `SysDeptServiceImplSafetyTest`
- 消息链路：`WebSocketServerImplTest`, `WebMessageServiceImplTest`, `DefaultMessageListenerImplTest`, `AsyncWebMessageDispatchServiceTest`
- 事务/回归：`SysDictTypeServiceImplTransactionTest`, `SysRoleServiceImplDeleteRoleCleanupTest`, `SysUserQueryServiceTest`, `TreeParentValidationContractTest`
- 构建：`mvn -pl yr-system test`, `mvn -pl yr-system -DskipTests compile`, `git diff --check`

Plan complete and saved to `docs/superpowers/plans/2026-03-24-yr-system-best-practice-remediation.md`. Ready to execute?
