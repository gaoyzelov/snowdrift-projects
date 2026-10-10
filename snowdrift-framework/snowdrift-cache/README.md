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
// 默认用法：按「业务参数指纹」防重，无需配置 key
@RepeatSubmit(interval = 5)
public Result<Void> createOrder(OrderCreateDTO dto) { ... }

// 收窄范围：只按订单号防重（同客户端 + 同接口下）
@RepeatSubmit(key = "#orderNo", interval = 5)
public Result<Void> createOrder(String orderNo) { ... }
```

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `key` | 空 | 防重 key 的**作用域**，SpEL 表达式；留空则使用「业务参数指纹」 |
| `message` | `"请勿重复提交，请稍后重试"` | 命中重复时的提示信息 |
| `interval` | 5 | 防重时间窗口（必须大于 0） |
| `timeUnit` | SECONDS | 时间单位 |

**最终缓存 key 形态：** `repeat:submit:ip:<客户端IP>:<请求URI>:<作用域>`（URI 与作用域中的 `\`、`:` 会被转义，规则同 `SnowdriftKeyGenerator`）。身份与接口维度由框架拼接，因此**无需在 `key` 里自己写用户/接口维度**，不同用户、不同接口不会互相误伤。

**作用域的两种来源：**

- 未配置 `key`（推荐）：业务参数经**字段序列化**（`FieldBased`，无 getter 的 POJO 也按字段取值）后取 MD5；`HttpServletRequest`/`HttpServletResponse`/`Errors` 等请求绑定参数与 null 参数不参与；`MultipartFile` 降级为「文件名 + 大小」（避免把文件读进内存）；Map 参数按**键序无关**方式参与（否则 JSON 字段顺序变化即可绕过防重）。
- 配置 `key`：取 SpEL 结果。

**以下情况放行（不防重）并记日志**，属"宁可漏防、不可误伤"的取舍：非 Web 环境（定时任务、MQ 消费）；客户端 IP 无法识别（用 `unknown` 当身份会让这类客户端共用同一个 key，反而误伤）；作用域生成失败（SpEL 解析失败/求值为空、参数序列化失败）。

> **后端要求：** 防重依赖 per-key 过期能力，Redis 与 Caffeine 后端均支持；但 Caffeine 是**进程内**缓存，多实例部署时防重只在单个 JVM 内生效，跨实例防重必须使用 Redis。
> **注意：** `interval` 必须大于 0（否则抛 `BizException("防重间隔必须大于 0")`），且**该校验先于请求上下文判断**——非 Web 环境也不会掩盖配置错误。`interval` 还应大于业务方法的最长执行时间，否则标记在处理中过期，并发请求可能穿透幂等防护。
> 业务执行失败（含抛 `Error`）时切面会删除标记（尽力而为，仅在缓存中仍是本请求写入的 nonce 时删除，删除失败仅记日志），允许用户修正后重新提交。
> **安全边界：** 身份取自 `IpUtil.getIp`，该方法无条件信任 `X-Forwarded-For`（可伪造），故防重只防"手抖/连点"，**不构成安全边界**；强幂等请让 `key` 带上用户维度并在业务侧加数据库唯一键兜底。

### 序列化器

缓存统一使用基于 Jackson 的 `JacksonCacheSerializer`（不再有可切换的第二个实现，`snowdrift.cache.serializer` 配置已移除）。值格式约定（改动前请先看这里）：

| 维度 | 约定 |
|------|------|
| 字段来源 | **字段口径**：关闭 getter/计算属性探测，只按字段读写；显式 `@JsonProperty` 仍生效、`@JsonIgnore` 仍排除 |
| 键序 | 属性名与 Map 键均按字母序输出（保证 `HashMap` 等无序容器产出确定性 JSON） |
| 日期时间 | 注册 `JavaTimeModule` 并关闭时间戳输出，`java.time` 类型统一按 ISO-8601 字符串落盘（`LocalDateTime` → `2026-10-09T12:00:00`；`LocalDate`/`LocalTime`/`OffsetDateTime`/`Instant` 同为 ISO；`java.util.Date` 走 Jackson 默认 ISO） |
| 空值 | null 字段与 Map 中的 null 值都不写出（`NON_NULL`） |
| 类型元数据 | `ICacheService` 路径**不写**（读取类型由调用方提供）；`@Cacheable` 路径写 `@class`（见下） |
| 读取宽容 | 忽略未知字段，兼容结构演进后的旧数据 |

```java
// 泛型/集合值：用带 Type 的重载，元素类型才能还原
List<Order> orders = cacheService.get("order:list", new TypeReference<List<Order>>() {
}.getType());
```

如需完全自定义序列化行为，注册自己的 `ICacheSerializer` Bean 即可替换默认实现：

```java
@Component
public class ProtoBufSerializer implements ICacheSerializer {
    @Override public String serialize(Object value) { /* ... */ }
    @Override public <T> T deserialize(String json, Class<T> type) { /* ... */ }
    @Override public <T> T deserialize(String json, Type type) { /* ... */ }   // 泛型重载
}
```

> **类型元数据与 `@Cacheable` 的边界：**
> - `ICacheService` 的序列化**不写**类型元数据（读取类型由调用方给）。
> - `@Cacheable` 的值序列化器**固定使用** `JacksonCacheSerializer.cacheValueMapper()`（写 `@class`）：Spring Cache 拿不到方法返回类型，类型信息必须随 JSON 落盘，否则反序列化会退化为 `LinkedHashMap` 并在强转时抛 `ClassCastException`。
> - `cacheValueMapper()` 使用 accept-all 的多态校验器，反序列化会实例化 JSON 中 `@class` 指定的类：**缓存数据必须只能由本应用自身写入**；实体**重命名/移动包**也会导致该侧旧缓存读取失败（需清库重建）。

### 缓存降级

`SnowdriftCachingErrorHandler` 对 Spring Cache 注解（`@Cacheable` / `@CachePut` / `@CacheEvict`）执行异常记录 WARN 后静默放行：GET 失败降级穿透、PUT/EVICT 失败跳过写入，缓存故障不阻断主流程。

## 装配与自定义

- 后端配置类只在**各自的 Bean 上**做条件判断：自定义 `ICacheService` 只会让框架实现退让，`CacheManager`、`snowdriftRedisTemplate` 与 key 前缀/TTL 配置**仍然生效**（此前类级条件会把它们一并关掉，导致 `@Cacheable` 静默退化）。
- 需要自定义 `CacheManager`：给自己的 Bean 加 `@Primary`，或用 `spring.autoconfigure.exclude` 排除本模块的自动配置。**不要**依赖框架侧的 `CacheManager` 自动退避——Redis 侧的 `CacheManager` 故意不带 `@ConditionalOnMissingBean`（否则 Boot 的默认 `CacheManager` 会先生效，使本模块的序列化器与 TTL 配置静默失效）；两个同类型 Bean 且都没有 `@Primary` 时，启动期即报 `NoUniqueBeanDefinitionException`。
- 框架专用的 `snowdriftRedisTemplate`（`RedisTemplate<String, String>`）**不再标注 `@Primary`**：应用按类型注入 `RedisTemplate` 时若容器中还有 Boot 的模板，会因多个候选而显式报错（而不是静默拿到 String-String 模板）；框架内部按 Bean 名注入。
- Bean Validation 实现（`hibernate-validator`）由 `snowdrift-base` 显式声明并向下传递，`@Validated @ConfigurationProperties` 可正常绑定；`bizlog-sdk` 偶然带进来的旧版实现已在 `snowdrift-log` 中排除。
- **自动配置顺序（改动前请先读）**：核心配置 `SnowdriftCacheConfiguration` 用 `@ConditionalOnBean(ICacheService.class)` / `@ConditionalOnBean(IDistributedLockService.class)` 决定是否注册 `@RepeatSubmit`、`@DistributedLock` 切面，而 Bean 条件**只能看到解析顺序上先处理过的配置类**。因此后端配置必须显式排在核心配置之前（`SnowdriftRedissonConfiguration` / `SnowdriftRedisConfiguration` / `SnowdriftCaffeineConfiguration` 均声明了 `before = SnowdriftCacheConfiguration.class`）。按类名排序时 `cache.config.*` 会排在 `cache.redis.*` **之前**，此前 Redis 侧缺少该声明，导致 **`@RepeatSubmit`、`@DistributedLock` 全部静默失效且不报错**；`SnowdriftRedisConfigurationTest` 中两个切面装配用例即为该回归的守卫。

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
简单类名#方法名[:参数段摘要]
UserService#findById（无参数：不摘要）
OrderService#create:8b1d4a...（参数段整体摘要：SHA-256 前 128 位，32 个十六进制字符）
```

