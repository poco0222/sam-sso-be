# Yr-System Official Best Practice Convergence Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `yr-system` 及其在 `yr-admin` 的直接暴露入口收敛到 `Spring Boot 2.7.18 + Java 17 + MyBatis-Plus` 的官方最佳实践基线，先清除 correctness（正确性）/security（安全性）缺陷，再完成 layering（分层）、persistence hygiene（持久层卫生）、type contract（类型契约）和 regression gate（回归门禁）收口。

**Architecture:** 采用 `P1 correctness and security -> P2 boundary and persistence hygiene -> P3 architectural convergence` 三阶段策略。先修所有可达且会导致 `500`、数据泄露、查询结果错误的链路，再治理 service/controller 边界、SQL 查询写法、时间类型和工具类契约，最后用 contract tests（契约测试）、controller permission tests（控制层权限测试）和 Maven 验证把“最佳实践”固化为仓库门禁。

**Tech Stack:** `JDK 17`, `Spring Boot 2.7.18`, `Spring Security`, `Spring MVC`, `JUnit 5`, `Mockito`, `AssertJ`, `MyBatis`, `MyBatis-Plus`, `Maven`

---

## Plan Positioning

- 本计划整合了当前两轮 `yr-system` review 结论，覆盖 `yr-system` 模块本体，以及其在 `yr-admin` 的直接暴露边界。
- 本计划不是对既有 `docs/superpowers/plans/2026-03-24-yr-system-best-practice-remediation.md` 的简单重复；它新增了 controller 权限边界、SQL correctness（SQL 正确性）、query semantics（查询语义）、service layering（服务分层）和 type contract（类型契约）收敛任务。
- 本计划默认只做最小必要修改，不顺手扩散到未验证的 repository-wide 重构。

## Scope Guardrails

- 所有实现都必须先补 failing test（失败测试）或 failing contract check（失败契约检查），再做最小实现。
- 所有新增或修改的 Java 代码注释必须满足仓库规则，作者统一使用 `PopoY`。
- 每个任务优先保持 API shape（接口形态）稳定；只有在分层或安全边界必须调整时，才允许改动 service/controller 契约。
- 对已有 SQL 语义的调整，必须在计划中明确“行为保持不变”还是“行为显式修正”，避免把 bugfix（缺陷修复）和语义重定义混在一起。
- 最终验收必须同时覆盖 `yr-system` 和相关 `yr-admin` 入口；只跑 `yr-system test` 不足以证明整改完成。

## Current Baseline

- 已确认 `mvn -v` 在显式切换到 `JDK 17` 后正常。
- 已确认 `mvn -pl yr-system test` 当前为 `BUILD SUCCESS`。
- 当前自动化不能覆盖多条真实缺陷，尤其是：
  - 可达 controller 的权限缺口
  - mapper XML 的 SQL 语法错误
  - 低频查询接口的条件拼装错误
  - controller 到 service 的暴露边界问题

## Findings Summary

### P1: Must Fix First

- `SysDeptMapper.xml` 共享 SQL 片段存在尾逗号，导致部门列表/详情/唯一性校验链路存在真实 SQL syntax risk（SQL 语法风险）。
- `SysUserMapper.xml.queryModeUserGroupInformationCollection` 的 `WHERE` 条件拼装错误，带过滤条件时会直接生成非法 SQL。
- `SysReceiveGroupMapper.xml.pageList` 与 `SysMsgTemplateMapper.xml.pageList` 的 `LIKE '%' #{...} '%'` 写法是非法 SQL。
- `SysMessageBodyService` 与 `SysMessageBodyReceiverMapper.xml` 对 `msg_from` 的语义不一致，导致发送人信息 join 错配。
- `SysUserServiceImpl.selectAllocatedList/selectUnallocatedList` 的数据权限约束不完整，至少 `allocatedList` 已经绕过 DataScope。
- `yr-admin` 中多个系统 controller 暴露了无权限保护的高风险接口，尤其是文件上传/下载/删除和部门驱动的用户枚举。

