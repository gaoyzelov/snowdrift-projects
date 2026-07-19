# snowdrift-web

Web 自动配置模块，提供 CORS、国际化、异步、JSON 序列化、全局异常处理等开箱即用的 Web 能力。

## 快速开始

```xml
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-web</artifactId>
</dependency>
```

## 配置

```yaml
snowdrift:
  web:
    cors:
      enabled: true                      # 默认 false
      allowed-origin-patterns:
        - "http://localhost:*"
      allow-credentials: false
  i18n:
    enabled: true
    default-locale: zh_CN
    supported-locales:
      - zh_CN
      - en_US
  async:
    enabled: true
    core-pool-size: 8
    max-pool-size: 16
    queue-capacity: 200
    keep-alive-seconds: 60
```

## 核心功能

### 全局异常处理 — WebExceptionHandler

`@RestControllerAdvice` 统一拦截所有异常，包装为 `Result` 返回，覆盖 14 种异常类型：

| 异常类型 | HTTP 状态 | 说明 |
|---------|----------|------|
| `BizException` | 动态 | 业务异常，取 `e.getCode()` |
| `MethodArgumentNotValidException` | 400 | `@Valid` 校验失败 |
| `BindException` | 400 | 参数绑定失败 |
| `ConstraintViolationException` | 400 | `@Validated` 方法参数校验失败 |
| `MethodArgumentTypeMismatchException` | 400 | 参数类型不匹配 |
| `HttpMessageNotReadableException` | 400 | JSON 解析失败 |
| `MissingServletRequestParameterException` | 400 | 缺少请求参数 |
| `MissingPathVariableException` | 400 | 缺少路径变量 |
| `MissingServletRequestPartException` | 400 | 缺少 multipart 部分 |
| `IllegalArgumentException` | 400 | 非法参数 |
| `MaxUploadSizeExceededException` | 413 | 文件上传超限 |
| `NoResourceFoundException` | 404 | 资源不存在 |
| `HttpRequestMethodNotSupportedException` | 405 | 方法不支持 |
| `HttpMediaTypeNotSupportedException` | 415 | 媒体类型不支持 |
| `NullPointerException` | 500 | 空指针 |
| `Exception` | 500 | 通用兜底 |

所有异常消息均经过 i18n 解析。

### 国际化（i18n）

请求级语言自动检测，响应消息自动翻译，支持 `I18nInterceptor` + `I18nMessageSource` + `ResultI18nAdvice` 三层架构。

资源文件命名：各模块独立维护 `src/main/resources/i18n/<prefix>-messages_<locale>.properties`。

```java
// 代码中获取国际化消息
String msg = I18nUtil.getMessage("common.success");
String formatted = I18nUtil.getMessage("order.paid", orderNo);
```

### 异步支持

`AsyncConfiguration` 提供 `TaskDecorator`，自动向 `@Async` 线程传递 `HttpContext`、`SecurityContext` 和 traceId，并在 `finally` 中清理。

```java
@Async
public CompletableFuture<Result<User>> getUser(Long id) {
    // SecurityContext.getUserId() 可用
    // HttpContext.getTraceId() 可用
}
```

### 链路追踪

`LogTraceFilter` 在 `@Order(HIGHEST_PRECEDENCE)` 最先执行，生成 `X-Trace-Id` 写入 MDC 和响应头。`HttpContextFilter` 紧随其后，填充 `HttpContext`。

### JSON 序列化

默认使用 Jackson + `JavaTimeModule`，`LocalDateTime` 使用 ISO-8601 格式不使用 timestamp。
