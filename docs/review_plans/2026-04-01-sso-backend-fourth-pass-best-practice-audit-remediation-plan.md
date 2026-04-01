# SSO Backend Fourth Pass Best Practice Audit Remediation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于 2026-04-01 的 live audit（实时审计）结果，把 `sam-sso-be` 剩余的 architecture residue（架构残余问题）与 code-level write-boundary/input-contract gap（代码级写入边界与输入契约缺口）收敛到可持续维护的状态，并为新对话执行提供最新 canonical handoff（权威交接入口）。

**Architecture:** 当前 `JDK 17 + Spring Boot 2.7.18` 多模块基线已经基本站稳，`SecurityFilterChain`（安全过滤链）、JWT（令牌）、Redis（缓存）、Liquibase（数据库变更管理）、RocketMQ（消息队列）与全量测试都可工作。与 2026-03-31 相比，整体已经不再是“架构没搭好”的问题；截至 `2026-04-01 09:45 CST`，`Task 1` 到 `Task 6` 已全部完成，本文计划范围内的 architecture/code-level open items（架构级/代码级开放项）已清零。

**Tech Stack:** Java 17, Spring Boot 2.7.18, Spring MVC, Spring Security, Spring Validation, Spring Data Redis, Jackson, MyBatis-Plus, Liquibase, RocketMQ, Quartz, JUnit 5, MockMvc, Mockito, AssertJ

---

## Scope

- backend root（后端根目录）：`/Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be`
- wrapper root（包装层根目录）：`/Users/PopoY/workingFiles/Projects/SAM/sso`
- 本文档是 2026-04-01 的最新 comprehensive backend audit（综合后端审计）与 remediation plan（整改计划），用于替代 2026-03-31 文档作为新的 canonical entrypoint（权威执行入口）。
- 本文档最初以 docs-first（文档先行）方式产出；截至 `2026-04-01 09:45 CST`，`Task 1` 到 `Task 6` 已在同仓库内按顺序执行完成；后续新对话若继续此仓库，默认应复用本文 execution status（执行状态）作为 handoff（交接）真值，并先进入 merge/finalization（合并/收尾）决策，而不是重新回到整改实施阶段。
- 由于本轮已重新 live revalidate（实时复核）代码与测试，以下旧结论默认不再单独展开，除非本计划某个 task 明确重新打开：
  - 2026-03-31 已完成的 `SysUser` / `SysProfile` / `SsoClient` / `SsoSyncTask` DTO 收口
  - 企业微信 `OAuth state`（授权状态参数）闭环与 URL 脱敏
  - `SysUser.toString()` 敏感字段输出
  - `application-prod.yml` 中 `demoEnabled` / `devtools` / 模板默认路径等已知项

## Fresh Baseline

- 已验证：

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home \
PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH \
mvn -v

JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home \
PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH \
mvn test
```

- 当前结果：
  - `mvn -v` -> `Apache Maven 3.9.12`
  - `java.version` -> `17.0.18`
  - `mvn test` -> `BUILD SUCCESS`
  - 全量基线 -> `Tests run: 94, Failures: 0, Errors: 0, Skipped: 5`

## Audit Conclusion

- 结论 1：当前 backend architecture（后端架构）已经基本合格，不需要再泛化成“大范围架构重构”；后续预算应优先投入到具体 code path（代码路径）与输入/写入边界。
- 结论 2：2026-03-31 已整改的认证与控制台主链路大部分仍保持关闭状态，本轮没有复现新的 `OAuth/token/permission`（授权/令牌/权限）高风险洞。
- 结论 3：审计基线时共有 1 个 architecture-level security residue（架构级安全残留）和 5 个 code-level implementation gap（代码级实现缺口）；`Task 1` 到 `Task 6` 完成后，当前计划范围内的 open items（开放项）已全部关闭，并经过定向验证与全量 `mvn test` 双重确认。

## Closed Or Downgraded Findings

- 已关闭：`/sso/client/list` 不再直接向前端返回 `clientSecret`。`SsoClientController.list` 已把 entity（实体）映射为 `SsoClientView`，而 `SsoClientView` 不包含 `clientSecret`。
- 已关闭：`RedisConfig` 不再对白名单开放 `java.lang.` / `java.util.` / `java.time.` 整段包前缀。当前 `yr-framework/src/main/java/com/yr/framework/config/RedisConfig.java:97-128` 已收紧为 concrete subtype allow-list（具体子类型允许列表），并由 `yr-framework/src/test/java/com/yr/framework/config/RedisConfigSecurityContractTest.java:52-156` 锁定登录态 round-trip、legacy `NON_FINAL` payload（历史载荷）与 `SameUrlDataInterceptor` 嵌套 `HashMap` 兼容面。
- 已关闭：`SysOrg changeStatus` 不再接收完整 `SysOrg entity（实体）`，也不再委托到通用 `updateSysOrg`。当前 `yr-admin/src/main/java/com/yr/web/controller/system/SysOrgController.java:78-209` 已切换到最小 DTO + 精简写入对象，`yr-system/src/main/java/com/yr/system/service/impl/SysOrgServiceImpl.java:203-224` 已改走专用 `updateOrgStatus`，`yr-system/src/main/resources/mapper/system/SysOrgMapper.xml:144-169` 已把状态更新与通用编辑 SQL 分离，并由 `yr-admin/src/test/java/com/yr/web/controller/system/SysOrgControllerContractTest.java:109-203`、`yr-system/src/test/java/com/yr/system/service/impl/SysOrgServiceImplSafetyTest.java:35-63` 锁定。
- 已关闭：`SysUserOrg add/changeEnabled` 不再透传完整 `SysUserOrg entity（实体）`。当前 `yr-admin/src/main/java/com/yr/web/controller/system/SysUserOrgController.java` 已切换到 `AddUserOrgRequest` / `ChangeUserOrgEnabledRequest`，`yr-system/src/main/java/com/yr/system/service/impl/SysUserOrgServiceImpl.java` 已统一兜底 `enabled/isDefault` 并校验 `enabled` 允许值，且由 `yr-admin/src/test/java/com/yr/web/controller/system/SysUserOrgControllerContractTest.java`、`yr-system/src/test/java/com/yr/system/service/impl/SysUserOrgServiceImplTest.java`、`yr-system/src/test/java/com/yr/system/service/impl/SysUserOrgServiceImplTransactionTest.java` 锁定新增、启停与事务边界。
- 已关闭：`SysDept add` 不再信任客户端自带 `orgId`。当前 `yr-admin/src/main/java/com/yr/web/controller/system/SysDeptController.java` 已移除 controller 层的登录组织回填，`yr-system/src/main/java/com/yr/system/service/impl/SysDeptServiceImpl.java` 会无条件以父部门 `orgId` 覆盖请求值，并由 `yr-admin/src/test/java/com/yr/web/controller/system/SysDeptControllerContractTest.java`、`yr-system/src/test/java/com/yr/system/service/impl/SysDeptServiceImplSafetyTest.java`、`yr-system/src/test/java/com/yr/system/service/impl/TreeParentValidationContractTest.java` 锁定。
- 已关闭：`GlobalExceptionHandler` 不再把未知异常或授权类异常的 raw message（原始消息）直接打进日志。当前 `yr-framework/src/main/java/com/yr/framework/web/exception/GlobalExceptionHandler.java:64-91` 已切换为固定模板日志，并由 `yr-framework/src/test/java/com/yr/framework/web/exception/GlobalExceptionHandlerContractTest.java:53-74` 锁定未知异常响应脱敏与日志不再原样回显敏感文本。
- 已关闭：`/login` 与 `/auth/wxwork/login` 已补齐 controller-level Bean Validation（控制器层参数校验）。当前 `yr-common/src/main/java/com/yr/common/core/domain/model/LoginBody.java:13-63` 已用 validation groups（校验分组）区分普通登录与企业微信登录，`yr-admin/src/main/java/com/yr/web/controller/system/SysLoginController.java:58-69` 与 `yr-admin/src/main/java/com/yr/web/controller/auth/WxworkAuthController.java:49-53` 已接入 `@Validated`，并由 `yr-admin/src/test/java/com/yr/login/SysLoginControllerContractTest.java:138-166`、`yr-admin/src/test/java/com/yr/login/WxworkLoginControllerContractTest.java:83-110` 锁定空字段 `400 Bad Request` 契约。
- 已关闭：`SysLoginService` 已为 WeCom（企业微信）HTTP 调用补上显式 timeout（超时）并复用单个客户端构造方式。当前 `yr-admin/src/main/java/com/yr/framework/web/service/SysLoginService.java:101-151`、`yr-admin/src/main/java/com/yr/framework/web/service/SysLoginService.java:447-457` 已统一 5 秒 connect/read timeout 和懒加载单例 `RestTemplate`，并由 `yr-admin/src/test/java/com/yr/framework/web/service/SysLoginServiceWxworkStateContractTest.java:189-216` 锁定两次调用只构造一次客户端。
- 已关闭：`SsoClient.toString()` 不再包含 `clientSecret`。当前 `yr-common/src/main/java/com/yr/common/core/domain/entity/SsoClient.java:19-36` 已通过 `@ToString(exclude = "clientSecret")` 收口敏感字段输出，并由 `yr-system/src/test/java/com/yr/system/service/impl/SsoClientServiceImplValidationContractTest.java:25-35` 锁定实体调试输出不再回显密钥。
- 已关闭：`retryTask` 只允许失败态任务重试，并补上状态条件更新防止并发重复翻转。当前 `yr-system/src/main/java/com/yr/system/service/impl/SsoSyncTaskServiceImpl.java:162-187` 已显式拒绝非 `FAILED` 状态，并通过 `status = FAILED` 条件更新切回 `RUNNING`，由 `yr-system/src/test/java/com/yr/system/service/impl/SsoSyncTaskRetrySafetyTest.java:27-58`、`yr-system/src/test/java/com/yr/system/service/impl/SsoSyncTaskServiceImplTest.java` 与 `yr-system/src/test/java/com/yr/system/service/impl/SsoSyncTaskFailurePersistenceTest.java` 一并锁定失败态、并发漂移与失败持久化语义。
- 已关闭：初始化导入关系表的 `operatorUserId` 已优先解析 `task.getCreateBy()` 的真实数字 ID，解析失败时才 fallback（回退）到兼容默认值。当前 `yr-system/src/main/java/com/yr/system/service/impl/SsoIdentityImportServiceImpl.java:75-76`、`yr-system/src/main/java/com/yr/system/service/impl/SsoIdentityImportServiceImpl.java:471-492` 已切换到“数字优先、非数字回退”的逻辑，并由 `yr-system/src/test/java/com/yr/system/service/impl/SsoIdentityImportServiceImplTest.java:116-145` 锁定关系表审计字段会使用 `createBy=9527`。
- 不采纳为 open finding（开放问题）：`yr-common` 中的 `spring-security-core` / `spring-security-crypto` 当前并非单纯依赖泄露，因为 `LoginUser` 与 `SecurityUtils` 仍直接依赖 Spring Security API；若要清理，需要先调整公共模型与工具类归属，不能把它当作单步可删项。

## Current Findings

- 当前按本文计划范围复核后，open findings（开放问题）已清零；后续若继续推进，默认进入 merge/finalization（合并/收尾）或下一轮增量 audit（增量审计）阶段。

## Test Gaps

- 本轮计划范围内的 test gap（测试缺口）已关闭：
  - `SysOrg` / `SysUserOrg` / `SysDept` / `SysLogin` / `WxworkLogin` / `GlobalExceptionHandler` 都已有 dedicated contract tests（专用契约测试）
  - `SsoClient` / `SsoSyncTask` / `SsoIdentityImport` 也已补齐对应的 safety/traceability 契约测试
  - 全量 `mvn test` 已通过，当前没有留在本文计划范围内的已知测试空白

## Execution Order

1. `Task 1` 收紧 `RedisConfig` 多态反序列化白名单，解决唯一剩余 architecture-level 风险
2. `Task 2` 拆分 `SysOrg` 写入口，堵住 `changeStatus` 通用更新旁路
3. `Task 3` 收紧 `SysUserOrg` 新增/启停/默认组织边界
4. `Task 4` 修复 `SysDept` 新增时的跨组织不变量漏洞
5. `Task 5` 收口登录与异常处理的运行时稳健性
6. `Task 6` 做低优先级模型卫生与导入审计收尾，并跑全量验证

## Task 1: Tighten Redis Polymorphic Deserialization

**Files:**
- Modify: `yr-framework/src/main/java/com/yr/framework/config/RedisConfig.java`
- Modify: `yr-framework/src/test/java/com/yr/framework/config/RedisConfigSecurityContractTest.java`
- Verify: `yr-framework/src/test/java/com/yr/framework/config/RedisConfigSecurityContractTest.java`

- [x] **Step 1: 先把真实缓存兼容面列清楚**

目标：
- 明确当前登录缓存真正需要反序列化的类型集合
- 不再用包前缀 `java.lang.` / `java.util.` 整段白名单兜底

Live evidence refresh（2026-04-01 08:46 CST）：
- `yr-framework/src/main/java/com/yr/framework/web/service/TokenService.java` 仍通过 `RedisCache.setCacheObject/getCacheObject` 直接写入和读取 `LoginUser`；当前登录缓存类型图至少覆盖 `LoginUser`、`SysUser`、`LinkedHashSet`、`ArrayList` 与 `SimpleGrantedAuthority`
- `yr-framework/src/main/java/com/yr/framework/interceptor/impl/SameUrlDataInterceptor.java` 也复用了同一个 `RedisTemplate<Object, Object>`，会把嵌套 `HashMap<String, Object>` 写入 Redis 做防重复提交校验；Task 1 的最小白名单不能只盯着 token cache
- 同一条 `SameUrlDataInterceptor` 载荷在 `repeatTime` 上会把 `Long` 作为 `Map<String, Object>` 的 object value（对象值）落库；在保留 `NON_FINAL` 的前提下，`java.lang.Long` 也必须被视为当前 live payload（载荷）的一部分
- `yr-common/src/main/java/com/yr/common/core/domain/entity/SysUser.java` 继承 `BaseEntity`，而 `yr-common/src/main/java/com/yr/common/core/domain/BaseEntity.java` 真实包含 `Date createTime/updateTime/loginDate` 与惰性初始化的 `HashMap params`；旧缓存 payload 若曾按 `DefaultTyping.NON_FINAL` 写入，`Date` 兼容也必须纳入本轮 decision（决策）
- `yr-common/src/main/java/com/yr/common/core/domain/entity/SysDept.java` 仍挂在 `SysUser.dept` 类型图下，且自带 `ArrayList children`；即便登录链路不一定每次都把部门对象填满，白名单决策也不能把它当成“理论上不存在”的类型

Decision update：
- 本 task 的目标从“移除宽泛包前缀”进一步细化为“保留旧缓存可读性的前提下，把允许的多态类型收敛到 live 代码已证实的具体模型与集合实现”
- 2026-04-01 live red-green 尝试发现：在当前 `FastJson2JsonRedisSerializer.writeValueAsString(t)` 直接按运行时 concrete type（具体类型）写根对象的前提下，只修改 `RedisConfig` 就切到 `DefaultTyping.OBJECT_AND_NON_CONCRETE` 会导致根级 `LoginUser/HashMap` 载荷缺失 `@class`，从而让 `RedisTemplate<Object, Object>` 的反序列化直接失败
- 因此本 task 在“不扩散到计划外 serializer 重构”的边界下，最终 decision 调整为：保留 `DefaultTyping.NON_FINAL` 以维持根对象与 legacy cache（历史缓存）可读性，但必须把 `allowIfSubType(...)` 从宽泛包前缀收紧为 live 代码已证实的具体类集合；其中 `HashMap<String, Object>` 的真实 object value 也要纳入允许列表，而不是只给容器类型开口
- 2026-04-01 live rerun 进一步确认：本仓库当前需要为定向 surefire（测试选择）命令补 `-Dsurefire.failIfNoSpecifiedTests=false`，否则 reactor（聚合构建）会先在 `yr-common` 因 “No tests matching pattern” 失败，拿不到真正的 red-phase（红灯阶段）信号；后续 Task 1 的验证命令以修正后的版本为准

- [x] **Step 2: 补红灯测试锁住“不允许宽泛白名单”**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home \
PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH \
mvn -pl yr-framework -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RedisConfigSecurityContractTest test
```

