# yr-system Async Web Message Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为 `yr-system` 站内消息发送链路接入真正的 Spring `@Async` 能力，同时保持同步校验语义和业务方调用方式不变。

**Architecture:** 在 `yr-framework` 的线程池配置上启用 Spring 异步能力，并在 `yr-system` 中引入独立的异步分发 Bean。`WebMessageServiceImpl` 继续负责同步校验和模板解析，校验通过后委托给异步分发 Bean 在 `threadPoolTaskExecutor` 中后台调用 `IWebSocketService.send(...)`。

**Tech Stack:** JDK 17, Spring Boot 2.7.18, Spring `@Async`, ThreadPoolTaskExecutor, JUnit 5, Mockito, Maven

---

### Task 1: 启用 Spring Async 基础设施

**Files:**
- Modify: `yr-framework/src/main/java/com/yr/framework/config/ThreadPoolConfig.java`
- Test: `yr-framework/src/test/java/com/yr/framework/config/ThreadPoolConfigAsyncIntegrationTest.java`

**Step 1: Write the failing test**

在 `ThreadPoolConfigAsyncIntegrationTest` 中创建最小 Spring 上下文，导入 `ThreadPoolConfig` 和测试用异步 Bean，锁定以下行为：

```java
@SpringJUnitConfig(classes = {ThreadPoolConfig.class, AsyncProbeService.class})
class ThreadPoolConfigAsyncIntegrationTest {

    @Autowired
    private AsyncProbeService asyncProbeService;

    @Test
    void shouldRunAsyncMethodOnConfiguredThreadPool() {
        String callerThread = Thread.currentThread().getName();

        String asyncThread = asyncProbeService.captureThreadName().join();

        assertThat(asyncThread).startsWith("yr-async-");
        assertThat(asyncThread).isNotEqualTo(callerThread);
    }

    @Service
    static class AsyncProbeService {
        @Async("threadPoolTaskExecutor")
        CompletableFuture<String> captureThreadName() {
            return CompletableFuture.completedFuture(Thread.currentThread().getName());
        }
    }
}
```

**Step 2: Run test to verify it fails**

Run: `export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl yr-framework -Dtest=ThreadPoolConfigAsyncIntegrationTest test`

Expected: FAIL，因为当前未启用 `@EnableAsync`，异步方法仍在调用线程执行，线程名前缀断言不成立。

**Step 3: Write minimal implementation**

修改 `ThreadPoolConfig.java`：

```java
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("yr-async-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

**Step 4: Run test to verify it passes**

Run: `export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl yr-framework -Dtest=ThreadPoolConfigAsyncIntegrationTest test`

Expected: PASS，且线程名前缀为 `yr-async-`

**Step 5: Commit**

```bash
git add yr-framework/src/main/java/com/yr/framework/config/ThreadPoolConfig.java yr-framework/src/test/java/com/yr/framework/config/ThreadPoolConfigAsyncIntegrationTest.java
git commit -m "feat: enable async thread pool for web messages"
```

### Task 2: 新增独立异步分发 Bean

**Files:**
- Create: `yr-system/src/main/java/com/yr/system/component/message/impl/AsyncWebMessageDispatchService.java`
- Create: `yr-system/src/test/java/com/yr/system/component/message/impl/AsyncWebMessageDispatchServiceTest.java`
- Modify: `yr-system/src/test/java/com/yr/system/architecture/YrSystemAsyncContractTest.java`

**Step 1: Write the failing test**

新增 `AsyncWebMessageDispatchServiceTest`，锁定“独立 Bean 承担异步分发且最终调用 websocket 服务”的契约：

```java
@ExtendWith(MockitoExtension.class)
class AsyncWebMessageDispatchServiceTest {

    @Mock
    private IWebSocketService webSocketService;

    @InjectMocks
    private AsyncWebMessageDispatchService dispatchService;

    @Test
    void shouldDelegateSendToWebSocketService() throws IOException {
        SysMsgTemplate message = new SysMsgTemplate();
        List<Long> users = List.of(1L, 2L);

        dispatchService.dispatch(users, message, "admin");

        verify(webSocketService).send(users, message, "admin");
    }
}
```

同时扩展 `YrSystemAsyncContractTest`，要求：

```java
assertThat(AsyncWebMessageDispatchService.class.getDeclaredMethod("dispatch", List.class, IMessageEntity.class, String.class)
        .getAnnotation(Async.class))
    .isNotNull();
```

**Step 2: Run test to verify it fails**

Run: `export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl yr-system -Dtest=AsyncWebMessageDispatchServiceTest,YrSystemAsyncContractTest test`

Expected: FAIL，因为异步分发 Bean 尚不存在。

**Step 3: Write minimal implementation**

创建 `AsyncWebMessageDispatchService.java`：

```java
@Service
public class AsyncWebMessageDispatchService {

    private final IWebSocketService webSocketService;

    public AsyncWebMessageDispatchService(IWebSocketService webSocketService) {
        this.webSocketService = webSocketService;
    }

