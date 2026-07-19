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

如果 Consumer 端未提供 `traceId`，Provider 端自动生成 UUID 作为兜底。

上下文注入失败时 Consumer 端会设置 `x-snowdrift-context-error=true` 标记，Provider 端检测到后跳过上下文恢复，记录 WARN 日志。

### 异常智能化处理

`DubboExceptionFilter` 在 Provider 端响应阶段根据异常类型智能判断是否可直接序列化到 Consumer 端：

| 异常类型 | 处理方式 | 原因 |
|---------|---------|------|
| 已检查异常（非 RuntimeException） | 直接传播 | Java 标准语义 |
| 方法签名声明的异常 | 直接传播 | Consumer 端有对应类 |
| 与接口同 JAR 包的异常 | 直接传播 | Consumer 端有对应类 |
| JDK 异常（`java.*` / `javax.*`） | 直接传播 | 所有 JVM 均有 |
| `RpcException` | 直接传播 | Dubbo 标准异常 |
| `BizException` | 直接传播 | Consumer 端必有 `snowdrift-common` |
| 其他未知异常 | 包装为 `BizException` | 防止序列化失败 |

### 调用日志

- **Consumer 端：** DEBUG 级别记录成功调用及其耗时，ERROR 级别记录失败调用
- **Provider 端：** DEBUG 级别记录成功调用及其耗时（含调用方 IP），ERROR 级别分别记录业务异常（`hasException`）和 RPC 异常（`RpcException`）

```log
// Consumer 端：
DEBUG Dubbo调用成功 [Consumer] com.example.OrderService.createOrder(), elapsed=45ms
ERROR Dubbo调用失败 [Consumer] com.example.OrderService.createOrder(), elapsed=1203ms, ...

// Provider 端：
DEBUG Dubbo服务成功 [Provider] com.example.OrderServiceImpl.createOrder(), caller=192.168.1.100, elapsed=42ms
ERROR Dubbo服务异常 [Provider] com.example.PaymentServiceImpl.pay(), caller=192.168.1.100, elapsed=..., ...   (业务异常)
ERROR Dubbo服务失败 [Provider] com.example.PaymentServiceImpl.pay(), caller=192.168.1.100, elapsed=..., ...   (RPC 异常)
```

> 接口名使用完全限定名（`invoker.getInterface().getName()`），方法名来自 `invocation.getMethodName()`。

## 如何验证过滤器是否生效

检查 Dubbo 调用日志中是否能看到上下文传播的 traceId，以及 Provider 端的日志中是否携带了正确的 `caller` IP。

