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
    bizType = BizTypeEnum.INSERT,  // 必填，业务操作类型
    module = "订单管理",
    summary = "创建订单",
    saveParams = true,             // 默认 true
    saveResult = false,            // 默认 false
    mask = {"phone", "idCard"}     // 需要脱敏的参数字段
)
@PostMapping("/order")
public Result<Order> createOrder(@RequestBody OrderDTO dto) {
    // ...
}
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `bizType` | BizTypeEnum | **必填** | 业务操作类型（无默认值，必须显式指定） |
| `enable` | boolean | true | 是否记录日志 |
| `saveParams` | boolean | true | 是否保存请求参数 |
| `saveResult` | boolean | false | 是否保存返回结果 |
| `module` | String | "" | 功能模块 |
| `summary` | String | "" | 操作摘要 |
| `mask` | String[] | {} | 需要脱敏的参数名（支持嵌套字段 `user.phone`） |

### @LoginLog — 登录日志

记录登录行为，自动捕获登录账号和登录结果。

```java
@LoginLog(accountField = "username")  // 指定账号字段名，默认 "username"
@PostMapping("/login")
public Result<TokenInfo> login(@RequestBody LoginDTO dto) {
    // ...
}
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enable` | boolean | true | 是否记录日志 |
| `accountField` | String | "username" | 账号字段名（用于从请求参数中提取登录账号） |

### 日志输出策略 — ILogService

通过 `ILogService` 接口切换日志存储方式：

| 实现 | 说明 | 激活条件 |
|------|------|---------|
| `StdoutLogServiceImpl` | 控制台输出（默认） | 无条件，兜底 |

业务方可实现 `ILogService` 接口进行持久化存储：

```java
@Component
public class DbLogService implements ILogService {
    @Override
    public void saveApiLog(ApiLogCreateDTO dto) {
        // 写入数据库
    }
    @Override
    public void saveLoginLog(LoginLogCreateDTO dto) {
        // 写入数据库
    }
    @Override
    public void saveOperateLog(OperateLogCreateDTO dto) {
        // 写入数据库
    }
}
```

| 方法 | 参数类型 | 说明 |
|------|---------|------|
| `saveApiLog` | `ApiLogCreateDTO` | 保存接口日志 |
| `saveLoginLog` | `LoginLogCreateDTO` | 保存登录日志 |
| `saveOperateLog` | `OperateLogCreateDTO` | 保存操作日志（基于 bizlog-sdk） |

另外，`LogRecordServiceImpl` 基于第三方 bizlog-sdk 实现 `ILogRecordService` 接口，提供开箱即用的持久化能力，业务方只需提供对应的 Repository Bean。

### 链路追踪 ID

`LogTraceUtil` 通过 MDC 管理 `traceId`，`LogTraceFilter`（在 `snowdrift-web` 中）在每个请求开始时注入 `X-Trace-Id` 响应头，便于日志关联。
