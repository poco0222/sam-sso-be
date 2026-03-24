# YR System Review Phase 1 P1 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修复编码规则链路中的两个 P1 风险，确保跨日/月/年时序列重置正确，并降低多实例部署下的重复编号风险。

**Architecture:** Phase 1 只处理 `CodeRuleUtils`。先通过失败测试锁定“错误时间比较”这一确定性 bug，再把“本地同步 + Redis 删除再自增”的重置方式替换成依赖 Redis 原子能力的实现。该阶段不改动编码规则对外接口和数据库表结构，只在工具类与单测层完成收口。

**Tech Stack:** JDK 17, Spring Boot 2.7.18, RedisTemplate, JUnit 5, Mockito, AssertJ, Spring Test

---

## 当前执行状态

- 更新时间：2026-03-16
- 当前阶段：待开始
- 当前任务：为 `CodeRuleUtils` 制定修复步骤
- 已确认问题：
  - `MONTH` 只比较 `MM`
  - `DAY` 只比较 `dd`
  - 重置逻辑仍使用 `delete(sequenceKey)` + `increment(sequenceKey, startValue)`
  - 当前 `CodeRuleUtilsTest` 只覆盖字符串拼接分支，未覆盖时间边界和重置路径

### Task 1: 锁定按月/按日重置的时间边界缺陷

**Files:**
- Modify: `yr-system/src/test/java/com/yr/system/utils/CodeRuleUtilsTest.java`
- Optional Create: `yr-system/src/test/java/com/yr/system/utils/CodeRuleUtilsResetStrategyTest.java`

**Step 1: 写失败的边界测试**

至少覆盖以下 3 个场景：

```java
@Test
void shouldResetMonthlySequenceWhenYearChangesButMonthStaysSame() {
    boolean reset = ReflectionTestUtils.invokeMethod(
            codeRuleUtils,
            "isResetSequence",
            CodeRuleConstant.ResetFrequency.MONTH,
            date("2025-03-01 00:00:00"),
            date("2026-03-01 00:00:00")
    );

    assertThat(reset).isTrue();
}

@Test
void shouldResetDailySequenceWhenMonthChangesButDayStaysSame() {
    boolean reset = ReflectionTestUtils.invokeMethod(
            codeRuleUtils,
            "isResetSequence",
            CodeRuleConstant.ResetFrequency.DAY,
            date("2026-03-16 00:00:00"),
            date("2026-04-16 00:00:00")
    );

    assertThat(reset).isTrue();
}

@Test
void shouldNotResetMonthlySequenceWithinSameYearAndMonth() {
    boolean reset = ReflectionTestUtils.invokeMethod(
            codeRuleUtils,
            "isResetSequence",
            CodeRuleConstant.ResetFrequency.MONTH,
            date("2026-03-01 00:00:00"),
            date("2026-03-31 23:59:59")
    );

    assertThat(reset).isFalse();
}
```

**Step 2: 运行测试确认当前失败**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=CodeRuleUtilsTest,CodeRuleUtilsResetStrategyTest test
```

Expected: FAIL，原因是当前实现只比较 `MM` 与 `dd`

**Step 3: 编写最小实现**

- 修改 `CodeRuleUtils.isResetSequence`
- `MONTH` 分支至少比较“年 + 月”
- `DAY` 分支至少比较“年 + 月 + 日”
- 推荐做法：
  - 用 `Calendar.YEAR + Calendar.MONTH`
  - 或直接比较 `yyyyMM` / `yyyyMMdd`
- 不要顺手改动 `YEAR`、`QUARTER` 之外的其他业务逻辑

**Step 4: 运行测试确认通过**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=CodeRuleUtilsTest,CodeRuleUtilsResetStrategyTest test
```

Expected: PASS

**Step 5: Commit**

```bash
git add yr-system/src/main/java/com/yr/system/utils/CodeRuleUtils.java yr-system/src/test/java/com/yr/system/utils/CodeRuleUtilsTest.java yr-system/src/test/java/com/yr/system/utils/CodeRuleUtilsResetStrategyTest.java
git commit -m "fix: correct code rule reset date boundaries"
```

### Task 2: 收口序列重置的多实例竞态窗口

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/utils/CodeRuleUtils.java`
- Create: `yr-system/src/test/java/com/yr/system/utils/CodeRuleUtilsSequenceResetTest.java`

**Step 1: 写失败的回归测试**

目标不是做真正分布式集成测试，而是先锁定“旧实现路径不再存在”：

```java
@Test
void shouldNotDeleteSequenceKeyBeforeResettingValue() {
    RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    CodeRuleUtils target = buildCodeRuleUtils(redisTemplate);

    assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
            target,
            "resetSequence",
            "code-rule:seq",
            1L
    )).doesNotThrowAnyException();

    verify(redisTemplate, never()).delete("code-rule:seq");
}
```

如果准备把原子能力抽成 helper，也可以改成：

```java
@Test
void shouldUseAtomicRedisAdvanceWhenResetRequired() {
    when(redisTemplate.execute(any(), anyList(), any())).thenReturn(1L);

    long next = ReflectionTestUtils.invokeMethod(target, "advanceSequenceAtomically", "code-rule:seq", 1L);

    assertThat(next).isEqualTo(1L);
}
```

**Step 2: 运行测试确认当前失败**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=CodeRuleUtilsSequenceResetTest test
```

Expected: FAIL，原因是当前实现仍显式调用 `redisTemplate.delete(sequenceKey)`

**Step 3: 编写最小实现**

- 目标：移除 `delete + increment` 的两步式重置
- 推荐方案：
  - 新增一个私有 helper，例如 `advanceSequenceAtomically`
  - 通过 `redisTemplate.execute(...)` 调用 Redis 原子脚本，完成“重置到 startValue”或“继续递增”
  - 保证重置路径在 Redis 侧一次完成
- 若不引入脚本，至少要加分布式锁，不再只依赖本地 `synchronized`
- `synchronized` 可以保留为单实例兜底，但不能再作为唯一并发保护

**Step 4: 运行编码规则回归集**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=CodeRuleUtilsTest,CodeRuleUtilsResetStrategyTest,CodeRuleUtilsSequenceResetTest test
```

Expected: PASS

**Step 5: Commit**

```bash
git add yr-system/src/main/java/com/yr/system/utils/CodeRuleUtils.java yr-system/src/test/java/com/yr/system/utils/CodeRuleUtilsSequenceResetTest.java
git commit -m "fix: make code rule sequence reset atomic"
```

### Task 3: 运行阶段验收

**Files:**
- Modify: none

**Step 1: 跑 Phase 1 回归集**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=CodeRuleUtilsTest,CodeRuleUtilsResetStrategyTest,CodeRuleUtilsSequenceResetTest test
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

- 按月/按日重置已具备跨年、跨月边界正确性
- 旧的 `delete + increment` 重置路径已被移除
- 编码规则链路对多实例部署更安全
