# YR System Review Remediation Overview Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 按 `P1 -> P2 -> P3` 的顺序修复 `yr-system` 本轮复查发现的遗漏项，优先收口编码正确性与并发唯一性风险，再补事务原子性、输入校验和数据库约束。

**Architecture:** 本轮修复采用“先行为正确，再结构收口”的方式推进。Phase 1 聚焦 `CodeRuleUtils` 的时间边界和分布式重置竞态，避免错误编号和重复编号；Phase 2 聚焦默认部门切换与树形新增链路的显式校验，消除事务半成功和 `NullPointerException`；Phase 3 通过 Liquibase 增量变更与服务层 fail-fast，补齐附件目录编码唯一约束，避免上传链路在脏数据下出现不稳定行为。

**Tech Stack:** JDK 17, Spring Boot 2.7.18, Spring Transaction, MyBatis-Plus, RedisTemplate, Liquibase, JUnit 5, Mockito, AssertJ

---

## 当前执行状态

- 更新时间：2026-03-16
- 当前阶段：待开始
- 当前任务：输出修复总览与分阶段计划
- 已确认基线：
  - `mvn -v` 默认仍指向 JDK 8，需要执行前显式切换到 JDK 17
  - 已在 JDK 17 下验证 `mvn -pl yr-system test`
  - 当前结果：`BUILD SUCCESS`
  - 当前测试结果：`Tests run: 52, Failures: 0, Errors: 0, Skipped: 0`
- 本计划状态：
  - `docs/review_plans/` 中原同名文档已被删除
  - 本次按最新复查结论重建为“待执行版本”

## 本轮 Findings 摘要

| 优先级 | 问题 | 关键文件 | 主要风险 |
| --- | --- | --- | --- |
| P1 | 编码规则按月/按日重置仅比较 `MM` / `dd` | `yr-system/src/main/java/com/yr/system/utils/CodeRuleUtils.java` | 跨年同月、跨月同日时序列不会重置，编码错误 |
| P1 | 编码序列重置依赖本地 `synchronized` + `Redis delete/increment` | `yr-system/src/main/java/com/yr/system/utils/CodeRuleUtils.java` | 多实例部署下可能在重置窗口发出重复编号 |
| P2 | 默认部门切换缺少事务保护和更新结果校验 | `yr-system/src/main/java/com/yr/system/service/impl/SysUserDeptServiceImpl.java` | 异常时用户可能丢失默认部门，或同时出现多个默认部门 |
| P2 | `addRank` / `addDuty` / `insertPost` 仍直接解引用父节点 | `yr-system/src/main/java/com/yr/system/service/impl/SysRankServiceImpl.java`、`SysDutyServiceImpl.java`、`SysPostServiceImpl.java` | 非法 `parentId` 会触发 500 与 NPE，而不是稳定业务异常 |
| P3 | `sys_attach_category.leaf_code` 缺数据库级唯一约束 | `yr-admin/src/main/resources/db/liquibase/changelog/system/changelog1.0.xml`、`yr-system/src/main/java/com/yr/system/service/impl/SysFileServiceImpl.java` | 并发或历史脏数据会让上传链路对同一编码命中多行，查询结果不稳定 |

## 分阶段策略

### Phase 1: P1 编码正确性与唯一性

- 先补 `CodeRuleUtils` 的时间边界回归测试，锁定“跨年同月”和“跨月同日”的错误行为。
- 再把序列重置从“本地同步 + 删除再自增”改为依赖 Redis 原子能力的实现，降低多实例重复号风险。
- Phase 1 完成后，编码规则链路要具备明确的时间契约和并发回归测试。

### Phase 2: P2 事务原子性与输入校验

- 把默认部门切换对齐到默认组织切换的处理方式，保证“先清旧默认，再设新默认”是一个原子操作。
- 给树形新增和更新链路补父节点存在性校验，把 NPE 改成可预测的 `CustomException`。
- Phase 2 完成后，服务层应不再因为非法输入直接抛框架级异常。

### Phase 3: P3 数据约束与上传链路加固

- 新增 Liquibase 增量变更，为 `sys_attach_category.leaf_code` 建立唯一约束。
- 在上传链路上补一层服务级 fail-fast，避免在迁移未完成或历史脏数据存在时返回不稳定结果。
- Phase 3 完成后，附件目录编码唯一性既有数据库兜底，也有服务层明确异常语义。

## 执行顺序与依赖

1. 先执行 `2026-03-16-yr-system-review-remediation-phase1-p1.md`
2. 再执行 `2026-03-16-yr-system-review-remediation-phase2-p2.md`
3. 最后执行 `2026-03-16-yr-system-review-remediation-phase3-p3.md`

依赖说明：

- Phase 1 不依赖其他阶段，建议最先执行，因为它覆盖的是最容易形成业务错误数据的链路。
- Phase 2 可独立推进，但建议在 Phase 1 之后执行，避免 review 同时混入两类高风险行为修复。
- Phase 3 的数据库唯一约束需要先确认线上或历史库没有重复 `leaf_code`，否则 Liquibase 变更会失败。

## 统一约束

- 每次运行 Maven 前先切到 JDK 17：

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -v
```

预期：`Java version: 17.0.18`

- 所有阶段都遵循“先写失败测试，再做最小修复，再跑阶段回归”的顺序。
- 事务修复必须至少覆盖一个异常路径回滚测试。
- 并发或分布式语义修复必须补“不会再走旧实现路径”的回归断言。
- Liquibase 修复必须使用新增 changelog，不能直接篡改已投产变更的历史语义。

## 阶段验收命令

### Phase 1 验收

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=CodeRuleUtilsTest,CodeRuleUtilsSequenceResetTest test
```

### Phase 2 验收

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=SysUserDeptServiceImplTransactionTest,TreeParentValidationContractTest test
```

### Phase 3 验收

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=SysFileServiceImplTest,SysAttachCategoryServiceImplTest test
```

最终验收：

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system test
```

## 计划文件索引

- `docs/review_plans/2026-03-16-yr-system-review-remediation-phase1-p1.md`
- `docs/review_plans/2026-03-16-yr-system-review-remediation-phase2-p2.md`
- `docs/review_plans/2026-03-16-yr-system-review-remediation-phase3-p3.md`

## 推荐执行口径

- 推荐按“一个 Task 一个 commit”的粒度推进。
- 每完成一个 Phase，就先做一次定向 review，再进入下一阶段。
- 若执行中发现同类遗漏项，优先归并到当前 Phase 的同类 Task，不要额外开新 Phase。
