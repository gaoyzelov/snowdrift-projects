# snowdrift-base

核心基础模块，提供统一响应、业务状态码、业务异常、通用枚举与工具类等基础设施能力。所有其他模块均依赖此模块；本模块为纯 Java/工具层，无 Spring 依赖。

## 快速开始

```xml
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-base</artifactId>
</dependency>
```

## 核心功能

### 统一响应 — Result\<T\>

所有 API 返回 `Result<T>` 统一格式，包含 `code` / `msg` / `data` / `timestamp` 四个字段。`Result` 一经创建即不可变，对象通过静态工厂或 Lombok Builder 生成。

```java
// 成功
return Result.ok(data);
return Result.ok("操作成功", data);   // 携带自定义成功消息

// 失败
return Result.err(ResultCode.BAD_REQUEST);
return Result.err(ResultCode.BAD_REQUEST.code(), "参数错误");  // 自定义消息
```

### 状态码 — ResultCode

`ResultCode` 为业务状态码常量（`record(int code, String msg)`），**非 HTTP 状态码**：HTTP 状态由容器/框架决定，业务结果统一经 `Result.code` 区分。数值刻意避开 HTTP 状态码区间（100–599），避免与 HTTP 语义混淆。

约定如下：

| 区间 | 含义 |
|------|------|
| `0 / 1` | 通用失败 / 成功（`ERR` / `OK`） |
| `1xxx` | 调用方 / 资源侧错误（参数、鉴权、不存在、冲突等） |
| `2xxx` | 服务端错误（内部错误、服务不可用等） |

常用错误码：`BAD_REQUEST=1000`、`UNAUTHORIZED=1001`、`FORBIDDEN=1002`、`NOT_FOUND=1003`、`METHOD_NOT_ALLOWED=1004`、`CONFLICT=1005`、`LOCK_FAILED=1006`、`PAYLOAD_TOO_LARGE=1007`、`UNSUPPORTED_MEDIA_TYPE=1008`、`TOO_MANY_REQUESTS=1009`、`INTERNAL_SERVER_ERROR=2000`、`SERVICE_UNAVAILABLE=2001`。

消息文案（`msg`）当前为代码内置中文文本，未做 i18n 拆分；如需覆盖业务文案，通过 `Result.err(code, msg)` 等入口自定义。

### 业务异常 — BizException

继承 `RuntimeException`，携带业务码 `code`（Integer）与可选 `args`（Object[]，当前作为预留字段，框架内暂无格式化消费方）。全部断言/校验/解析工具失败时抛出的均为 `BizException`。

```java
// 直接给消息
throw new BizException("订单不存在");

// 携带 ResultCode
throw new BizException(ResultCode.NOT_FOUND);

// 携带自定义码与消息
throw new BizException(1003, "订单不存在");

// 携带消息与参数占位
throw new BizException("订单 {0} 不存在", new Object[]{orderId});
```

| 方法 | 说明 |
|------|------|
| `getCode()` | 获取异常业务码 |
| `getMessage()` | 获取异常消息 |
| `getArgs()` | 获取参数占位（预留） |

### 枚举

| 类 | code | 说明 |
|----|------|------|
| `StatusEnum` | 0/1 | 启用/禁用（DISABLED/ENABLED） |
| `YesNoEnum` | 0/1 | 是否（NO/YES） |
| `BizTypeEnum` | 0~9 | 业务操作类型（INSERT/UPDATE/DELETE/SELECT/EXPORT/IMPORT/OTHER） |
| `DataScopeEnum` | -1,0~4 | 数据权限范围（NONE/ALL/DEPT/DEPT_AND_SUB/SELF/CUSTOM） |
| `IEnum<E>` | — | 通用枚举接口，提供 `getByCode()` / `getByNote()` 静态查找 |

base 内四个枚举均已标注 `@JsonValue`（按 `code` 序列化）+ `@JsonCreator`（按 `code` 反序列化）：

- `DataScopeEnum`：`null` 或未知编码回退 `NONE`（无权限），避免缺省配置意外扩大数据可见范围；
- `StatusEnum` / `YesNoEnum` / `BizTypeEnum`：`null` 映射为 `null`，未知编码抛 `BizException`（默认码 `ERR`，由全局异常处理兜底为参数错误）。

> 注意：`schedule` / `oss` 等模块自定义的 `IEnum` 目前仍走 Jackson 默认（按枚举名），如需统一请在其实现类补充上述注解。

### 断言工具 — AssertUtil

提供比 Spring Assert 更丰富的断言方法，失败统一抛出 `BizException`。

