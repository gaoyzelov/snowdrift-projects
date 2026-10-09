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
- `getExpire`：单位秒，`-1` 表示永不过期、`-2` 表示 key 不存在（Redis 与 Caffeine 两个实现语义一致）。
- Hash 操作（`hget/hput/hdelete`）与 `increment` **仅 Redis 后端支持**；Caffeine 调用会抛 `UnsupportedOperationException`。
- **per-key TTL 两个后端都支持**：Caffeine 通过 Caffeine 的 `expireAfter(Expiry)` + `Policy.VarExpiration` 实现「带 TTL 写入 / `expire` / `getExpire`」；未显式指定 TTL 时按全局 `key-ttl` 过期（配置为非正数表示永不过期）。
- 显式传入的 TTL 必须为正数，否则直接抛 `BizException("缓存TTL必须大于 0")`（不会落到后端抛各自的原始异常）。

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

> **后端要求：** 防重依赖 per-key 过期能力，Redis 与 Caffeine 后端均支持；但 Caffeine 是**进程内**缓存，多实例部署时防重只在单个 JVM 内生效，跨实例防重必须使用 Redis。
> **注意：** `interval` 必须大于 0（否则抛 `BizException("防重间隔（interval）必须大于 0")`——为 0 时标记会立即过期、防重窗口为空）。`interval` 还应大于业务方法的最长执行时间，否则标记在处理中过期，并发请求可能穿透幂等防护。
> 业务执行抛出异常时切面会删除标记（尽力而为，删除失败仅记日志），允许用户修正后重新提交。

### 序列化器切换

```yaml
snowdrift.cache.serializer: FASTJSON2    # JACKSON（默认）或 FASTJSON2；只影响 ICacheService
```

两个内置实现 **`JacksonCacheSerializer` 与 `FastJson2CacheSerializer` 已对齐，可互换**：同一对象产出**同一个 JSON 字符串**，且一方的数据另一方能直接读回（`SerializerEquivalenceTest` 逐字断言）。

对齐口径（改动任一实现前请先看这里）：

| 维度 | 约定 |
|------|------|
| 字段来源 | **字段口径**：只按字段读写（Jackson 关闭 getter/计算属性探测，fastjson2 用 `FieldBased`）；显式 `@JsonProperty` 仍生效、`@JsonIgnore` 仍排除 |
| 键序 | 属性名与 Map 键均按字母序输出（两边一致，也保证 `HashMap` 等无序容器产出确定性 JSON） |
| 日期时间 | `LocalDateTime`、`java.util.Date`、`java.sql.Timestamp` → `yyyy-MM-dd HH:mm:ss`（**JVM 默认时区**，非 ISO）；`LocalDate`/`LocalTime`/`OffsetDateTime`/`Instant` 保持各自 ISO 风格 |
| 空值 | null 字段与 Map 中的 null 值都不写出 |
| 类型元数据 | **都不写**（`@class`/`@type` 均不出现），读取类型由调用方提供 |
| 读取宽容 | `LocalDateTime`/`Date` 同时接受空格与 ISO 两种写法 |

```java
// 泛型/集合值：用带 Type 的重载，元素类型才能还原
List<Order> orders = cacheService.get("order:list", new TypeReference<List<Order>>() {
}.getType());
```

或注册自定义 `ICacheSerializer` Bean 完全替换：

```java
@Component
public class ProtoBufSerializer implements ICacheSerializer {
    @Override public String serialize(Object value) { /* ... */ }
    @Override public <T> T deserialize(String json, Class<T> type) { /* ... */ }
    @Override public <T> T deserialize(String json, Type type) { /* ... */ }   // 泛型重载
}
```

> **类型元数据与 `@Cacheable` 的边界：**
> - `ICacheService` 的两个内置序列化器都**不写**类型元数据（读取类型由调用方给），因此值格式与所选实现无关。
> - `@Cacheable` 的值序列化器**固定使用** `JacksonCacheSerializer.cacheValueMapper()`（写 `@class`），**不随 `snowdrift.cache.serializer` 切换**：Jackson 的 `@class` 与 fastjson2 的 `@type` 两种类型元数据互不可读，若随配置切换，改一次配置就会让既有 `@Cacheable` 缓存整体读不出来。
> - `cacheValueMapper()` 使用 accept-all 的多态校验器，反序列化会实例化 JSON 中 `@class` 指定的类：**缓存数据必须只能由本应用自身写入**；实体**重命名/移动包**也会导致该侧旧缓存读取失败（需清库重建）。
> - `FastJson2RedisSerializer`（原先为 `@Cacheable` 提供的 fastjson2 版本）已随上述调整移除；如需 fastjson2 承担 `@Cacheable` 的值序列化，请自行实现 `RedisSerializer<Object>` 并注册。

### 缓存降级

