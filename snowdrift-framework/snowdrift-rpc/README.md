# snowdrift-rpc

RPC 集成模块，基于 Dubbo 实现跨服务上下文传播。

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

## 核心功能

### 上下文自动传播

Dubbo 消费端调用时自动向 RPC 附件注入 `traceId` 和 `SecurityContext`，提供端自动恢复，无需手动传递。

| 传播内容 | 来源 | 恢复位置 |
|---------|------|---------|
| `traceId` | MDC | MDC |
| `userId` | SecurityContextHolder | SecurityContextHolder |
| `tenantId` | SecurityContextHolder | SecurityContextHolder |
| `username` | SecurityContextHolder | SecurityContextHolder |

所有过滤器通过 Dubbo `@Activate` SPI 注册，**无需额外配置**，引入即生效。

### 过滤器链

| 过滤器 | 位置 | 排序 | 职责 |
|--------|------|------|------|
| `DubboConsumerContextFilter` | 消费端 | -200 | 发送前注入上下文 |
| `DubboProviderContextFilter` | 提供端 | -200 | 接收后恢复上下文，finally 清理 |
| `DubboExceptionFilter` | 提供端 | -100 | 异常序列化兼容处理 |
| `DubboProviderLogFilter` | 提供端 | 100 | 服务端调用日志（耗时、结果） |
| `DubboConsumerLogFilter` | 消费端 | 100 | 消费端调用日志（耗时、结果） |

### 异常智能化处理

`DubboExceptionFilter` 根据异常类型智能决定是传播还是包装：

- ✅ 已声明异常、JDK 异常、`BizException`、`RpcException` → 直接传播
- ❌ 未知异常（消费端可能无对应类） → 包装为 `BizException` 以确保序列化安全性

### 调用日志

消费端和服务端均在 DEBUG 级别记录调用耗时，ERROR 级别记录调用失败。

```log
// 消费端
DEBUG [Dubbo-Consumer] OrderService.createOrder(123) -> 45ms

// 提供端
DEBUG [Dubbo-Provider] OrderServiceImpl.createOrder(123) <- 192.168.1.1
```