```java
AssertUtil.notNull(obj, "对象不能为空");
AssertUtil.notBlank(str, "字符串不能为空");
AssertUtil.notEmpty(list, "集合不能为空");
AssertUtil.isTrue(condition, "条件不成立");
AssertUtil.isFalse(condition, "条件应不成立");
AssertUtil.inside(item, list, "元素不在集合内");
AssertUtil.custom(() -> check(), "校验失败");   // 延迟求值，返回 null/false 视为不成立
```

### 校验工具 — ValidateUtil

格式校验工具类，全部返回 `boolean`、不抛异常。常用：

```java
ValidateUtil.isIdCard("...");          // 身份证（15/18 位，含地区/日期/18 位校验码）
ValidateUtil.isMobilePhone("138...");  // 手机号（结构校验 ^1[3-9]\d{9}$）
ValidateUtil.isFixedPhone("010-...");  // 固定电话（可带区号）
ValidateUtil.isEmail("...");           // 邮箱
ValidateUtil.isURL("https://...");     // URL
ValidateUtil.isBankCard("6222...");    // 银行卡（16/19 位，支持空格分组）
ValidateUtil.isVIN("...");             // 车架号
ValidateUtil.isCarLicense("京A...");   // 车牌号
```

### 脱敏工具 — DesensitizeUtil

按类别对敏感文本脱敏，识别失败的输入会保守处理为全掩码（`******`），不会回显原文：

```java
DesensitizeUtil.mobilePhone("13812345678");  // 138****5678
DesensitizeUtil.idCard("110...");            // 保留前 4 后 4，中间掩码
DesensitizeUtil.email("test@example.com");   // t****@example.com
DesensitizeUtil.bankCard("6222 ...");        // 保留前 4 后 4，支持 16/19 位及空格
DesensitizeUtil.chineseName("张三丰");        // 张**
DesensitizeUtil.ip("192.168.1.1");           // 192.*.*.*
DesensitizeUtil.process(text, regex, replace); // 自定义正则脱敏
```

### 加密工具 — EncryptUtil

覆盖摘要 / HMAC / AES-GCM / RSA 等常用算法。

```java
// 摘要（仅用于完整性校验，勿用于口令存储）
EncryptUtil.md5(text);
EncryptUtil.sha256(text);

// HMAC-SHA256（返回 Base64）
EncryptUtil.hmacSha256(text, key);

// AES-GCM（推荐：随机 IV，输出 Base64(iv + ciphertext + tag)，返回十六进制密钥）
String aesKey = EncryptUtil.aesKey();
String cipher = EncryptUtil.aesGcmEncrypt(plain, aesKey);
String plain = EncryptUtil.aesGcmDecrypt(cipher, aesKey);

// RSA（默认 2048 位；rsaEncrypt/rsaDecrypt 接收裸 Base64 DER 密钥）
RsaKeyPair pair = EncryptUtil.rsaKeyPairBase64();
String enc = EncryptUtil.rsaEncrypt(text, pair.getPublicKey());
String dec = EncryptUtil.rsaDecrypt(enc, pair.getPrivateKey());

// PEM 格式（用于与外部系统交换 / 导入）
RsaKeyPair pem = EncryptUtil.rsaKeyPairPem();
```

注意：

- `md5` / `sha1` / `sha256` 均为**无盐**原始哈希，切勿用于口令存储；
- `rsaEncrypt` 单次明文上限为 `密钥位数 / 8 - 11` 字节（2048 位密钥最大 245 字节），超限会抛出明确异常，请先分段或改用对称加密；
- `aesEcbEncrypt` / `aesEcbDecrypt` 已废弃，仅用于兼容旧 ECB 数据，新代码请使用 GCM。

### 雪花 ID — SnowflakeUtil

Twepoch 为 2015-01-01 的雪花算法实现；相同 `workerId+datacenterId` 复用同一实例，`nextId()` 线程安全，支持毫秒内序列溢出等待与时钟回拨（2 秒内容忍）处理。

```java
SnowflakeUtil sf = SnowflakeUtil.getInstance();     // workerId=0, datacenterId=0
SnowflakeUtil sf = SnowflakeUtil.getInstance(1);     // datacenterId 默认 0
SnowflakeUtil sf = SnowflakeUtil.getInstance(1, 2);
long id = sf.nextId();
```

> 多节点/多 JVM 部署时请为每个节点分配不同的 `workerId`/`datacenterId`，使用默认实例会产生重复 ID。

### 时间工具 — DateTimeUtil

基于 `java.time` 的格式化 / 解析 / 转换 / 区间判断；内置 `yyyy-MM-dd HH:mm:ss`、`yyyy-MM-dd`、`HH:mm:ss` 等默认格式。解析失败统一抛出携带原因链的 `BizException`。

