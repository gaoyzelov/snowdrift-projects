# snowdrift-cache

多后端缓存模块，提供统一的 `ICacheService` API + 声明式分布式锁 + 防重复提交能力。

## 模块结构

```
snowdrift-cache
├── snowdrift-cache-core        ← 通用层：ICacheService、CacheSerializer、@DistributedLock、@RepeatSubmit
├── snowdrift-cache-caffeine    ← Caffeine 本地缓存（无外部依赖）
├── snowdrift-cache-redis       ← Redis 缓存（基于 RedisTemplate）
└── snowdrift-cache-redisson    ← Redisson 缓存 + 分布式锁（看门狗自动续期）
```

## 快速开始

按需引入一个后端实现即可，核心 API（`snowdrift-cache-core`）会作为传递依赖自动引入。

```xml
<!-- 选择一个后端 -->
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-cache-redisson</artifactId>
</dependency>
<!-- 或 caffeine / redis -->
```

后端自动选择优先级：**Redisson > Redis > Caffeine**。未引入任何后端时，Caffeine 作为兜底。

## 配置

```yaml
snowdrift:
  cache:
    key-prefix: app               # 默认 null，无前缀
    key-ttl: 30m
    max-size: 10000
    serializer: JACKSON           # JACKSON（默认）或 FASTJSON2
```

## 代码示例

### 缓存操作

注入 `ICacheService` 即可使用：

```java
@Autowired
private ICacheService cacheService;

// 读写
User user = cacheService.get("user:1", User.class);
cacheService.put("user:1", user);
cacheService.put("user:1", user, Duration.ofMinutes(10));  // 自定义 TTL

// 不存在才写入（原子操作）
boolean success = cacheService.putIfAbsent("lock:pay:123", "1", Duration.ofSeconds(30));

// 删除
cacheService.delete("user:1");
long count = cacheService.delete(List.of("key1", "key2"));  // Collection<String>, 返回删除数量

// 查询
boolean exists = cacheService.exists("user:1");
long ttl = cacheService.getExpire("user:1");  // 秒，-1=永不过期，-2=key 不存在
Set<String> keys = cacheService.keys("user:*");  // 通配符：支持 * 和 ?
```

### 分布式锁 — @DistributedLock

```java
// key 必填，支持 SpEL 动态 key
@DistributedLock(key = "'order:pay:' + #orderId", waitTime = 3, leaseTime = 10)
public void payOrder(Long orderId) {
    // 获取锁后执行，失败时抛出 BizException
}

// 注入编程式 API
@Autowired
private DistributedLockService lockService;

boolean locked = lockService.tryLock("key", 3, 10, TimeUnit.SECONDS);
// ...
lockService.unlock("key");
```

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `key` | **必填** | SpEL 表达式，锁的 key |
| `message` | `"cache.lock.failed"` | 失败时的提示信息（i18n key 或直接文本） |
| `args` | {} | i18n 参数 |
| `waitTime` | 0 | 等待时间（秒），0=立即失败 |
| `leaseTime` | -1 | 持有时间（秒），-1=Redisson 看门狗自动续期 |
| `timeUnit` | SECONDS | 时间单位 |

> 分布式锁仅 Redisson 后端支持。Caffeine/Redis 后端会抛出异常。

### 防重复提交 — @RepeatSubmit

```java
// key 必填，5 秒内同一订单号只允许一次提交
@RepeatSubmit(key = "#orderNo", interval = 5)
@PostMapping("/order")
public Result<Void> createOrder(@RequestBody String orderNo) {
    // ...
}
```

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `key` | **必填** | SpEL 表达式 |
| `message` | `"cache.repeat.submit"` | 失败时的提示信息（i18n key 或直接文本） |
| `args` | {} | i18n 参数 |
| `interval` | 5 | 时间窗口 |
| `timeUnit` | SECONDS | 时间单位 |

> **注意：** `interval` 应大于业务方法的最长执行时间。若处理时间超过 TTL，标记会在执行过程中过期，导致并发请求穿透幂等防护。

### 序列化器切换

```yaml
snowdrift.cache.serializer: FASTJSON2
```

或注入自定义 `CacheSerializer` Bean：

```java
@Component
public class ProtoBufSerializer implements CacheSerializer {
    @Override public String serialize(Object value) { /* Protobuf 序列化 */ }
    @Override public <T> T deserialize(String json, Class<T> type) { /* Protobuf 反序列化 */ }
}
```

### 缓存降级

`SnowdriftCachingErrorHandler` 在 Spring Cache 注解（`@Cacheable` / `@CachePut` / `@CacheEvict`）失败时记录 WARN 日志并静默放行，不阻断业务方法执行。

## 后端对比

| 特性 | Caffeine | Redis | Redisson |
|------|----------|-------|----------|
| 部署 | 嵌入应用 | 需 Redis 服务 | 需 Redis 服务 |
| per-key TTL | ❌ 降级为全局 | ✅ | ✅ |
| 分布式锁 | ❌ | ❌ | ✅ 看门狗 |
| key 通配符扫描 | ✅ 正则 | ✅ SCAN | ✅ SCAN |
| 多实例共享 | ❌ | ✅ | ✅ |

## 配置属性参考

### snowdrift.cache

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `key-prefix` | String | null | key 前缀 |
| `key-ttl` | Duration | 30m | 默认 TTL |
| `max-size` | Long | 10000 | Caffeine 最大条目数 |
| `serializer` | SerializerType | JACKSON | 序列化器（JACKSON / FASTJSON2） |
