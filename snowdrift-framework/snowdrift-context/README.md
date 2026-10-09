# snowdrift-context

请求上下文模块，提供 HTTP 上下文与安全上下文，基于阿里巴巴 `TransmittableThreadLocal`（TTL）实现线程安全的存取与跨线程池传递。

## 快速开始

```xml
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-context</artifactId>
</dependency>
```

（内部依赖 `snowdrift-base` 与 `transmittable-thread-local`，会作为传递依赖自动引入。）

## 核心功能

### 安全上下文 — SecurityContextHolder

登录成功后写入上下文，业务代码通过 Holder 读取当前用户。

```java
// 设置上下文（登录/认证成功时）
SecurityContext ctx = SecurityContext.builder()
        .userId(1L)
        .tenantId(100L)
        .deptId(10L)
        .username("admin")
        .nickname("管理员")
        .roleKeys(List.of("admin"))
        .permissions(List.of("sys:user:list"))
        .build();
SecurityContextHolder.setContext(ctx);

// 强制获取（不存在时抛 BizException）
SecurityContext ctx = SecurityContextHolder.getContext();

// 可选获取（可为 null，不抛异常；供日志/MQ/Dubbo 等“上下文可选”场景）
SecurityContext maybe = SecurityContextHolder.peekContext();

// 便捷方法（均依赖 getContext，未登录时抛 BizException）
Long userId = SecurityContextHolder.getUserId();
String username = SecurityContextHolder.getUsername();
String nickname = SecurityContextHolder.getNickname();
String operator = SecurityContextHolder.getOperator();   // 昵称优先，无则账号
Long tenantId = SecurityContextHolder.getTenantId();
Long deptId = SecurityContextHolder.getDeptId();

// 清除上下文（请求结束/finally）
SecurityContextHolder.clear();
```

> **语义区分**：`getContext()` 在无上下文时抛 `BizException`，适合“必须已登录”的业务；`peekContext()` 返回 null 不抛异常，适合基础设施（日志、MQ/RPC 传播）等无上下文也合法的路径。
>
> **注意“有上下文但字段为空”是合法状态**：MQ/Dubbo 消费端会用消息头恢复上下文（`MqContextPropagator` 无条件 `setContext`），消息未携带身份信息时就是一个各字段为 null 的空上下文。此时 `getUserId()` 等便捷方法返回 `null` 而**不**抛异常——需要“必须携带身份”的判断请自行校验具体字段（如 `getUserId() != null`），不要依赖 `catch (BizException)` 兜底。

| 字段 | 类型 | 说明 |
|------|------|------|
| `userId` | Long | 用户 ID |
| `username` | String | 登录账号 |
| `nickname` | String | 显示名称 |
| `tenantId` | Long | 租户 ID |
| `deptId` | Long | 部门 ID |
| `roleKeys` | List\<String\> | 角色 Key 列表（RBAC） |
| `permissions` | List\<String\> | 权限标识列表（RBAC） |
| `platform` | String | 平台标识 |
| `attributes` | Map\<String,Object\> | 扩展属性 |

> 数据权限（部门/自定义）不直接存于 `SecurityContext`，由 ORM 层通过 `IDataScopeProvider` + 上下文中的 userId/deptId 计算。

### HTTP 上下文 — HttpContextHolder

存储请求级信息，供日志、审计等使用。

```java
HttpContext ctx = HttpContextHolder.getContext();  // 未设置时返回 null
ctx.getUri();           // 请求 URI
ctx.getMethod();        // HTTP 方法
ctx.getUserAgent();     // User-Agent
ctx.getIp();            // 客户端 IP
ctx.getIpLocation();    // IP 归属地
ctx.getParamMap();      // 请求参数

HttpContextHolder.clear();
```

> `traceId` 通过 MDC（`LogTraceUtil`）管理，不放在 HttpContext 中。

## 线程传递与清理

- 两个 Holder 均基于 **TTL**：使用 TTL 包装过的执行器提交任务时，上下文会传递给任务线程。框架自带的异步执行器 `snowdriftAsyncExecutor` 已通过 `TtlRunnable` 装饰（`snowdrift.async.enabled=true` 时生效）；`@Async` 是否使用该执行器取决于下游应用是否把它作为默认执行器。
- **只对"已存在的"池化线程需要显式包装**：池化线程是在设置上下文之前创建的，必须用 `TtlExecutors` / `TtlRunnable` 包装后提交才能读到上下文（未包装则读不到）。而 `setContext` 之后 `new Thread(...)` 新建的线程会因 TTL 继承自 `InheritableThreadLocal` **直接继承**该上下文——无需包装、也**不受包装控制**，这一点常与预期不符；注意不要在持有上下文时创建长生命周期线程（子线程拿到后自行 `clear()` 或按需使用）。
- 传递的是**同一实例而非拷贝**：父线程与子线程拿到同一个 `SecurityContext`/`HttpContext` 对象，子线程调用 setter 或修改 `roleKeys`/`attributes`/`paramMap` 等可变字段，父线程与同链路的其它线程都会看到修改。需要隔离时由调用方自行拷贝后再放入上下文。
- 池化线程内 `setContext` 写入不会残留到该线程的下一个任务（TTL 在任务结束后还原线程状态）。
- 上下文的清除由安全过滤器/监听器、RPC/MQ 过滤器在调用结束统一执行；业务内若手动 `setContext`，请务必在 `finally` 中 `clear()`，避免线程复用导致上下文串台。

