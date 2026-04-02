# sam-sso-be

## 项目定位

`sam-sso-be` 是当前 `SSO`（单点登录）登录中心 backend（后端）工程。到 `2026-04-02` 的二期边界，仓库重点承载三类能力：

- `Auth Access Protocol`（认证接入协议）：`/auth/authorize` 浏览器跳转与 `/auth/exchange` 服务端换票
- `Client Governance`（客户端接入治理）：`client`（客户端）白名单、登录方式、密钥轮换与接入说明
- `Sync Observability`（同步观测）：`sync-task`（同步任务）客户端维度摘要、批次定位与补偿入口

当前边界 **明确不扩展** 到统一 `RBAC`（角色权限）、统一 `workflow`（工作流）或由登录中心直接替下游系统签发最终业务 `JWT`（令牌）。

## 当前构建模块

- `yr-admin`：Spring Boot 启动入口与控制器层
- `yr-framework`：`Spring Security`（安全框架）、拦截器、配置等基础设施
- `yr-system`：`SSO` 主数据、客户端治理、同步任务与认证接入协议核心服务
- `yr-common`：公共工具、通用实体与响应结构
- `yr-quartz`：定时任务能力

## 环境要求

当前构建基线为 `JDK 17 + Spring Boot 2.7.18`。

- `JDK 17`
- `Maven 3.9+`
- `Redis`（缓存）：二期一次性 `code`（授权码）当前落在 `Redis`，默认 `TTL`（生存时间）为 `300` 秒

执行 Maven 命令前必须先切到 `JDK 17`：

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -v
```

若 `mvn -v` 仍显示 `Java version: 1.8.x`，说明当前 shell（终端）还没切到 `JDK 17`。

## 常用命令

```bash
# 构建启动模块及依赖
mvn -pl yr-admin -am package

# 本地启动
mvn -pl yr-admin -am spring-boot:run

# 回放二期后端定向验收
mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=SsoAuthorizeControllerContractTest,SsoCodeExchangeControllerContractTest,SsoAuthorizationCodeServiceImplTest,SsoSecuritySurfaceContractTest,SsoClientControllerContractTest,SsoClientIntegrationGuideContractTest,SsoSyncTaskControllerContractTest,SsoSyncTaskServiceImplTest \
  test
```

## Phase 2 Access Integration Runbook

### 1. 协议边界

当前二期接入协议固定为：

1. 下游系统把浏览器导向 `GET /auth/authorize`
2. `SSO` 校验 `clientCode + redirectUri + state`
3. 若用户尚未登录，`SSO` 先 `302` 到现有 `/login?redirect=...`
4. 登录成功后，`SSO` 生成一次性 `code` 并 `302` 回下游回调地址
5. 下游系统服务端调用 `POST /auth/exchange`
6. `SSO` 返回标准化 `identity payload`（身份载荷）
7. 下游系统基于该身份载荷签发自己的本地 `JWT`

边界强调：

- `SSO` **只返回 identity payload，不直接返回下游业务 token**
- 一次性 `code` 当前使用 `Redis getAndDelete` 原子消费，只能成功换取一次
- 下游系统继续保留本地 `role / menu / permission / workflow`

### 2. 接入前检查

在 `client`（客户端）控制台完成以下准备后，再交付给下游系统联调：

- `client` 处于启用态：`status = 0`
- `redirectUris`（回调地址白名单）已配置，且为合法 `http/https` 地址
- 至少启用一种登录方式：`allowPasswordLogin = Y` 或 `allowWxworkLogin = Y`
- 若该系统需要主数据镜像同步，则 `syncEnabled = Y`
- 已通过“新增客户端”或“轮换密钥”拿到一次性明文 `clientSecret`

### 3. 浏览器授权跳转

#### 请求

- 方法：`GET`
- 路径：`/auth/authorize`
- 查询参数：
  - `clientCode`：客户端编码
  - `redirectUri`：当前回调地址，必须命中白名单
  - `state`：下游系统自带防重放参数，不能为空

示例：

```text
/auth/authorize?clientCode=sam-mgmt&redirectUri=https%3A%2F%2Fdownstream.example.com%2Fcallback&state=demo-state
```

#### 行为

- 匿名访问：返回 `302` 到 `/login?redirect=...`
- 已登录访问：返回 `302` 到合法 `redirectUri`
- 只有成功链路才会发生 `302` 回跳；若参数校验或业务校验失败，当前实现不会带着 `error` 参数回跳下游，而是由 `SSO` 直接返回受控 JSON 错误响应

#### 回调参数

成功回调至少包含：

- `code`：一次性授权码
- `state`：原样透传的下游防重放参数

补充说明：

- 如果回调地址本身已经带有同值 `state`，当前实现只追加 `code`，不会重复追加第二个 `state`
- 当前一次性 `code` 默认 `TTL = 300s`

### 4. 服务端换票

#### 请求

- 方法：`POST`
- 路径：`/auth/exchange`
- 请求体：

```json
{
  "clientCode": "sam-mgmt",
  "clientSecret": "<创建或轮换后获取的一次性明文>",
  "code": "<浏览器回跳得到的一次性code>"
}
```

#### 响应

当前 `exchange` 返回 `AjaxResult`（通用响应壳），真实对接应以 `data` 字段为最终契约：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "exchangeId": "exchange-001",
    "traceId": "trace-001",
    "clientCode": "sam-mgmt",
    "userId": 9,
    "username": "phase2-user",
    "nickName": "Phase 2 User",
    "orgId": 18,
    "orgName": "Youngron",
    "deptId": 108,
    "deptName": "IAM"
  }
}
```

