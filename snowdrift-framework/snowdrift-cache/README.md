# snowdrift-cache

多后端缓存模块，提供统一的 `ICacheService` 编程式缓存 API + Spring Cache 支持 + 声明式分布式锁 + 防重复提交能力。

## 模块结构

```
snowdrift-cache
├── snowdrift-cache-base        ← 通用层：ICacheService / IDistributedLockService / ICacheSerializer，
│                                 序列化实现、@DistributedLock、@RepeatSubmit、缓存降级 & Key 生成
├── snowdrift-cache-caffeine    ← Caffeine 本地缓存实现（无外部依赖，兜底后端）
└── snowdrift-cache-redis       ← Redis 缓存（RedisTemplate）+ Redisson 分布式锁（含看门狗自动续期）
```

> 模块内不再有独立的 `-redisson` 子模块，Redisson 锁相关代码位于 `snowdrift-cache-redis`。

## 快速开始

按需引入一个后端模块即可，核心 API（`snowdrift-cache-base`）作为传递依赖自动引入。

```xml
<!-- 选一：Redis 缓存 + 分布式锁 -->
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-cache-redis</artifactId>
</dependency>

<!-- 或：Caffeine 本地缓存 -->
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-cache-caffeine</artifactId>
</dependency>
```

后端激活优先级：**Redis 存在时优先**；Caffeine 自动配置仅在容器中没有其它 `ICacheService` Bean 时生效（兜底）。引入 `snowdrift-cache-redis` 即同时获得分布式锁能力。

## 配置

```yaml
snowdrift:
  cache:
    key-prefix: app               # 默认空串，无前缀
    key-ttl: 1h
    max-size: 10000               # 仅 Caffeine 生效
    serializer: JACKSON           # JACKSON（默认）或 FASTJSON2
```

`key-prefix` 未配置时 key 不带前缀；配置后存储形如 `app:user:1`。

## 代码示例

### 缓存操作

注入 `ICacheService` 即可使用：

```java
@Autowired
private ICacheService cacheService;

User user = cacheService.get("user:1", User.class);
cacheService.put("user:1", user);
cacheService.put("user:1", user, Duration.ofMinutes(10));   // 自定义 TTL

// 不存在才写入（原子）
boolean success = cacheService.putIfAbsent("lock:pay:123", "1", Duration.ofSeconds(30));

cacheService.delete("user:1");
long count = cacheService.batchDelete(List.of("key1", "key2"));

boolean exists = cacheService.exists("user:1");
long ttl = cacheService.getExpire("user:1");   // 剩余秒数，-1=永不过期

// 原子自增并刷新过期时间（Lua INCR+EXPIRE），用于限流计数/失败次数统计
long n = cacheService.increment("login:fail:1", Duration.ofMinutes(5));
```

说明：
- `getExpire`：`-1` 表示永不过期；key 不存在时各实现自行约定（Redis 返回 -1/-2 透传语义、Caffeine 抛 `UnsupportedOperationException`），已不在接口契约中承诺 `-2`。
- Hash 操作（`hget/hput/hdelete`）与 `increment` 仅 Redis 后端支持。
- **per-key TTL 仅 Redis 支持**：Caffeine 对「带 TTL 写入 / expire / getExpire」会抛 `UnsupportedOperationException`，其缓存整体按全局 `key-ttl` 过期。

### 分布式锁 — @DistributedLock

```java
// key 必填，支持 SpEL 动态 key
@DistributedLock(key = "'order:pay:' + #orderId", waitTime = 3, leaseTime = 10)
public void payOrder(Long orderId) {
    // 获取锁后执行；获取失败抛 BizException
}
```

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `key` | **必填** | SpEL 表达式，锁的 key |
| `message` | `"操作正在处理中，请勿重复提交"` | 获取锁失败时的提示信息 |
| `waitTime` | 0 | 等待时间（秒），0 = 不等待、失败即抛 |
| `leaseTime` | -1 | 持有时间（秒），-1 = Redisson 看门狗自动续期 |
| `timeUnit` | SECONDS | 时间单位 |

编程式 API：

