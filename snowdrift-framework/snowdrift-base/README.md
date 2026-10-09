# snowdrift-base

核心基础模块，提供统一响应、业务状态码、业务异常、通用枚举与工具类等基础设施能力。所有其他模块均依赖此模块；本模块为纯 Java/工具层，无 Spring 依赖。

## 快速开始

要求 Java 17+、Maven 3.8+；使用 Snowdrift BOM 管理依赖版本。以下片段展示 API 用法，业务对象由调用方提供。

```xml
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-base</artifactId>
</dependency>
```

## 核心功能

### 统一响应 — Result\<T\>

`Result<T>` 包含 `code` / `msg` / `data` / `timestamp` 四个字段，Lombok `@Data` + `@Accessors(chain = true)` 生成链式 setter，但不保证 `data` 深度不可变。推荐使用静态工厂，自动填写毫秒时间戳；直接 `new Result<>()` 时未赋值字段为 Java 默认值（`timestamp=0`）。

序列化/反序列化遵循 Lombok 生成属性的默认 Jackson 行为（含 setter 写入）；业务侧的时间格式等序列化配置仍由调用方决定。

```java
// 成功
return Result.ok(data);
return Result.ok("操作成功", data);   // 携带自定义成功消息

// 失败
return Result.err(ResultCode.BAD_REQUEST);
return Result.err(ResultCode.BAD_REQUEST.code(), "参数错误");  // 自定义消息

// 链式修改（例如统一追加提示）
Result<String> result = Result.ok(data).setMsg("已更新");
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

消息文案（`msg`）当前为代码内置中文文本，未做 i18n 拆分；如需覆盖业务文案，通过 `Result.err(code, msg)` 等入口自定义。业务方可直接创建 `new ResultCode(40001, "订单不存在")`，无需修改框架常量。

### 业务异常 — BizException

继承 `RuntimeException`，携带业务码 `code`（Integer）与可选 `args`（Object[]）。`args` 为预留参数，不自动格式化，也不自动调用 Spring `MessageSource`；跨进程传输时，调用方须保证参数元素及其对象图可序列化。

构造器覆盖：无参（ERR）、仅消息（ERR）、int 码+消息、消息+args、ResultCode、ResultCode+args、消息+cause、Throwable。静态 `throwEx` 系列用于 `return BizException.throwEx(...)` 写法。断言失败抛 `BizException`；布尔校验方法返回 `false`；日期解析、HTTP IO 等异常以 `BizException` 包装，原因链策略见对应工具节。

```java
// 直接给消息
throw new BizException("订单不存在");

// 携带 ResultCode
throw new BizException(ResultCode.NOT_FOUND);

// 携带自定义码与消息
throw new BizException(new ResultCode(40001, "订单不存在"));
// 也保留直接指定业务码的兼容入口
throw new BizException(40001, "订单不存在");