Expected:
- FAIL before implementation because current source still contains broad prefix whitelist / `DefaultTyping.NON_FINAL`

- [x] **Step 3: 收敛实现**

实现要求：
- 优先选择更小的 allow-list（允许列表）而不是更大的 package 前缀
- 若必须保留类型信息，至少把类型范围限定到 `LoginUser/SysUser/SimpleGrantedAuthority` 与必要集合实现
- 若 live 代码证明 `RedisTemplate<Object, Object>` 根对象仍依赖 `NON_FINAL` 才能保留 `@class`，允许保留 `NON_FINAL`，但必须同步把 subtype 白名单收紧为具体类而不是包前缀
- 不能破坏现有 token cache（令牌缓存）兼容性

- [x] **Step 4: 跑定向测试并记录结果**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home \
PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH \
mvn -pl yr-framework -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RedisConfigSecurityContractTest test
```

Expected:
- PASS

**Execution Status (2026-04-01 08:54 CST)**

- Status: `Task 1` completed
- Scope note:
  - 本轮严格限制在 Task 1 计划内：只修改 `RedisConfig`、`RedisConfigSecurityContractTest` 与当前 plan 文档本身，没有扩散到 `FastJson2JsonRedisSerializer` 或其它 Redis 基础设施重构
- Live evidence and decision updates:
  - 原计划给出的定向测试命令在当前 reactor 下会先因为 `yr-common` 没有匹配测试而失败；已把 Task 1 的验证命令统一修正为 `-Dsurefire.failIfNoSpecifiedTests=false`
  - `SameUrlDataInterceptor` 真实复用了同一个 `RedisTemplate<Object, Object>`，其 `HashMap<String, Object>` 载荷不仅需要 `HashMap` 本身，还会把 `repeatTime` 以 `java.lang.Long` 类型元信息写入 Redis
  - live red-green 证明：在当前 `FastJson2JsonRedisSerializer.writeValueAsString(t)` 直接按运行时 concrete type（具体类型）写根对象的前提下，只靠 `RedisConfig` 切到 `OBJECT_AND_NON_CONCRETE` 会让根级 `LoginUser/HashMap` 载荷丢失 `@class` 并反序列化失败；因此本 task 最终决策改为“保留 `NON_FINAL` 根类型标记，但把 subtype 白名单收紧为 live 已证实的具体类集合”
- Red-phase evidence:
  - 首次按原文档命令执行：
    - `JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH mvn -pl yr-framework -am -Dtest=RedisConfigSecurityContractTest test`
    - 结果：`BUILD FAILURE`
    - 原因：`yr-common` 先报 `No tests matching pattern "RedisConfigSecurityContractTest" were executed`
  - 修正命令后 red-phase：
    - `JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH mvn -pl yr-framework -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RedisConfigSecurityContractTest test`
    - 结果：`BUILD FAILURE`
    - 首轮失败点：
      - `RedisConfig` 仍保留 `.allowIfSubType("java.lang.")/.allowIfSubType("java.util.")/.allowIfSubType("java.time.")` 宽泛包前缀白名单
      - 当前 `NON_FINAL` 写入会把 `SysUser/SysDept/Date/HashMap` 具体类型元信息直接打进 payload，说明兼容面必须按真实类型来收口
      - 切到 `OBJECT_AND_NON_CONCRETE` 的实验版实现又暴露出根对象 `@class` 丢失与 legacy `Date` payload 反序列化失败，已据此回写 decision 并收敛方案
- Green-phase implementation summary:
  - `RedisConfig.buildRedisTypeValidator()` 从包前缀白名单改为具体类 allow-list：`LoginUser`、`SysUser`、`SysDept`、`LinkedHashSet`、`ArrayList`、`HashMap`、`Date`、`Long`、`SimpleGrantedAuthority`
  - 保留 `DefaultTyping.NON_FINAL`，只用于维持当前 `RedisTemplate<Object, Object>` 根对象与 legacy cache 的可读性；不再把 `java.lang/java.util/java.time` 整段暴露给反序列化
  - `RedisConfigSecurityContractTest` 新增并锁定三类兼容面：登录态 round-trip、legacy `NON_FINAL` payload（含 typed `Date` 与派生 `admin` 字段）回读、`SameUrlDataInterceptor` 的嵌套 `HashMap` 载荷回读
- Final verification:
  - `JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH mvn -pl yr-framework -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=RedisConfigSecurityContractTest test`
  - Result: `BUILD SUCCESS`
  - Summary: `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`

## Task 2: Split SysOrg Write Paths

**Files:**
- Create: `yr-admin/src/main/java/com/yr/web/controller/system/dto/SysOrgCreateRequest.java`
- Create: `yr-admin/src/main/java/com/yr/web/controller/system/dto/SysOrgUpdateRequest.java`
- Create: `yr-admin/src/main/java/com/yr/web/controller/system/dto/SysOrgStatusUpdateRequest.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysOrgController.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysOrgServiceImpl.java`
- Modify: `yr-system/src/main/resources/mapper/system/SysOrgMapper.xml`
- Create: `yr-admin/src/test/java/com/yr/web/controller/system/SysOrgControllerContractTest.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SysOrgServiceImplSafetyTest.java`

- [x] **Step 1: 先用契约测试锁住当前旁路**

目标：
- `changeStatus` 只能转发 `orgId/status/updateBy/updateAt`
- controller 不再接收完整 `SysOrg entity`
- 非法状态/空 `orgId` 要在 controller 层稳定返回受控错误

- [x] **Step 2: 拆 DTO 与专用 service/path**

实现要求：
- `add` / `edit` / `changeStatus` 各自使用不同 request DTO
- `updateOrgStatus` 不得继续走通用 `updateSysOrg`
- `SysOrgMapper.updateSysOrg` 不能再更新 `create_by/create_at`
- 若 `changeStatus` 需要级联启停逻辑，也应由专用方法显式承载

- [x] **Step 3: 跑定向测试**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home \
PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH \
mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysOrgControllerContractTest,SysOrgServiceImplSafetyTest test
```