## 序列化与打印

- 两个上下文模型都实现 `Serializable`（`serialVersionUID = 1L`），可跨进程/缓存传递。Java 序列化往返由单元测试覆盖；Dubbo 默认的 Hessian2 往返（含 `attributes` 等集合字段）为手工用 `hessian-lite` 实测确认，没有对应的仓内测试。
- **两个模型都显式提供公开无参构造器**（`@NoArgsConstructor` + 包级私有 `@AllArgsConstructor` 供 `@Builder` 使用）。这不是装饰：`@Builder` 会生成唯一的包级私有全参构造器并让类失去无参构造器，而 Jackson 一类框架必须靠无参构造器实例化。此前缺少它导致 **Redis 令牌存储路径在每个已登录请求上抛 `SerializationException`→500**（`SnowdriftSecuritySpringConfiguration` 用 `Jackson2JsonRedisSerializer<>(TokenEntry.class)` 存取内嵌 `SecurityContext` 的令牌条目，`RedisTokenStore.get` 反序列化失败；登录只写不读所以看起来正常）。已用真实 `TokenEntry` + 配置同款序列化器实测修复前后对比（失败 → 成功读回），并加了防回退的单元测试（断言公开无参构造器存在且 setter 可填充）。
- 两个模型都已把**可能含机密**的字段排除出 `toString()`：`SecurityContext.attributes`（可能存放 `token`）与 `HttpContext.paramMap`（由 `HttpContextFilter` 从 Servlet 参数原样填入，登录接口会带明文口令）。被排除的字段仍参与 `equals`/`hashCode`；需要打印参数时请显式取 `getParamMap()` 并自行脱敏。
- `SecurityContextHolder.getOperator()` 在昵称与账号都为空时返回 `null`，调用方需自行兜底。

## 模块边界

- 纯工具/模型层：不注册自动配置、不依赖 Spring Web，可独立使用。
- 两个 Holder 通过 `static final` 的 `com.alibaba.ttl.TransmittableThreadLocal` 存取，实例本身线程安全；但上下文**对象**可变且按引用跨线程共享，线程安全需调用方自行保证（见上节）。

## 测试

模块已引入 `spring-boot-starter-test`（scope test，版本由 parent 托管、不传递给下游），**4 个测试类、41 个用例**（`mvn -pl snowdrift-framework/snowdrift-context test` 实测全绿），覆盖模型 builder/Java 序列化/toString 脱敏，以及两个 Holder 的读写、null 与异常语义、便捷方法、TTL 跨线程传递（含"未包装读不到""传递同一实例""clear 后不传递""池内写入不残留""新建线程会继承"等边界）：

| 测试类 | 覆盖点 |
|--------|--------|
| `HttpContextTest` | builder/setter、equals、`toString` 不输出 paramMap、公开无参构造器、Java 序列化往返 |
| `SecurityContextTest` | 全字段 builder、空上下文、`toString` 不输出 attributes、公开无参构造器（Jackson/Redis 回归）、序列化往返 |
| `HttpContextHolderTest` | set/get/clear、null 拒绝、final+私有构造、TTL 传递与不残留、同实例共享 |
| `SecurityContextHolderTest` | 抛异常/返回 null 两种获取语义、6 个便捷方法与 `getOperator` 回退、TTL 传递语义 |

```bash
mvn -pl snowdrift-framework/snowdrift-context test
```

## 升级注意（本轮变更）

- **新增公开无参构造器**（`SecurityContext` / `HttpContext`，配合原有的 `@Builder`）：修复 Redis 令牌存储路径的 Jackson 反序列化失败。纯新增、二进制兼容；`@Data` 的 setter 与 `@Builder` 行为不变。
- `HttpContext.toString()` 不再输出 `paramMap`（此前会打印原始请求参数）；依赖旧输出格式解析日志的场景需调整。
- `HttpContextHolder` / `SecurityContextHolder` 改为 `final` 且构造器私有；此前若有 `new XxxHolder()`（无意义用法）或继承它们的代码会编译失败，静态方法调用不受影响。
- 文档更正（无代码变更）：`HttpContext.ipLocation` 字段 javadoc“IP地址”→“IP 归属地”；`HttpContextHolder.getContext()` 明确“未设置返回 null”；两个 `setContext` 明确“不接受 null，清空用 clear()”；`SecurityContextHolder` 各便捷方法明确“无上下文抛异常、上下文未携带该字段返回 null”。
