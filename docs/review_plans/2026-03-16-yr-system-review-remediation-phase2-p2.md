# YR System Review Phase 2 P2 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修复默认部门切换的事务遗漏，并为职级、职务、岗位树补齐父节点存在性校验，让服务层面对非法输入时稳定返回业务异常。

**Architecture:** Phase 2 采用“对齐已修复模式 + 最小输入校验”的方式推进。默认部门切换直接参照 `SysUserOrgServiceImpl.setDefaultUserOrg` 的修复口径补事务与结果校验；树形链路则不做结构性重构，只在入口处补显式判空与异常文案，并用契约测试替换当前隐式 NPE。

**Tech Stack:** JDK 17, Spring Boot 2.7.18, Spring Transaction, MyBatis-Plus, JUnit 5, Mockito, AssertJ

---

## 当前执行状态

- 更新时间：2026-03-16
- 当前阶段：待开始
- 当前任务：为 `SysUserDeptServiceImpl` 和树形新增链路制定修复步骤
- 已确认问题：
  - `SysUserDeptServiceImpl.setDefaultUserDept` 仍缺少 `@Transactional`
  - 旧默认部门清理和新默认部门设置都没有检查更新结果
  - `SysRankServiceImpl.addRank`
  - `SysDutyServiceImpl.addDuty`
  - `SysPostServiceImpl.insertPost`
  - 上述链路都可能在父节点缺失时直接抛出 NPE

### Task 1: 让默认部门切换具备原子性

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysUserDeptServiceImpl.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysUserDeptServiceImplTransactionTest.java`

**Step 1: 写失败的事务回归测试**

参考已有的 `SysUserOrgServiceImplTransactionTest`，锁定两个契约：

```java
@Test
void shouldDeclareTransactionalOnSetDefaultUserDept() throws NoSuchMethodException {
    Method method = SysUserDeptServiceImpl.class.getMethod("setDefaultUserDept", Long.class, Long.class);

    assertThat(method.getAnnotation(Transactional.class)).isNotNull();
}

@Test
void shouldRollbackWhenUpdatingNewDefaultDeptFails() {
    ProbeTransactionManager txManager = new ProbeTransactionManager();
    SysUserDeptServiceImpl proxied = buildProxiedService(txManager, mapperWithSecondUpdateFailure());

    assertThatThrownBy(() -> proxied.setDefaultUserDept(10L, 20L))
            .isInstanceOf(CustomException.class)
            .hasMessage("设置失败");

    assertThat(txManager.getRollbackCount()).isEqualTo(1);
}
```

**Step 2: 运行测试确认当前失败**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=SysUserDeptServiceImplTransactionTest test
```

Expected: FAIL，原因是当前方法没有事务边界，也没有对更新失败显式抛错

**Step 3: 编写最小实现**

- 在 `setDefaultUserDept` 上增加 `@Transactional(rollbackFor = Exception.class)`
- 清理旧默认部门时：
  - 若旧默认存在但清理失败，应抛 `CustomException`
- 设置新默认部门时：
  - `updateById` 或等价更新返回失败时，应抛 `CustomException("设置失败")`
- 保持现有查询路径不变，不在本 Task 顺手做查询重构

**Step 4: 运行测试确认通过**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=SysUserDeptServiceImplTransactionTest test
```

Expected: PASS

**Step 5: Commit**

```bash
git add yr-system/src/main/java/com/yr/system/service/impl/SysUserDeptServiceImpl.java yr-system/src/test/java/com/yr/system/service/impl/SysUserDeptServiceImplTransactionTest.java
git commit -m "fix: make default user dept switch transactional"
```

### Task 2: 把树形新增链路从 NPE 改成显式业务异常

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysRankServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysDutyServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysPostServiceImpl.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/TreeParentValidationContractTest.java`

**Step 1: 写失败的契约测试**

至少覆盖新增入口：

```java
@Test
void shouldRejectMissingParentWhenAddingRank() {
    SysRankServiceImpl service = buildRankServiceReturningNullParent();
    SysRank command = new SysRank();
    command.setParentId(999L);

    assertThatThrownBy(() -> service.addRank(command))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining("上级职级");
}

@Test
void shouldRejectMissingParentWhenAddingDuty() {
    SysDutyServiceImpl service = buildDutyServiceReturningNullParent();
    SysDuty command = new SysDuty();
    command.setParentId(999L);

    assertThatThrownBy(() -> service.addDuty(command))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining("上级职务");
}

@Test
void shouldRejectMissingParentWhenInsertingPost() {
    SysPostServiceImpl service = buildPostServiceReturningNullParent();
    SysPost command = new SysPost();
    command.setParentId(999L);

    assertThatThrownBy(() -> service.insertPost(command))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining("上级岗位");
}
```

如时间允许，同步补更新入口的契约测试：

- `updateRank`
- `updateDuty`
- `updatePost`

**Step 2: 运行测试确认当前失败**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=TreeParentValidationContractTest test
```

Expected: FAIL，当前行为会直接触发 `NullPointerException`

**Step 3: 编写最小实现**

- `SysRankServiceImpl.addRank`：
  - 若 `parent == null`，抛 `CustomException("上级职级不存在")`
- `SysDutyServiceImpl.addDuty`：
  - 若 `parent == null`，抛 `CustomException("上级职务不存在")`
- `SysPostServiceImpl.insertPost`：
  - 若 `parentInfo == null`，抛 `CustomException("上级岗位不存在")`
- 同步检查更新入口：
  - `updateRank`
  - `updateDuty`
  - `updatePost`
- 这些改动只做判空和异常文案，不改变原有树路径计算逻辑

**Step 4: 运行回归集**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=TreeParentValidationContractTest test
```

Expected: PASS

**Step 5: Commit**

```bash
git add yr-system/src/main/java/com/yr/system/service/impl/SysRankServiceImpl.java yr-system/src/main/java/com/yr/system/service/impl/SysDutyServiceImpl.java yr-system/src/main/java/com/yr/system/service/impl/SysPostServiceImpl.java yr-system/src/test/java/com/yr/system/service/impl/TreeParentValidationContractTest.java
git commit -m "fix: validate tree parent existence in system services"
```

### Task 3: 运行阶段验收

**Files:**
- Modify: none

**Step 1: 跑 Phase 2 回归集**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=SysUserDeptServiceImplTransactionTest,TreeParentValidationContractTest test
```

Expected: PASS

**Step 2: 跑完整模块测试**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system test
```

Expected: `BUILD SUCCESS`

**Step 3: 记录阶段输出**

- 默认部门切换已具备事务边界
- 设置默认部门失败时可整体回滚
- 树形新增与更新接口面对无效父节点时返回明确业务异常，不再抛 NPE