### P2: Strongly Recommended

- `ISysFileService` 直接暴露 `MultipartFile` / `HttpServletResponse`，service layer（服务层）与 Web transport（Web 传输层）耦合。
- `SysFileMapper.pageByCondition` 展示的是原始文件名，筛选的却是存储文件名，查询语义不一致。
- 多个列表查询对索引列使用 `date_format(...)`，不符合 index-friendly query（索引友好查询）最佳实践。
- `selectDeptRoleTreeList` 使用非确定性的 `GROUP BY` 写法，在严格 SQL mode（SQL 模式）下可能失败，在宽松模式下结果不稳定。
- 多个可达 mapper 仍保留 `select *`，容易引入 schema drift（表结构漂移）和隐式映射风险。

### P3: Architectural Convergence

- `MatcherUtils.parse` 缺少前置条件保护，`Map` 缺键时会抛 `NullPointerException`。
- `Reflections` 对 `IllegalAccessException` 只记日志不 fail-fast（快速失败）。
- `SysMessageBody` 仍使用 Hutool `DateTime`，未对齐 `java.time` 类型体系。
- `SysReceiveGroupVo` / `SysMsgTemplateVo` 的分页泛型声明不整洁，VO 契约未完全收口。

## File Map

- Modify: `yr-system/src/main/resources/mapper/system/SysDeptMapper.xml`
  责任：修复共享 SQL 片段语法错误、收敛部门树/角色树查询语义。
- Modify: `yr-system/src/main/resources/mapper/system/SysUserMapper.xml`
  责任：修复用户分组查询 SQL、恢复/对齐数据权限相关查询契约、优化时间范围查询。
- Modify: `yr-system/src/main/resources/mapper/system/SysReceiveGroupMapper.xml`
  责任：修复 `LIKE` 语法错误。
- Modify: `yr-system/src/main/resources/mapper/system/SysMsgTemplateMapper.xml`
  责任：修复 `LIKE` 语法错误。
- Modify: `yr-system/src/main/resources/mapper/system/SysMessageBodyReceiverMapper.xml`
  责任：统一发送人 join 语义。
- Modify: `yr-system/src/main/resources/mapper/system/SysFileMapper.xml`
  责任：统一文件名展示与搜索语义。
- Modify: `yr-system/src/main/resources/mapper/system/SysConfigMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysDictTypeMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysLogininforMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysOperLogMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysRoleMapper.xml`
  责任：把基于 `date_format(...)` 的时间过滤收敛为 index-friendly query。
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysUserServiceImpl.java`
  责任：恢复或替代 DataScope 契约，统一已分配/未分配用户查询行为。
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SysUserServiceImplDataScopeContractTest.java`
  责任：锁定角色用户分配查询必须带数据范围约束，防止 `allocatedList/unallocatedList` 再次绕过 DataScope。
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SysUserServiceImplDelegationTest.java`
  责任：锁定 `SysUserServiceImpl` 对 `SysUserQueryService` 和 mapper 的委托边界，避免安全修复把查询职责重新揉回大 service。
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysDeptServiceImpl.java`
  责任：收口父节点 fail-fast 与部门树相关行为。
- Modify: `yr-system/src/main/java/com/yr/system/service/ISysFileService.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysFileServiceImpl.java`
  责任：移除 Web-specific types（Web 专用类型）对 service 的污染。
