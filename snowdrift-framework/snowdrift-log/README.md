# snowdrift-log

日志审计模块：提供 AOP 切面驱动的**接口访问日志**与**登录日志**，并把第三方 **bizlog（mzt logapi）** 的操作日志事件桥接到统一的 `ILogService` 输出。Servlet Web 环境下自动装配（`@ConditionalOnWebApplication`）。

## 快速开始

```xml
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-log</artifactId>
</dependency>
```

若使用操作日志（bizlog `@LogRecord`），还需引入 bizlog starter 并在启动类启用：

```java
@EnableLogRecord
@SpringBootApplication
public class Application { ... }
```

## 核心功能

### 接口日志 — @ApiLog

记录接口的入参、出参、耗时、操作人等信息。**匿名/无登录上下文的请求也可记录**（此时 userId/operator 为 null）。

```java
@ApiLog(bizType = BizTypeEnum.INSERT, module = "订单管理", summary = "创建订单")
@PostMapping("/order")
public Result<Order> createOrder(@RequestBody OrderDTO dto) { ... }

// 需要脱敏时，指明参数字段名（JSON 任意层级、同名即脱敏）
@ApiLog(bizType = BizTypeEnum.UPDATE, module = "用户", summary = "修改用户",
        saveResult = true, mask = {"phone", "idCard"})
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `bizType` | BizTypeEnum | **必填** | 业务操作类型（无默认值，必须显式指定） |
| `enable` | boolean | true | 是否记录日志 |
| `saveParams` | boolean | true | 是否保存请求参数（自动剔除 Servlet/流/文件上传等不可序列化参数） |
| `saveResult` | boolean | false | 是否保存返回结果 |
| `module` | String | "" | 功能模块 |
| `summary` | String | "" | 操作摘要 |
| `mask` | String[] | {} | 需脱敏的字段名：对 JSON **任意层级、名称匹配（不区分大小写）**的字段替换为 `******`；不支持按点路径精确定位 |

> 结果判定：方法抛异常 **或返回的 `Result.code != 1`** 均记为失败（`status=0`，附根因/业务消息，异常仍原样抛给上层）；`Error` 亦记为失败。

### 登录日志 — @LoginLog

记录登录行为，自动从请求参数提取登录账号并区分“成功 / 业务失败 / 抛异常”三种结果。

```java
@LoginLog(accountField = "username")   // 从登录请求参数中提取账号的字段名，默认 "username"
@PostMapping("/login")
public Result<TokenInfo> login(@RequestBody LoginDTO dto) { ... }
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enable` | boolean | true | 是否记录登录日志 |
| `accountField` | String | "username" | 用于提取登录账号的字段名 |

> 结果判定：方法抛异常或返回的 `Result.code != 1` 记为失败（`status=0`，附异常/业务消息），否则成功（`status=1`）。

### 日志输出 — ILogService

所有日志最终都汇聚到 `ILogService` 的三个方法。模块**自带默认实现** `SnowdriftDefaultLogServiceImpl`（仅 debug 打印），因此不必强制提供；需要落库/上报时注册自定义 Bean 即可覆盖（`@ConditionalOnMissingBean`）。

```java
@Component
public class DbLogService implements ILogService {
    @Override public void saveApiLog(ApiLogHolder holder) { /* 写库 */ }
    @Override public void saveLoginLog(LoginLogHolder holder) { /* 写库 */ }
    @Override public void saveOperateLog(OperateLogHolder holder) { /* 写库 */ }
}
```

| 方法 | 参数 | 说明 |
|------|------|------|
| `saveApiLog` | `ApiLogHolder` | 接口访问日志 |
| `saveLoginLog` | `LoginLogHolder` | 登录日志 |
| `saveOperateLog` | `OperateLogHolder` | 操作日志 |

### 操作日志 — bizlog 桥接

`snowdrift-log` 实现 bizlog 的 `ILogRecordService`（`SnowdriftLogRecordServiceImpl`），把 `@LogRecord` 触发的事件组装为 `OperateLogHolder` 后交给 `ILogService.saveOperateLog`：

- 业务代码照常使用 bizlog 的 `@LogRecord`（需要启用 `@EnableLogRecord` 并引入其依赖）。
- 无登录上下文的系统任务也会记录，此时 userId/tenantId/operator 为 null。
- **日志查询**（`queryLog` / `queryLogByBizNo`）在默认实现中**未实现**（抛 `UnsupportedOperationException`）；如需查询请自行实现 bizlog 的 `ILogRecordService`。

### 链路追踪 ID

- `LogTraceUtil` 通过 **MDC** 管理 `traceId`。
- `snowdrift-web` 的 `LogTraceFilter` 在每个请求开始写入 `traceId`，并把 `X-Trace-Id` 回写到响应头，便于前端与日志关联。
- 各日志 Holder（`ApiLogHolder`/`OperateLogHolder`）会携带当前 `traceId`。

## 数据模型

| Holder | 主要字段 |
|--------|---------|
| `ApiLogHolder` | traceId、appName、method、uri、requestParams、responseBody、ip、ua、bizModule、bizType、summary、duration、status、errorMsg、userId、tenantId、operator、operateTime |
| `LoginLogHolder` | username、ip、ipLocation、ua、status、msg、loginTime |
| `OperateLogHolder` | traceId、bizId、bizModule、bizType、content、userId、tenantId、operator、operateTime |

## 模块边界

- 仅 Servlet Web 应用自动装配；切面（`ApiLogAspect`、`LoginLogAspect`）与默认日志服务均在 `SnowdriftLogConfiguration` 注册。
- 日志采集、存储失败不影响主业务（各采集点均 try/catch 记 error 后放行）。
