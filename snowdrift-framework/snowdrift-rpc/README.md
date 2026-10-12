# snowdrift-rpc

RPC 集成模块，基于 Dubbo 实现跨服务上下文传播、异常处理兼容和调用日志。

## 模块结构

```
snowdrift-rpc
└── snowdrift-rpc-dubbo         ← Dubbo 过滤器链：上下文传播、异常处理、调用日志
```

## 快速开始

```xml
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-rpc-dubbo</artifactId>
</dependency>
```

引入依赖即生效，所有过滤器通过 Dubbo `@Activate` SPI 自动注册，**无需额外配置**。

## Dubbo 配置

### 默认行为

5 个过滤器按类型和优先级自动激活：

| 过滤器 | 端 | 排序 | SPI 名称 |
|--------|------|------|------|
| `DubboConsumerContextFilter` | Consumer | -200 | `dubboConsumerContextFilter` |
| `DubboProviderContextFilter` | Provider | -200 | `dubboProviderContextFilter` |
| `DubboExceptionFilter` | Provider | -100 | `dubboExceptionFilter` |
| `DubboProviderLogFilter` | Provider | 100 | `dubboProviderLogFilter` |
| `DubboConsumerLogFilter` | Consumer | 100 | `dubboConsumerLogFilter` |

### 自定义过滤器列表

如需精确控制（如禁用某个过滤器），在 Dubbo 配置中显式列出需要的过滤器：

```yaml
dubbo:
  provider:
    filter: dubboProviderContextFilter,dubboExceptionFilter,dubboProviderLogFilter
  consumer:
    filter: dubboConsumerContextFilter,dubboConsumerLogFilter
```

```properties
# 或 properties 格式
dubbo.provider.filter=dubboProviderContextFilter,dubboExceptionFilter,dubboProviderLogFilter
dubbo.consumer.filter=dubboConsumerContextFilter,dubboConsumerLogFilter
```

### 添加自定义过滤器

在已有过滤器基础上追加自定义过滤器：

```java
@Activate(group = CommonConstants.PROVIDER, order = 50)
public class CustomProviderFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 自定义逻辑
        return invoker.invoke(invocation);
    }
}
```

然后在 `META-INF/dubbo/org.apache.dubbo.rpc.Filter` 文件中注册：

```
customProvider=com.example.CustomProviderFilter
```

配置中追加：

```yaml
dubbo:
  provider:
    filter: dubboProviderContextFilter,dubboExceptionFilter,customProvider,dubboProviderLogFilter
```

### 过滤器禁用方式

单独禁用某个过滤器，保留其他：

```yaml
dubbo:
  provider:
    filter: "-dubboProviderLogFilter"           # 排除指定过滤器
  consumer:
    filter: "-dubboConsumerLogFilter"
```

### 过滤器排序覆盖

默认排序无需修改。如需调整（不推荐），通过配置覆盖：

```yaml
dubbo:
  provider:
    filter: dubboExceptionFilter,dubboProviderContextFilter
```

> 配置中过滤器按声明顺序执行。默认顺序经过验证，不建议随意调整。

## 核心功能

### 上下文自动传播

Consumer 端调用时自动向 RPC 附件注入 `traceId` 和 `SecurityContext`，Provider 端自动恢复，调用结束后在 `finally` 中清理。

| 传播内容 | Header Key | 来源 | 恢复位置 |
|---------|-----------|------|---------|
| 链路追踪 ID | `x-snowdrift-trace-id` | MDC `traceId` | MDC |
| 用户 ID | `x-snowdrift-security-context` | `SecurityContextHolder` | `SecurityContextHolder` |
| 租户 ID | `x-snowdrift-security-context` | `SecurityContextHolder` | `SecurityContextHolder` |
| 登录账号 | `x-snowdrift-security-context` | `SecurityContextHolder` | `SecurityContextHolder` |

- 如果 Consumer 端未提供 `traceId`，Provider 端自动生成 UUID 作为兜底。
- `SecurityContext` 为**可选**：系统任务等无登录上下文的 Consumer 调用不会注入（`TraceId` 仍透传），Provider 端在无上下文时按自身逻辑兜底（如租户拦截 fail-closed 抛错 / 审计自动填充系统值）。
- 仅当上下文**注入失败**（如不可序列化等真实异常）时，Consumer 端才设置 `x-snowdrift-context-error=true` 标记；Provider 端检测到后跳过上下文恢复并记录 WARN。

