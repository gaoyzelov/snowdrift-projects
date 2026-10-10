# snowdrift-web

Web 自动配置模块，提供 CORS、国际化、异步、JSON 序列化、全局异常处理、XSS 防护、静态资源映射等开箱即用的 Web 能力。

## 快速开始

```xml
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-web</artifactId>
</dependency>
```

## 配置参考

```yaml
snowdrift:
  cors:
    enabled: true                          # 默认 false
    path: "/**"                            # 允许的路径，默认 /**
    allowed-origin-patterns:               # 允许的源（支持通配符），默认 *
      - "http://localhost:*"
    allowed-methods:                       # 允许的方法，默认 GET,POST,PUT,DELETE,OPTIONS
      - GET
      - POST
    allowed-headers:                       # 允许的请求头，默认 *
      - "*"
    allow-credentials: false               # 是否允许携带 Cookie，默认 false
    max-age: 3600                          # 预检请求缓存时间（秒），默认 3600

  i18n:
    enabled: true                          # 默认 false
    default-locale: zh_CN                  # 默认语言
    param-name: lang                       # URL 语言参数名，默认 lang
    supported-locales:                     # 支持的语言列表
      - zh_CN
      - en_US
    base-names:                            # 资源文件基础名称
      - i18n/messages
    encoding: UTF-8                        # 资源文件编码，默认 UTF-8
    cache-seconds: 3600                    # 缓存时间（秒），-1 永不缓存，默认 3600
    use-code-as-default-message: true      # key 未找到时返回 key 本身，默认 true

  async:
    enabled: true                          # 默认 false
    core-pool-size: 2                      # 核心线程数，默认 2
    max-pool-size: 10                      # 最大线程数，默认 10
    queue-capacity: 256                    # 队列容量，默认 256
    thread-name-prefix: async-             # 线程名前缀，默认 async-
    wait-for-tasks-to-complete-on-shutdown: true  # 关闭时等待任务完成，默认 true
    await-termination-seconds: 60          # 关闭等待超时（秒），默认 60

  xss:
    enabled: true                          # 默认 false
    exclude-path-patterns:                 # 排除路径（Ant 风格），如富文本接口
      - /admin/richtext/**

  resource:
    enabled: true                          # 默认 false
    mappings:                              # 静态资源映射列表
      - path-pattern: "/files/**"          # 访问路径
        location: "file:D:/data/uploads/"  # 资源位置
        cache-period: 3600                 # 缓存时间（秒），默认 3600
        use-cache-control: true            # 是否启用缓存控制，默认 true
```

## 核心功能

### 全局异常处理 — WebExceptionHandler

`@RestControllerAdvice` 统一拦截异常并包装为 `Result` 返回。

**响应语义：**
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

### 异步支持

`SnowdriftExecutorConfiguration` 配置基于 `ThreadPoolTaskExecutor` 的异步线程池，通过 `TtlRunnable` 装饰器向 `@Async` 线程传播 `HttpContext`、`SecurityContext` 和 traceId；`SnowdriftAsyncConfiguration` 实现 `AsyncConfigurer` 注册线程池和未捕获异常处理器。

- **上下文传播**：非 HTTP 线程（定时任务/MQ 等）触发时 HttpContext 缺失也不报错。
- **拒绝策略**：线程池满时直接拒绝任务，抛出 `BizException(TOO_MANY_REQUESTS)`。
- **优雅关闭**：默认等待 60 秒让已提交任务完成。

```java
@Async
public CompletableFuture<Result<User>> getUser(Long id) {
    // SecurityContext.getUserId() 可用
    // LogTraceUtil.getTraceId() 可用
}
```

### 国际化（i18n）— 现状与方向

`I18nUtil` 提供按 key 取消息的工具方法；请求级语言由 `I18nInterceptor` 设置（URL 语言参数 > `Accept-Language` > 默认语言）。资源文件命名：各模块独立维护 `src/main/resources/i18n/<prefix>-messages_<locale>.properties`。

**重要说明：**
- 全局异常响应**不走 i18n**——消息为 `ResultCode` 内置中文或业务中文直文，避免未启用/缺 key 时把 key 透给前端。
- `snowdrift.i18n.enabled` 默认关闭；未启用（`messageSource==null`）时 `I18nUtil.getMessage(...)` 记 `log.warn` 后**原样返回 key**；已启用时是否回退 key 取决于 `use-code-as-default-message`（默认 `true` → 缺 key 返回 key 本身；设为 `false` → 缺 key 抛 `NoSuchMessageException`）。请勿依赖它对前端文案做翻译。
- 项目方向是**整体去掉 i18n**，各模块已陆续改为中文直文 / `ResultCode` 文案；届时 `I18nUtil` 相关链路会收敛或移除。

### XSS 防护 — Filter + MVC 双层防御

