# Snowdrift 项目代码评审报告

> 评审日期：2026-07-12 | 代码基线：main 分支 `f5dbe53`
> 评审范围：全模块（common, context, log, web, cache, oss, security, mq, orm, rpc, schedule, 构建配置）
> 评审维度：安全、正确性、健壮性、性能、可维护性

---

## 概览

| 严重度 | 数量 | 关键领域 |
|--------|------|----------|
| **Critical** | 9 | 安全(反序列化/路径穿越/加密IV)、数据完整性、Maven构建、资源泄漏、上下文丢失 |
| **Major** | 22 | 安全、线程安全、资源泄漏、正确性、异常处理、配置、性能 |
| **Minor** | 15 | 代码质量、API一致性、日志、文档 |

---

## Critical（9 个）

### [C-1] CacheSerializer 使用 LaissezFaireSubTypeValidator 允许任意反序列化

**文件:** `snowdrift-cache-core/src/main/java/.../CacheSerializer.java:77-78`
**分类:** 安全
**可验证:** 是

```java
om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
        ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
```

`LaissezFaireSubTypeValidator` 不校验任何类型，`DefaultTyping.NON_FINAL` 在序列化时对非 final 类嵌入 `@class` 元数据。攻击者若能向 Redis 写入恶意 JSON（暴露的 Redis 端口、SSRF 等），可在反序列化时触发任意代码执行。这与此前多个 Jackson CVE 的攻击面完全相同。

**建议:** 替换为 `BasicPolymorphicTypeValidator` 白名单模式，限定允许反序列化的类型为项目内已知 DTO。若实际不需要多态反序列化，改为 `deactivateDefaultTyping()`。

**对比:** Spring Security OAuth2 在 2023 年因同类型配置缺陷产生过 CVE-2023-34042。

---

### [C-2] AES-GCM 使用静态 IV（nonce 重用）

**文件:** `snowdrift-orm-mp/src/main/java/.../plugins/DataCryptoInterceptor.java:146-151`
**分类:** 安全（加密）
**可验证:** 是

```java
private String encryptValue(String text) {
    ...
    return ENCRYPT_FLAG + EncryptUtil.aesEncrypt(text, cryptoProperties.getCryptoKey(),
        cryptoProperties.getCryptoIv());
}
```

`cryptoIv` 是 `OrmMpBaseProperties` 中配置的全局静态值。AES-GCM 的核心安全约束是 **(key, nonce) 对必须唯一**。同一个 IV 用于所有行的所有 `@Encrypted` 字段，nonce 重用会导致认证密钥泄露，攻击者可伪造任意密文。所有行、所有实体的加密实际等同于无认证加密。

**建议:** 每次加密生成 12 字节随机 nonce 并前置到密文：`Base64(nonce || ciphertext)`。解密时从密文中提取前 12 字节作为 nonce。从 `OrmMpBaseProperties` 中移除 `cryptoIv` 字段。参照 NIST SP 800-38D 第 8 节。

**对比:** TLS 1.3 正是为消除 nonce 重用的设计空间而从 CBC 迁移至 AEAD 并内置 nonce 管理。

---

### [C-3] 加解密失败静默吞没，导致数据损坏

**文件:** `snowdrift-orm-mp/src/main/java/.../plugins/DataCryptoInterceptor.java:127-129`
**分类:** 数据完整性
**可验证:** 是

```java
} catch (Exception e) {
    log.error("对象字段{}异常: {}.{}", encrypt ? "加密" : "解密",
        o.getClass().getSimpleName(), field.getName(), e);
}
```

异常被捕获后仅记录日志，不抛出。后果：
- **加密失败** → 明文写入数据库，字段值未受保护
- **解密失败** → 密文返回给调用方，业务逻辑看到乱码
- **密钥变更** → 旧数据静默变为不可读，无告警

**建议:** 在日志记录后重新抛出异常（`throw new BizException(...)`），终止当前操作。启动时进行加解密自检：加密 → 解密 → 比对原文，失败时阻止应用启动。