`SnowdriftCachingErrorHandler` 对 Spring Cache 注解（`@Cacheable` / `@CachePut` / `@CacheEvict`）执行异常记录 WARN 后静默放行：GET 失败降级穿透、PUT/EVICT 失败跳过写入，缓存故障不阻断主流程。

## 装配与自定义

- 后端配置类只在**各自的 Bean 上**做条件判断：自定义 `ICacheService` 只会让框架实现退让，`CacheManager`、`snowdriftRedisTemplate` 与 key 前缀/TTL 配置**仍然生效**（此前类级条件会把它们一并关掉，导致 `@Cacheable` 静默退化）。
- 需要自定义 `CacheManager`：给自己的 Bean 加 `@Primary`，或用 `spring.autoconfigure.exclude` 排除本模块的自动配置。**不要**依赖框架侧的 `CacheManager` 自动退避——Redis 侧的 `CacheManager` 故意不带 `@ConditionalOnMissingBean`（否则 Boot 的默认 `CacheManager` 会先生效，使本模块的序列化器与 TTL 配置静默失效）；两个同类型 Bean 且都没有 `@Primary` 时，启动期即报 `NoUniqueBeanDefinitionException`。
- 框架专用的 `snowdriftRedisTemplate`（`RedisTemplate<String, String>`）**不再标注 `@Primary`**：应用按类型注入 `RedisTemplate` 时若容器中还有 Boot 的模板，会因多个候选而显式报错（而不是静默拿到 String-String 模板）；框架内部按 Bean 名注入。
- Bean Validation 实现（`hibernate-validator`）由 `snowdrift-base` 显式声明并向下传递，`@Validated @ConfigurationProperties` 可正常绑定；`bizlog-sdk` 偶然带进来的旧版实现已在 `snowdrift-log` 中排除。

## 后端对比

| 特性 | Caffeine | Redis（+Redisson） |
|------|----------|--------------------|
| 部署 | 进程内，无外部依赖 | 需 Redis 服务 |
| per-key TTL | ✅（`VarExpiration`；未指定时按全局 `key-ttl`） | ✅ |
| Hash 操作 / increment | ❌ | ✅ |
| 分布式锁 | ❌ | ✅ Redisson 看门狗自动续期 |
| 多实例共享 | ❌ | ✅ |
| Spring Cache | ✅ CaffeineCacheManager | ✅ RedisCacheManager（统一前缀 + TTL） |

> 如需更底层能力（如通配扫描 keys），直接使用各后端原生 API（Caffeine `Cache` / RedisTemplate）。

## @Cacheable 的 key 生成

容器内注册了统一的 `SnowdriftKeyGenerator`（`CachingConfigurer#keyGenerator`），生成格式：

```
[prefix:]完整类名#方法名[:参数类型:参数值...]
com.example.UserService#findById:java.lang.Long:1
```

