# YR System Review User Import Exception Classification Remediation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修复 `SysUserImportService` 把系统异常误判成“数据格式错误”的问题，同时保留“业务校验失败逐条汇总、单条事务隔离”的既有导入契约。

**Architecture:** 本次修复只收敛 `SysUserImportService.importUser` 的异常分层，不改导入入口、不改 `SysUserWriteService` 的逐条事务模型，也不引入新的异常体系。`CustomException` 继续作为预期业务失败被收集并汇总；非预期运行时异常改为立即中止导入并原样向上抛出，让控制层和全局异常处理保留真实错误语义。

**Tech Stack:** JDK 17, Spring Boot 2.7.18, Spring Transaction, MyBatis-Plus, JUnit 5, Mockito, AssertJ

---

## 当前执行状态

- 更新时间：2026-03-16
- 当前阶段：待开始
- 当前任务：为 `SysUserImportService` 输出分阶段修复计划
- 已确认事实：
  - `yr-system/src/main/java/com/yr/system/service/impl/SysUserImportService.java` 当前使用 `catch (Exception exception)`，会把系统异常也归类成导入失败明细
  - `yr-system/src/test/java/com/yr/system/service/impl/SysUserServiceImplTransactionTest.java` 已锁定“逐条事务处理”的既有契约
  - `/system/user/importData` 控制器成功路径直接返回 `AjaxResult.success(message)`，失败路径依赖异常传播与全局异常处理
- 本计划目标边界：
  - 不改变“单条导入失败不会回滚此前成功数据”的现有事务模型
  - 不改变导入成功消息的拼装格式
  - 只修复“系统异常要快速失败并保留原始语义”这一类问题

## 问题拆解

| 维度 | 当前行为 | 目标行为 | 风险 |
| --- | --- | --- | --- |
| 业务校验失败 | 被汇总到 `failureMsg`，循环继续 | 保持不变 | 不能误伤现有导入体验 |
| 非预期系统异常 | 也被汇总到 `failureMsg`，循环继续 | 立即中止并原样抛出 | 当前会掩盖真实故障并制造部分成功副作用 |
| 事务语义 | 单条写入在独立事务中提交/回滚 | 保持不变 | 修复异常分层时不能把导入链路改成整批事务 |
| 测试覆盖 | 已覆盖业务异常导致的逐条提交 | 需补系统异常快速失败 | 容易只改实现、不补契约测试 |

## Phase 1: 锁定异常分层契约

### Task 1: 为导入服务补“业务异常继续、系统异常停止”的失败测试

**Files:**
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysUserImportServiceExceptionHandlingTest.java`
- Reference: `yr-system/src/main/java/com/yr/system/service/impl/SysUserImportService.java`
- Reference: `yr-common/src/main/java/com/yr/common/exception/CustomException.java`
- Reference: `yr-system/src/test/java/com/yr/system/service/impl/SysUserServiceImplTransactionTest.java`

**Step 1: 写失败的单元测试**

新增两个契约测试，第一条锁定“业务异常继续收集”，第二条锁定“系统异常立即中止”：

```java
/**
 * @file 验证 SysUserImportService 的异常分层行为
 * @author Codex
 * @date 2026-03-16
 */
package com.yr.system.service.impl;