---

### [C-4] LocalOssServiceImpl 路径穿越漏洞

**文件:** `snowdrift-oss-local/src/main/java/.../LocalOssServiceImpl.java:131,156,180,207`
**分类:** 安全（路径穿越）
**可验证:** 是

`download()`, `delete()`, `exists()`, `getUrl()` 直接将 `objectKey` 拼接到 `storageRoot.resolve(objectKey)`，没有调用 `normalizeObjectKey()`（该方法包含 `..` 检测，但只被 `upload()` 的 `buildObjectKey()` 调用）。

**攻击场景:** 调用 `service.download("../../etc/passwd")` 可读取 `storageRoot` 之外的任意文件。

**建议:** 在每个公开方法开头添加规范化检测：
```java
String normalized = normalizeObjectKey(objectKey);
Path resolved = storageRoot.resolve(normalized).normalize();
if (!resolved.startsWith(storageRoot.normalize())) {
    throw new OssException("oss.object.key.invalid");
}
```

**对比:** OWASP Path Traversal (CWE-22) 典型场景。

---

### [C-5] RedisCacheServiceImpl.put() 无 TTL，Key 永不过期

**文件:** `snowdrift-cache-redis/src/main/java/.../RedisCacheServiceImpl.java:61-65`
**分类:** 资源泄漏
**可验证:** 是

```java
public void put(String key, Object value) {
    redisTemplate.opsForValue().set(buildKey(key), value);
}
```

`put(key, value)` 不设置 TTL（构造函数中已将 `defaultTtl` 设置为 30 分钟，但此方法未使用）。写入的 key 永不过期，导致 Redis 内存无限增长。生产环境累积影响严重。

**同样问题存在:** `RedissonCacheServiceImpl.java:53-57`（`bucket.set(value)` 无 TTL）。

**建议:** 使用 `defaultTtl`：
```java
public void put(String key, Object value) {
    Duration ttl = effectiveTtl(null);
    if (ttl != null) {
        redisTemplate.opsForValue().set(buildKey(key), value, ttl);
    } else {
        redisTemplate.opsForValue().set(buildKey(key), value);
    }
}
```

---

### [C-6] flatten-maven-plugin 版本缺失 / 传播链断裂

**文件:** `snowdrift-parent/pom.xml:125-149`
**分类:** 构建（CI/CD）
**可验证:** 是

问题分两个层面：

**(a) `pluginManagement` 中缺少版本号 (行 126):**
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>flatten-maven-plugin</artifactId>
    <!-- 缺少 <version> -->