- 每个参数都带**类型名**，避免同名重载方法（`m(Integer)` / `m(Long)` / `m(String)`）在参数值相同时撞 key；
- 用**完整类名**，避免不同包下的同名类（`p1.Inner` / `p2.Inner`）撞 key；
- 复杂对象按**字段序列化**（不依赖 `toString()`，跨实例/跨 JVM 稳定）；片段超过 64 字符时取 `sha256:<摘要>`；
- 片段中的 `\` 与 `:` 会被转义，`m("a:b")` 与 `m("a","b")` 不会撞 key；
- 已知歧义：同一形参类型下 `null` 与 `""` 生成相同片段；
- 类名/包名参与 key，因此**实体重命名或移动包会改变 key**（旧缓存自然失效）；`key-prefix` 由 `ICacheService` 拼接，不参与 `@Cacheable` 的 key（Redis 侧的 cacheName 前缀由 CacheManager 负责）。

## 测试

三个子模块均已引入 `spring-boot-starter-test`（scope test，版本由 parent 托管），**共 122 个用例**（`mvn -pl snowdrift-framework/snowdrift-cache/snowdrift-cache-<backend> -am test` 实测全绿）：

| 模块 | 测试类 | 覆盖点 |
|------|--------|--------|
| cache-base | `AbstractCacheServiceTest`(18) | key 前缀、序列化委派、TTL 兜底、11 类参数校验（含 delete/batchDelete、非正 TTL）、其余操作 |
| cache-base | `SnowdriftKeyGeneratorTest`(11) | 完整类名 + 参数类型格式、重载/摘要/跨包碰撞回归、转义、null 与空串歧义 |
| cache-base | `SerializerEquivalenceTest`(8) | **两实现等价性验收**：字段口径/键序/日期时间逐字一致、交叉互读、泛型重载还原元素类型 |
| cache-base | `CacheSerializerTest`(10) | 两个序列化器共同契约（setter-less 字段还原、循环引用、非法 JSON、集合/Map 限制）与各自格式差异 |
| cache-base | `SpELUtilTest`(6) | 静态 key 直通、按参数求值、null/非法表达式回落、缓存不改变语义 |
| cache-base | `RepeatSubmitAspectTest`(11) | 首次/重复/异常/中断路径、nonce 校验、interval=0 回归、key 解析失败 |
| cache-base | `DistributedLockAspectTest`(7) | 加锁/释放/异常与 Error 释放、加锁失败码与消息、自定义 message |
| cache-base | `SnowdriftCachingErrorHandlerTest`(2) | 四类异常静默降级 |
| cache-base | `SnowdriftCacheConfigurationTest`(7) | 默认/切换序列化器、自定义覆盖、属性绑定、切面条件装配 |
| cache-caffeine | `SnowdriftCaffeineCacheServiceImplTest`(13) | 真实 Caffeine：默认/显式 TTL、per-key 过期、putIfAbsent、淘汰、不支持操作、getExpire 契约 |
| cache-caffeine | `SnowdriftCaffeineConfigurationTest`(4) | 兜底装配与退让回归、自定义 CacheManager |
| cache-redis | `SnowdriftRedisCacheServiceImplTest`(12) | mock RedisTemplate：命令映射、TTL 换算、Lua increment 秒数取整、前缀 |
| cache-redis | `SnowdriftRedisConfigurationTest`(7) | 模板/管理器装配、前缀与 TTL 进入配置、`@Cacheable` 值序列化器固定为带 `@class` 的实现（回归）、自定义 ICacheService 回归 |
| cache-redis | `SnowdriftRedissonDistributedLockServiceImplTest`(6) | mock RLock：加解锁、中断语义、executeWithLock |

## 升级注意（本轮变更）

- **`@Cacheable` 的 key 格式变更**：`SnowdriftKeyGenerator` 现在使用「完整类名 + 参数类型 + 参数值」，并给超长参数加 `sha256:` 前缀。旧 key 全部失效（按 TTL 自然淘汰或清库重建）。
- **行为变更**：显式非正 TTL 抛 `BizException`；`delete`/`batchDelete` 补 key 非空校验；`@RepeatSubmit(interval <= 0)` 抛 `BizException`；Caffeine 对“永不过期”条目 `getExpire` 返回 `-1`。
- **装配变更**：后端配置的条件由类级下移到 `ICacheService` 的 Bean 方法（自定义 `ICacheService` 不再连带关闭 `CacheManager`/`snowdriftRedisTemplate`）；`snowdriftRedisTemplate` 去掉 `@Primary`；新增 `snowdrift.cache.redisson.enabled`（默认 true）。
- **修复**：`FastJson2CacheSerializer` 读取侧补 `FieldBased`，修复「私有字段 + getter、无 setter」的类在 `serializer=FASTJSON2` 下**静默读到默认值**的数据丢失问题。
- **依赖变更**：`hibernate-validator` + `tomcat-embed-el` 由 `snowdrift-base` 显式声明并向下传递；`snowdrift-log` 排除了 `bizlog-sdk` 偶然带入的 6.1.5 版实现。
- **两个序列化器对齐（本轮）**：统一字段口径、键序（字母序）、日期时间格式（`LocalDateTime`/`Date`/`Timestamp` 为 `yyyy-MM-dd HH:mm:ss` + JVM 默认时区，其余保持 ISO 风格），并让两侧读取同时接受空格与 ISO 写法 → 切换 `snowdrift.cache.serializer` 不再改变值格式，数据也可互相读取。
- **新增泛型 API**：`ICacheService#get(String, Type)`、`#hget(String, String, Type)` 与 `ICacheSerializer#deserialize(String, Type)`；`ICacheService#get(key, List.class)` 只能得到 Map 元素的老问题，改用这些重载即可正确还原元素类型。
- **`@Cacheable` 固定单一实现**：值序列化器恒为 `cacheValueMapper()`（`@class`），不再随 `snowdrift.cache.serializer` 切换；同时**移除** `FastJson2RedisSerializer`（其 `@type` 与 `@class` 互不可读）。

## 配置属性参考

### snowdrift.cache

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `key-prefix` | String | `""` | key 全局前缀（空串无前缀） |
| `key-ttl` | Duration | `1h` | 全局默认 TTL |
| `max-size` | long | `10000` | Caffeine 最大条目数 |
| `serializer` | SerializerType | `JACKSON` | 序列化器（JACKSON / FASTJSON2） |

### snowdrift.cache.redisson

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | `true` | 是否启用 Redisson 自动配置（分布式锁）。类路径存在 Redisson 时默认生效；设为 `false` 可让 `redisson-spring-boot-starter` 自己的自动配置（`redisson.yaml`、ACL、SSL 证书等）接管 |