// 保留模板和参数；需要格式化时由调用方显式处理
throw new BizException("订单 {0} 不存在", new Object[]{orderId});
```

| 方法 | 说明 |
|------|------|
| `getCode()` | 获取异常业务码 |
| `getMessage()` | 获取异常消息 |
| `getArgs()` | 获取预留参数（未提供则返回 null） |

### 枚举

base 仅提供 `IEnum<E>` 接口：`getCode()` / `getNote()`，以及返回 `Optional` 的 `getByCode(Class, code)` / `getByNote(Class, note)`。

- 未找到匹配项返回 `Optional.empty()`；传入 null 或非枚举类型抛 `BizException`（消息为“必须传入枚举类型”），与本模块异常口径一致。
- `IEnum` 本身不规定 JSON 序列化方式，具体枚举自行声明注解。
- `StatusEnum` / `YesNoEnum` 已移除；`BizTypeEnum` 位于 `snowdrift-log`；`DataScopeEnum` 位于 `snowdrift-orm-base`，不再属于 base。

### 断言工具 — AssertUtil

提供对象、字符串、集合、对象数组、包含关系、立即布尔与延迟条件断言，失败统一通过 `BizException.throwEx` 抛出。

```java
AssertUtil.notNull(obj, "对象不能为空");
AssertUtil.notBlank(str, "字符串不能为空");
AssertUtil.notEmpty(list, "集合不能为空");   // 集合与对象数组两个重载
AssertUtil.inside(item, list, "元素不在集合内");
AssertUtil.predicate(() -> check(), "校验失败"); // 求值一次，仅 Boolean.TRUE 通过；predicate 为 null 时抛“断言条件不能为空”
```

### 校验工具 — ValidateUtil

格式校验工具类，对无效输入返回 `false`；URL 走 `RegexConst.URL` 预编译正则，其余条目为各自格式的严格子集。常用：

```java
ValidateUtil.isIdCard("...");          // 身份证（15/18 位，含地区/日期/18 位校验码）
ValidateUtil.isMobilePhone("138...");  // 手机号（结构校验 ^1[3-9]\d{9}$）
ValidateUtil.isFixedPhone("010-...");  // 固定电话（可带区号）
ValidateUtil.isEmail("...");           // 邮箱
ValidateUtil.isURL("https://example.com"); // URL 正则（http/https/ftp）
ValidateUtil.isBankCard("6222...");    // 银行卡（16/19 位，支持空格分组）
ValidateUtil.isVIN("...");             // 车架号
ValidateUtil.isCarLicense("京A...");   // 车牌号
```

边界说明：
- 身份证支持 15/18 位 ASCII 数字结构（18 位末位允许 X/x），检查省级前缀、真实出生日期、非未来日期与 18 位校验码；不查询完整行政区历史表，也不验证身份真实性。
- URL 为正则子集：要求 http/https/ftp 前缀与至少两段主机名，不接受 IPv4 字面量主机、单标签主机等；仅做格式检查，不做可达性或安全校验。
- 银行卡仅校验 16/19 位及空格分组格式，不做 Luhn/BIN 校验；VIN、邮箱、车牌是格式子集，不是业务真实性认证。

### 脱敏工具 — DesensitizeUtil

手机号、固话、身份证、邮箱、银行卡、车牌和 IP 的非空非法输入返回 `******`，空白输入返回空串。IP 会先去掉首尾空白再识别，`host:port`、主机名等无法识别为 IP 的写法同样落到全掩码，不会原样输出。

地址保留前 6 个 Unicode 码点，6 个码点以内整体掩码（避免保留前缀覆盖整串、掩码形同虚设）；姓名保留首个码点、其余按码点个数掩码。两者都按码点计数与推进，不会把扩展区汉字的代理对切成无法编码的孤立代理。

`process` 是底层正则替换：空正则或未匹配时可能原样返回，不能当作自动识别的安全脱敏入口。

```java
DesensitizeUtil.mobilePhone("13812345678");  // 138****5678
DesensitizeUtil.idCard(idCard);              // 合法号码保留前 4 后 4，否则全掩码
DesensitizeUtil.email("test@example.com");   // t****@example.com
DesensitizeUtil.bankCard("6222 8888 8888 8888"); // 6222********8888
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
String cipher = EncryptUtil.aesGcmEncrypt("待加密内容", aesKey);
String decoded = EncryptUtil.aesGcmDecrypt(cipher, aesKey);

// RSA（PKCS#1 v1.5 填充；接收裸 Base64 DER 公钥/私钥）
EncryptUtil.RsaKeyPair pair = EncryptUtil.rsaKeyPairBase64();
String enc = EncryptUtil.rsaEncrypt("待加密内容", pair.getPublicKey());
String dec = EncryptUtil.rsaDecrypt(enc, pair.getPrivateKey());