```java
String s = DateTimeUtil.getDateTimeString(LocalDateTime.now());
LocalDateTime dt = DateTimeUtil.parseLocalDateTime("2026-09-04 12:00:00");
LocalDate d = DateTimeUtil.parseLocalDate("2026-09-04");
LocalDateTime t = DateTimeUtil.timestampToLocalDateTime(System.currentTimeMillis());
long ts = DateTimeUtil.localDateTimeToTimestamp(dt);
boolean in = DateTimeUtil.isBetween(dt, start, end);
long days = DateTimeUtil.betweenDays(start, end);
LocalDateTime startOfDay = DateTimeUtil.getStartOfDay(LocalDate.now());
```

### HTTP 工具 — HttpUtil

基于 JDK `HttpClient` 的同步请求封装，支持 GET / POST / PUT / DELETE、JSON / Form 表单、对象反序列化；默认连接与读取超时 30 秒。非 2xx 或 IO 中断时抛 `BizException`。

```java
String html = HttpUtil.get("https://example.com");
String json = HttpUtil.postJson("https://api.example.com/order", orderObj);
Map<String, String> form = Map.of("username", "admin");
String resp = HttpUtil.postForm("https://api.example.com/login", form);
Order order = HttpUtil.getForObject("https://api.example.com/order/1", Order.class);
```

### IP 工具 — IpUtil

客户端 IP 获取与 IP 归属地查询。`ip2region` 库懒加载 `classpath` 根目录下的 `ip2region.xdb`（缺失时不阻断启动，仅告警）。

```java
String ip = IpUtil.getIp(request);                 // 请求来源 IP
String location = IpUtil.getIpLocation(ip, " ");   // 归属地（国家/省/市/运营商拼接）
String[] region = IpUtil.parseIp(ip);              // 原始地域数组
IpUtil.isValidIp(ip);  IpUtil.isIpv4(ip);  IpUtil.isIpv6(ip);  IpUtil.isInternalIp(ip);
```

> `getIp` 优先读取 `X-Forwarded-For` 等转发头，仅在可信反向代理（已覆写该头）后使用；用于限流等安全判断时请以 `getRemoteAddr` 为准。

### Servlet 工具 — ServletUtil

请求头 / 参数获取与 JSON 响应写出。

```java
String ua = ServletUtil.getUserAgent(request);
Map<String, String> headers = ServletUtil.getHeaderMap(request);
Map<String, String> params = ServletUtil.getParamMap(request);
ServletUtil.writeJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED, Result.err(ResultCode.UNAUTHORIZED));
```

### Cron 工具 — CronUtil

Cron 表达式生成与校验（6 或 7 段：秒 分 时 日 月 周 [年]，遵循 Quartz/Spring 约定——日/周字段恰有一个为 `?`；星期支持 `MON-SUN` 缩写）。

```java
String cron = CronUtil.getCron(LocalDateTime.of(2026, 9, 4, 12, 0, 0));
boolean ok = CronUtil.isValid("0 0 12 ? * MON");
CronUtil.everyMinute();  CronUtil.everyHour();  CronUtil.everyDay();
CronUtil.everyWeek();    CronUtil.everyMonth(); CronUtil.everyYear();
CronUtil.everyMinutes(30);  CronUtil.everyHours(2);   // 每隔 N 分钟/小时
CronUtil.workDay();  CronUtil.atTime(12, 30, 0);  CronUtil.atWorkDayTime(9, 0, 0);
```

### 反射工具 — ReflectUtil

```java
Method m = ReflectUtil.getMethod(Order.class, "getTotal");        // 无参版对重载方法返回第一个匹配并告警
List<Field> fields = ReflectUtil.getDeclaredFields(order);        // 排除 static/synthetic 字段
List<Field> all = ReflectUtil.getDeclaredFields(order, true);     // 含父类，子类优先按名去重
```

> `getMethod` 在方法不存在时抛出 `BizException`。

### 通用常量

| 类 | 说明 |
|----|------|
| `StrConst` | 字符串常量（EMPTY、COMMA、COLON、SLASH、UNDERLINE、TRACE_ID、MASK_REPLACEMENT 等） |
| `RegexConst` | 常用正则表达式（URL、CARD_NO、MOBILE_PHONE、FIXED_PHONE、EMAIL、BANK_CARD、VIN、CAR_LICENSE、PWD 等） |

## 模块定位

- 纯工具 / 模型层，不依赖 Spring，不产生自动配置（无 `AutoConfiguration.imports`）。
- 依赖为 Servlet/Validation API、Fastjson2、Jackson-annotations、commons-lang3/collections4、Guava、ip2region、SLF4J、Lombok。
- 雪花 ID、加密、脱敏、校验等类均可直接脱离 Spring 环境使用。
