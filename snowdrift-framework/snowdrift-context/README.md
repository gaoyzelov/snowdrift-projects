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

- 两个 Holder 均基于 **TTL**：在使用框架包装的异步执行器（`@Async`、线程池等）时上下文可透传到子线程。
- 若自行创建线程 / 线程池，请使用 `TtlExecutors` / `TtlRunnable` 包装后再传递，否则 TTL 不会生效。
- 上下文的清除由安全过滤器/监听器、RPC/MQ 过滤器在调用结束统一执行；业务内若手动 `setContext`，请务必在 `finally` 中 `clear()`，避免线程复用导致上下文串台。

## 模块边界

- 纯工具/模型层：不注册自动配置、不依赖 Spring Web，可独立使用。
- 通过 `com.alibaba.ttl.TransmittableThreadLocal` 存取，非静态可变状态共享，线程安全。
