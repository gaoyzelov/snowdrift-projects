# snowdrift-common

核心基础模块，提供统一响应、业务异常、工具类等基础设施能力，所有其他模块均依赖此模块。

## 快速开始

```xml
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-common</artifactId>
</dependency>
```

## 核心功能

### 统一响应 — Result\<T\>

所有 API 返回 `Result<T>` 统一格式，包含 `code` / `msg` / `data` / `timestamp` 四个字段。

```java
// 成功
return Result.ok(data);

// 失败
return Result.err(ResultCode.BAD_REQUEST);
```

状态码由 `ResultCode` 记录管理，`msg` 字段存储 i18n key（如 `"common.success"`），不硬编码文本。

### 业务异常 — BizException

继承 `RuntimeException`，携带 `code`（int）和可选 `args`（Object[]）用于 i18n 格式化。

```java
// 用 ResultCode 常量抛出
throw new BizException(ResultCode.NOT_FOUND);

// 用 i18n key 字符串抛出
throw new BizException("order.pay.failed", new Object[]{orderId});
```

| 方法 | 说明 |
|------|------|
| `getCode()` | 获取异常码 |
| `getRawMessage()` | 获取 i18n key（无 code 前缀） |
| `getArgs()` | 获取格式化参数 |

### 断言工具 — AssertUtil

提供比 Spring Assert 更丰富的断言方法，所有方法失败时抛出 `BizException`。

```java
AssertUtil.notNull(obj, "common.not.found");
AssertUtil.notBlank(str, "validation.required");
AssertUtil.isTrue(condition, "param.invalid");
AssertUtil.inside(value, List.of(1, 2, 3), "value.out.of.range");
AssertUtil.custom(() -> complexCheck(), "check.failed");
```

### 校验工具 — ValidateUtil

全面的格式校验工具类，全部返回 boolean，不抛异常。

```java
ValidateUtil.isIdCard("110101199001011234");   // 身份证号（支持 15/18 位）
ValidateUtil.isMobilePhone("13800138000");      // 手机号
ValidateUtil.isEmail("test@example.com");       // 邮箱
ValidateUtil.isURL("https://example.com");      // URL
ValidateUtil.isBankCard("6222021234567890");    // 银行卡
ValidateUtil.isVIN("LSVAA4188E2123456");        // 车架号
ValidateUtil.isCarLicense("京A12345");          // 车牌号
```

### 加密工具 — EncryptUtil

覆盖常用加密算法，支持 MD5 / SHA / HMAC / AES / RSA。

```java
// 摘要
EncryptUtil.md5(text);
EncryptUtil.sha256(text);

// HMAC
EncryptUtil.hmacSha256(text, key);

// AES-GCM（推荐）
String cipher = EncryptUtil.aesGcmEncrypt(plainText, aesKey);
String plain = EncryptUtil.aesGcmDecrypt(cipher, aesKey);

// RSA
EncryptUtil.RsaKeyPair pair = EncryptUtil.rsaKeyPairPem();
String enc = EncryptUtil.rsaEncrypt(text, pair.getPublicKey());
String dec = EncryptUtil.rsaDecrypt(enc, pair.getPrivateKey());
```

> 注：`aesEcbEncrypt/aesEcbDecrypt` 已废弃，仅用于兼容旧 ECB 格式数据。新代码请使用 GCM。

### 雪花 ID — SnowflakeUtil

Twepoch 为 2015-01-01 的雪花算法实现，单例模式。

```java
// 默认 workerId=0, datacenterId=0
SnowflakeUtil sf = SnowflakeUtil.getInstance();
long id = sf.nextId();

// 指定 worker 和 datacenter
SnowflakeUtil sf = SnowflakeUtil.getInstance(1, 2);
```

> 分布式部署时请为每个节点分配不同的 workerId 和 datacenterId，避免 ID 碰撞。

### 其他工具类

| 类 | 功能 |
|----|------|
| `DateTimeUtil` | 时间格式化/解析/转换 |
| `HttpUtil` | Java 11 HttpClient 封装（GET/POST/表单） |
| `IpUtil` | IP 解析 + ip2region 归属地查询 |
| `ServletUtil` | 请求头/参数获取，JSON 响应写入 |
| `ReflectUtil` | 反射方法查找、字段获取 |
| `CronUtil` | Cron 表达式生成与校验（支持星期缩写） |
| `DesensitizeUtil` | 数据脱敏（手机号、身份证、邮箱等） |

### 通用常量

| 类 | 说明 |
|----|------|
| `StrConst` | 字符串常量（EMPTY、COMMA、COLON、SLASH 等） |
| `RegexConst` | 常用正则表达式（手机号、邮箱、身份证等） |

### 枚举

| 类 | 说明 |
|----|------|
| `EnabledEnum` | 启用/禁用（ENABLED=1, DISABLED=0） |
| `YesNoEnum` | 是否枚举（YES=1, NO=0） |
| `BizTypeEnum` | 业务操作类型（INSERT/UPDATE/DELETE/OTHER 等） |
| `DataScopeEnum` | 数据权限范围（ALL/DEPT/SELF/DEPT_AND_SUB/CUSTOM），支持 `@JsonCreator/@JsonValue` |
| `IEnum<T>` | 通用枚举接口，提供 `getByCode()` / `getByNote()` 查找方法 |
