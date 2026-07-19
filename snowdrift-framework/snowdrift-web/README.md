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
    core-pool-size: 2                    # 默认 2
    max-pool-size: 10                    # 默认 10
    queue-capacity: 256                  # 默认 256
    await-termination-seconds: 60        # 关闭时等待任务完成的超时，默认 60
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

`LogTraceFilter` 最早执行，生成 `X-Trace-Id` 写入 MDC 和响应头。

### XSS 防护

`XssFilter` 对请求参数、请求头、QueryString、请求属性做 XSS 清洗，阻断存储型 XSS。

```yaml
snowdrift:
  web:
    xss:
      enabled: true
      exclude-path-patterns:       # 排除路径（Ant 风格），如富文本接口
        - /admin/richtext/**
```

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | Boolean | false | 是否启用 XSS 过滤 |
| `exclude-path-patterns` | List\<String\> | [] | 排除路径，不进行过滤 |

默认使用 `SimpleXssCleaner` 做 HTML 实体转义（零额外依赖）：

| 原始字符 | 转义后 |
|---------|--------|
| `&` | `&amp;` |
| `<` | `&lt;` |
| `>` | `&gt;` |
| `"` | `&quot;` |
| `'` | `&#39;` |

**可插拔清洗器：** `XssCleaner` 接口支持注入自定义实现。例如管理后台场景可引入 Jsoup，用 Safelist 白名单保留富文本标签同时去掉恶意脚本：

```java
@Component
public class JsoupXssCleaner implements XssCleaner {
    @Override
    public String clean(String value) {
        return Jsoup.clean(value, Safelist.relaxed());
    }
}
```

### 请求体重复读取

`CachedBodyFilter` 在 Filter 链前端一次性读取 Body 并缓存为 `byte[]`，后续所有 Filter 和 Controller 均可重复调用 `getInputStream()` / `getReader()`。

### 过滤器链路

```
请求进入
  → LogTraceFilter       (HIGHEST_PRECEDENCE)        生成 traceId
  → CachedBodyFilter     (HIGHEST_PRECEDENCE + 5)    缓存 Body
  → XssFilter            (HIGHEST_PRECEDENCE + 10)   XSS 清洗（可配开关/排除路径）
  → HttpContextFilter    (HIGHEST_PRECEDENCE + 1)    填充 HttpContext
  → Controller
```

### JSON 序列化

默认使用 Jackson + `JavaTimeModule`，`LocalDateTime` 使用 `yyyy-MM-dd HH:mm:ss` 格式不使用 timestamp。
