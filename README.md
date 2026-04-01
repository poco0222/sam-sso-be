# sam-sso-be

## 项目说明

该仓库是当前一期 `SSO`（单点登录）后端工程，边界聚焦在 `user/org/dept` 主数据、同步任务、`RocketMQ`（消息队列）分发与基础认证能力。

当前构建基线为 `JDK 17 + Spring Boot 2.7.18`。其中 `yr-system` 是当前阶段重点校验模块，执行 Maven 命令前必须先切换到 JDK 17。

## 当前构建模块

- `yr-admin`：Spring Boot 启动入口
- `yr-framework`：安全、拦截器、配置等框架层能力
- `yr-system`：`SSO` 主数据、同步任务与系统管理核心能力
- `yr-common`：公共工具、通用实体与基础响应结构
- `yr-quartz`：定时任务能力

## 最新边界基线

- `yr-framework` 显式承载 `Spring Security`、`Redis`、`JWT` 等安全基础设施，不再直接依赖 `yr-system`
- `yr-common` 已移除 `spring-boot-starter-security`、`spring-boot-starter-data-redis`、`mybatis-plus-boot-starter`、`rocketmq-spring-boot-starter` 等重量 starter（启动器）
- `yr-admin` 与 `yr-quartz` 按消费关系显式依赖 `yr-system`，不再通过 `yr-framework` 的传递依赖间接拿业务 Bean
- 当前全仓标准验证入口为 `mvn test`

## 当前未纳入构建范围

- `sam-erp`
- `sam-yrda`
- `sam-plc`
- `sam-spots`
- `yr-generator`
- `yr-demo`
- `yr-activiti7`

## 构建前检查

先确认 Maven 当前绑定的 Java 版本：

```bash
mvn -v
```

如果输出仍然类似下面这样，说明当前 shell 还停留在 JDK 8：

```text
Java version: 1.8.x
```

## `yr-system` 标准验证入口

请在当前终端先设置 JDK 17，再执行模块测试：

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-system test
```

预期结果：

```text
BUILD SUCCESS
```

## 常用命令

```bash
# 构建启动模块及依赖
mvn -pl yr-admin -am package

# 本地启动
mvn -pl yr-admin -am spring-boot:run

# 验证 yr-system
mvn -pl yr-system test
```

## 常见错误

如果在 JDK 8 下直接执行 Maven，常见症状是编译阶段报错：

```text
无效的标记: --release
```

这表示 Maven 读取到了仓库里配置的 Java 17 编译参数，但当前实际运行的 `javac` 仍是 JDK 8。处理方式不是修改 `pom.xml`，而是先把 `JAVA_HOME` 和 `PATH` 切到 JDK 17 后重新执行。

## 说明

- 数据库脚本本次未作为模板收敛依据，需要后续按新项目实际情况自行整理
- `SSO` backend 当前最新执行入口见 `docs/review_plans/2026-03-31-sso-backend-comprehensive-best-practice-audit-remediation-plan.md`
- 上述 comprehensive audit remediation plan 是当前 canonical entrypoint（权威执行入口）；新开对话时应从该文档的 `Task 1` 顺序执行
- `docs/review_plans/2026-03-30-sso-backend-second-pass-gap-remediation-plan.md` 仅保留上一轮最新收口快照，不再作为当前执行入口
- `docs/review_plans/2026-03-30-sso-backend-third-pass-fresh-review-remediation-plan.md` 仅保留上一轮 fresh review 快照，不再作为当前执行入口
- `docs/review_plans/2026-03-30-sso-backend-tail-closure-remediation-plan.md` 仅保留上一轮收口快照，不再作为当前执行入口
- `docs/review_plans/2026-03-27-sso-backend-best-practice-audit-remediation-overview.md` 仅保留历史快照，不再作为当前执行入口
- `yr-system` Phase 0 验收记录见 `docs/review_plans/2026-03-16-yr-system-phase-0-baseline.md`
