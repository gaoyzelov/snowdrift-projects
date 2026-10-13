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

记录接口的耗时、操作人、URI/IP/UA 等元信息。**匿名/无登录上下文的请求也可记录**（此时 userId/operator 为 null）。

> 入参与出参是**按需开启**的：`saveParams` / `saveResult` 默认均为 `false`，避免口令、Token 等敏感内容被默认写入日志；需要时显式打开并配合 `mask` 脱敏。

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
| `saveParams` | boolean | **false** | 是否保存请求参数（自动剔除 Servlet/流/文件上传等不可序列化参数；参数中含文件数组时序列化会安全失败，此时不记录参数） |
| `saveResult` | boolean | false | 是否保存返回结果 |
| `module` | String | "" | 功能模块 |
| `summary` | String | "" | 操作摘要 |
| `mask` | String[] | {} | 需脱敏的字段名：对 JSON **任意层级、名称精确匹配（不区分大小写）**的字段替换为 `******`；不支持按点路径精确定位 |

> **凭据字段无条件脱敏，无需配置 `mask`**：`password` / `passwd` / `pwd` / `secret` / `token`
> / `credential` / `authorization` / `privateKey` 这组词按**后缀**匹配（`userPassword`、`clientSecret`、
> `csrfToken` 都会命中），请求参数与响应体一视同仁。
> 触发原因是 `HttpContext.paramMap` 记录的是**原始** URL 参数，登录接口的明文口令正在其中 ——
> 若只在配置了 `mask` 的接口上脱敏，等于让「忘了写 `mask`」变成凭据落库。`mask` 里写的名字仍按精确匹配，
> 以免写了 `id` 这类短名后把 `orderId`/`userId` 一并抹掉。

> 结果判定：方法抛异常 **或返回的 `Result.code != 1`**（含 `code` 为 `null`）均记为失败（`status=0`，附根因/业务消息，异常仍原样抛给上层）；`Error` 亦记为失败。

> `mask` 的能力边界：它是**按 JSON 键名**脱敏，因此 ① 值本身是 JSON 字符串时的内嵌字段不会被解析脱敏；② `errorMsg`（异常消息）不参与脱敏，`throw new IllegalArgumentException("token=" + token)` 这类消息会原文落库，敏感信息请勿拼进异常消息；③ 名称按序列化后的键匹配，被 `@JsonProperty` 改名的字段需填**序列化后的名字**。

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
- **装配优先级**：桥接 Bean 带 `@Primary`。`@EnableLogRecord` 通过 `@Import` 引入的配置属于“用户级配置”，会先于自动配置解析并注册 bizlog 自带的 `DefaultLogRecordServiceImpl`（只打一行 info）；若桥接只用 `@ConditionalOnMissingBean`，它会直接退让而永不生效。因此这里刻意用 `@Primary`，让 bizlog 按类型注入时拿到框架桥接。
- **自定义操作日志服务**：设置 `snowdrift.log.record-service=false` 关闭框架桥接，再注册自己的 `ILogRecordService` 并标注 `@Primary`。⚠️ 若**不**关闭桥接又给自己的实现标 `@Primary`，容器里会出现两个 `@Primary`，按类型注入时报 `NoUniqueBeanDefinitionException`（bizlog 的 `LogRecordInterceptor` 会在启动期注入该类型，表现为应用启动失败）。
- 无登录上下文的系统任务也会记录，此时 userId/tenantId/operator 为 null。
- 登录日志的账号提取支持两种写法：对象入参按 `accountField` 字段名提取；裸参数（如 `login(@RequestParam String username, ...)`）按**方法形参名**匹配 `accountField` 提取（依赖编译期 `-parameters`，父 pom 已开启）。两者都取不到时记为 `unknown`。
- 操作人取框架 `SecurityContext`（昵称优先、回落账号）；bizlog `LogRecord` 自带的 `operator`/`tenant` 字段**不参与映射**。
- **日志查询**（`queryLog` / `queryLogByBizNo`）在默认实现中**未实现**（抛 `UnsupportedOperationException`）；如需查询请自行实现 bizlog 的 `ILogRecordService`。

### 链路追踪 ID

- `LogTraceUtil` 通过 **MDC** 管理 `traceId`。
- `snowdrift-web` 的 `LogTraceFilter` 在每个请求开始写入 `traceId`（**优先复用入站 `X-Trace-Id`**，没有才新生成 UUID），并把 `X-Trace-Id` 回写到响应头，便于跨服务与前端关联。
- 各日志 Holder（`ApiLogHolder`/`OperateLogHolder`）会携带当前 `traceId`。

## 数据模型

| Holder | 主要字段 |
|--------|---------|
| `ApiLogHolder` | traceId、appName、method、uri、requestParams、responseBody、ip、ua、bizModule、bizType、summary、duration、status、errorMsg、userId、tenantId、operator、operateTime |
| `LoginLogHolder` | username、ip、ipLocation、ua、status、msg、loginTime |
| `OperateLogHolder` | traceId、bizId、bizModule、bizType、content、userId、tenantId、operator、operateTime |