> 前缀不在本生成器中拼接：`@Cacheable` 的 Redis key 前缀由 `RedisCacheManager` 负责（`prefix:cacheName:key`），
> Caffeine 侧不带前缀；`ICacheService` 的 `key-prefix` 由 `AbstractCacheService#buildKey` 拼接。

- 无参数时不带摘要；带参数时把各参数「类型:值」按序拼接（值转义）成参数段，**整体一次取摘要**（SHA-256 前 128 位），key 长度恒定、参数值不进入明文 key（排查时需按入参自行重算摘要）；
- 每个参数都带**类型名**（简单名）参与摘要输入，避免同名重载方法（`m(Integer)` / `m(Long)` / `m(String)`）在参数值相同时撞 key；
- 类名与参数类型名都用**简单名**缩短 key（`UserServiceImpl` 而非 `com.x...UserServiceImpl`）；代价是不同包下的同名类若共用同一 `cacheName` 且方法名、参数相同会撞 key（极少见）；
- 复杂对象按**字段序列化**（不依赖 `toString()`，跨实例/跨 JVM 稳定）；
- 参数值中的 `\` 与 `:` 会被转义后再摘要，`m("a:String:b")` 与 `m("a","b")` 不会摘要出相同的 key；
- 已知歧义：同一形参类型下 `null` 与 `""` 摘要输入相同 → key 相同；
- 简单类名参与 key：类**重命名**会改变 key（旧缓存自然失效），**移动包不再影响 key**；`key-prefix` 由 `ICacheService` 拼接，不参与 `@Cacheable` 的 key（Redis 侧的 cacheName 前缀由 CacheManager 负责）。

## 测试

三个子模块均已引入 `spring-boot-starter-test`（scope test，版本由 parent 托管），**共 127 个用例**（`mvn -pl snowdrift-framework/snowdrift-cache/snowdrift-cache-<backend> -am test` 实测全绿）：

| 模块 | 测试类 | 覆盖点 |
|------|--------|--------|
| cache-base | `AbstractCacheServiceTest`(18) | key 前缀、序列化委派、TTL 兜底、11 类参数校验（含 delete/batchDelete、非正 TTL）、其余操作 |
| cache-base | `SnowdriftKeyGeneratorTest`(11) | 简单类名格式、参数段整体摘要（128 位/恒定长度）、重载与位置语义、转义、null 与空串歧义 |
| cache-base | `CacheSerializerTest`(7) | 缓存序列化器（Jackson）契约：setter-less 字段还原、循环引用、非法 JSON、集合/Map 限制、泛型 `Type` 重载还原元素类型、`cacheValueMapper` 类型信息 |
| cache-base | `SpELUtilTest`(6) | 静态 key 直通、按参数求值、null/非法表达式回落、缓存不改变语义 |
| cache-base | `RepeatSubmitAspectTest`(21) | 首次/重复/异常（含 `Error`）/中断路径、nonce 校验、interval 前置校验、非 Web 与 IP 不可识别放行、参数指纹（字段口径 / Map 键序无关 / 无 getter POJO）、URI 与作用域转义、请求绑定参数与文件参数过滤 |
| cache-base | `DistributedLockAspectTest`(7) | 加锁/释放/异常与 Error 释放、加锁失败码与消息、自定义 message |
| cache-base | `SnowdriftCachingErrorHandlerTest`(2) | 四类异常静默降级 |
| cache-base | `SnowdriftCacheConfigurationTest`(6) | 默认注册 Jackson 序列化器、自定义覆盖、属性绑定、切面条件装配 |
| cache-base | `SnowdriftCachingIntegrationTest`(2) | **端到端**：`@Cacheable` 实际采用框架 `KeyGenerator`（key 形如 `简单类名#方法:摘要`）与框架 `CacheErrorHandler`（缓存故障穿透） |
| cache-redis | `SnowdriftRedisConfigurationTest` 中的 `redisWinsOverCaffeine` | **装配优先级**：Redis 与 Caffeine 同时在类路径时 Redis 生效、Caffeine 退让（首次被验证） |
| cache-caffeine | `SnowdriftCaffeineCacheServiceImplTest`(14) | 真实 Caffeine：默认/显式 TTL、per-key 过期、putIfAbsent、淘汰、不支持操作、getExpire 契约 |
| cache-caffeine | `SnowdriftCaffeineConfigurationTest`(4) | 兜底装配与退让回归、自定义 CacheManager |
| cache-redis | `SnowdriftRedisCacheServiceImplTest`(13) | mock RedisTemplate：命令映射、TTL 换算、Lua increment 秒数取整、前缀 |
| cache-redis | `SnowdriftRedisConfigurationTest`(9) | 模板/管理器装配、前缀与 TTL 进入配置、`@Cacheable` 值序列化器固定为带 `@class` 的实现（回归）、自定义 ICacheService 回归、**Redis 后端下两个切面的装配回归** |
| cache-redis | `SnowdriftRedissonDistributedLockServiceImplTest`(7) | mock RLock：加解锁、中断语义、executeWithLock |

