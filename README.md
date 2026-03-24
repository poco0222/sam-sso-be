# sam-project-be-empty

## 项目说明

该仓库是从既有多模块后端工程中剥离业务模块后保留下来的空壳模板，
用于后续新项目快速复用基础平台能力。

当前构建基线为 `JDK 17 + Spring Boot 2.7.18`。其中 `yr-system` 是当前阶段重点校验模块，执行 Maven 命令前必须先切换到 JDK 17。

## 当前保留模块

- `yr-admin`：Spring Boot 启动入口
- `yr-framework`：安全、拦截器、配置等框架层能力
- `yr-system`：组织架构、用户、角色、菜单、字典、参数、日志等系统管理能力
- `yr-common`：公共工具、通用实体与基础响应结构
- `yr-quartz`：定时任务能力
- `yr-activiti7`：工作流基础能力

## 已剔除模块

- `sam-erp`
- `sam-yrda`
- `sam-plc`
- `sam-spots`
- `yr-generator`
- `yr-demo`

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
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home
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
- 详细设计与实施计划见 `docs/plans/` 目录
- `yr-system` Phase 0 验收记录见 `docs/review_plans/2026-03-16-yr-system-phase-0-baseline.md`