import com.yr.common.core.domain.entity.SysUser;
import com.yr.common.exception.CustomException;
import com.yr.system.mapper.SysUserMapper;
import com.yr.system.service.ISysConfigService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysUserImportServiceExceptionHandlingTest {

    @Test
    void shouldCollectBusinessFailureAndContinueProcessingLaterUsers() {
        ISysConfigService configService = mock(ISysConfigService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserWriteService writeService = mock(SysUserWriteService.class);
        SysUserImportService importService = new SysUserImportService(configService, userMapper, writeService);
        SysUser invalidUser = buildUser("phase4-invalid");
        SysUser trailingUser = buildUser("phase4-trailing");

        when(configService.selectConfigByKey("sys.user.initPassword")).thenReturn("Init@123");
        when(userMapper.selectUserByUserName(anyString())).thenReturn(null);
        doThrow(new CustomException("职级不能为空")).when(writeService).insertUser(any(SysUser.class));

        assertThatThrownBy(() -> importService.importUser(List.of(invalidUser, trailingUser), false, "phase4"))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("账号 phase4-invalid 导入失败：职级不能为空");

        verify(writeService).insertUser(invalidUser);
        verify(writeService).insertUser(trailingUser);
    }

    @Test
    void shouldFailFastWhenUnexpectedRuntimeExceptionOccurs() {
        ISysConfigService configService = mock(ISysConfigService.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        SysUserWriteService writeService = mock(SysUserWriteService.class);
        SysUserImportService importService = new SysUserImportService(configService, userMapper, writeService);
        SysUser brokenUser = buildUser("phase4-broken");
        SysUser untouchedUser = buildUser("phase4-untouched");

        when(configService.selectConfigByKey("sys.user.initPassword")).thenReturn("Init@123");
        when(userMapper.selectUserByUserName(anyString())).thenReturn(null);
        doThrow(new IllegalStateException("db boom")).when(writeService).insertUser(brokenUser);

        assertThatThrownBy(() -> importService.importUser(List.of(brokenUser, untouchedUser), false, "phase4"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db boom");

        verify(writeService).insertUser(brokenUser);
        verify(writeService, never()).insertUser(untouchedUser);
    }

    private SysUser buildUser(String userName) {
        SysUser user = new SysUser();
        user.setUserName(userName);
        user.setRankId(1L);
        return user;
    }
}
```

**Step 2: 运行测试确认当前失败**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=SysUserImportServiceExceptionHandlingTest test
```

Expected:

- `shouldCollectBusinessFailureAndContinueProcessingLaterUsers` 可能通过
- `shouldFailFastWhenUnexpectedRuntimeExceptionOccurs` 当前应失败，因为系统异常会被包装成 `CustomException`

**Step 3: 编写最小实现**

修改 `yr-system/src/main/java/com/yr/system/service/impl/SysUserImportService.java`：

```java
for (SysUser user : userList) {
    try {
        SysUser existedUser = userMapper.selectUserByUserName(user.getUserName());
        if (StringUtils.isNull(existedUser)) {
            user.setPassword(SecurityUtils.encryptPassword(password));
            user.setCreateBy(operName);
            sysUserWriteService.insertUser(user);
            successNum++;
            successMsg.append("<br/>").append(successNum).append("、账号 ").append(user.getUserName()).append(" 导入成功");
        } else if (Boolean.TRUE.equals(isUpdateSupport)) {
            user.setUpdateBy(operName);
            sysUserWriteService.updateUser(user);
            successNum++;
            successMsg.append("<br/>").append(successNum).append("、账号 ").append(user.getUserName()).append(" 更新成功");
        } else {
            failureNum++;
            failureMsg.append("<br/>").append(failureNum).append("、账号 ").append(user.getUserName()).append(" 已存在");
        }
    } catch (CustomException exception) {
        failureNum++;
        String msg = "<br/>" + failureNum + "、账号 " + user.getUserName() + " 导入失败：";
        failureMsg.append(msg).append(exception.getMessage());
        log.error(msg, exception);
    } catch (RuntimeException exception) {
        log.error("账号 {} 导入出现系统异常，中止后续导入", user.getUserName(), exception);
        throw exception;
    }
}
```

实现约束：

- 只捕获 `CustomException` 作为可预期业务失败
- 非预期 `RuntimeException` 直接抛出，不写入 `failureMsg`
- 不改成功消息、不改已有业务异常文案

**Step 4: 运行测试确认通过**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=SysUserImportServiceExceptionHandlingTest test
```

Expected: PASS

**Step 5: Commit**

```bash
git add yr-system/src/main/java/com/yr/system/service/impl/SysUserImportService.java yr-system/src/test/java/com/yr/system/service/impl/SysUserImportServiceExceptionHandlingTest.java
git commit -m "fix: fail fast on unexpected user import errors"
```

## Phase 2: 锁定逐条事务与快速失败的组合语义

### Task 2: 扩展事务回归测试，证明修复没有破坏逐条提交模型

**Files:**
- Modify: `yr-system/src/test/java/com/yr/system/service/impl/SysUserServiceImplTransactionTest.java`
- Reference: `yr-system/src/main/java/com/yr/system/service/impl/SysUserWriteService.java`
- Reference: `yr-system/src/main/java/com/yr/system/service/impl/SysUserImportService.java`

**Step 1: 为事务测试补一个“系统异常中止后续导入”的用例**

在现有事务测试类中补一条集成式契约测试，锁定“前一条成功已提交、当前条系统异常回滚、后续条不再处理”：

```java
@Test
void shouldStopImportingRemainingUsersWhenUnexpectedSystemExceptionOccurs() {
    ProbeTransactionManager transactionManager = new ProbeTransactionManager();
    SysUserMapper userMapper = mock(SysUserMapper.class);
    ISysRankService rankService = mock(ISysRankService.class);
    ISysUserOrgService userOrgService = mock(ISysUserOrgService.class);
    ISysUserRankService userRankService = mock(ISysUserRankService.class);
    ISysConfigService configService = mock(ISysConfigService.class);
    SysUserWriteService writeTarget = new SysUserWriteService(userMapper, rankService, userOrgService, userRankService);
    SysUserWriteService writeProxy = createWriteServiceProxy(writeTarget, transactionManager);
    SysUserImportService importService = new SysUserImportService(configService, userMapper, writeProxy);
    SysUserServiceImpl userService = buildUserService(userMapper, writeProxy, importService);
    SysUser successUser = buildUser("phase4-success", 1L);
    SysUser brokenUser = buildUser("phase4-broken", 1L);
    SysUser untouchedUser = buildUser("phase4-untouched", 1L);
    SysRank sysRank = new SysRank();
    sysRank.setId(1L);
    sysRank.setRankType("LEAF");

    setAuthenticatedOrg(66L);
    when(configService.selectConfigByKey("sys.user.initPassword")).thenReturn("Init@123");
    when(userMapper.selectUserByUserName(anyString())).thenReturn(null);
    when(rankService.getById(1L)).thenReturn(sysRank);
    when(userMapper.insertUser(argThat(user -> "phase4-success".equals(user.getUserName())))).thenAnswer(invocation -> {
        SysUser insertedUser = invocation.getArgument(0);
        insertedUser.setUserId(303L);
        return 1;
    });
    when(userMapper.insertUser(argThat(user -> "phase4-broken".equals(user.getUserName()))))
            .thenThrow(new IllegalStateException("db boom"));
    when(userOrgService.count(any(QueryWrapper.class))).thenReturn(0L);
    doAnswer(invocation -> null).when(userOrgService).addSysUserOrg(any(SysUserOrg.class));
    when(userRankService.save(any(SysUserRank.class))).thenReturn(true);

    assertThatThrownBy(() -> userService.importUser(List.of(successUser, brokenUser, untouchedUser), false, "phase4"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("db boom");

    assertThat(transactionManager.getBeginCount()).isEqualTo(2);
    assertThat(transactionManager.getCommitCount()).isEqualTo(1);
    assertThat(transactionManager.getRollbackCount()).isEqualTo(1);
    verify(userMapper, never()).insertUser(argThat(user -> "phase4-untouched".equals(user.getUserName())));
}
```

如果测试代码较长，可把 `buildUserService(...)` 抽成私有辅助方法，避免复制构造参数。

**Step 2: 运行测试确认当前失败**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=SysUserServiceImplTransactionTest test
```

Expected:

- 现有 `shouldCommitSuccessfulRowsWhenAnotherImportedUserFails` 继续通过
- 如果先单独写这条测试再运行旧实现，新增系统异常用例应失败，因为导入会继续循环并最终抛 `CustomException`
- 如果该测试是在 Phase 1 完成后补上，则它应直接通过，此时把它视为“事务组合语义已被锁定”的验证，不再额外改实现

**Step 3: 复跑测试并确认无需额外实现**

本 Task 默认不再新增结构性改动，只确认下列结果：

- 业务异常仍被汇总成 `CustomException`
- 非预期系统异常原样向上抛出
- 未处理到的后续用户不会触发写入
- 已成功提交的前序用户事务保持提交

**Step 4: 运行事务回归集**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=SysUserImportServiceExceptionHandlingTest,SysUserServiceImplTransactionTest test
```

Expected: PASS

**Step 5: Commit**

```bash
git add yr-system/src/test/java/com/yr/system/service/impl/SysUserServiceImplTransactionTest.java
git commit -m "test: lock user import exception classification contract"
```

## Phase 3: 全量回归与交付检查

### Task 3: 跑阶段验收并复核调用面

**Files:**
- Modify: none
- Verify: `yr-admin/src/main/java/com/yr/web/controller/system/SysUserController.java`
- Verify: `yr-system/src/main/java/com/yr/system/service/impl/SysUserServiceImpl.java`

**Step 1: 复核调用链不需要额外适配**

检查以下事实：

- `SysUserServiceImpl.importUser(...)` 只是简单委托给 `SysUserImportService`
- `SysUserController.importData(...)` 成功时依旧返回成功消息
- 控制层不依赖“所有失败都包装成 `CustomException`”这一旧行为

复核命令：

```bash
rg -n "importUser\\(" yr-admin/src/main/java yr-system/src/main/java
```

Expected: 只看到 Controller -> ServiceImpl -> ImportService 的单向调用链

**Step 2: 跑定向验收**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system -Dtest=SysUserImportServiceExceptionHandlingTest,SysUserServiceImplTransactionTest,SysUserServiceImplDelegationTest test
```

Expected: PASS

**Step 3: 跑完整模块测试**

Run:

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system test
```

Expected:

- `BUILD SUCCESS`
- 不出现新增失败用例

**Step 4: 记录验收结论**

需要确认并记录以下结果：

- 业务校验失败仍然按行汇总
- 系统异常不再被误报为“数据格式不正确”
- 导入发生系统故障时，不会继续处理后续用户
- 逐条事务模型仍然成立，已提交数据不会被误回滚

**Step 5: Commit**

```bash
git add yr-system/src/main/java/com/yr/system/service/impl/SysUserImportService.java yr-system/src/test/java/com/yr/system/service/impl/SysUserImportServiceExceptionHandlingTest.java yr-system/src/test/java/com/yr/system/service/impl/SysUserServiceImplTransactionTest.java
git commit -m "chore: verify user import failure classification"
```

## 验收标准

- `SysUserImportService.importUser` 不再捕获整个 `Exception`
- `CustomException` 仍会被汇总成导入失败明细
- `IllegalStateException`、`DataAccessException` 等非预期运行时异常会立即向上抛出
- 新增测试能稳定区分“业务失败继续收集”和“系统异常快速失败”
- `mvn -pl yr-system test` 在 JDK 17 下通过

## 风险与注意事项

- 不要把整个导入方法改成 `@Transactional`，否则会破坏当前“逐条提交”的既有契约
- 不要顺手改导入结果文案，否则会让已有测试和前端提示一起漂移
- 如果执行中发现控制层必须统一包装系统异常，再单开 follow-up review，不在本计划里顺手扩 scope
- 若线上依赖“系统异常也返回汇总字符串”的旧行为，需要先和调用方确认后再落地