// PEM 格式仅用于交换，不直接传给上述裸 Base64 入参
EncryptUtil.RsaKeyPair pem = EncryptUtil.rsaKeyPairPem();
```

注意：

- `md5` / `sha1` / `sha256` 均为**无盐**原始哈希，切勿用于口令存储；
- 摘要输出小写 Hex；HMAC 输出 Base64；这是各 API 的明确协议，不可混用。
- RSA 新密钥只允许 2048/3072/4096 位，默认 2048；不再生成 512/1024 位弱密钥。
- `rsaEncrypt/rsaDecrypt` 是当前唯一的 RSA 加解密入口，采用 PKCS#1 v1.5 填充（2048 位最多 245 字节，超限直接报错）；该填充存在 Bleichenbacher 风险，敏感大数据建议 AES-GCM 加密后再用 RSA 保护密钥。
- `aesEcbEncrypt/aesEcbDecrypt` 已废弃，仅用于旧 ECB 数据兼容。
- GCM 使用共享安全随机源，每次生成 12 字节 IV、16 字节认证标签；解密拒绝过短/被篡改密文。
- 失败日志不输出密钥、明文、密文；`RsaKeyPair` 仍为 `@Data` 对象，`toString()` 会输出私钥，请勿直接记录该对象。
- GCM 密文短于 `IV + 认证标签`（28 字节）时直接拒绝。

### 雪花 ID — SnowflakeUtil

Twepoch 为 `1420041600000L` 的雪花算法实现；相同 `workerId+datacenterId` 复用同一实例，`nextId()` 线程安全。两个节点编号均为 0~31；回拨小于 2000ms 时沿用上次逻辑时间，达到 2000ms 即拒绝生成；同一毫秒序列耗尽时等待时钟前进。

```java
SnowflakeUtil sf = SnowflakeUtil.getInstance(1, 2); // workerId=1, datacenterId=2
// getInstance() 使用 0,0；getInstance(1) 使用 1,0
long id = sf.nextId();
```

> 多节点/多 JVM 部署必须分配不冲突的节点编号组合。工具不提供分布式编号分配、重启状态持久化或时钟注入；默认 0,0 只适合单 JVM，重启及编号复用的安全性也由部署方保证。

### 时间工具 — DateTimeUtil

基于 `java.time` 的格式化 / 解析 / 转换 / 区间判断；内置 `yyyy-MM-dd HH:mm:ss`、`yyyy-MM-dd`、`HH:mm:ss` 等常量。

默认与 pattern 入口使用 STRICT 解析（`yyyy` 视为纪元年并缺省公元），拒绝 2 月 30 日、非闰年 2 月 29 日、24:00:00 等输入；传入自定义 `DateTimeFormatter` 的重载尊重其 ResolverStyle。解析与格式化失败都统一抛 `BizException` 并保留原因链（例如用日期格式去格式化 `LocalTime`，得到的是 `BizException: 时间格式化失败`，不是 JDK 的 `UnsupportedTemporalTypeException`）。

格式缓存基于 Guava，最多 256 项，键包含 pattern 与格式 Locale，动态 pattern 不会无限增长。默认时区每次调用读取，运行期修改时区即时生效；时间戳/Date 转换另有显式 `ZoneId` 重载，夏令时空缺/重叠遵循 JDK `atZone` 规则。

```java
String s = DateTimeUtil.getDateTimeString(LocalDateTime.now());
LocalDateTime dt = DateTimeUtil.parseLocalDateTime("2026-09-04 12:00:00");
LocalDate d = DateTimeUtil.parseLocalDate("2026-09-04", "yyyy-MM-dd");
LocalDateTime t = DateTimeUtil.timestampToLocalDateTime(System.currentTimeMillis(), ZoneId.of("UTC"));
long ts = DateTimeUtil.localDateTimeToTimestamp(dt, ZoneId.of("Asia/Shanghai"));
boolean in = DateTimeUtil.isBetween(dt, start, end);
long days = DateTimeUtil.betweenDays(start, end);
LocalDateTime startOfDay = DateTimeUtil.getStartOfDay(LocalDate.now());
```

### HTTP 工具 — HttpUtil

基于 JDK 17 `HttpClient` 的同步封装，支持 GET / POST / PUT / DELETE、Fastjson2 JSON、Form 和对象反序列化。默认连接超时及请求超时均为 30 秒；`HttpRequest.timeout` 遵循 JDK 行为，不应视为独立的响应体逐次读取超时。

- 请求地址先做校验（非空、合法 URI、协议为 http/https、主机非空）后交由 JDK HttpClient 处理；相对地址与 `ftp://` 等不受支持的协议统一抛 `BizException`，不再泄漏 JDK 的 `IllegalArgumentException`。空白 POST/PUT 请求体保留原样。
- Form 的 `Duration` 重载接受亚秒超时；null、零、负超时拒绝。自定义同名请求头覆盖默认值；`Host`、`Content-Length` 等 JDK 受限头会被转成 `BizException`，因此不要把 Servlet 请求头映射整体转发过来。
- 查询参数在 `#fragment` 之前追加；已有查询串保持原样不重新编码，新参数用 UTF-8 URL 编码，参数名/值不能为 null。
- 文本响应与 `downloadFile` 的响应体都会完整读入内存，**均无大小上限**（Javadoc 已按此声明）。请求 URL 不可信或响应体规模不可控时存在内存耗尽风险，需要限制大小或下载大文件时应使用专用流式客户端。
- 请求/下载成功范围为 2xx；`isAccessible` 使用 GET 丢弃响应体，2xx/3xx 返回 true，默认请求超时 5 秒；任意失败（含 URL 非法）均返回 false 而不抛异常。
- IO 失败/中断时日志仅记录 URL 和错误摘要，异常对外使用通用消息、不携带底层异常。中断恢复线程标记，`isAccessible` 返回 false，其余请求抛 `BizException`。
- 工具自身日志不输出请求头/表单参数与响应体，但地址非法、协议不受支持及 HTTP 失败时会记录目标 URL（**含查询串**）；不要把凭据放进查询串，上层记录异常链时仍需按业务要求脱敏。
- 默认允许普通重定向，不内置 SSRF 白名单、重试或 multipart 编码。外部 URL 必须由调用方限制目标地址及重定向；非幂等请求不可盲目重试。

