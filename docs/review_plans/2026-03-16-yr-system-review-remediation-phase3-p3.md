# YR System Review Phase 3 P3 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为附件目录编码补齐数据库级唯一约束，并加固上传链路在历史脏数据或迁移窗口下的异常语义。

**Architecture:** Phase 3 采用“先服务层 fail-fast，再数据库增量约束”的策略。先让 `SysFileServiceImpl` 在命中重复 `leaf_code` 时给出确定性业务异常，避免继续依赖 `getOne` 的不稳定语义；再通过 Liquibase 新增 changelog 和唯一索引，将约束真正下沉到数据库。该阶段不改历史 changelog 的既有内容，而是新增增量变更文件。

**Tech Stack:** JDK 17, Spring Boot 2.7.18, MyBatis-Plus, Liquibase, JUnit 5, Mockito, AssertJ

---

## 当前执行状态

- 更新时间：2026-03-16
- 当前阶段：待开始
- 当前任务：为附件目录编码唯一性补修复计划
- 已确认问题：
  - `sys_attach_category.leaf_code` 在 Liquibase 中只有普通索引，没有唯一约束
  - 上传链路按 `leaf_code` 读取目录时仍使用 `getOne`
  - 如果历史数据已存在重复编码，上传接口行为不稳定，且数据库没有最终兜底
  - `yr-admin/src/main/resources/db/liquibase/master.xml` 使用 `includeAll`，适合新增增量 changelog

### Task 1: 让上传链路对重复 `leaf_code` 显式失败

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysFileServiceImpl.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysFileServiceImplTest.java`

**Step 1: 写失败的服务层回归测试**

```java
@Test
void shouldFailFastWhenLeafCodeMatchesMultipleCategories() {
    ISysAttachCategoryService attachCategoryService = mock(ISysAttachCategoryService.class);
    when(attachCategoryService.list(any(QueryWrapper.class))).thenReturn(List.of(new SysAttachCategory(), new SysAttachCategory()));

    SysFileServiceImpl service = buildFileService(attachCategoryService);

    assertThatThrownBy(() -> service.uploadFile(mockMultipartFile(), "DUP_CODE", "biz-1", "BIZ"))
            .isInstanceOf(CustomException.class)
            .hasMessageContaining("附件目录编码存在重复");
}
```

**Step 2: 运行测试确认当前失败**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=SysFileServiceImplTest test
```

Expected: FAIL，当前实现仍直接调用 `getOne`

**Step 3: 编写最小实现**

- 在 `uploadFile` 中改为显式查询目录集合
- 处理分支：
  - `0` 条：抛 `CustomException("未找到附件目录：" + leafCode)`
  - `1` 条：继续原逻辑
  - `>1` 条：抛 `CustomException("附件目录编码存在重复，请联系管理员修复数据：" + leafCode)`
- 本 Task 不处理数据库索引，只收口服务层语义

**Step 4: 运行测试确认通过**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=SysFileServiceImplTest test
```

Expected: PASS

**Step 5: Commit**

```bash
git add yr-system/src/main/java/com/yr/system/service/impl/SysFileServiceImpl.java yr-system/src/test/java/com/yr/system/service/impl/SysFileServiceImplTest.java
git commit -m "fix: fail fast on duplicate attach category codes"
```

### Task 2: 新增 Liquibase 增量变更，为 `leaf_code` 建立唯一约束

**Files:**
- Create: `yr-admin/src/main/resources/db/liquibase/changelog/system/changelog1.1-attach-leaf-code-unique.xml`
- Optional Create: `yr-admin/src/main/resources/db/liquibase/changelog/system/changelog1.1-attach-leaf-code-dedup.sql`

**Step 1: 先做预检查设计**

新 changelog 应先校验历史库中是否已有重复编码：

```sql
SELECT COUNT(1)
FROM (
    SELECT leaf_code
    FROM sys_attach_category
    WHERE leaf_code IS NOT NULL
      AND leaf_code <> ''
    GROUP BY leaf_code
    HAVING COUNT(1) > 1
) duplicated_leaf_codes;
```

若结果不为 `0`，迁移应 `HALT`，不要 `MARK_RAN`。

**Step 2: 写增量 changelog**

推荐结构：

```xml
<changeSet id="sys_attach_2026-03-16-001" author="codex" context="attach leaf code unique">
    <preConditions onFail="HALT" onError="HALT">
        <sqlCheck expectedResult="0">
            SELECT COUNT(1)
            FROM (
                SELECT leaf_code
                FROM sys_attach_category
                WHERE leaf_code IS NOT NULL
                  AND leaf_code <> ''
                GROUP BY leaf_code
                HAVING COUNT(1) > 1
            ) duplicated_leaf_codes
        </sqlCheck>
    </preConditions>
    <sql>
        ALTER TABLE sys_attach_category
        DROP INDEX sys_attach_category_n1,
        ADD UNIQUE KEY sys_attach_category_u1 (leaf_code)
    </sql>
</changeSet>
```

**Step 3: 校验变更纳入入口**

- `master.xml` 已使用 `includeAll`
- 只需确认新文件落在 `yr-admin/src/main/resources/db/liquibase/changelog/system/`
- 不要修改 `changelog1.0.xml` 的历史定义

**Step 4: 记录上线前置条件**

- 先在目标环境执行重复编码检查 SQL
- 若存在重复 `leaf_code`，先人工清洗或编写一次性修复 SQL
- 清洗完成后再执行 Liquibase 迁移

**Step 5: Commit**

```bash
git add yr-admin/src/main/resources/db/liquibase/changelog/system/changelog1.1-attach-leaf-code-unique.xml
git commit -m "feat: enforce unique leaf code for attach category"
```

### Task 3: 补一个附件目录服务侧的重复编码回归测试

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysAttachCategoryServiceImpl.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysAttachCategoryServiceImplTest.java`

**Step 1: 写失败的回归测试**

```java
@Test
void shouldRejectDuplicatedLeafCodeOnCreate() {
    SysAttachCategoryServiceImpl service = buildAttachCategoryServiceWithExistingLeafCode("DOC_CODE");
    SysAttachCategory command = new SysAttachCategory();
    command.setLeafFlag(SysAttachCategory.LEAF_CATEGORY);
    command.setLeafCode("DOC_CODE");
    command.setAllowedFileType(".pdf");

    assertThatThrownBy(() -> service.updateOrInsert(command))
            .isInstanceOf(CustomException.class)
            .hasMessage("叶子节点编码已存在");
}
```

这条测试当前大概率已经通过，但保留它是为了把服务层契约和数据库约束同时固化下来。

**Step 2: 运行测试确认行为**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=SysAttachCategoryServiceImplTest test
```

Expected: PASS 或先补最小调整后 PASS

**Step 3: 如有必要做最小清理**

- 若测试需要，补构造器注入下的最小 mock 装配
- 不要在本 Task 扩大为目录树结构重构

**Step 4: Commit**

```bash
git add yr-system/src/test/java/com/yr/system/service/impl/SysAttachCategoryServiceImplTest.java yr-system/src/main/java/com/yr/system/service/impl/SysAttachCategoryServiceImpl.java
git commit -m "test: lock attach category leaf code uniqueness contract"
```

### Task 4: 运行阶段验收

**Files:**
- Modify: none

**Step 1: 跑服务层回归集**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=SysFileServiceImplTest,SysAttachCategoryServiceImplTest test
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

- 上传链路面对重复 `leaf_code` 时会显式失败
- 数据库唯一约束已具备增量迁移方案
- 后续环境上线前只需先做重复编码清洗检查