- Create: `yr-system/src/main/java/com/yr/system/domain/model/SysFileUploadCommand.java`
- Create: `yr-system/src/main/java/com/yr/system/domain/model/SysFileDownloadPayload.java`
  责任：承接文件 service 的 domain contract（领域契约）。
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysMessageBodyService.java`
  责任：统一 `msg_from` 的真实语义。
- Modify: `yr-system/src/main/java/com/yr/system/utils/MatcherUtils.java`
- Modify: `yr-system/src/main/java/com/yr/system/utils/Reflections.java`
  责任：收口 util contract（工具类契约）与异常语义。
- Modify: `yr-system/src/main/java/com/yr/system/domain/entity/SysMessageBody.java`
  责任：对齐 `java.time`。
- Modify: `yr-system/src/main/java/com/yr/system/domain/vo/SysReceiveGroupVo.java`
- Modify: `yr-system/src/main/java/com/yr/system/domain/vo/SysMsgTemplateVo.java`
  责任：整理分页 VO 契约。
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysFileController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysUserController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysDeptController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysRoleController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysReceiveGroupController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysMsgTemplateController.java`
  责任：收口 controller 权限边界，并承接文件下载/上传的 HTTP 细节。
- Modify: `yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlContractTest.java`
  责任：扩展 SQL contract（SQL 契约）覆盖面。
- Create: `yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlSyntaxContractTest.java`
  责任：锁定尾逗号、`LIKE '%' #{...} '%'`、`WHERE AND` 等高风险拼装模式。
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/TreeParentValidationContractTest.java`
  责任：锁定部门/岗位树父子链路在 SQL hygiene 调整后仍保持 fail-fast 与祖级更新语义稳定。
- Create: `yr-admin/src/test/java/com/yr/web/controller/system/SysSystemControllerPermissionContractTest.java`
  责任：锁定指定 controller 入口必须带 `@PreAuthorize`。
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysFileServiceContractTest.java`
  责任：锁定文件 service 不再暴露 Servlet/Web 类型。
- Modify: `yr-system/src/test/java/com/yr/system/utils/MatcherUtilsTest.java`
  责任：补 util null-safety 回归。
- Create: `yr-system/src/test/java/com/yr/system/domain/vo/SysMessageBodySerializationTest.java`
  责任：锁定消息实体时间序列化契约。
- Modify: `yr-system/src/test/java/com/yr/system/domain/vo/MetaVoSerializationTest.java`
  责任：锁定分页/VO 整理后现有元信息 JSON 契约不回退。

## Decision Checkpoints

- `msg_from` 的 canonical contract（规范契约）必须先明确：
  - 方案 A：继续存 `username`
  - 方案 B：改存 `userId`
  - 计划默认优先采用方案 A 的最小修复，因为当前 `insertSysMessageBody` 已明确写入 `username`
- 文件下载 service contract（文件下载服务契约）默认采用“service 返回 payload，controller 写 response”的分层方式，不继续把 `HttpServletResponse` 留在 `yr-system`
- controller 权限整改默认采用“显式 allowlist（显式白名单）”策略；如果某个接口必须无权限开放，需要在测试白名单里注明原因

### Task 1: 修复 P1 SQL correctness 与查询语义问题

**Files:**
- Modify: `yr-system/src/main/resources/mapper/system/SysDeptMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysUserMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysReceiveGroupMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysMsgTemplateMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysMessageBodyReceiverMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysFileMapper.xml`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysMessageBodyService.java`
- Modify: `yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlContractTest.java`
- Create: `yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlSyntaxContractTest.java`

- [ ] **Step 1: 先补失败的 SQL contract tests**

新增或扩展以下断言：

```java
assertMapperDoesNotContainIgnoringCase("SysDeptMapper.xml", ", from");
assertMapperDoesNotContainIgnoringCase("SysReceiveGroupMapper.xml", "like '%' #{");
assertMapperDoesNotContainIgnoringCase("SysMsgTemplateMapper.xml", "like '%' #{");
assertMapperDoesNotContainIgnoringCase("SysUserMapper.xml", "where and");
```

并为 `msg_from` 语义、文件名筛选字段建立源代码级断言。

- [ ] **Step 2: 跑定向测试确认当前失败**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=YrSystemSqlContractTest,YrSystemSqlSyntaxContractTest test
```

Expected: FAIL，指出尾逗号、非法 `LIKE` 和 `WHERE` 拼装错误。