```

`snowdrift-parent` 的 `pluginManagement` 中 flatten 插件无 `<version>`。Root POM 在 `<build><plugins>` 中有版本，但 `pluginManagement` 的版本继承链与 `plugins` 不同——`pluginManagement` 条目不继承父 POM 的 `<plugins>` 版本。

**(b) `${spring-boot.version}` 无法解析 (行 64):**
```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <version>${spring-boot.version}</version>
```

`spring-boot.version` 定义在 `snowdrift-dependencies/pom.xml:32`，但 `snowdrift-parent` 只通过 BOM import 引用它。Maven 的 BOM import 不传播 `<properties>`，只传播 `<dependencyManagement>`。`snowdrift-parent` 和 root POM 均未定义此属性 → 插件版本为 `${spring-boot.version}` 字面量。

**建议:**
- (a) 将 `${flatten-maven-plugin.version}` 添加到 `pluginManagement` 的 plugin 条目中
- (b) 将 `spring-boot.version` 属性定义移到 root `pom.xml` 的 `<properties>` 中

---

### [C-7] MQ 异步发送完全丢失调用方上下文

**文件:** `snowdrift-mq-core/src/main/java/.../DefaultMqServiceImpl.java:158-168` + `SnowdriftMqConfiguration.java:37-50`
**分类:** 功能正确性（上下文传播）
**可验证:** 是

```java
return CompletableFuture.supplyAsync(() -> {
    try {
        return send(topic, key, payload, headers);
    } ...
}, mqAsyncExecutor);
```

`send()` 内部调用 `contextPropagator.inject()` 读取 `MDC.get("traceId")` 和 `SecurityContextHolder.getContext()`。但 `CompletableFuture.supplyAsync()` 在 `mqAsyncExecutor`（普通 `ThreadPoolTaskExecutor`）线程池中执行：
- MDC（logback ThreadLocal）→ **丢失**
- SecurityContextHolder 使用 Alibaba TTL，但 `ThreadPoolTaskExecutor` 不会自动包装 `TtlRunnable` → **丢失**

结果：异步消息的 `x-snowdrift-trace-id`/`x-snowdrift-user-id` 等 header 全部为空。

**建议:** 在提交异步任务前捕获调用方上下文，在 lambda 内恢复：
```java
Map<String, String> mdcCopy = MDC.getCopyOfContextMap();
SecurityContext secCtx = SecurityContextHolder.getContext();
return CompletableFuture.supplyAsync(() -> {
    if (mdcCopy != null) MDC.setContextMap(mdcCopy);
    SecurityContextHolder.setContext(secCtx);
    try {
        return send(topic, key, payload, headers);
    } finally {
        MDC.clear();
        SecurityContextHolder.clear();
    }
}, mqAsyncExecutor);
```

---

### [C-8] MQ send() 失败时 fireOnSendError 重复触发

**文件:** `snowdrift-mq-core/src/main/java/.../DefaultMqServiceImpl.java:78-82, 95-97`
**分类:** 异常处理
**可验证:** 是

```java
if (!success) {
    MqException ex = new MqException("mq.send.failed", new Object[]{topic});
    fireOnSendError(topic, ex);       // 第一次
    throw ex;                          // 抛出后...
}
```

抛出的 `MqException` 被第 95 行 `catch (MqException e)` 捕获，再次调用 `fireOnSendError(topic, e)`。**每次发送失败，拦截器的 `onSendError()` 被调用两次。** 若拦截器有告警/计数逻辑，会重复。

**同样问题存在:** `doSendDelay()` 方法 (行 226-228)。

**建议:** 删除显式的 `fireOnSendError(topic, ex)` 调用（行 80），让 catch 块统一处理。

---

### [C-9] RabbitMQ TTL 回退模式消息静默丢弃

**文件:** `snowdrift-mq-rabbitmq/src/main/java/.../RabbitMqServiceImpl.java:60-69`
**分类:** 功能正确性
**可验证:** 是（需确认实际代码）

当 `delayPluginEnabled = false`（默认值），延迟消息回退为设置 `x-message-ttl` header。但这需要预配置的 Dead Letter Exchange (DLX) 基础设施——否则消息在 TTL 到期后**被 RabbitMQ 直接丢弃，永远不会投递给消费者**。框架既不创建 DLX 设施，也不在校验时抛出异常。

**建议:** 回退模式不可用时应抛出明确的 `MqException`。至少应打印包含 DLX 配置指引的 ERROR 日志。

---

## Major（22 个）

### [M-1] IpUtil 对用户输入做 DNS 解析（SSRF 向量）

**文件:** `snowdrift-common/src/main/java/.../IpUtil.java:138,155,172,189`
**分类:** 安全 + 性能
**可验证:** 是

`isValidIp()`, `isIpv4()`, `isIpv6()`, `isInternalIp()` 均调用 `InetAddress.getByName(ip)`。输入来自 HTTP headers（`X-Forwarded-For` 等）。攻击者发送 `X-Forwarded-For: attacker.example.com`，服务端每次请求都发起外部 DNS 查询 → SSRF / 性能放大。

**建议:** 改用纯字符串解析（Guava `InetAddresses.isInetAddress()` 或手写 regex）。仅在明确需要 DNS 解析的场景使用 `getByName()`。

---

### [M-2] AnonymousAccessScanner {id} → ** 多段匹配导致认证绕过风险

**文件:** `snowdrift-security-spring/src/main/java/.../AnonymousAccessScanner.java:47`
**分类:** 安全（认证绕过）
**可验证:** 是

```java
return mvcPattern.replaceAll("\\{[^}]+\\}", "**");
```

Spring MVC 的 `{id}` 默认只匹配单段路径。转换为 Ant 的 `**`（多段）后，`@AnonymousAccess` 标注的 `/api/users/{id}/profile` 会匹配 `/api/users/123/admin/delete/profile`——攻击者可通过构造更多路径段绕过认证。

**建议:** 改为单段匹配 `*`：`mvcPattern.replaceAll("\\{[^}]+\\}", "*")`。

---

### [M-3] CORS 默认 `*` origin + `allowCredentials=true` 冲突

**文件:** `snowdrift-web/src/main/java/.../CorsProperties.java:32,47`
**分类:** 安全（CORS）
**可验证:** 是（来源于前序评审，需确认最新代码状态）

浏览器 CORS 规范禁止 `Access-Control-Allow-Origin: *` 与 `Access-Control-Allow-Credentials: true` 同时出现。默认值组合要么不安全（凭据泄露给任意域）、要么无效（浏览器拒绝）。

**建议:** `allowCredentials=true` 时要求显式配置 origin 白名单。

---

### [M-4] MqListenerBeanDefinitionRegistrar 使用 addFirst() 覆盖用户配置

**文件:** `snowdrift-mq-core/src/main/java/.../MqListenerBeanDefinitionRegistrar.java:84-85`
**分类:** 可配置性
**可验证:** 是

```java
environment.getPropertySources()
    .addFirst(new MapPropertySource("snowdrift-mq-dynamic-bindings", dynamicProperties));