Expected:
- PASS

**Execution Status (2026-04-01 09:07 CST)**

- Status: `Task 2` completed
- Scope note:
  - 本轮严格按计划边界执行，只收口 `SysOrg add/edit/changeStatus` 的写入对象、状态专用 service/path 与 `updateSysOrg` SQL 的审计字段；没有扩散去重构 `SysOrg` 其它读接口或组织删除逻辑
- Live evidence and decision updates:
  - Task 2 的定向 surefire 命令在当前 reactor 下同样需要 `-Dsurefire.failIfNoSpecifiedTests=false` 才能穿过上游模块拿到真实红灯，因此已把后续 Task 2-6 的定向测试命令统一修正
  - 当前仓库里最贴近的已完成模式来自 `SysUser changeStatus` 与 `SsoClient changeStatus`：controller 层使用最小 DTO + `@Validated` 返回受控 `400`，service 层走专用状态方法；Task 2 已按这一模式对齐
- Red-phase evidence:
  - 首次按原文档命令执行：
    - `JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH mvn -pl yr-admin,yr-system -am -Dtest=SysOrgControllerContractTest,SysOrgServiceImplSafetyTest test`
    - 结果：`BUILD FAILURE`
    - 原因：`yr-common` 先报 `No tests matching pattern "SysOrgControllerContractTest, SysOrgServiceImplSafetyTest" were executed`
  - 修正命令后 red-phase：
    - `JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysOrgControllerContractTest,SysOrgServiceImplSafetyTest test`
    - 结果：`BUILD FAILURE`
    - 首轮失败点：
      - `SysOrgServiceImpl.updateOrgStatus` 仍直接委托 `updateSysOrg(sysOrg)`
      - `SysOrgMapper.xml` 的 `updateSysOrg` 仍允许更新 `create_by/create_at`
      - controller 红灯虽然尚未跑到 `yr-admin`，但新契约测试已经锁住空 `orgId` / 空或非法 `status` 必须返回 `400`，以及 `changeStatus` 不能透传 `orgCode/orgName/parentId/orderNum/leader/remark/createBy/createAt`