- [ ] **Step 3: 做最小实现**

最小实现要求：
- `selectDeptVo` 去掉尾逗号。
- `SysReceiveGroupMapper.xml` 与 `SysMsgTemplateMapper.xml` 统一改为 `LIKE concat('%', #{...}, '%')`。
- `queryModeUserGroupInformationCollection` 改成真正合法的 `<where>` 结构。
- 统一 `msg_from` 的存储/关联语义，默认让 join 对齐 `username`。
- `SysFileMapper.pageByCondition` 的文件名筛选字段改为与展示字段一致。

- [ ] **Step 4: 跑定向验证**

Run:

```bash
mvn -pl yr-system -Dtest=YrSystemSqlContractTest,YrSystemSqlSyntaxContractTest,SysReceiveGroupServiceTest,SysMsgTemplateServiceTest,SysMessageBodyServiceTest test
```

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add yr-system/src/main/resources/mapper/system/SysDeptMapper.xml \
        yr-system/src/main/resources/mapper/system/SysUserMapper.xml \
        yr-system/src/main/resources/mapper/system/SysReceiveGroupMapper.xml \
        yr-system/src/main/resources/mapper/system/SysMsgTemplateMapper.xml \
        yr-system/src/main/resources/mapper/system/SysMessageBodyReceiverMapper.xml \
        yr-system/src/main/resources/mapper/system/SysFileMapper.xml \
        yr-system/src/main/java/com/yr/system/service/impl/SysMessageBodyService.java \
        yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlContractTest.java \
        yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlSyntaxContractTest.java
git commit -m "fix: 收敛yr-system高风险SQL与查询语义"
```

### Task 2: 收口 DataScope 与 controller 权限边界

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysUserServiceImpl.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysFileController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysUserController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysDeptController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysRoleController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysReceiveGroupController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysMsgTemplateController.java`
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SysUserServiceImplDataScopeContractTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SysUserServiceImplDelegationTest.java`
- Create: `yr-admin/src/test/java/com/yr/web/controller/system/SysSystemControllerPermissionContractTest.java`

- [ ] **Step 1: 先补失败的 permission contract tests**

在 `SysSystemControllerPermissionContractTest` 中为以下入口建立断言：
- `SysFileController` 的 `page/upload/download/delete`
- `SysUserController` 的 `mode-group-objecct/page`、`selectSysUserById`、`batchSelectUserByDeptId`、`listV2ForAF`
- `SysDeptController` 的 `list`、`treeselect`、`deptRoletreeselect`、`getAllSysDeptForOptions`、`getChildrenDept`
- `SysRoleController` 的 `listAll`、`allocatedList`、`unallocatedList`
- `SysReceiveGroupController` / `SysMsgTemplateController` 的写接口和分页接口

断言这些方法要么有 `@PreAuthorize`，要么在 allowlist 里有明确注释说明。

同时扩展以下 service tests：
- `SysUserServiceImplDataScopeContractTest`
  断言 `selectAllocatedList` 与 `selectUnallocatedList` 不再裸调用 mapper，而是显式带数据范围约束或被受保护的查询路径承接。
- `SysUserServiceImplDelegationTest`
  断言安全修复后，`SysUserServiceImpl` 仍把复杂查询委托给 `SysUserQueryService`，不把 controller 过滤逻辑重新塞回 service。

- [ ] **Step 2: 跑权限契约测试确认当前失败**

Run:

```bash
mvn -pl yr-admin -am -Dtest=SysSystemControllerPermissionContractTest test
```

Expected: FAIL，指出多个 controller 方法缺权限注解。

- [ ] **Step 3: 做最小实现**

实现要求：
- 为 controller 补齐 `@PreAuthorize`。
- 恢复或替代 `SysUserServiceImpl.selectAllocatedList/selectUnallocatedList` 的 DataScope 契约。
- 若 `allocatedList` 不能直接恢复老注解，则必须在 mapper/service 层显式补组织或数据范围条件。

- [ ] **Step 4: 跑定向验证**

Run:

```bash
mvn -pl yr-admin -am -Dtest=SysSystemControllerPermissionContractTest test
mvn -pl yr-system -Dtest=SysUserServiceImplDataScopeContractTest,SysUserServiceImplDelegationTest test
```

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add yr-system/src/main/java/com/yr/system/service/impl/SysUserServiceImpl.java \
        yr-admin/src/main/java/com/yr/web/controller/system/SysFileController.java \
        yr-admin/src/main/java/com/yr/web/controller/system/SysUserController.java \
        yr-admin/src/main/java/com/yr/web/controller/system/SysDeptController.java \
        yr-admin/src/main/java/com/yr/web/controller/system/SysRoleController.java \
        yr-admin/src/main/java/com/yr/web/controller/system/SysReceiveGroupController.java \
        yr-admin/src/main/java/com/yr/web/controller/system/SysMsgTemplateController.java \
        yr-admin/src/test/java/com/yr/web/controller/system/SysSystemControllerPermissionContractTest.java
git commit -m "fix: 收口system控制器权限边界与数据权限契约"
```

