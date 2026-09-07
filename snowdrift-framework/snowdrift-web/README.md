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
    thread-name-prefix: async-           # 线程名前缀，默认 async-
    wait-for-tasks-to-complete-on-shutdown: true  # 关闭时等待任务完成，默认 true
```

## 核心功能

### 全局异常处理 — WebExceptionHandler

`@RestControllerAdvice` 统一拦截异常并包装为 `Result` 返回。

**响应语义（重点）：**
- **HTTP 状态**：本 advice 返回 `Result`，HTTP 状态统一为 **200**（业务成败由 body.code 区分）；安全模块在 filter 级会单独设置 HTTP 401/403。
- **body.code**：遵循 `ResultCode`——成功 `1`、失败 `0`、`1xxx` 客户端/资源侧、`2xxx` 服务端。
- **body.msg**：为 `ResultCode` 内置中文或业务抛出的中文直文，**不依赖 i18n 开关**，不会把 i18n key 透给前端。

| 异常类型 | body.code | 说明 |
|---------|----------|------|
| `BizException` | `e.getCode()` | 业务异常 |
| `MethodArgumentNotValidException` | `BAD_REQUEST(1000)` | `@Valid` 校验失败 |
| `BindException` | `BAD_REQUEST(1000)` | 参数绑定失败 |
| `ConstraintViolationException` | `BAD_REQUEST(1000)` | `@Validated` 方法参数校验 |
| `HandlerMethodValidationException` | `BAD_REQUEST(1000)` | Boot 3.x 方法签名参数校验（已覆盖，不再落 500） |
| `MethodArgumentTypeMismatchException` | `BAD_REQUEST(1000)` | 参数类型不匹配 |
| `HttpMessageNotReadableException` | `BAD_REQUEST(1000)` | JSON 解析失败 |
| `MissingServletRequestParameterException` | `BAD_REQUEST(1000)` | 缺少请求参数 |
| `MissingPathVariableException` | `BAD_REQUEST(1000)` | 缺少路径变量 |
| `MissingServletRequestPartException` | `BAD_REQUEST(1000)` | 缺少 multipart 部分 |
| `IllegalArgumentException` | `BAD_REQUEST(1000)` | 非法参数 |
| `MaxUploadSizeExceededException` | `PAYLOAD_TOO_LARGE(1007)` | 文件上传超限 |
| `NoResourceFoundException` | `NOT_FOUND(1003)` | 资源不存在 |
| `HttpRequestMethodNotSupportedException` | `METHOD_NOT_ALLOWED(1004)` | 方法不支持 |
| `HttpMediaTypeNotSupportedException` | `UNSUPPORTED_MEDIA_TYPE(1008)` | 媒体类型不支持 |
| `NullPointerException` | `INTERNAL_SERVER_ERROR(2000)` | 空指针 |
| `Exception` | `INTERNAL_SERVER_ERROR(2000)` | 通用兜底 |

### 国际化（i18n）— 现状与方向

`I18nUtil` 提供按 key 取消息的工具方法；请求级语言由 `I18nInterceptor` 设置（URL 语言参数 > `Accept-Language` > 默认语言）。资源文件命名：各模块独立维护 `src/main/resources/i18n/<prefix>-messages_<locale>.properties`。

**重要现状（与旧表述不同）：**
- 全局异常响应**不走 i18n**——消息为 `ResultCode` 内置中文或业务中文直文，避免未启用/缺 key 时把 key 透给前端。
- `snowdrift.i18n.enabled` 默认关闭；未启用（`messageSource==null`）或 bundle 缺 key 时，`I18nUtil.getMessage(...)` 仅 `log.warn` 后**原样返回 key**。请勿依赖它对前端文案做翻译。
- 项目方向是**整体去掉 i18n**，各模块已陆续改为中文直文 / `ResultCode` 文案；届时 `I18nUtil` 相关链路会收敛或移除。

### 异步支持

`AsyncConfiguration` 提供 `TaskDecorator`，向 `@Async` 线程传递 `HttpContext`、`SecurityContext` 和 traceId，并在 `finally` 中清理。上下文为**可选**：非 HTTP 线程（定时任务/MQ 等）触发时 HttpContext 缺失也不报错；线程池拒绝策略为 `CallerRunsPolicy`（由提交线程兜底执行，不抛 500）。

```java
@Async
public CompletableFuture<Result<User>> getUser(Long id) {
    // SecurityContext.getUserId() 可用
    // LogTraceUtil.getTraceId() 可用
}
```

### 链路追踪

`LogTraceFilter` 最早执行：有入站 `X-Trace-Id` 则复用（网关/上游透传的链路 ID），否则生成，写入 MDC 与响应头；请求结束在 finally 清理。

### XSS 防护

`XssFilter` 对请求参数、请求头、QueryString、请求属性做 XSS 清洗，阻断存储型 XSS。

```yaml
snowdrift:
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

`CachedBodyFilter` 在 Filter 链前端把普通 JSON/表单 Body 缓存为 `byte[]`，后续所有 Filter 和 Controller 均可重复调用 `getInputStream()` / `getReader()`。**安全限流**：`multipart/*`、`application/octet-stream` 及 `Content-Length > 5MB` 的请求不缓存（避免大文件整包入内存），仍由下游单次消费。

### 过滤器链路

```
请求进入
  → LogTraceFilter       (HIGHEST_PRECEDENCE)        生成 traceId
  → HttpContextFilter    (HIGHEST_PRECEDENCE + 1)    填充 HttpContext
  → CachedBodyFilter     (HIGHEST_PRECEDENCE + 5)    缓存 Body
  → XssFilter            (HIGHEST_PRECEDENCE + 10)   XSS 清洗（可配开关/排除路径）
  → Controller
```

### JSON 序列化

默认使用 Jackson + `JavaTimeModule`，`LocalDateTime` 使用 `yyyy-MM-dd HH:mm:ss` 格式不使用 timestamp。