## 升级注意（本轮变更）

- **`@Cacheable` 的 key 格式再次变更（参数段统一摘要）**：`SnowdriftKeyGenerator` 改为「简单类名#方法名[:SHA-256 前 128 位参数段摘要]」，带参数时参数段整体一次摘要（固定 32 个十六进制字符，长度恒定、参数值不落明文）。旧 key 全部失效（按 TTL 自然淘汰或清库重建）。
- **行为变更**：显式非正 TTL 抛 `BizException`；`delete`/`batchDelete` 补 key 非空校验；`@RepeatSubmit(interval <= 0)` 抛 `BizException`；Caffeine 对“永不过期”条目 `getExpire` 返回 `-1`。
- **装配变更**：后端配置的条件由类级下移到 `ICacheService` 的 Bean 方法（自定义 `ICacheService` 不再连带关闭 `CacheManager`/`snowdriftRedisTemplate`）；`snowdriftRedisTemplate` 去掉 `@Primary`；新增 `snowdrift.cache.redisson.enabled`（默认 true）。
- **统一序列化器（本轮）**：**移除** `FastJson2CacheSerializer`、`SerializerType` 与 `snowdrift.cache.serializer` 配置，`ICacheService` 与 `@Cacheable` 统一使用基于 Jackson 的 `JacksonCacheSerializer`（`JacksonCacheSerializer` 也相应简化：仅注册裸 `JavaTimeModule`，`java.time` 类型按 ISO-8601 落盘，`LocalDateTime` 由 `yyyy-MM-dd HH:mm:ss` 改为 `2026-10-09T12:00:00`）。原 `serializer=FASTJSON2` 配置失效（被忽略），且 `LocalDateTime` 格式变更，此前写入的缓存需清库或等 TTL 自然淘汰。
- **依赖变更**：`hibernate-validator` + `tomcat-embed-el` 由 `snowdrift-base` 显式声明并向下传递；`snowdrift-log` 排除了 `bizlog-sdk` 偶然带入的 6.1.5 版实现。
- **新增泛型 API**：`ICacheService#get(String, Type)`、`#hget(String, String, Type)` 与 `ICacheSerializer#deserialize(String, Type)`；`ICacheService#get(key, List.class)` 只能得到 Map 元素的老问题，改用这些重载即可正确还原元素类型。
- **`@Cacheable` 值序列化器**：恒为 `cacheValueMapper()`（写 `@class`）；Spring Cache 拿不到方法返回类型，类型信息必须随 JSON 落盘。
- **细节修复**：Caffeine 后端对超过纳秒上限（约 292 年）的 `key-ttl` 不再抛 `ArithmeticException`，按"永不过期"处理；`SnowdriftRedisCacheServiceImpl` / `SnowdriftRedissonDistributedLockServiceImpl` 补构造期协作者校验（缺失时快速失败，不再等到首个命令才 NPE）。
- **修复（切面静默失效）**：Redis / Redisson 后端下 `@RepeatSubmit`、`@DistributedLock` 此前因自动配置顺序（后端排在核心配置之后）而**完全没有生效**；现由后端配置的 `before = SnowdriftCacheConfiguration.class` 修正。升级后这两个注解会真正开始拦截——若历史上依赖过“防重/加锁没生效”的现状（例如靠数据库唯一键兜底），请回归验证。

## 配置属性参考

### snowdrift.cache

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `key-prefix` | String | `""` | key 全局前缀（空串无前缀） |
| `key-ttl` | Duration | `1h` | 全局默认 TTL |
| `max-size` | long | `10000` | Caffeine 最大条目数 |

### snowdrift.cache.redisson

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | boolean | `true` | 是否启用 Redisson 自动配置（分布式锁）。类路径存在 Redisson 时默认生效；设为 `false` 可让 `redisson-spring-boot-starter` 自己的自动配置（`redisson.yaml`、ACL、SSL 证书等）接管 |