### Task 3: 解耦文件 service 与 Web transport

**Files:**
- Create: `yr-system/src/main/java/com/yr/system/domain/model/SysFileUploadCommand.java`
- Create: `yr-system/src/main/java/com/yr/system/domain/model/SysFileDownloadPayload.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/ISysFileService.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysFileServiceImpl.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysFileController.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysFileServiceContractTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SysFileServiceImplTest.java`

- [ ] **Step 1: 先补失败的 service contract test**

在 `SysFileServiceContractTest` 中断言：
- `ISysFileService` 不再暴露 `MultipartFile`
- `ISysFileService` 不再暴露 `HttpServletResponse`
- 文件下载改为返回 `SysFileDownloadPayload`

- [ ] **Step 2: 跑测试确认当前失败**

Run:

```bash
mvn -pl yr-system -Dtest=SysFileServiceContractTest,SysFileServiceImplTest test
```

Expected: FAIL，当前 service contract 仍依赖 Web 类型。

- [ ] **Step 3: 做最小实现**

实现要求：
- controller 负责把 `MultipartFile` 解析为 `SysFileUploadCommand`
- service 返回 transport-agnostic（与传输层无关）的 `SysFileDownloadPayload`
- controller 负责把 payload 写入 `HttpServletResponse`
- 不改变文件上传/下载成功语义

- [ ] **Step 4: 跑定向验证**

Run:

```bash
mvn -pl yr-system -Dtest=SysFileServiceContractTest,SysFileServiceImplTest test
mvn -pl yr-admin -am -Dtest=SysSystemControllerPermissionContractTest test
```

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add yr-system/src/main/java/com/yr/system/domain/model/SysFileUploadCommand.java \
        yr-system/src/main/java/com/yr/system/domain/model/SysFileDownloadPayload.java \
        yr-system/src/main/java/com/yr/system/service/ISysFileService.java \
        yr-system/src/main/java/com/yr/system/service/impl/SysFileServiceImpl.java \
        yr-admin/src/main/java/com/yr/web/controller/system/SysFileController.java \
        yr-system/src/test/java/com/yr/system/service/impl/SysFileServiceContractTest.java \
        yr-system/src/test/java/com/yr/system/service/impl/SysFileServiceImplTest.java