```java
String html = HttpUtil.get("https://example.com");
String json = HttpUtil.postJson("https://api.example.com/order", orderObj);
Map<String, String> form = Map.of("username", "admin");
String resp = HttpUtil.postForm("https://api.example.com/login", form);
Order order = HttpUtil.getForObject("https://api.example.com/order/1", Order.class);
```

### IP 工具 — IpUtil

客户端 IP 获取与 IPv4 归属地查询，地址解析基于 Guava `InetAddresses`，不触发 DNS。

`getIp(request)` 默认信任所有直连节点（`ip -> true`），因此可读取 `X-Forwarded-For` 等转发头；仅建议部署在可信反向代理之后时使用。需要严格边界时，使用带可信代理 Predicate 的重载：只有直连节点可信才检查 `X-Forwarded-For`，从右向左剥离可信节点；首个非可信地址作为客户端地址。无 XFF 时才考虑 X-Real-IP。

转发链不可解析（某个节点不是合法 IP）或超长（>8192 字符）时无法确定真实客户端，返回 `unknown`（`StrConst.UNKNOWN`），**不回退为直连地址**——直连地址属于可信代理自身，把它当成客户端地址会误导后续判定；调用方需显式处理 `unknown`。同理，带作用域但网卡不存在的 IPv6（如 `fe80::1%zzz`）视为无法解析，不会向外抛 `IllegalArgumentException`。

```java
String ip = IpUtil.getIp(request);                 // 默认信任直连节点，读取转发头
Set<String> proxies = Set.of("10.0.0.1", "10.0.0.2");
String clientIp = IpUtil.getIp(request, proxies::contains); // 仅信任指定代理
String location = IpUtil.getIpLocation(ip, " ");   // 归属地（国家/省/市/运营商拼接）
String[] region = IpUtil.parseIp(ip);              // 原始地域数组
IpUtil.isValidIp(ip);  IpUtil.isIpv4(ip);  IpUtil.isIpv6(ip);  IpUtil.isInternalIp(ip);
```

