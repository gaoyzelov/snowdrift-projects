# snowdrift-context

请求上下文模块，提供 HTTP 上下文和安全上下文的线程安全存储与传递。

## 快速开始

```xml
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-context</artifactId>
</dependency>
```

## 核心功能

### 安全上下文 — SecurityContextHolder

基于阿里巴巴 `TransmittableThreadLocal`（TTL）实现，跨线程池安全传递。

```java
// 设置上下文（登录后）
SecurityContext ctx = SecurityContext.builder()
        .userId(1L)
        .tenantId(100L)
        .deptId(10L)
        .username("admin")
        .nickname("管理员")
        .dataScope(1)
        .build();
SecurityContextHolder.setContext(ctx);

// 获取上下文（弱校验，不存在时返回空上下文）
SecurityContext ctx = SecurityContextHolder.getContext();
Long userId = ctx.getUserId();

// 获取上下文（强校验，不存在时抛出 BizException）
SecurityContext ctx = SecurityContextHolder.getRequiredContext();

// 便捷方法
Long userId = SecurityContextHolder.getUserId();
String username = SecurityContextHolder.getUsername();
Long tenantId = SecurityContextHolder.getTenantId();
DataScopeEnum scope = SecurityContextHolder.getDataScope();

// 清除上下文（请求结束或 finally 块）
SecurityContextHolder.clear();
```

| 上下文字段 | 类型 | 说明 |
|-----------|------|------|
| `userId` | Long | 用户 ID |
| `tenantId` | Long | 租户 ID |
| `deptId` | Long | 部门 ID |
| `username` | String | 登录账号 |
| `nickname` | String | 显示名称 |
| `dataScope` | Integer | 数据权限范围（对应 DataScopeEnum） |

### HTTP 上下文 — HttpContextHolder

同样基于 TTL 实现，存储请求级信息。

```java
// 获取上下文
HttpContext ctx = HttpContextHolder.getContext();
ctx.getUri();            // 请求 URI
ctx.getMethod();         // HTTP 方法
ctx.getIp();             // 客户端 IP
ctx.getIpLocation();     // IP 归属地
ctx.getUserAgent();      // User-Agent
ctx.getParamMap();       // 请求参数

// 清除
HttpContextHolder.clear();
```

> `traceId` 通过 MDC（`LogTraceUtil`）管理，不在 HttpContext 中。

### TransmittableThreadLocal

两个 Holder 均使用 Alibaba TTL 而非普通 `ThreadLocal`，确保在使用 `@Async`、`CompletableFuture`、线程池等场景下上下文能正确传递到子线程。
