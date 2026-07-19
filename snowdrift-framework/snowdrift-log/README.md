# snowdrift-log

日志审计模块，提供 AOP 切面驱动的操作日志和登录日志记录能力。

## 快速开始

```xml
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-log</artifactId>
</dependency>
```

## 核心功能

### @ApiLog — 操作日志

记录接口调用的入参、出参、耗时、操作人等信息。

```java
@ApiLog(
    bizType = BizTypeEnum.INSERT,
    module = "订单管理",
    summary = "创建订单",
    mask = {"phone", "idCard"}  // 需要脱敏的参数字段
)
@PostMapping("/order")
public Result<Order> createOrder(@RequestBody OrderDTO dto) {
    // ...
}
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `bizType` | BizTypeEnum | OTHER | 业务操作类型 |
| `module` | String | "" | 功能模块 |
| `summary` | String | "" | 操作摘要 |
| `mask` | String[] | {} | 需要脱敏的参数名（支持嵌套字段 `user.phone`） |

### @LoginLog — 登录日志

记录登录行为，自动捕获登录账号和登录结果。

```java
@LoginLog(module = "系统登录")
@PostMapping("/login")
public Result<TokenInfo> login(@RequestBody LoginDTO dto) {
    // ...
}
```

### 日志输出策略 — ILogService

通过 `ILogService` 接口切换日志存储方式，默认实现：

| 实现 | 说明 | 激活条件 |
|------|------|---------|
| `StdoutLogServiceImpl` | 控制台输出（默认） | 无条件，兜底 |
| `LogRecordServiceImpl` | 持久化存储（基于 bizlog-sdk） | 业务方提供 Repository Bean 后自动切换 |

```java
// 自定义日志存储（注册同名 Bean 覆盖）
@Component
public class DbLogService implements ILogService {
    @Override
    public void saveApiLog(ApiLogInfo log) {
        // 写入数据库
    }
    @Override
    public void saveLoginLog(LoginLogInfo log) {
        // 写入数据库
    }
}
```

### 链路追踪 ID

`LogTraceUtil` 通过 MDC 管理 `traceId`，`LogTraceFilter`（在 `snowdrift-web` 中）在每个请求开始时注入 `X-Trace-Id` 响应头，便于日志关联。