- Green-phase implementation summary:
  - 新增 `SysOrgCreateRequest`、`SysOrgUpdateRequest`、`SysOrgStatusUpdateRequest`，并在 `SysOrgController` 中分别构造最小 `SysOrg` 写入对象
  - `changeStatus` 入口改为 `@Validated` DTO，请求只允许 `orgId/status`，并由 controller 写入 `updateBy`
  - `SysOrgServiceImpl.updateOrgStatus` 改为专用路径：读取当前组织、校验停用时子组织约束、补 `updateAt`、调用专用 `sysOrgMapper.updateOrgStatus`，并显式承载“禁用转启用时恢复父组织状态”的级联逻辑
  - `SysOrgMapper.updateSysOrg` 已移除 `create_by/create_at` 更新，新增 `updateOrgStatus` 专用 SQL 只写 `status/update_by/update_at`
- Final verification:
  - `JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysOrgControllerContractTest,SysOrgServiceImplSafetyTest test`
  - Result: `BUILD SUCCESS`
  - Summary: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`

## Task 3: Lock SysUserOrg Write Boundary

**Files:**
- Create: `yr-admin/src/main/java/com/yr/web/controller/system/dto/AddUserOrgRequest.java`
- Create: `yr-admin/src/main/java/com/yr/web/controller/system/dto/ChangeUserOrgEnabledRequest.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysUserOrgController.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysUserOrgServiceImpl.java`
- Create: `yr-admin/src/test/java/com/yr/web/controller/system/SysUserOrgControllerContractTest.java`
- Modify or Create: `yr-system/src/test/java/com/yr/system/service/impl/SysUserOrgServiceImplTest.java`

- [x] **Step 1: 用测试锁住“不允许透传 isDefault/enabled/audit 字段”**

目标：
- `POST /system/user-org` 只允许 `userId/orgId`
- `PUT /changeEnabled` 只允许 `id/enabled`
- `setDefaultUserOrg` 仍保留专用入口语义

- [x] **Step 2: 落地最小写对象**

实现要求：
- 新增时服务端显式设置默认 `enabled/isDefault`
- 不得继续把客户端传入的完整 entity 直接 `save`
- 若历史前端依赖默认启用行为，要在服务端统一兜底并补注释

- [x] **Step 3: 跑定向测试**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home \
PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH \
mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysUserOrgControllerContractTest,SysUserOrgServiceImplTest,SysUserOrgServiceImplTransactionTest test
```

Expected:
- PASS

**Execution Status (2026-04-01 09:14 CST)**

- Status: `Task 3` completed
- Scope note:
  - 本轮只收口 `SysUserOrg add/changeEnabled/setDefault` 这一组写边界，没有扩散去重构分页查询、当前用户组织查询或关系表 mapper 结构
- Live evidence and decision updates:
  - `changeEnabled` 当前虽然只把 `id/enabled` 传给 service，但 controller 仍接收完整 `SysUserOrg entity`，因此 Task 3 不只是“service 兜底”，也必须把 controller 输入契约同步收口成专用 DTO
  - 仓库内最贴近的现成模式来自 `SysUser changeStatus` 与 `SsoClient changeStatus`：controller 侧 `@Validated` + 最小 DTO，service 侧专用入口；Task 3 已按这个模式对齐
- Red-phase evidence:
  - 定向命令：
    - `JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysUserOrgControllerContractTest,SysUserOrgServiceImplTest,SysUserOrgServiceImplTransactionTest test`
    - 结果：`BUILD FAILURE`
    - 首轮失败点：
      - `addSysUserOrg` 仍直接 `save(sysUserOrg)`，导致客户端传入的 `enabled/isDefault` 原样下沉
      - `changeEnabledById` 对非法 `enabled=2` 不抛受控错误
      - controller 红灯虽尚未跑到 `yr-admin`，但契约测试已经锁住新增入口必须只接收 `userId/orgId`、`changeEnabled` 必须对缺少 `id` 或非法 `enabled` 返回 `400`
- Green-phase implementation summary:
  - 新增 `AddUserOrgRequest` 与 `ChangeUserOrgEnabledRequest`，`SysUserOrgController` 不再绑定完整 `SysUserOrg`
  - `addSysUserOrg` controller 只构造 `userId/orgId`，service 统一重建关系对象并显式兜底 `enabled=1/isDefault=0`
  - `changeEnabledById` 在 controller 层通过 DTO 校验空值/非法值，在 service 层再补 `enabled` allowed value（允许值）兜底
  - 原有 `setDefaultUserOrg` 事务边界保持不变，并由既有事务测试继续覆盖
- Final verification:
  - `JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysUserOrgControllerContractTest,SysUserOrgServiceImplTest,SysUserOrgServiceImplTransactionTest test`
  - Result: `BUILD SUCCESS`
  - Summary: `Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`

## Task 4: Fix SysDept Org Invariant