> 默认入口信任所有直连节点，用于限流等安全判断时请改用 Predicate 重载并明确信任边界。代理必须正确清洗/追加转发头；如容器已处理转发头，还需正确配置容器的信任范围。

归属地数据库规则：
- 使用 ip2region 2.7.0 全内存查询；默认加载随 jar 分发的 `classpath:ip2region.xdb`。
- 可在 JVM 启动时设置 `-Dsnowdrift.ip2region.path=/path/ip2region.xdb` 指定外部文件。首次成功加载后不热更新，更新文件后需重启；无需为数据更新重新发布框架。
- 初始化失败不阻断应用并记录错误，下次调用重新尝试；IPv6、内网、非法地址不送入 IPv4 数据库。
- `parseIp` 返回数据库原始分段数组（以 `|` 分隔，当前内置库为 4 段：国家/省/市/运营商），空结果、无效/内网 IP、数据库缺失时返回 5 个空串。展示优先使用 `getIpLocation`，它过滤空串、`0`、`内网IP`。
- `isInternalIp` 覆盖回环、站点本地（192.168/10.x/172.16-31）、链路本地（169.254/fe80::）；不包含 0.0.0.0、CGNAT 100.64/10、IPv6 ULA 等其它保留段，也不是“公网可访问”或 SSRF 安全判断。

### Servlet 工具 — ServletUtil

请求头 / 参数获取与 Fastjson2 JSON 响应写出。`getHeaderMap` 只保留同名头的第一个值，`getParamMap` 以逗号拼接多值参数，属于有损便捷视图；需要无损数据时使用 Servlet 原生 API。

`writeJsonResponse` 设置 HTTP 状态、application/json、UTF-8 并刷新 writer，不关闭容器 writer。它不使用 Spring MVC 的 Jackson 自定义配置；跨两种输出路径的业务数据格式须由应用统一。

```java
String ua = ServletUtil.getUserAgent(request);
Map<String, String> headers = ServletUtil.getHeaderMap(request);
Map<String, String> params = ServletUtil.getParamMap(request);
ServletUtil.writeJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED, Result.err(ResultCode.UNAUTHORIZED));
```

### Cron 工具 — CronUtil

Cron 表达式生成与 **Quartz 基础子集**校验，不是 Spring cron 校验器，也不宣称覆盖完整 Quartz 语法：

- 6/7 段：秒 分 时 日 月 周 [年]；日/周必须恰有一个为 `?`。
- 支持数字、`*`、列表、非跨界范围、步长、星期缩写；周编号为 Quartz 的 SUN=1…SAT=7，不是 Spring 的 0/7=SUN。
- 年只支持 1970~2099 单值或 `*`；`getCron` 也限制此年份范围。
- 不支持月名、L/W/#、宏和跨界范围；返回 false 可能表示语法超出子集，并非后端一定不支持。提交任务前使用调度后端原生校验器。
- `everyMinutes/everyHours` 按时钟字段步进，跨小时/日会重置，不保证任意 N 都是连续固定间隔；`workDay` 仅表示周一至周五，不处理法定节假日。

```java
String cron = CronUtil.getCron(LocalDateTime.of(2026, 9, 4, 12, 0, 0));
boolean ok = CronUtil.isValid("0 0 12 ? * MON");
String everyMinute = CronUtil.EVERY_MINUTE;
String everyDay = CronUtil.EVERY_DAY;
// 其他常量：EVERY_HOUR / EVERY_WEEK / EVERY_MONTH / EVERY_YEAR
CronUtil.everyMinutes(30);  CronUtil.everyHours(2);   // 每隔 N 分钟/小时
CronUtil.workDay();  CronUtil.atTime(12, 30, 0);  CronUtil.atWorkDayTime(9, 0, 0);
```