注意：

- 当前 `/auth/exchange` **不会** 返回下游业务 `token`
- `client` 控制台里的 `identity payload fields`（身份载荷字段）更偏向对接 checklist（清单）；真实接口契约请以 `/auth/exchange` 响应为准

### 5. 失败语义

当前已锁定的最小失败语义如下：

- `state不能为空`：`/auth/authorize` 缺少 `state`
- `clientCode不能为空`：授权跳转或换票缺少 `clientCode`
- `redirectUri不能为空`：授权跳转缺少回调地址
- `clientSecret不能为空`：换票缺少 `clientSecret`
- `code不能为空`：换票缺少一次性授权码
- `clientCode无效或客户端不存在`：客户端不存在或已停用
- `redirectUri不在白名单内`：当前回调地址未命中 `redirectUris`
- `state不匹配`：回调地址内已带冲突 `state`
- `授权码无效或已过期`：一次性 `code` 不存在、已过期、已消费或缓存载荷不可解析
- `clientCode与授权码不匹配`：当前换票客户端与授权码归属客户端不一致
- `clientSecret无效`：换票时密钥不匹配

当前错误码契约需要一起看 `HTTP status`（HTTP 状态码）与 `AjaxResult.code`（响应码）：

- 参数校验失败：
  - `HTTP status = 400`
  - `AjaxResult.code = 400`
  - 典型场景：`clientCode不能为空`、`redirectUri不能为空`、`state不能为空`、`clientSecret不能为空`、`code不能为空`
- 业务校验失败（当前 `SSO` access protocol 未单独定义 machine-readable sub-code（机器可读子错误码）时）：
  - `HTTP status = 200`
  - `AjaxResult.code = 500`
  - 典型场景：`clientCode无效或客户端不存在`、`redirectUri不在白名单内`、`state不匹配`、`授权码无效或已过期`、`clientSecret无效`
- 成功换票：
  - `HTTP status = 200`
  - `AjaxResult.code = 200`

当前协议 **没有** 额外定义独立的 machine-readable error code（机器可读错误码）表，因此下游系统若需要自动化分支处理，应暂时组合使用：

1. `HTTP status`
2. `AjaxResult.code`
3. `AjaxResult.msg`

失败落地行为补充说明：

- `/auth/authorize` 失败时，不会重定向到下游 `redirectUri`，而是直接由 `SSO` 返回受控 JSON 错误
- `/auth/exchange` 失败时，同样直接返回受控 JSON 错误，不会返回任何下游业务 `JWT`
- 如果下游系统希望在浏览器侧展示更友好的失败页，应由自己的授权入口页处理 `SSO` 返回的错误响应，而不是假设回调地址一定会收到 `error` 查询参数

### 6. 下游系统本地 JWT 建议

推荐下游系统按以下顺序落地本地 `JWT`：

1. 浏览器回到下游系统回调地址后，先校验本地保存的 `state`
2. 由下游系统服务端调用 `/auth/exchange`
3. 用 `exchangeId / traceId / userId` 建立登录审计与追踪
4. 结合下游本地用户、组织镜像与本地权限模型签发本地 `JWT`
5. 后续业务接口继续只依赖下游本地 `JWT`，不要把运行时授权判断回退到登录中心

### 7. 最小验收建议

二期后端最少要回放以下三组能力：

- `authorize` 控制器契约：匿名重定向登录页、已登录回跳下游回调
- `exchange` 控制器契约：参数校验、只返回标准化身份载荷
- `authorization code service`（授权码服务）：白名单、一次性消费、过期与密钥校验

推荐直接执行：

```bash
export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -pl yr-admin,yr-system -am -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=SsoAuthorizeControllerContractTest,SsoCodeExchangeControllerContractTest,SsoAuthorizationCodeServiceImplTest,SsoSecuritySurfaceContractTest,SsoClientControllerContractTest,SsoClientIntegrationGuideContractTest,SsoSyncTaskControllerContractTest,SsoSyncTaskServiceImplTest \
  test
```

## 文档入口

- 当前二期 wrapper docs（包装层文档）执行入口：`docs/superpowers/plans/2026-04-02-sso-phase2-implementation-plan.md`
- 当前二期方向设计文档：`docs/superpowers/specs/2026-04-02-sso-phase2-direction-design.md`
- 若只看 backend（后端）接入协议，请优先阅读本 README 的 `Phase 2 Access Integration Runbook`