**Files:**
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysDeptController.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SysDeptServiceImpl.java`
- Create or Modify: `yr-system/src/test/java/com/yr/system/service/impl/SysDeptServiceImplSafetyTest.java`
- Create: `yr-admin/src/test/java/com/yr/web/controller/system/SysDeptControllerContractTest.java`

- [x] **Step 1: 先补跨组织红灯**

目标：
- 新增部门时，无论请求体是否带 `orgId`，最终都必须与 `parentId` 所属组织一致
- 发现不一致时要么 fail-fast（快速失败），要么直接强制派生为父部门组织；二选一后保持全链路一致

- [x] **Step 2: 收敛实现**

实现要求：
- 服务端必须以 parent dept（父部门）为组织归属真值来源
- 如果要继续保留 entity 绑定，至少在 service 层覆盖掉请求体里的 `orgId`
- 推荐顺手为 `add` / `edit` 补最小 DTO，避免未来继续扩散字段

- [x] **Step 3: 跑定向测试**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home \
PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH \
mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysDeptControllerContractTest,SysDeptServiceImplSafetyTest,TreeParentValidationContractTest test
```

Expected:
- PASS

**Execution Status (2026-04-01 09:17 CST)**

- Status: `Task 4` completed
- Scope note:
  - 本轮按计划做最小修正，只处理“新增部门时 orgId 以谁为准”这一条不变量，没有顺手把 `SysDept` 整体切成 DTO 体系
- Live evidence and decision updates:
  - controller 当前真正妨碍不变量收口的不是“透传字段太多”，而是它会在 `orgId == null` 时用当前登录组织回填请求值；因此 Task 4 需要同时改 controller 和 service，前者停止注入登录组织，后者无条件按父部门派生 `orgId`
  - 现有 `TreeParentValidationContractTest` 已覆盖父节点缺失和换父节点时祖级链路重写，Task 4 只需要新增“父组织覆盖请求 orgId”这一条 safety 契约即可，不必另起一套树形测试
- Red-phase evidence:
  - 定向命令：
    - `JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysDeptControllerContractTest,SysDeptServiceImplSafetyTest,TreeParentValidationContractTest test`
    - 结果：`BUILD FAILURE`
    - 首轮失败点：
      - `SysDeptServiceImpl.insertDept` 在请求体自带 `orgId=999` 时仍把它原样写入，而不是覆盖成父部门 `orgId`
      - `SysDeptController.add` 也会在 `orgId == null` 时回填当前登录组织，说明 controller 正在和 service 的父部门真值规则互相打架
- Green-phase implementation summary:
  - `SysDeptController.add` 已移除基于 `SecurityUtils.getOrgId()` 的 controller 层回填，只保留 `createBy` 审计字段写入
  - `SysDeptServiceImpl.insertDept` 已改为无条件 `dept.setOrgId(parent.getOrgId())`，确保父部门组织归属始终是真值来源
  - 新增 `SysDeptControllerContractTest` 锁定 controller 不再回填登录组织，`SysDeptServiceImplSafetyTest` 锁定 service 会覆盖请求 `orgId`
- Final verification:
  - `JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysDeptControllerContractTest,SysDeptServiceImplSafetyTest,TreeParentValidationContractTest test`
  - Result: `BUILD SUCCESS`
  - Summary: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`

## Task 5: Harden Login And Exception Operational Contracts

**Files:**
- Modify: `yr-framework/src/main/java/com/yr/framework/web/exception/GlobalExceptionHandler.java`
- Modify: `yr-common/src/main/java/com/yr/common/core/domain/model/LoginBody.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/system/SysLoginController.java`
- Modify: `yr-admin/src/main/java/com/yr/web/controller/auth/WxworkAuthController.java`
- Modify: `yr-admin/src/main/java/com/yr/framework/web/service/SysLoginService.java`
- Create or Modify: `yr-framework/src/test/java/com/yr/framework/web/exception/GlobalExceptionHandlerContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/login/SysLoginControllerContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/login/WxworkLoginControllerContractTest.java`
- Modify: `yr-admin/src/test/java/com/yr/framework/web/service/SysLoginServiceWxworkStateContractTest.java`

Live evidence refresh（2026-04-01 09:36 CST）：
- `yr-admin/src/main/java/com/yr/framework/web/service/SysLoginService.java:431-435` 仍在 `requestWxworkJson` 内每次直接 `restTemplateBuilder.build().getForObject(...)`，说明 Task 5 的 timeout（超时）/client reuse（客户端复用）问题确实属于当前 live code，而不是附加优化项
- `yr-framework/src/main/java/com/yr/framework/web/exception/GlobalExceptionHandler.java:61-83`、`yr-common/src/main/java/com/yr/common/core/domain/model/LoginBody.java:11-114`、`yr-admin/src/main/java/com/yr/web/controller/system/SysLoginController.java:57-68`、`yr-admin/src/main/java/com/yr/web/controller/auth/WxworkAuthController.java:48-52` 共同确认：异常日志脱敏、controller-level Bean Validation 与 WeCom 登录稳健性都仍是同一 task 的 live surface

Decision update：
- Task 5 的 red/green 验证必须补上 `SysLoginServiceWxworkStateContractTest`，否则计划里的定向命令无法覆盖“显式 timeout + 单个客户端构造复用”这一条既定整改目标
- 在“不扩散到计划外 DTO 重构”的边界下，优先尝试在 `LoginBody` 上使用 validation groups（校验分组）同时支撑普通登录与企业微信登录；只有实测证明同一个 POJO 无法兼顾时，才退回拆分 request DTO

- [x] **Step 1: 锁定异常日志与登录请求校验红灯**

目标：
- 未知异常日志不得继续原样打印敏感 message
- `/login` 与 `/auth/wxwork/login` 的空字段要稳定返回 `400` 契约
- 企业微信登录链路必须有 red test（红灯测试）证明当前仍缺显式 timeout 与客户端复用

- [x] **Step 2: 落地实现**

实现要求：
- `GlobalExceptionHandler` 对未知异常日志改为固定模板 + throwable，不拼接原始 message
- `LoginBody` 拆分或补 Bean Validation；若同一个 POJO 难以兼顾普通登录与微信登录，可改为两个 request DTO
- 为 WeCom HTTP 调用补显式 connect/read timeout，并复用单个客户端构造方式

- [x] **Step 3: 跑定向测试**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home \
PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH \
mvn -pl yr-framework,yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=GlobalExceptionHandlerContractTest,SysLoginControllerContractTest,WxworkLoginControllerContractTest,SysLoginServiceWxworkStateContractTest test
```