    @Async("threadPoolTaskExecutor")
    public void dispatch(List<Long> users, IMessageEntity message, String fromUserId) {
        webSocketService.send(users, message, fromUserId);
    }
}
```

**Step 4: Run test to verify it passes**

Run: `export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl yr-system -Dtest=AsyncWebMessageDispatchServiceTest,YrSystemAsyncContractTest test`

Expected: PASS

**Step 5: Commit**

```bash
git add yr-system/src/main/java/com/yr/system/component/message/impl/AsyncWebMessageDispatchService.java yr-system/src/test/java/com/yr/system/component/message/impl/AsyncWebMessageDispatchServiceTest.java yr-system/src/test/java/com/yr/system/architecture/YrSystemAsyncContractTest.java
git commit -m "feat: add async web message dispatch service"
```

### Task 3: 让 WebMessageServiceImpl 改为同步校验后异步分发

**Files:**
- Modify: `yr-system/src/main/java/com/yr/system/component/message/impl/WebMessageServiceImpl.java`
- Modify: `yr-system/src/test/java/com/yr/system/component/message/impl/WebMessageServiceImplTest.java`

**Step 1: Write the failing test**

扩展 `WebMessageServiceImplTest`，新增两类用例：

```java
@Mock
private AsyncWebMessageDispatchService asyncDispatchService;

@Test
void shouldDispatchAsyncAfterValidationSucceeds() {
    SysMsgTemplate template = buildMessageTemplate();
    when(templateService.get("TPL")).thenReturn(template);
    when(receiveGroupService.getReceiveGroupList("GROUP")).thenReturn(buildReceiveGroup(1L));

    webMessageService.sendMessage("TPL", "GROUP", Map.of("${temp.name}", "ok"));

    verify(asyncDispatchService).dispatch(List.of(1L), template, "admin");
    verifyNoInteractions(messageService);
}

@Test
void shouldNotDispatchAsyncWhenUsersEmpty() {
    SysMsgTemplate template = buildMessageTemplate();
    SysReceiveGroup receiveGroup = new SysReceiveGroup();
    receiveGroup.setReMode(ModeType.USER_GROUP.name());
    receiveGroup.setGroupObjectList(Collections.emptyList());
    when(templateService.get("TPL")).thenReturn(template);
    when(receiveGroupService.getReceiveGroupList("GROUP")).thenReturn(receiveGroup);

    assertThatThrownBy(() -> webMessageService.sendMessage("TPL", "GROUP", null))
            .isInstanceOf(CustomException.class);

    verifyNoInteractions(asyncDispatchService);
}
```

**Step 2: Run test to verify it fails**

Run: `export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl yr-system -Dtest=WebMessageServiceImplTest test`

Expected: FAIL，因为当前实现仍直接调用 `IWebSocketService`

**Step 3: Write minimal implementation**

在 `WebMessageServiceImpl.java` 中注入 `AsyncWebMessageDispatchService`，并把发送逻辑改为：

```java
private final AsyncWebMessageDispatchService asyncDispatchService;

public WebMessageServiceImpl(IWebSocketService messageService,
                             ISysMsgTemplateService templateService,
                             ISysReceiveGroupService receiveGroupService,
                             AsyncWebMessageDispatchService asyncDispatchService) {
    this.messageService = messageService;
    this.templateService = templateService;
    this.receiveGroupService = receiveGroupService;
    this.asyncDispatchService = asyncDispatchService;
}

private void sendUserMessage(IMessageEntity msgTemplate, List<Long> userList) {
    if (ObjectUtils.isEmpty(userList)) {
        throw new CustomException("模组没有用户，请先添加模组或者用户");
    }
    logger.info("消息内容:{}", JSON.toJSONString(msgTemplate));
    asyncDispatchService.dispatch(userList, msgTemplate, "admin");
}
```

同时移除不再需要的 `IOException` 吞错分支。

**Step 4: Run test to verify it passes**

Run: `export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl yr-system -Dtest=WebMessageServiceImplTest test`

Expected: PASS，且同步异常语义保持不变

**Step 5: Commit**

```bash
git add yr-system/src/main/java/com/yr/system/component/message/impl/WebMessageServiceImpl.java yr-system/src/test/java/com/yr/system/component/message/impl/WebMessageServiceImplTest.java
git commit -m "refactor: dispatch web messages asynchronously"
```

### Task 4: 全链路验证

**Files:**
- Verify only

**Step 1: Run targeted module tests**

Run: `export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl yr-framework,yr-system -Dtest=ThreadPoolConfigAsyncIntegrationTest,AsyncWebMessageDispatchServiceTest,WebMessageServiceImplTest,YrSystemAsyncContractTest test`

Expected: PASS

**Step 2: Run full regression tests**

Run: `export JAVA_HOME=/Users/PopoY/Library/Java/JavaVirtualMachines/corretto-17.0.18/Contents/Home && export PATH="$JAVA_HOME/bin:$PATH" && mvn -pl yr-framework,yr-system test`

Expected: `BUILD SUCCESS`

**Step 3: Commit verification-ready state**

```bash
git add yr-framework/src/main/java/com/yr/framework/config/ThreadPoolConfig.java yr-framework/src/test/java/com/yr/framework/config/ThreadPoolConfigAsyncIntegrationTest.java yr-system/src/main/java/com/yr/system/component/message/impl/AsyncWebMessageDispatchService.java yr-system/src/main/java/com/yr/system/component/message/impl/WebMessageServiceImpl.java yr-system/src/test/java/com/yr/system/component/message/impl/AsyncWebMessageDispatchServiceTest.java yr-system/src/test/java/com/yr/system/component/message/impl/WebMessageServiceImplTest.java yr-system/src/test/java/com/yr/system/architecture/YrSystemAsyncContractTest.java
git commit -m "feat: enable real async web message delivery"
```