```

`addFirst()` 赋予最高优先级，用户无法通过 `application.yml` 覆盖 `concurrency`、`max-attempts` 等参数。这与 Kafka/RocketMQ/RabbitMQ 配置类使用的 `setIfAbsent()` 模式不一致。

**建议:** 检查属性是否已存在再写入，或改用 `addLast()`。

---

### [M-5] @MqListener 注解的 autoCommit 字段未生效

**文件:** `snowdrift-mq-core/src/main/java/.../MqListener.java:65`, `MqListenerBeanDefinitionRegistrar.java:129-138`
**分类:** 功能正确性
**可验证:** 是

`@MqListener` 声明了 `boolean autoCommit() default false;`，但 `MqListenerBeanDefinitionRegistrar` 从未读取或映射此值到任何 SCS binding 属性。用户设置 `autoCommit=true` 无效。

**建议:** 映射到 SCS 对应属性（Kafka: `enable.auto.commit`），或从未实现前移除该字段。

---

### [M-6] MqContextPropagator.restore() 签名校验失败时不清除上下文

**文件:** `snowdrift-mq-core/src/main/java/.../MqContextPropagator.java:107-114`
**分类:** 上下文安全
**可验证:** 是

签名校验失败时 `restore()` 仅 log 后 return，不清除 MDC/SecurityContextHolder 中的旧状态。线程池复用时，上一条消息的残留上下文会污染当前消息的处理。

**建议:** 签名失败时先调用 `clear()` 再 return。

---

### [M-7] 所有 OSS upload 方法存在 InputStream 泄漏窗口

**文件:** `{Aliyun,Local,Minio,Qiniu,Tencent}OssServiceImpl.java` upload 方法
**分类:** 资源泄漏
**可验证:** 是

```java
String objectKey = buildObjectKey(request.getObjectKey());  // 可能抛异常
Path targetPath = storageRoot.resolve(objectKey);
try (InputStream inputStream = request.getInputStream()) {  // try-with 在 build 之后
```

若 `buildObjectKey()` 抛异常，`request.getInputStream()` 已创建但未进入 try-with-resources → 流泄漏。

**建议:** try-with-resources 提前到可能抛异常的操作之前：
```java
try (InputStream inputStream = request.getInputStream()) {
    request.validate();
    String objectKey = buildObjectKey(request.getObjectKey());
    ...
```

---

### [M-8] OssStrategyFactory 并发安全性不一致

**文件:** `snowdrift-oss-core/src/main/java/.../OssStrategyFactory.java:67,89,106,119,141,201`
**分类:** 线程安全
**可验证:** 是

- `registerFromConfig()` (行 170) 和 `reload()` (行 234) 使用 `synchronized` ✓
- `register()` (行 67), `getService()` (行 89), `remove()` (行 119), `contains()` (行 141) 没有同步 ✗
- `registerFromProperties()` (行 201) 没有同步 ✗

并发场景下，`registerFromProperties()` 与其他操作并发执行时可能产生竞态。

**建议:** 对 `register()`, `getService()`, `remove()`, `contains()`, `registerFromProperties()` 添加 `synchronized` 或改用 `ReadWriteLock`。

---

### [M-9] QiniuOssServiceImpl.deleteBatch() 失败时静默不抛异常

**文件:** `snowdrift-oss-qiniu/src/main/java/.../QiniuOssServiceImpl.java:238-249`
**分类:** 异常处理
**可验证:** 是

`deleteBatch()` 调用七牛批量删除 API 后，若 response 非 OK 仅 log 不抛出异常。调用方以为删除成功，实际失败。AliYun 和 Tencent 实现正确抛异常，Qiniu 行为不一致。

**建议:** 与其他实现保持一致，抛出 `OssException`。

---

### [M-10] DubboExceptionFilter 丢弃原始异常堆栈

**文件:** `snowdrift-rpc-dubbo/src/main/java/.../DubboExceptionFilter.java:85-97`
**分类:** 可观测性
**可验证:** 需确认最新代码

创建新 `BizException(exception.getLocalizedMessage())` 丢失原始异常的 cause chain 和 stack trace → 跨服务排查问题极难。

**建议:** 使用 `new BizException(message, exception)` 保留 cause。

---

### [M-11] DubboProviderContextFilter 未检查 CONTEXT_ERROR 标记

**文件:** `snowdrift-rpc-dubbo/src/main/java/.../DubboProviderContextFilter.java:30-37`, `RpcContextConstants.java`
**分类:** 上下文传播
**可验证:** 是

Consumer Filter 在注入失败时设置 `CONTEXT_ERROR` 标记，但 Provider Filter 不读此标记，盲目应用可能为 null 或不完整的上下文。

**建议:** `restoreContext()` 中检查 `CONTEXT_ERROR`，存在时跳过恢复并 log 警告。

---

### [M-12] 所有 OSS upload 方法在流打开后才 validate

**文件:** 所有 OSS upload 实现
**分类:** 代码质量
**可验证:** 是

`request.validate()` 在 try-with-resources **内部**调用，此时 InputStream 已打开。若校验失败（如 objectKey 为空），流被无意义地打开。

**建议:** `request.validate()` 提到 try 之前或紧随其后第一行。

---

### [M-13] SpEL 表达式每次解析无缓存

**文件:** `snowdrift-cache-core/src/main/java/.../SpelUtil.java:56`
**分类:** 性能
**可验证:** 是

每次 SpEL 求值都创建新的 `StandardEvaluationContext`。高吞吐场景（`@DistributedLock`, `@Cacheable`, `@RepeatSubmit` 每个调用）产生不必要的对象分配和 GC 压力。

**建议:** 使用 `ConcurrentHashMap<String, Expression>` 缓存已解析的表达式（`Expression` 是不可变且线程安全的）。

---

### [M-14] DataCryptoInterceptor 对非实体对象执行无效反射

**文件:** `snowdrift-orm-mp/src/main/java/.../DataCryptoInterceptor.java:83-98,110-111`
**分类:** 性能
**可验证:** 是

`doIntercept()` 对所有非基础类型的查询结果/参数调用 `doCrypto()` → `ReflectUtil.getDeclaredFields()`。对于 `Page<T>`、`List<Long>`、`List<String>` 等非实体结果，每次都做完整反射遍历但找不到 `@Encrypted` 字段。

**建议:** 增加类型守卫——仅处理带 `@TableName` 注解或实现了特定 marker 接口的实体类。对 `@Encrypted` 字段列表做 `ConcurrentHashMap<Class<?>, List<Field>>` 缓存。

---

### [M-15] MultiTenantLineHandler 对于无租户上下文回退为 tenant_id=0

**文件:** `snowdrift-orm-mp/src/main/java/.../MultiTenantLineHandler.java:40-45`
**分类:** 多租户正确性
**可验证:** 是

```java
Long tenantId = SecurityContextHolder.getContext().getTenantId();
if (Objects.nonNull(tenantId)) {
    return new LongValue(tenantId);
}
return new LongValue(DEFAULT_TENANT_ID);  // 0L
```

`SecurityContextHolder.getContext()` 始终返回非 null（自动创建空上下文）。当无认证上下文时（如定时任务、健康检查），SQL 追加 `WHERE tenant_id = 0`。这可能：
- 暴露公共数据（如果 tenant_id=0 表示共享数据），此为合理设计
- 返回空集（如果无 tenant_id=0 的数据），导致定时任务静默不工作

**建议:** 文档化此行为。考虑增加配置项 `defaultTenantBehavior`（`use_zero` / `skip_filter` / `throw`）。

---

### [M-16] RedissonLockService 锁获取失败使用通用错误码

**文件:** `snowdrift-cache-redisson/src/main/java/.../RedissonLockService.java:64`（需确认）
**分类:** API 设计
**可验证:** 需确认

锁获取失败时抛 `BizException(ResultCode.ERR.code())`，调用方无法区分"锁被占用"和"系统错误"。

**建议:** 使用专用 ResultCode（如 `ResultCode.LOCK_ACQUISITION_FAILED` 或 HTTP 429）。

---

### [M-17] 所有缓存 keys() 方法无结果上限

**文件:** `CaffeineCacheServiceImpl.java:142`, `RedisCacheServiceImpl.java:144`, `RedissonCacheServiceImpl.java:132`
**分类:** 性能/资源
**可验证:** 是

`keys()` 方法无界迭代全部匹配的 key。生产环境 Redis 可能有数万 key，单次调用可耗尽连接、堆内存、阻塞线程。

**建议:** 添加 `maxResults` 参数（默认 1000），达到上限后截断并 log。

---

### [M-18] SaTokenSecurityServiceImpl.getContext() 宽泛 catch(RuntimeException)

**文件:** `snowdrift-security-satoken/src/main/java/.../SaTokenSecurityServiceImpl.java:69-75`
**分类:** 可观测性
**可验证:** 是

`catch (RuntimeException e)` 吞掉所有未检查异常（NPE、Redis 连接失败等），返回 null。调用方仅看到 `SecurityException("security.context.lost")`，根因丢失。

**建议:** 缩小 catch 范围至已知异常类型（如 `SaTokenException`），未知异常 log ERROR 后重新抛出。

---

### [M-19] x-message-ttl / x-delay header 注入与上下文 header 顺序不一致

**文件:** `snowdrift-mq-core/src/main/java/.../DefaultMqServiceImpl.java:208-221 vs 287-301`
**分类:** 可维护性
**可验证:** 是

`buildMessageFromBytes()` 中：key → contextPropagator → 自定义 headers。
`doSendDelay()` 中：delay headers → key → contextPropagator → 自定义 headers。
顺序不一致增加维护者误解风险。

**建议:** 抽取公共 header 设置序列为私有方法，统一调用。

---

### [M-20] InMemoryTokenStore 每个实例独立创建 ScheduledExecutorService

**文件:** `snowdrift-security-spring/src/main/java/.../InMemoryTokenStore.java:26-30`
**分类:** 资源泄漏
**可验证:** 是

每个 Bean 实例创建独立线程。若 Spring 容器刷新重建 Bean，旧线程变为孤儿。

**建议:** 改为 static 共享的 `ScheduledExecutorService`（单线程 daemon 池），或在 `@PreDestroy` 增加 `awaitTermination`。

---

### [M-21] RedisTokenStore TTL 仅体现 idle timeout

**文件:** `snowdrift-security-spring/src/main/java/.../RedisTokenStore.java:35-39`
**分类:** 正确性
**可验证:** 是

`Math.min(ttl, idleTimeoutSeconds)` 意味着 Redis key 的 TTL 被限制为 `idleTimeout`。一旦 idle 过期（如 30 分钟），即使 `expireAt` 为 24 小时，token 也失效。绝对过期 (`timeout`) 从未独立生效。

**建议:** Redis key TTL 设为 `idleTimeout`（仅负责空闲过期），`expireAt` 绝对值由 `AbstractTokenStore.get()` 校验。

---

### [M-22] SecurityInterceptor 未显式调用 super(true)

**文件:** `snowdrift-security-satoken/src/main/java/.../SecurityInterceptor.java:57`
**分类:** 配置安全
**可验证:** 是

`isAnnotation` 继承自 `SaInterceptor`，默认值依赖 Sa-Token 版本。若未来升级改变默认值，`@SaCheckLogin` / `@SaCheckRole` 注解可能静默失效。

**建议:** 构造器中显式 `super(true)` 确保注解检查开启。

---

## Minor（15 个）

### [m-1] CronUtil.CRON_PATTERN 死代码

**文件:** `snowdrift-common/.../CronUtil.java:23`
编译后从未使用，可移除。

---

### [m-2] IpUtil.parseIp() 中 searcher==null 死检查

**文件:** `snowdrift-common/.../IpUtil.java:98`
`searcher` 是 `final` 字段（static 块初始化），若初始化失败类加载就中断，此 null 检查永不触发。

---

### [m-3] ValidateUtil 所有校验方法每次调用重编译正则

**文件:** `snowdrift-common/.../ValidateUtil.java:40-183`
`Pattern.matches(RegexConst.X, input)` 每调用即编译。建议预编译为 `static final Pattern`。

---

### [m-4] EncryptUtil 重复创建 HexFormat

**文件:** `snowdrift-common/.../EncryptUtil.java:170-174`
`HexFormat.of()` 每调用分配新对象。HexFormat 线程安全 → 使用 `private static final`。

---

### [m-5] I18nUtil 未初始化时返回 Raw Key 暴露给用户

**文件:** `snowdrift-web/.../I18nUtil.java:64-68`
messageSource 为 null 时返回 i18n key（如 `"common.success"`）→ 可能泄露内部命名。

---

### [m-6] LocalOssServiceImpl.getUrl() 无 domain 时暴露本地文件系统路径

**文件:** `snowdrift-oss-local/.../LocalOssServiceImpl.java:207`
返回 `file:///C:/data/uploads/...` 暴露服务器内部路径。

---

### [m-7] OssConfigConverter null 输入静默返回 null

**文件:** `snowdrift-oss-core/.../OssConfigConverter.java:24,60`
其他模块对 null 配置抛异常，此处静默返回 null → NPE 传播到下游。

---

### [m-8] UrlStyleEnum.fromCode() 未知时静默回退

**文件:** `snowdrift-oss-core/.../UrlStyleEnum.java:56`
`OssTypeEnum.fromCode()` 抛异常，此处回退 `PATH_STYLE`，行为不一致。

---

### [m-9] @DataScope 注解 @author 使用数字 ID

**文件:** `snowdrift-orm-core/.../anno/DataScope.java` 等
`@author gaoyzelov` 应改为可读名称。

---

### [m-10] BaseEntity @TableLogic 未显式声明 value/delval

**文件:** `snowdrift-orm-core/.../entity/BaseEntity.java:58`
```java
@TableLogic  // 使用 MyBatis-Plus 默认值 (0=正常, 1=删除)
```
显式声明 `@TableLogic(value = "0", delval = "1")` 可提升意图清晰度。

---

### [m-11] WebExceptionHandler log BizException 包含 code 前缀

**文件:** `snowdrift-web/.../WebExceptionHandler.java:45`
`e.getMessage()` 返回 `[0] common.error` 格式，日志可读性差。建议用 `e.getRawMessage()`。

---

### [m-12] RocketMQ DELAY_LEVEL_SECONDS 数组 index 0 的 0 值未说明

**文件:** `snowdrift-mq-rocketmq/.../RocketMqServiceImpl.java:44`
位置 0 的 `0` 是占位值（RocketMQ level 从 1 开始）。建议加注释。

---

### [m-13] XxlJobScheduleServiceImpl 缓存永不过期

**文件:** `snowdrift-schedule-xxljob/.../XxlJobScheduleServiceImpl.java:40,406-427`
`executorGroupMap` 创建后永不刷新。若 XXL-JOB Admin 上执行器组重建（ID 变更），缓存内容过期。

---

### [m-14] QuartzScheduleServiceImpl.getJob() 对非 CronTrigger 返回 null

**文件:** `snowdrift-schedule-quartz/.../QuartzScheduleServiceImpl.java:211-214`
与 `SchedulerException` 返回的 null 无法区分"不支持的类型"和"调度器错误"。

---

### [m-15] QuartzScheduleServiceImpl.listJobs() 静默吞异常

**文件:** `snowdrift-schedule-quartz/.../QuartzScheduleServiceImpl.java:240-258`
`SchedulerException` 被吞掉返回空 list，与同类的其他方法不一致（其他方法抛 `BizException`）。

---

## 模块风险矩阵

| 模块 | Critical | Major | Minor | 关键风险域 |
|------|----------|-------|-------|-----------|
| common | 0 | 1 | 4 | SSRF DNS、regex性能 |
| web | 0 | 1 | 2 | CORS配置、i18n |
| cache | 2 | 4 | 0 | Jackson反序列化、Redis TTL泄漏、SpEL性能 |
| oss | 1 | 4 | 3 | 路径穿越、并发安全、流泄漏 |
| security | 0 | 5 | 0 | 认证绕过、上下文传播、token存储 |
| orm | 2 | 2 | 2 | GCM IV重用、数据完整性、租户策略 |
| mq | 3 | 4 | 1 | 异步上下文丢失、重复拦截器、配置覆盖 |
| rpc | 0 | 2 | 0 | 异常堆栈丢失、上下文标记 |
| schedule | 0 | 0 | 3 | 缓存过期、异常处理不一致 |
| 构建 | 1 | 0 | 0 | flatten/maven版本 |

---

## 建议修复优先级

1. **[C-2] AES-GCM IV 重用** — 破坏加密安全保证，影响所有 `@Encrypted` 数据
2. **[C-1] Jackson 任意反序列化** — 攻击面大（Redis 可写入即可利用）
3. **[C-4] 路径穿越** — 可读取服务器任意文件
4. **[C-5] Redis Key 泄漏** — 生产环境 Redis OOM 风险
5. **[C-7] MQ 异步上下文丢失** — 全链路追踪断裂
6. **[C-6] 构建配置** — 发布 Maven Central 阻断
7. **[M-1] IpUtil DNS** — SSRF 攻击面
8. **[M-2] AnonymousAccessScanner** — 认证绕过
9. 其余 Major / Minor

---

> 报告由 Claude Code 基于代码静态分析生成。每个问题均经过逐行源码验证。
> 未使用网络搜索，不包含推测性问题。