```java
@Autowired
private IDistributedLockService lockService;

boolean locked = lockService.tryLock("order:pay:1", 3, 10, TimeUnit.SECONDS);
if (locked) {
    try {
        // 业务
    } finally {
        lockService.unlock("order:pay:1");
    }
}

// 或直接用回调（自动加锁/解锁）
User user = lockService.executeWithLock("order:pay:1", 3, 10, TimeUnit.SECONDS,
        () -> orderService.doPay(orderId));
```

> - 分布式锁由 **Redisson** 提供，需引入 `snowdrift-cache-redis`；未引入时无 `IDistributedLockService` Bean，`@DistributedLock` 切面不生效（注解静默无效，请确保按需引入）。
> - `tryLock` 等待期间线程被中断会抛 `BizException("获取分布式锁被中断")`，与“锁竞争失败”语义区分。
> - `@DistributedLock` 拿到锁后由切面在 finally 释放；业务执行时长不应超过 `leaseTime`（非看门狗时），否则锁会被其它请求抢占。

### 防重复提交 — @RepeatSubmit

```java
// key 必填，默认 5 秒窗口内同一订单号只允许提交一次
@RepeatSubmit(key = "#orderNo", interval = 5)
public Result<Void> createOrder(String orderNo) {
    // ...
}
```

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `key` | **必填** | SpEL 表达式，请求标识 |
| `message` | `"请勿重复提交，请稍后重试"` | 命中重复时的提示信息 |
| `interval` | 5 | 防重时间窗口 |
| `timeUnit` | SECONDS | 时间单位 |

> **后端要求：** 防重依赖 per-key 过期能力，仅 Redis 后端生效；Caffeine 本地后端不支持，使用时会抛 `UnsupportedOperationException`。
> **注意：** `interval` 应大于业务方法的最长执行时间。若执行时长超过 TTL，标记会在处理中过期，并发请求可能穿透幂等防护。
> 业务执行抛出异常时切面会删除标记（尽力而为，删除失败仅记日志），允许用户修正后重新提交。

### 序列化器切换

```yaml
snowdrift.cache.serializer: FASTJSON2
```

或注册自定义 `ICacheSerializer` Bean 完全替换：

```java
@Component
public class ProtoBufSerializer implements ICacheSerializer {
    @Override public String serialize(Object value) { /* ... */ }
    @Override public <T> T deserialize(String json, Class<T> type) { /* ... */ }
}
```

> **安全/兼容提示：**
> - Jackson 默认序列化器启用 `DefaultTyping`，会写入 `@class` 类型元数据：实体**重命名/移动包**会导致旧缓存读取失败，需清理缓存后重建；仅信任应用自身写入的缓存。
> - `@Cacheable` 走 Spring Cache 时，Redis 值序列化器（Jackson/Fastjson2）同样依赖类型元数据还原多态对象；`FastJson2RedisSerializer` 启用了 `SupportAutoType`，前提是缓存数据由本应用写入、无外部篡改风险。
> - 切换序列化器会改变存储格式，切换后旧缓存无法读取，建议清库或灰度。

### 缓存降级

`SnowdriftCachingErrorHandler` 对 Spring Cache 注解（`@Cacheable` / `@CachePut` / `@CacheEvict`）执行异常记录 WARN 后静默放行：GET 失败降级穿透、PUT/EVICT 失败跳过写入，缓存故障不阻断主流程。

## 后端对比

| 特性 | Caffeine | Redis（+Redisson） |
|------|----------|--------------------|
| 部署 | 进程内，无外部依赖 | 需 Redis 服务 |
| per-key TTL | ❌（调用抛 `UnsupportedOperationException`，整体按全局 `key-ttl`） | ✅ |
| Hash 操作 / increment | ❌ | ✅ |
| 分布式锁 | ❌ | ✅ Redisson 看门狗自动续期 |
| 多实例共享 | ❌ | ✅ |
| Spring Cache | ✅ CaffeineCacheManager | ✅ RedisCacheManager（统一前缀 + TTL） |

> 如需更底层能力（如通配扫描 keys），直接使用各后端原生 API（Caffeine `Cache` / RedisTemplate）。

## 配置属性参考

### snowdrift.cache

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `key-prefix` | String | `""` | key 全局前缀（空串无前缀） |
| `key-ttl` | Duration | `1h` | 全局默认 TTL |
| `max-size` | long | `10000` | Caffeine 最大条目数 |
| `serializer` | SerializerType | `JACKSON` | 序列化器（JACKSON / FASTJSON2） |