Expected:
- PASS

**Execution Status (2026-04-01 09:34 CST)**

- Status: `Task 5` completed
- Scope note:
  - 本轮按计划做最小整改，没有把 `LoginBody` 再拆成多个 request DTO，而是用 validation groups（校验分组）收口普通登录与企业微信登录两条路径
- Live evidence and decision updates:
  - 计划里的 `yr-framework + yr-admin` 联合定向命令在 red-phase（红灯阶段）会先被 `yr-framework` 的日志脱敏失败短路，因此为了把 controller/service 两类失败面看全，本轮补跑了 `yr-admin` 专用定向命令并把结果一并纳入 Task 5 证据
  - `SysLoginService` 的首版登录成功路径红灯测试会被 `AsyncManager` 静态初始化噪音抢占失败位；基于 live 代码复核后，decision（决策）更新为把 timeout/client reuse 契约直接锁在 `requestWxworkJson` 两次调用上，确保失败原因只落在本 task 的整改目标
- Red-phase evidence:
  - 定向命令 1：
    - `JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH mvn -pl yr-framework,yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=GlobalExceptionHandlerContractTest,SysLoginControllerContractTest,WxworkLoginControllerContractTest,SysLoginServiceWxworkStateContractTest test`
    - 结果：`BUILD FAILURE`
    - 首轮失败点：
      - `GlobalExceptionHandlerContractTest.shouldNotLogUnexpectedExceptionMessageVerbatim` 证明 `handleException` 仍会把 `jdbc password leaked` 原样打进日志
  - 定向命令 2：
    - `JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH mvn -pl yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SysLoginControllerContractTest,WxworkLoginControllerContractTest,SysLoginServiceWxworkStateContractTest test`
    - 结果：`BUILD FAILURE`
    - 首轮失败点：
      - `SysLoginControllerContractTest` 证明 `/login` 对空 `username/password` 仍返回 `200`
      - `WxworkLoginControllerContractTest` 证明 `/auth/wxwork/login` 对空 `code/state` 仍返回 `200`
      - `SysLoginServiceWxworkStateContractTest` 证明 `requestWxworkJson` 既未调用 `setConnectTimeout` / `setReadTimeout`，又在两次请求中执行了两次 `restTemplateBuilder.build()`
- Green-phase implementation summary:
  - `GlobalExceptionHandler` 已把 `NoHandlerFound` / `AccessDenied` / `AccountExpired` / `UsernameNotFound` / `Exception` 统一改成固定日志模板，避免继续拼接原始异常 message
  - `LoginBody` 已新增 `PasswordLoginValidation` / `WxworkLoginValidation` 分组，并为 `username/password/code/state` 增加 `@NotBlank`
  - `SysLoginController` 与 `WxworkAuthController` 已分别对两条登录入口接入分组校验，使非法请求在 controller 层稳定返回 `400 Bad Request`
  - `SysLoginService` 已补 5 秒 connect/read timeout，并通过懒加载单例 `RestTemplate` 复用 WeCom HTTP 客户端
- Final verification:
  - `JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH mvn -pl yr-framework,yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=GlobalExceptionHandlerContractTest,SysLoginControllerContractTest,WxworkLoginControllerContractTest,SysLoginServiceWxworkStateContractTest test`
  - Result: `BUILD SUCCESS`
  - Summary: `Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`

## Task 6: Close Remaining Hygiene And Traceability Gaps

**Files:**
- Modify: `yr-common/src/main/java/com/yr/common/core/domain/entity/SsoClient.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SsoSyncTaskServiceImpl.java`
- Modify: `yr-system/src/main/java/com/yr/system/service/impl/SsoIdentityImportServiceImpl.java`
- Create or Modify: `yr-system/src/test/java/com/yr/system/service/impl/SsoClientServiceImplValidationContractTest.java`
- Create: `yr-system/src/test/java/com/yr/system/service/impl/SsoSyncTaskRetrySafetyTest.java`
- Modify or Create: `yr-system/src/test/java/com/yr/system/service/impl/SsoIdentityImportServiceImplTest.java`

Live evidence refresh（2026-04-01 09:39 CST）：
- `yr-common/src/main/java/com/yr/common/core/domain/entity/SsoClient.java:16-39` 仍使用 Lombok `@Data`，live 代码没有显式排除 `clientSecret` 出 `toString`
- `yr-system/src/main/java/com/yr/system/service/impl/SsoSyncTaskServiceImpl.java:161-170` 仍会在没有失败态校验的前提下直接把任务翻到 `RUNNING`，说明 retry safety（重试安全）仍是真实 open gap，而不是文档遗留描述
- `yr-system/src/main/java/com/yr/system/service/impl/SsoIdentityImportServiceImpl.java:461-479` 仍显示 `resolveOperator(task)` 已读取 `createBy`，但 `resolveOperatorUserId(task)` 继续硬编码返回 `"1"`
- `yr-system/src/test/java/com/yr/system/service/impl/SsoClientServiceImplValidationContractTest.java` 与 `yr-system/src/test/java/com/yr/system/service/impl/SsoIdentityImportServiceImplTest.java` 已经存在，因此 Task 6 的测试工作应优先复用并扩展现有测试面；截至 `2026-04-01 09:40 CST`，`yr-system/src/test/java/com/yr/system/service/impl/SsoSyncTaskRetrySafetyTest.java` 也已补成红灯测试，当前待完成的是 production hardening（生产代码硬化）与最终验证

