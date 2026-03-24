# yr-system 站内消息异步化设计

## 背景

当前仓库已经具备 `threadPoolTaskExecutor` 线程池 Bean，但尚未启用 Spring `@Async` 能力。此前 `yr-system` 中曾通过 `async()` 风格接口尝试表达“异步发送”，但该实现并不符合 Spring 官方异步代理模型，也无法提供真正的后台执行能力。

本轮只覆盖 `yr-system` 的站内消息发送链路，不扩展到项目内其它模块或其它推送场景。

## 目标

为 `DefaultMessageClientImpl -> WebMessageServiceImpl -> IWebSocketService/WebSocketServerImpl` 这条站内消息链路引入真正的 Spring `@Async` 能力，同时保持现有业务方调用方式和同步校验语义不变。

## 设计结论

### 方案选择

采用“同步校验 + 独立异步分发 Bean”的方案：

- `WebMessageServiceImpl` 保留同步编排职责
- 新增专用异步分发 Bean 承担 `@Async("threadPoolTaskExecutor")` 后台发送
- `IWebSocketService` / `WebSocketServerImpl` 继续只负责真正的 WebSocket 发送与状态回写

不采用直接在 `IWebMessageService` 上加 `@Async` 的方案，因为那会把模板解析、接收组解析、空用户校验一并推迟到后台线程，破坏现有同步异常语义。

### 线程池与配置

- 在 `yr-framework` 的 `ThreadPoolConfig` 中启用 `@EnableAsync`
- 继续使用现有 `threadPoolTaskExecutor`
- 为线程池设置明确的线程名前缀，例如 `yr-async-`
- 初始化执行器并保留现有拒绝策略，避免异步线程池处于“可用但不可观测”的状态

### 调用链路

1. 业务方继续调用 `DefaultMessageClientImpl`
2. `WebMessageServiceImpl` 同步完成模板查询、接收组查询、占位符解析、空参数校验
3. 校验通过后，把“已解析的消息对象 + 接收人列表 + 发送人”交给新的异步分发 Bean
4. 异步分发 Bean 在 `threadPoolTaskExecutor` 线程池中调用 `IWebSocketService.send(...)`
5. `WebSocketServerImpl` 继续执行消息落库前后钩子、状态回写和单用户发送失败兜底

### 异常语义

- 同步阶段异常保持原样：
  - 模板不存在
  - 接收组不存在
  - 接收人为空
  - 模板解析异常
- 异步阶段异常不再回抛给业务调用线程：
  - 单个用户发送失败沿用现有逻辑，记录日志并标记为未发送
  - 整个异步任务异常记录上下文日志，不影响主业务线程

### 边界约束

- 不改业务方调用接口
- 不扩展到 `sendGlobalMsgToUser(...)`
- 不对全项目做统一异步标准化改造
- 不在本轮新增异步返回值契约，消息发送仍对业务方表现为“提交即返回”

## 测试策略

### 单元测试

- `WebMessageServiceImplTest`
  - 校验成功时应委托给异步分发 Bean
  - 校验失败时不应触发异步分发
- `AsyncWebMessageDispatchServiceTest`
  - 断言异步分发 Bean 会调用 `IWebSocketService.send(...)`

### 集成测试

- `ThreadPoolConfigAsyncIntegrationTest`
  - 在最小 Spring 上下文中验证 `@Async("threadPoolTaskExecutor")` 方法运行在线程池线程
  - 断言线程名前缀为 `yr-async-`

### 架构测试

- 保留 `YrSystemAsyncContractTest` 中“不得回退到 `async()` 快捷接口”的约束
- 增加“真正的异步能力落在独立异步分发 Bean 上”的约束

## 风险控制

- 避免在同一 Bean 内自调用 `@Async`，防止 Spring 代理失效
- 保持同步校验不变，避免业务侧异常语义回归
- 通过线程名断言验证异步不是“伪异步”
- 最终至少运行 `yr-framework` 与 `yr-system` 相关测试，再做全量回归确认