### 反射工具 — ReflectUtil

```java
Method m = ReflectUtil.getMethod(Order.class, "getTotal");        // 按名称查找，重载歧义时返回第一个并告警
Method exact = ReflectUtil.getMethod(Order.class, "getTotal", new Class<?>[0]); // 精确查找无参方法
Method withArg = ReflectUtil.getMethod(Order.class, "getTotal", String.class); // 精确查找指定参数
List<Field> fields = ReflectUtil.getDeclaredFields(order);        // 排除 static/synthetic 字段
List<Field> all = ReflectUtil.getDeclaredFields(order, true);     // 含父类，子类优先按名去重
```

> 方法查找只搜索当前类，不包含父类；不存在或不允许反射访问时抛 `BizException`。字段入口接受对象或 `Class<?>`，排除 static/synthetic 字段；是否包含父类由第二参数控制。

### 通用常量

| 类 | 说明 |
|----|------|
| `StrConst` | 字符串常量（EMPTY、COMMA、COLON、SLASH、UNDERLINE、TRACE_ID、MASK_REPLACEMENT 等） |
| `RegexConst` | 常用正则表达式（URL、CARD_NO、MOBILE_PHONE、FIXED_PHONE、EMAIL、BANK_CARD、VIN、CAR_LICENSE、CHINESE、I18N 等） |

`RegexConst.PWD`（8-15 位字母数字密码规则）已删除，密码策略请由业务侧定义。`URL` 与 `CRON` 都只是**宽松的形状校验**，不能当语义校验用：`URL` 不校验主机名结构，`http://abc`、`http://1.2.3.4` 判为 true，而 `http://a` 反而为 false（`[^\s/$.?#]` 之后是一个未转义的 `.`）；`CRON` 与 `CronUtil.isValid` 口径不同，既不接受 `?` 与星期缩写（`MON-FRI`）也不校验取值范围，对本库自己生成的表达式（`EVERY_DAY`、`workDay()`、`CronUtil.getCron()`）返回 false，却能放过 `1 2 3 4 5` 这类无意义串——校验 cron 请用 `CronUtil.isValid` 或调度后端原生校验器，发请求请用只接受 http/https 的 `HttpUtil`。`CHINESE` 仅覆盖基础汉字区 `\u4e00-\u9fa5`，不能用于完整 Unicode 汉字判定。

## 测试

base 模块已引入 `spring-boot-starter-test`（scope test，版本由 parent 的 dependencyManagement 托管，不传递给下游），并补齐单元测试：**15 个测试类、329 个用例**（`mvn -pl snowdrift-framework/snowdrift-base test` 实测全绿），覆盖正常流程、边界与异常场景。HTTP 相关用例使用 JDK 内置 `HttpServer` 起本地服务，不依赖外部网络。

| 测试类 | 覆盖点 |
|--------|--------|
| `ResultTest` / `ResultCodeTest` / `BizExceptionTest` / `IEnumTest` | 工厂方法与码值、timestamp、链式 setter、构造器与 `throwEx`、枚举按 code/note 查找与非法入参 |
| `AssertUtilTest` | 五类断言的通过与抛出文案、数组/集合重载、null 条件 |
| `ValidateUtilTest` | 手机/固话/身份证（15、18 与校验码、闰年、地区码）/银行卡/VIN/车牌/邮箱/URL 的正反例与边界 |
| `DesensitizeUtilTest` | 各脱敏器的正常、空白、非法与空白归一化路径；代理对（扩展区汉字）不被截断；掩码不改变长度 |
| `DateTimeUtilTest` | 格式化/解析往返、STRICT 拒绝 2 月 30 日与 24:00:00、解析与格式化的 `BizException` 口径、格式化器缓存、区间与差值、时区转换 |
| `CronUtilTest` | 生成表达式的自校验、Quartz 子集的正反例（`?` 互斥、星期缩写、步长越界）、参数越界抛异常 |
| `EncryptUtilTest` | 摘要已知向量、HMAC、AES-GCM 往返与随机 IV/篡改/密钥长度、RSA 往返与明文长度上限、ECB 兼容路径 |
| `SnowflakeUtilTest` | 实例复用、ID 唯一与单调、跨线程并发、非法 workerId/datacenterId、时钟回拨（容忍阈值内/外）与毫秒内序列用尽 |
| `HttpUtilTest` | URL/表单拼接与编码、Basic Auth、地址与请求头非法时的 `BizException`，以及基于 JDK `HttpServer` 的 GET/POST/JSON/表单/下载/超时/中断 |
| `IpUtilTest` / `ServletUtilTest` / `ReflectUtilTest` | 代理链解析与畸形链、内网/非法 IP、归属地过滤；头/参数视图与 JSON 写出；方法与字段反射、父类去重 |