Decision update：
- Task 6 不需要新起重复测试文件来覆盖 `SsoClient` 与 `SsoIdentityImport`；优先扩展现有 `SsoClientServiceImplValidationContractTest` 和 `SsoIdentityImportServiceImplTest`，并把新增的 `SsoSyncTaskRetrySafetyTest` 作为 retry safety（重试安全）专用红绿灯载体
- `retryTask` 的整改目标细化为两层：先显式拒绝非 `FAILED` 状态重试；再在可行范围内补状态条件更新，避免并发下重复把同一任务翻回 `RUNNING`
- `resolveOperatorUserId(task)` 的整改目标保持最小化：优先解析 `task.getCreateBy()` 为真实操作人 ID，解析失败时再 fallback（回退）到兼容值，避免扩散成全链路账号体系重构

- [x] **Step 1: 收口敏感字段输出**

目标：
- `SsoClient.toString()` 不再包含 `clientSecret`
- 明确区分“当前外部接口已安全”与“内部模型仍需硬化”

- [x] **Step 2: 收口 retry 幂等与导入审计**

实现要求：
- `retryTask` 只允许在明确失败态重试
- 若可行，增加版本/状态条件更新，避免并发重复重试
- `resolveOperatorUserId(task)` 优先从 `task.getCreateBy()` 解析真实操作人，失败时才 fallback

- [x] **Step 3: 跑定向测试并补全量验证**

Run:

```bash
cd /Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be
JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home \
PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH \
mvn -pl yr-system,yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoClientServiceImplValidationContractTest,SsoSyncTaskRetrySafetyTest,SsoIdentityImportServiceImplTest test

JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home \
PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH \
mvn test
```

Expected:
- PASS
- 全量基线保持 `Tests run` 不回退

**Execution Status (2026-04-01 09:45 CST)**

- Status: `Task 6` completed
- Scope note:
  - 本轮按计划做最低必要整改，没有扩散到 `SsoClient` 之外的模型重写，也没有把 `SsoSyncTask` 引入新的版本字段体系；幂等保护仅收敛到失败态校验与状态条件更新
- Live evidence and decision updates:
  - live 复核确认 `SsoClient.toString`、`retryTask` 和 `resolveOperatorUserId(task)` 三条问题都仍真实存在，因此 Task 6 保持原边界不变
  - 在 red-phase（红灯阶段）里，`SsoSyncTaskRetrySafetyTest` 已新增并纳入文档；因此 Task 6 的测试面调整为“扩展既有 `SsoClient` / `SsoIdentityImport` 测试 + 新增专用 `retry` 安全测试”
  - 全量 `mvn test` 暴露出仓库内既有 `SsoSyncTaskServiceImplTest` / `SsoSyncTaskFailurePersistenceTest` 仍假设旧的 `retryTask` 行为；这些测试已按同一条计划内契约同步收敛，属于 Task 6 的必需回归修正，而不是额外重构
- Red-phase evidence:
  - 定向命令：
    - `JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH mvn -pl yr-system,yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoClientServiceImplValidationContractTest,SsoSyncTaskRetrySafetyTest,SsoIdentityImportServiceImplTest test`
    - 结果：`BUILD FAILURE`
    - 首轮失败点：
      - `SsoClientServiceImplValidationContractTest` 证明 `SsoClient.toString()` 仍直接输出 `secret-123`
      - `SsoSyncTaskRetrySafetyTest` 证明 `retryTask` 在非失败态/状态漂移时没有给出受控错误，而是继续掉进执行器链路
      - `SsoIdentityImportServiceImplTest` 证明关系表 `insertInitImport` 的 `operatorUserId` 仍固定为 `1L`，没有使用 `createBy=9527`
- Green-phase implementation summary:
  - `SsoClient` 已通过 `@ToString(exclude = "clientSecret")` 收口实体调试输出
  - `SsoSyncTaskServiceImpl.retryTask` 已新增失败态校验，并通过 `status = FAILED` 条件更新把任务翻回 `RUNNING`，避免并发重复重试
  - `SsoIdentityImportServiceImpl.resolveOperatorUserId` 已改成“优先解析数字型 `createBy`，否则回退到默认兼容 ID”
  - 受影响的既有 `retryTask` 回归测试也已同步到新契约，确保局部修正不会在全量回归中被旧预期打回
- Final verification:
  - 定向命令：
    - `JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH mvn -pl yr-system,yr-admin -am -Dsurefire.failIfNoSpecifiedTests=false -Dtest=SsoClientServiceImplValidationContractTest,SsoSyncTaskRetrySafetyTest,SsoIdentityImportServiceImplTest test`
    - Result: `BUILD SUCCESS`
    - Summary: `Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`
  - 全量命令：
    - `JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home PATH=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home/bin:$PATH mvn test`
    - Result: `BUILD SUCCESS`
    - Summary: `Tests run: 94, Failures: 0, Errors: 0, Skipped: 5`

## Completion Criteria

- `RedisConfig` 不再保留宽泛包级多态白名单
- `SysOrg` / `SysUserOrg` / `SysDept` 的写入边界全部变成“最小 DTO + 专用 service/path”
- 登录入口与未知异常日志达到更符合 Spring 官方 best practice 的契约
- `SsoClient` / `SsoSyncTask` / `SsoIdentityImport` 的低优先级卫生项完成
- `mvn test` 全量通过
- README 的 canonical handoff 与最新 plan 保持一致

## Recommended Continuation Prompt

请以 `/Users/PopoY/workingFiles/Projects/SAM/sso/sam-sso-be` 为 `cwd`，汇总当前本地改动并准备进入 merge/finalization（合并/收尾）阶段：先基于 `docs/review_plans/2026-04-01-sso-backend-fourth-pass-best-practice-audit-remediation-plan.md` 确认 `Task 1` 到 `Task 6` 都已完成，再整理验证结果、README/plan handoff（交接）一致性，以及是否要执行 `git add/commit/push` 或继续做下一轮增量 audit（审计）。