git commit -m "refactor: 解耦文件服务与Web传输层"
```

### Task 4: 收敛 SQL performance 与 deterministic query 写法

**Files:**
- Modify: `yr-system/src/main/resources/mapper/system/SysUserMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysRoleMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysConfigMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysDictTypeMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysLogininforMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysOperLogMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysDeptMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysOrgMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysPostMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysUserDeptMapper.xml`
- Modify: `yr-system/src/main/resources/mapper/system/SysUserPostMapper.xml`
- Modify: `yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlContractTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/TreeParentValidationContractTest.java`

- [ ] **Step 1: 先补失败的 persistence contract tests**

扩展 `YrSystemSqlContractTest`：
- 可达列表查询不得对时间列使用 `date_format(column, ...)`
- 可达 mapper 不得保留裸 `select *`
- `selectDeptRoleTreeList` 不得保留非确定性 `GROUP BY`

同时扩展 `TreeParentValidationContractTest`：
- 锁定部门/岗位树在父节点缺失、祖级更新和子树重排场景下仍维持既有 fail-fast 语义
- 确保 SQL hygiene 调整不会把树形行为回退成 silent wrong result（静默错误结果）

- [ ] **Step 2: 跑测试确认当前失败**

Run:

```bash
mvn -pl yr-system -Dtest=YrSystemSqlContractTest test
```

Expected: FAIL，指出 `date_format`、`select *`、`GROUP BY` 风险点。

- [ ] **Step 3: 做最小实现**

实现要求：
- 把时间过滤统一改成范围谓词。
- 为可达查询显式列出列清单。
- 把 `selectDeptRoleTreeList` 改成 deterministic query（确定性查询），必要时拆成子查询或显式聚合。

- [ ] **Step 4: 跑定向验证**

Run:

```bash
mvn -pl yr-system -Dtest=YrSystemSqlContractTest,SysDeptServiceImplSafetyTest,TreeParentValidationContractTest test
```

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add yr-system/src/main/resources/mapper/system/SysUserMapper.xml \
        yr-system/src/main/resources/mapper/system/SysRoleMapper.xml \
        yr-system/src/main/resources/mapper/system/SysConfigMapper.xml \
        yr-system/src/main/resources/mapper/system/SysDictTypeMapper.xml \
        yr-system/src/main/resources/mapper/system/SysLogininforMapper.xml \
        yr-system/src/main/resources/mapper/system/SysOperLogMapper.xml \
        yr-system/src/main/resources/mapper/system/SysDeptMapper.xml \
        yr-system/src/main/resources/mapper/system/SysOrgMapper.xml \
        yr-system/src/main/resources/mapper/system/SysPostMapper.xml \
        yr-system/src/main/resources/mapper/system/SysUserDeptMapper.xml \
        yr-system/src/main/resources/mapper/system/SysUserPostMapper.xml \
        yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlContractTest.java
git commit -m "refactor: 收敛可达SQL的性能与确定性写法"
```

### Task 5: 收口 service/util 契约与 fail-fast 行为

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysDeptServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/utils/MatcherUtils.java`
- Modify: `yr-system/src/main/java/com/yr/system/utils/Reflections.java`
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SysDeptServiceImplSafetyTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/utils/MatcherUtilsTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/utils/ReflectionsTest.java`

- [ ] **Step 1: 先补失败测试**

新增或扩展断言：
- `insertDept` 在父节点缺失时抛 `CustomException`
- `MatcherUtils.parse` 在 `Map` 缺键或 `kvs == null` 时给出明确异常或约定降级结果
- `Reflections` 在反射失败时不吞异常

- [ ] **Step 2: 跑测试确认当前失败**

Run:

```bash
mvn -pl yr-system -Dtest=SysDeptServiceImplSafetyTest,MatcherUtilsTest,ReflectionsTest test
```

Expected: FAIL。

- [ ] **Step 3: 做最小实现**

实现要求：
- service 和 util 明确前置条件与异常语义
- 不允许继续把 `IllegalAccessException` 静默记录后返回不完整结果
- `MatcherUtils` 需定义统一缺失占位符策略，默认推荐 fail-fast

- [ ] **Step 4: 跑定向验证**

Run:

```bash
mvn -pl yr-system -Dtest=SysDeptServiceImplSafetyTest,MatcherUtilsTest,ReflectionsTest test
```

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add yr-system/src/main/java/com/yr/system/service/impl/SysDeptServiceImpl.java \
        yr-system/src/main/java/com/yr/system/utils/MatcherUtils.java \
        yr-system/src/main/java/com/yr/system/utils/Reflections.java \
        yr-system/src/test/java/com/yr/system/service/impl/SysDeptServiceImplSafetyTest.java \
        yr-system/src/test/java/com/yr/system/utils/MatcherUtilsTest.java \
        yr-system/src/test/java/com/yr/system/utils/ReflectionsTest.java
git commit -m "fix: 收口服务与工具类契约的快速失败语义"
```

### Task 6: 对齐时间类型与消息/分页模型契约

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/domain/entity/SysMessageBody.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysMessageBodyService.java`
- Modify: `yr-system/src/main/resources/mapper/system/SysMessageBodyMapper.xml`
- Modify: `yr-system/src/main/java/com/yr/system/domain/vo/SysReceiveGroupVo.java`
- Modify: `yr-system/src/main/java/com/yr/system/domain/vo/SysMsgTemplateVo.java`
- Create: `yr-system/src/test/java/com/yr/system/domain/vo/SysMessageBodySerializationTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/domain/vo/MetaVoSerializationTest.java`

- [ ] **Step 1: 先补失败测试**

新增断言：
- `SysMessageBody` 序列化后仍输出稳定时间格式
- `PageVo` 子类泛型声明与自身语义一致
- `MetaVoSerializationTest` 继续验证现有 `vo` JSON 结构不因分页泛型整理而变化

- [ ] **Step 2: 跑测试确认当前失败**

Run:

```bash
mvn -pl yr-system -Dtest=SysMessageBodySerializationTest,MetaVoSerializationTest test
```

Expected: FAIL 或缺少断言覆盖；至少 `MetaVoSerializationTest` 需要先补充对分页/元信息输出的断言。

- [ ] **Step 3: 做最小实现**

实现要求：
- 把 `SysMessageBody` 的时间类型收敛到 `java.time`
- 保持 JSON 格式兼容
- 修正 `SysReceiveGroupVo` / `SysMsgTemplateVo` 的泛型定义

- [ ] **Step 4: 跑定向验证**

Run:

```bash
mvn -pl yr-system -Dtest=SysMessageBodySerializationTest,MetaVoSerializationTest,SysMessageBodyServiceTest test
```

Expected: PASS。

- [ ] **Step 5: 提交本任务**

```bash
git add yr-system/src/main/java/com/yr/system/domain/entity/SysMessageBody.java \
        yr-system/src/main/java/com/yr/system/service/impl/SysMessageBodyService.java \
        yr-system/src/main/resources/mapper/system/SysMessageBodyMapper.xml \
        yr-system/src/main/java/com/yr/system/domain/vo/SysReceiveGroupVo.java \
        yr-system/src/main/java/com/yr/system/domain/vo/SysMsgTemplateVo.java \
        yr-system/src/test/java/com/yr/system/domain/vo/SysMessageBodySerializationTest.java
git commit -m "refactor: 对齐消息时间类型与分页VO契约"
```

### Task 7: 建立最终 regression gate 并收口文档状态

**Files:**
- Modify: `docs/superpowers/plans/2026-03-24-yr-system-official-best-practice-convergence.md`
- Modify: `yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/web/controller/system/SysSystemControllerPermissionContractTest.java`

- [ ] **Step 1: 为计划文档补执行状态区**

在本计划底部增加：
- 当前阶段
- 已完成任务
- 剩余风险
- 实际验证结果