```bash
mvn -pl snowdrift-framework/snowdrift-base -am test
```

## 升级注意

- `Result` 改为 `@Data` 链式 setter；`Result.ok(data).setMsg(...)` 等链式调用成为推荐写法。
- `RegexConst.PWD` 已删除；依赖它的代码需迁移到业务侧策略。
- 加密失败日志不再携带完整异常链，避免密钥/明文泄漏；排查需在调用方补日志。
- 非法日期（如 2 月 30 日、非闰年 2 月 29 日）不再被 SMART 规则修正；需要宽松解析时传入自定义 formatter。
- ip2region 数据库可通过 `-Dsnowdrift.ip2region.path=` 外置，IP 库更新无需重新发布框架。
- **行为变更（本次）**：`DesensitizeUtil.ip` 对空白入参返回空串、对无法识别的入参返回 `******`（原为先 `trim` 后原样返回）；`DesensitizeUtil.address` 对 6 个码点以内的入参整体掩码（原为长度 4~6 时原样输出）；`DesensitizeUtil.address`/`chineseName` 改为按码点推进（原实现会切断代理对/抛出越界）；`ValidateUtil.isIdCard` 的 15 位分支改为只接受 ASCII 数字。
- **异常口径变更（本次）**：`IpUtil.getIp(request, trustedProxy)` 在转发链非法或超长时返回 `unknown`（原为回退直连地址），带不存在网卡作用域的 IPv6 不再抛 `IllegalArgumentException`；`IpUtil.getIpLocation` 会过滤空串（原会返回 `"----"` 这类只有分隔符的结果）；`HttpUtil` 对相对地址、非 http(s) 协议、受限请求头统一抛 `BizException`（原为 JDK 的 `IllegalArgumentException`）；`DateTimeUtil` 的格式化失败也抛 `BizException`（原为 `UnsupportedTemporalTypeException`）。
- **细节修复（本次）**：`BizException` 与 `EncryptUtil.RsaKeyPair` 补 `@Serial serialVersionUID = 1L`（异常/密钥对经 RPC、MQ、缓存跨进程传递时，不会因类结构演进抛 `InvalidClassException`）；`EncryptUtil.md5/sha1/sha256` 在算法不可用时保留具体原因（`不支持的摘要算法：xxx`），不再被统一成「xx摘要计算失败」；`DateTimeUtil.getDateString(LocalDate)` / `getTimeString(LocalTime)` 也走统一异常包装，格式化失败一律 `BizException`。
- 本轮未拆分 base、删除 xdb、改变依赖 scope 或统一全框架 JSON 栈；`args` 也仍为预留，不代表国际化已经接通。

## 模块定位

- 纯工具 / 模型层，不依赖 Spring，不产生自动配置（无 `AutoConfiguration.imports`）。
- 依赖为 Servlet/Validation API、Fastjson2、Jackson-annotations、commons-lang3/collections4、Guava、ip2region、SLF4J、Lombok；测试依赖 `spring-boot-starter-test`（scope test）。
- 雪花 ID、加密、脱敏、校验等类均可直接脱离 Spring 环境使用。