## 配置项

| 配置 | 默认 | 说明 |
|------|------|------|
| `snowdrift.log.record-service` | `true` | 是否注册 bizlog → `ILogService` 的桥接 Bean；设为 `false` 可让业务自定义的 `ILogRecordService` 接管 |

## 模块边界

- 仅 Servlet Web 应用自动装配；切面（`ApiLogAspect`、`LoginLogAspect`）与默认日志服务均在 `SnowdriftLogConfiguration` 注册。
- 切面 Bean 均带 `@ConditionalOnMissingBean`：业务自定义同名切面时框架切面退让，不会出现双切面重复记录。
- 日志采集、存储失败不影响主业务：各采集点均 try/catch 记 error 后放行，且兜底范围是 **`Throwable`**——fastjson2 对循环引用/深嵌套对象抛的是 `StackOverflowError`（`Error`），只兜 `Exception` 会穿透 `finally`，丢掉已成功的返回值或掩盖业务原本的异常。
- `ApiLogAspect` 的入参序列化是**逐段兜底**的：个别入参不可序列化（如 `MultipartFile[]`）时，URL 参数仍会被记录。

## 测试

模块已引入 `spring-boot-starter-test`（scope test，版本由 parent 托管、不传递给下游），**9 个测试类、80 个用例**（`mvn -pl snowdrift-framework/snowdrift-log test` 实测全绿）：

| 测试类 | 覆盖点 |
|--------|--------|
| `ApiLogAspectTest` | 默认只记元信息、无上下文、业务失败/异常/`Error` 判定、`enable=false`、`saveParams`/`saveResult`、`mask` 递归与大小写、深度上限、URL 参数合并、Servlet/文件参数剔除、坏参数不拖垮 URL 参数、自引用对象图不影响业务返回、日志链路抛 `Error` 不影响业务返回 |
| `LoginLogAspectTest` | 成功/业务失败/异常三态、消息回落、自定义 `accountField`（DTO 与 Map）、形参名提取、取不到形参名、`code=null` 回归、`enable=false`、存储失败或抛 `Error` 不影响登录、抛 `Error` 也被记录 |
| `LoginLogAspectIntegrationTest` | 走真实 AspectJ 代理的端到端：DTO 入参、裸参数按形参名提取、自定义 `accountField`、异常路径 |
| `SnowdriftLogConfigurationTest` | Servlet 环境装配、`@EnableLogRecord` 下桥接仍生效（回归）、自定义 `ILogService` 覆盖、桥接与自定义 `ILogRecordService` 的优先级与关闭开关、切面可覆盖、非 Web 环境退避 |
| `SnowdriftLogRecordServiceImplTest` | bizlog 字段映射、操作人回落、无上下文仍记录、存储失败被吞、查询未实现 |
| `SnowdriftDefaultLogServiceImplTest` | debug 开关行为（用 logback `ListAppender` 断言）、空 Holder 容错 |
| `LogTraceUtilTest` | 生成/自定义/清除/未设置/传 `null`；工具类 final + 私有构造 |
| `BizTypeEnumTest` | 编码与说明、`IEnum` 契约、`of` 命中/未命中/`null`、编码唯一 |
| `LogHolderTest` | 三个 Holder 的 builder 全字段、Java 序列化往返、`serialVersionUID=1L`、空对象容错 |

```bash
mvn -pl snowdrift-framework/snowdrift-log test
```

## 升级注意（本轮变更）

- **行为变更**：`SnowdriftLogConfiguration` 的操作日志桥接改为 `@Primary` 并在 `@EnableLogRecord` 下生效（此前会被 bizlog 默认实现顶掉、永不生效）；新增开关 `snowdrift.log.record-service=false`。
- **行为变更**：切面采集异常的兜底范围由 `Exception` 扩到 `Throwable`（`Error` 不再穿透 `finally` 影响业务）；两个切面 Bean 增加 `@ConditionalOnMissingBean`。
- **行为变更**：`LoginLogAspect` 的 `@AfterThrowing` 由 `Exception` 改为绑定 `Throwable`（`Error` 也会记登录日志）；账号提取支持裸参数按形参名解析（此前记为 `unknown` 且每次登录刷一条 ERROR 堆栈）；`Result.code` 为 `null` 时不再抛 NPE 丢日志，按失败记录。
- **行为变更**：`ApiLogAspect` 入参不可序列化时不再连带丢弃 URL 参数。
- 三个日志 Holder 补 `@Serial serialVersionUID = 1L`：若此前用原生 Java 序列化落过盘，旧数据将无法反序列化（仓内无存量）。
- 文档更正：`saveParams` 默认值为 `false`（README 此前误写为 `true`）；`LoginLogHolder.ipLocation` 字段说明更正为“IP 归属地”；补充 `mask` 能力边界与桥接装配优先级说明。