> **安全（Provider 默认不信任服务间传来的身份）**：Dubbo 附件完全由调用方控制——任何能连到 Provider 端口的一方都能塞入任意 `userId`/`tenantId`/`roleKeys`。因此 **Provider 端默认只恢复 `traceId`，不恢复 `SecurityContext`**（会记一条 WARN 说明原因）。
>
> 要让 Provider 采用调用方传来的身份，必须显式声明调用方可信（mTLS / 服务网格 / 网关已校验并注入身份）后开启：
>
> ```bash
> -Dsnowdrift.rpc.dubbo.trust-inbound-security-context=true
> # 或环境变量 SNOWDRIFT_RPC_DUBBO_TRUST_INBOUND_SECURITY_CONTEXT=true
> ```
>
> 若下游依赖 Provider 侧的 `SecurityContext`（多租户过滤、操作人审计），未开启时会在使用点 fail-closed 报错，而不是静默使用伪造身份。

### 异常智能化处理

`DubboExceptionFilter` 在 Provider 端响应阶段根据异常类型智能判断是否可直接序列化到 Consumer 端：

| 异常类型 | 处理方式 | 原因 |
|---------|---------|------|
| 已检查异常（非 RuntimeException） | 直接传播 | Java 标准语义 |
| 方法签名声明的异常 | 直接传播 | Consumer 端有对应类 |
| 与接口同 JAR 包的异常 | 直接传播 | Consumer 端有对应类 |
| JDK 异常（`java.*` / `javax.*` / `jakarta.*`） | 直接传播 | 所有 JVM 均有 |
| `RpcException` | 直接传播 | Dubbo 标准异常 |
| `BizException` | 直接传播 | Consumer 端必有 `snowdrift-base` |
| 其他未知异常 | 包装为 `BizException` | 防止序列化失败 |
| **已被 Dubbo 内置 `ExceptionFilter` 包装的异常** | **替换为无细节的 `BizException`** | 见下 |

> **关于内置 `ExceptionFilter` 的包装**：Dubbo 内置的 `ExceptionFilter`（`order=0`，比本过滤器更靠内层）会把
> 「未在方法签名声明、且与接口不在同一 codebase」的异常改写成
> `new RuntimeException(StringUtils.toString(原异常))`，而该 message **包含原异常的类名与完整堆栈**
> （内部包名、文件、行号）。本过滤器在外层收到后会把这类包装**剥离成 `RPC 调用异常`**，
> 避免内部实现细节随响应泄露给调用方。
>
> 相应地，`DubboExceptionFilter` 的激活顺序（`order=-100`）**必须保持在内置 `ExceptionFilter` 之外**，
> 否则在响应阶段只能看到已改写后的 `java.lang.RuntimeException`，既无法剥离细节、也无法按本表判定原异常类型。
> `DubboExceptionFilterTest` 中有专门的不变量用例守护这一点。

### 调用日志

- **Consumer 端：** 成功调用 DEBUG；`BizException`（跨服务业务异常）INFO；其它异常 ERROR
- **Provider 端：** 成功调用 DEBUG（含调用方 IP）；`BizException` INFO；其它异常 / `RpcException` ERROR

```log
// Consumer 端：
DEBUG Dubbo RPC调用成功 [Consumer] com.example.OrderService.createOrder(), elapsed=45ms
INFO  Dubbo RPC业务异常 [Consumer] com.example.OrderService.createOrder(), elapsed=1203ms, msg=订单不存在
ERROR Dubbo RPC调用异常 [Consumer] com.example.OrderService.createOrder(), elapsed=99ms   (result 携带非 BizException)
ERROR Dubbo RPC调用失败 [Consumer] com.example.PaymentService.pay(), elapsed=99ms          (抛 RpcException，原样上抛)

// Provider 端：
DEBUG Dubbo RPC服务成功 [Provider] com.example.OrderServiceImpl.createOrder(), caller=192.168.1.100, elapsed=42ms
INFO  Dubbo RPC业务异常 [Provider] com.example.OrderServiceImpl.createOrder(), caller=192.168.1.100, elapsed=1203ms, msg=订单不存在
ERROR Dubbo RPC服务异常 [Provider] com.example.OrderServiceImpl.createOrder(), caller=192.168.1.100, elapsed=...   (result 携带非 BizException)
ERROR Dubbo RPC服务失败 [Provider] com.example.PaymentServiceImpl.pay(), caller=192.168.1.100, elapsed=...          (抛 RpcException，原样上抛)
```

> 接口名使用完全限定名（`invoker.getInterface().getName()`），方法名来自 `invocation.getMethodName()`。

## 如何验证过滤器是否生效

检查 Dubbo 调用日志中是否能看到上下文传播的 traceId，以及 Provider 端的日志中是否携带了正确的 `caller` IP。

