# yr-system 最佳实践遗漏项修复 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修复 `yr-system` 本轮复查确认的最佳实践遗漏项，并补齐对应回归测试。

**Architecture:** 先用单元测试锁定 Mapper 绑定、批量删除关联清理、事务边界与 WebSocket 接口收敛行为，再做最小代码修复。保持现有模块边界不变，只修正错误实现和误导性接口。

**Tech Stack:** JDK 17, Spring Boot 2.7.18, Spring Transaction, MyBatis XML, JUnit 5, Mockito, Maven

---

### Task 1: 锁定失败测试

**Files:**
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysUserServiceImplDeleteUserCleanupTest.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysRoleServiceImplDeleteRoleCleanupTest.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysDictTypeServiceImplTransactionTest.java`
- Create: `yr-system/src/test/java/com/yr/system/architecture/YrSystemAsyncContractTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/architecture/YrSystemPhase2ArchitectureTest.java`

**Step 1: 写失败测试**

- 验证批量删用户会调用 `deleteUserRole` 与 `deleteUserPost`
- 验证批量删角色会调用 `deleteRoleDept`
- 验证 `deleteDictTypeByIds` 具备事务注解
- 验证 `IWebSocketService` / `WebSocketServerImpl` 不再暴露 `async()`
- 验证 `SysDeptMapper.xml` 插入部门时使用 `#{accounteUnit}`

**Step 2: 运行失败测试**

Run: `export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl yr-system -Dtest=SysUserServiceImplDeleteUserCleanupTest,SysRoleServiceImplDeleteRoleCleanupTest,SysDictTypeServiceImplTransactionTest,YrSystemAsyncContractTest,YrSystemPhase2ArchitectureTest test`

Expected: 至少有本轮新增断言失败。

### Task 2: 实现最小修复

**Files:**
- Modify: `yr-system/src/main/resources/mapper/system/SysDeptMapper.xml`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysUserServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysRoleServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysDictTypeServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/component/message/IWebSocketService.java`
- Modify: `yr-system/src/main/java/com/yr/system/component/message/impl/WebSocketServerImpl.java`

**Step 1: 修复 XML 与服务层**

- 把 `accounteUnit` 改为 `#{accounteUnit}`
- 恢复批量删用户和批量删角色的关联清理
- 为字典类型批量删除补 `@Transactional`
- 删除 WebSocket `async()` 定义与实现

**Step 2: 运行定向测试**

Run: 同 Task 1 命令

Expected: 新增测试全部通过。

### Task 3: 全量验证

**Files:**
- Verify only

**Step 1: 运行 yr-system 全量测试**

Run: `export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl yr-system test`

Expected: `BUILD SUCCESS`