#### 第一层：XssFilter（请求参数/Header/QueryString）

`XssFilter` 对请求参数、请求头、QueryString 做 XSS 清洗。通过 `snowdrift.xss.enabled=true` 启用（默认关闭），`exclude-path-patterns` 排除指定路径（Ant 风格）。

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | Boolean | false | 是否启用 XSS 过滤 |
| `exclude-path-patterns` | List\<String\> | [] | 排除路径，不进行过滤 |

#### 第二层：XssJsonBodyAdvice（@RequestBody JSON Body）

`XssJsonBodyAdvice` 在 Spring MVC 层对 `@RequestBody` 反序列化后的对象递归清洗 String 值，弥补 `XssFilter` 仅覆盖参数/Header 而不覆盖 JSON Body 的防护盲区。与 `XssFilter` 共用同一个 `XssCleaner`。

**清洗范围：**

| 类型 | 清洗方式 |
|------|---------|
| `String` | 直接调用 `XssCleaner.clean()` |
| `Map` | 遍历 value，对 String 值递归清洗（不可变 Map 跳过） |
| `List` | 通过 `ListIterator.set()` 原地替换清洗后的值 |
| `Set` 等 Collection | 递归处理内部的 Map/Collection 结构 |
| 数组 | 通过 `Array.set()` 原地替换 |
| 复杂 POJO | 不做深度反射（安全边界） |

**安全防护：**
- 通过 `IdentityHashMap` 检测循环引用，防止无限递归
- 不可变集合 / 不可变 Map 静默跳过（捕获 `UnsupportedOperationException`）
- 与 `XssFilter` 共享 `exclude-path-patterns` 排除路径

#### 可插拔清洗器

默认使用 `SimpleXssCleaner` 做 HTML 实体转义（零额外依赖）：

| 原始字符 | 转义后 |
|---------|--------|
| `&` | `&amp;` |
| `<` | `&lt;` |
| `>` | `&gt;` |
| `"` | `&quot;` |
| `'` | `&#39;` |

`XssCleaner` 接口（`@FunctionalInterface`）支持注入自定义实现。例如管理后台场景可引入 Jsoup，用 Safelist 白名单保留富文本标签同时去掉恶意脚本：

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

`CachedBodyFilter` 在 Filter 链前端把普通 JSON/表单 Body 缓存为 `byte[]`，后续所有 Filter 和 Controller 均可重复调用 `getInputStream()` / `getReader()`。`CachedBodyRequestWrapper` 同时处理了 `x-www-form-urlencoded` 请求的表单参数解析，确保 Body 被缓存后 `getParameter()` 系列方法仍可正常使用。

**安全限流**：`multipart/*`、`application/octet-stream`、未知长度（chunked，`Content-Length < 0`）及 `Content-Length > 5MB` 的请求不缓存（避免大文件整包入内存），仍由下游单次消费。

### 链路追踪

`LogTraceFilter` 在请求入口执行：有入站 `X-Trace-Id` 则复用（网关/上游透传的链路 ID），否则生成新的 traceId，写入 MDC 与响应头；请求结束在 `finally` 中清理。

### 静态资源映射

通过 `snowdrift.resource.enabled=true` 启用（默认关闭），支持配置多个路径映射，将本地文件目录映射为 HTTP 可访问的静态资源。

### 日期时间格式化

模块在两个层面统一了日期时间格式：

| 层面 | 覆盖范围 | 格式 |
|------|---------|------|
| **Jackson JSON 序列化**（`SnowdriftJacksonTimeConfiguration`） | `@RequestBody` / `@ResponseBody` | `LocalDateTime` → `yyyy-MM-dd HH:mm:ss`；`LocalDate` → `yyyy-MM-dd`；`LocalTime` → `HH:mm:ss` |
| **Spring MVC 参数绑定**（`SnowdriftWebConfiguration.addFormatters`） | `@RequestParam` / 表单参数 | 同上 |

### CORS 跨域

通过 `snowdrift.cors.enabled=true` 启用（默认关闭）。生产环境请显式配置允许的源、方法和请求头，避免使用通配符 `*`。

## 过滤器链路

```
请求进入
  → CachedBodyFilter     (HIGHEST_PRECEDENCE)        缓存 Body，支持下游重复读取
  → HttpContextFilter    (HIGHEST_PRECEDENCE + 1)    填充 HttpContext（IP/URI/UA 等）
  → LogTraceFilter       (HIGHEST_PRECEDENCE + 5)    生成/复用 traceId，写入 MDC 与响应头
  → XssFilter            (HIGHEST_PRECEDENCE + 10)   XSS 清洗请求参数/Header/QueryString（可配开关/排除路径）
  → Controller
  → XssJsonBodyAdvice    (@ControllerAdvice)         XSS 清洗 @RequestBody JSON Body（与 XssFilter 共享开关/排除路径）
```