- [ ] **Step 2: 跑最终验收**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system test
mvn -pl yr-admin -am -Dtest=SysSystemControllerPermissionContractTest test
git diff --check
```

Expected:
- `yr-system`: `BUILD SUCCESS`
- `yr-admin` permission contract: PASS
- `git diff --check`: 无空白错误

- [ ] **Step 3: 请求一次最终 review**

要求 review 覆盖：
- P1 SQL correctness
- 权限边界
- 文件 service 分层
- 时间类型迁移
- SQL hygiene 回归门禁

- [ ] **Step 4: 根据 review 做最后修补**

只接受有证据的 review finding（评审问题），不顺手做额外重构。

- [ ] **Step 5: 提交收尾**

```bash
git add docs/superpowers/plans/2026-03-24-yr-system-official-best-practice-convergence.md \
        yr-system/src/test/java/com/yr/system/architecture/YrSystemSqlContractTest.java \
        yr-admin/src/test/java/com/yr/web/controller/system/SysSystemControllerPermissionContractTest.java
git commit -m "docs: 收口yr-system官方最佳实践整改计划与回归门禁"
```

## Final Acceptance Checklist

- 所有可达的 P1 SQL 缺陷都已修复并有自动化守护。
- `yr-admin` 暴露给系统管理域的高风险接口已补齐权限边界，或被明确列入 allowlist。
- `ISysFileService` 不再暴露 `MultipartFile` / `HttpServletResponse`。
- `DataScope` 在角色用户分配查询上恢复或被等价替代。
- 可达列表查询不再对索引列使用 `date_format(column, ...)`。
- 可达 mapper 的高风险 `select *` 已被显式列清单替换。
- 消息链路的发送人语义一致。
- `SysMessageBody` 时间类型已对齐 `java.time`。
- `yr-system` 与 `yr-admin` 的关键契约测试全部通过。

## Recommended Execution Order

1. Task 1
2. Task 2
3. Task 3
4. Task 5
5. Task 4
6. Task 6
7. Task 7

依赖说明：
- Task 1 和 Task 2 是阻塞项，必须先做。
- Task 3 必须在 Task 2 之后执行，因为它会改 `SysFileController`。
- Task 4 放在 Task 5 之后，是为了先稳定 service/util 契约，再批量改 SQL。
- Task 6 需要等待 Task 1 的消息语义先定下来。

## Execution Status

### 当前阶段

- `Task 7` 的最终验收已完成，当前处于收尾记录阶段。

### 已完成任务

- `Task 1`：已完成并提交 `0f428ad fix: 收敛yr-system高风险SQL与查询语义`
- `Task 2`：已完成并提交 `99f34b7 fix: 收口system控制器权限边界与数据权限契约`
- `Task 3`：已完成并提交 `6e5f41f refactor: 解耦文件服务与Web传输层`
- `Task 5`：已完成并提交 `82cb782 fix: 收口服务与工具类契约的快速失败语义`
- `Task 4`：已完成并提交 `c8e3933 refactor: 收敛可达SQL的性能与确定性写法`
- `Task 6`：已完成并提交 `ef3c7ab refactor: 对齐消息时间类型与分页VO契约`

### 剩余风险

- 当前 worktree 仅剩一处为通过最终 gate 而修正的注释文本变更，尚未提交。
- 本计划文件目前位于主工作目录且为 `untracked` 状态，不属于当前 worktree 分支提交物；若需要纳入版本控制，需要单独决定文档落位策略。
- 最终 review 已请求，但在本轮交互窗口内未返回有证据的 finding；当前没有已知 blocker。

### 实际验证结果

- `Task 4` 定向验证通过：
  `mvn -pl yr-system -Dtest=YrSystemSqlContractTest,SysDeptServiceImplSafetyTest,TreeParentValidationContractTest test`
- `Task 6` 定向验证通过：
  `mvn -pl yr-system -Dtest=SysMessageBodySerializationTest,MetaVoSerializationTest,SysMessageBodyServiceTest test`
- 最终 `yr-system` 验收通过：
  `mvn -pl yr-system test`
  结果：`Tests run: 169, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`
- 最终 `yr-admin` permission contract 验收通过：
  `mvn -pl yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysSystemControllerPermissionContractTest test`
  结果：`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`
- `git diff --check` 最终为干净状态，无空白错误。
