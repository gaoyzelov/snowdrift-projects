<p align="center">
  <img src="docs/logo.png" alt="Snowdrift Logo" width="700px" />
</p>

# Snowdrift（雪堆）

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/java-17-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.5.14-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/spring--cloud-2025.0.2-brightgreen.svg)](https://spring.io/projects/spring-cloud)
[![MyBatis-Plus](https://img.shields.io/badge/mybatis--plus-3.5.15-blue.svg)](https://baomidou.com/)
[![Sa-Token](https://img.shields.io/badge/sa--token-1.45.0-5b8c5a.svg)](https://sa-token.cc/)
[![Redisson](https://img.shields.io/badge/redisson-4.3.0-red.svg)](https://redisson.org/)
[![Dubbo](https://img.shields.io/badge/dubbo-3.3.6-orange.svg)](https://dubbo.apache.org/)
[![Knife4j](https://img.shields.io/badge/knife4j-4.5.0-brightgreen.svg)](https://doc.xiaominfo.com/)
[![Hutool](https://img.shields.io/badge/hutool-5.8.44-6c9d99.svg)](https://hutool.cn/)
[![XXL-JOB](https://img.shields.io/badge/xxl--job-3.4.1-blue.svg)](https://www.xuxueli.com/xxl-job/)

**Snowdrift（雪堆）** 是一个面向 Spring Cloud 微服务架构的开源基础设施脚手架，以"开箱即用、按需引入、安全默认"为设计理念，为 Java 后端开发团队提供经过工程验证的企业级基础能力层。

项目涵盖 **12 个功能模块、34 个 Maven 子模块**，覆盖了微服务开发中最高频的基础设施需求——从统一响应与异常处理，到多后端缓存与分布式锁；从多租户 ORM 增强到消息队列封装；从对象存储策略化接入到 Dubbo RPC 上下文传播。所有模块均在 Spring Boot 3.x 自动配置机制之上构建，通过 `@ConditionalOnProperty` 等条件注解实现按需激活，零代码侵入即可集成到既有项目中。

## 快速开始

```bash
# 完整构建（跳过测试）
mvn clean install -DskipTests

# 构建单个模块
mvn clean install -pl snowdrift-framework/snowdrift-common

# 构建深层子模块
mvn clean install -pl snowdrift-framework/snowdrift-cache/snowdrift-cache-redisson
```

**环境要求**：Java 17+、Maven 3.8+

## 模块总览

```
snowdrift（雪堆）
├── snowdrift-dependencies   ← BOM：统一管理全部第三方依赖版本
├── snowdrift-parent         ← 父 POM：插件管理、编译参数、注解处理器
├── snowdrift-framework      ← 框架聚合模块
│   ├── snowdrift-common     ← 核心基础：Result<T>、ResultCode、BizException、工具类
│   ├── snowdrift-context    ← 请求上下文：HttpContext / SecurityContext（TTL 跨线程）
│   ├── snowdrift-log        ← 日志审计：@ApiLog / @LoginLog AOP 切面
│   ├── snowdrift-web        ← Web 自动配置：CORS、i18n、异常处理、异步上下文传播
│   ├── snowdrift-cache      ← 多后端缓存：Caffeine / Redis / Redisson
│   │                         ← 分布式能力：@DistributedLock、@RepeatSubmit
│   ├── snowdrift-oss        ← 对象存储：Local / MinIO / 阿里云 / 七牛云 / 腾讯云
│   ├── snowdrift-security   ← 安全认证：Sa-Token / Spring Security 双实现
│   ├── snowdrift-schedule   ← 分布式调度：Quartz / XXL-JOB 双实现
│   ├── snowdrift-mq         ← 消息队列：Kafka / RocketMQ / RabbitMQ
│   ├── snowdrift-orm        ← ORM 增强：自动填充、多租户、数据权限、字段加密
│   └── snowdrift-plugin     ← 插件基础设施（规划中）
```

## 📖 文档索引

各模块详细使用说明及配置参考：

| 模块 | 文档 | 说明 |
|------|------|------|
| snowdrift-common | [README](snowdrift-framework/snowdrift-common/README.md) | 统一响应、业务异常、加密/校验/雪花ID等工具类 |
| snowdrift-context | [README](snowdrift-framework/snowdrift-context/README.md) | HTTP/Security 上下文，TTL 跨线程传播 |
| snowdrift-log | [README](snowdrift-framework/snowdrift-log/README.md) | @ApiLog/@LoginLog 审计日志，可插拔日志存储 |
| snowdrift-web | [README](snowdrift-framework/snowdrift-web/README.md) | CORS、i18n、异步、全局异常处理 |
| snowdrift-cache | [README](snowdrift-framework/snowdrift-cache/README.md) | 多后端缓存、@DistributedLock、@RepeatSubmit |
| snowdrift-oss | [README](snowdrift-framework/snowdrift-oss/README.md) | 对象存储（5 种后端），策略模式驱动 |
| snowdrift-security | [README](snowdrift-framework/snowdrift-security/README.md) | Sa-Token / Spring Security 双实现 |
| snowdrift-schedule | [README](snowdrift-framework/snowdrift-schedule/README.md) | Quartz / XXL-JOB 分布式调度 |
| snowdrift-mq | [README](snowdrift-framework/snowdrift-mq/README.md) | Kafka / RocketMQ / RabbitMQ 消息队列 |
| snowdrift-orm | [README](snowdrift-framework/snowdrift-orm/README.md) | MyBatis-Plus：多租户、数据权限、字段加密 |
| snowdrift-rpc | [README](snowdrift-framework/snowdrift-rpc/README.md) | Dubbo 上下文传播、异常处理 |
| snowdrift-plugin | [README](snowdrift-framework/snowdrift-plugin/README.md) | 插件基础设施（规划中） |

## 核心特性

### 统一响应

所有 API 返回 `Result<T>` 统一格式（code / msg / data / timestamp），全局 `WebExceptionHandler` 覆盖 16 种异常类型，异常自动兜底。

```java
// 成功
return Result.ok(data);
// 失败（i18n key）
throw new BizException("common.not.found");
// 失败（自定义）
return Result.err(ResultCode.BAD_REQUEST);
```

### 国际化（i18n）

每个模块独立维护 i18n 资源，请求级语言切换，响应消息自动解析。

| 模块 | 资源文件 | 覆盖范围 |
|------|---------|---------|
| snowdrift-web | `web-messages` | 通用状态码、参数校验、文件上传 |
| snowdrift-oss | `oss-messages` | 全部 5 种存储后端 |
| snowdrift-security | `security-messages` | 认证/鉴权异常 |
| snowdrift-cache | `cache-messages` | 缓存操作、分布式锁、防重提交 |
| snowdrift-schedule | `schedule-messages` | Quartz + XXL-JOB |

```yaml
snowdrift:
  i18n:
    enabled: true
    default-locale: zh_CN
    supported-locales:
      - zh_CN
      - en_US
```

### 对象存储（OSS）

策略模式驱动，一套 API 适配 5 种后端，配置即切换。

```yaml
snowdrift:
  oss:
    default-config-key: default
    configs:
      default:
        oss-type: minio
        endpoint: http://localhost:9000
        access-key: admin
        secret-key: admin123
        bucket: my-bucket
```

支持的后端：

| 后端 | 模块 | 批量删除 | 私有 Bucket |
|------|------|---------|------------|
| Local | `snowdrift-oss-local` | 逐条 | — |
| MinIO | `snowdrift-oss-minio` | ✅ 原生 | ✅ |
| 阿里云 OSS | `snowdrift-oss-aliyun` | ✅ 原生 | ✅ |
| 七牛云 Kodo | `snowdrift-oss-qiniu` | ✅ 原生 | ✅ |
| 腾讯云 COS | `snowdrift-oss-tencent` | ✅ 原生 | ✅ |

### 缓存与分布式协调

多后端缓存 + 声明式分布式锁 + 防重复提交。

```java
// 分布式锁（SpEL 动态 key）
@DistributedLock(key = "'order:pay:' + #orderId", waitTime = 3, leaseTime = 10)
public void pay(Long orderId) { ... }

// 防重复提交（5 秒内同一 key 只允许一次）
@RepeatSubmit(key = "#orderNo", interval = 5)
public Result<Void> createOrder(String orderNo) { ... }
```

```yaml
snowdrift:
  cache:
    key-prefix: app
    key-ttl: 30m
    max-size: 10000
```

后端自动选择优先级：**Redisson > Redis > Caffeine**。

### 安全认证

双安全框架实现，共用 `ISecurityService` 抽象，`@AnonymousAccess` 标记公开接口。

| 实现 | 模块 | 激活条件 |
|------|------|---------|
| Sa-Token | `snowdrift-security-satoken` | `snowdrift.security.sa-token.enabled=true` |
| Spring Security | `snowdrift-security-spring` | `snowdrift.security.spring.enabled=true` |

```yaml
snowdrift:
  security:
    header-name: Authorization
    timeout: 86400
    exclude-path-patterns:
      - /swagger-ui/**
      - /v3/api-docs/**
```

### 分布式调度

统一 `IScheduleService` 抽象，Quartz 和 XXL-JOB 双实现，动态任务 CRUD。

```java
// 添加任务
XxlJobRequest request = new XxlJobRequest();
request.setName("myHandler");
request.setCron("0 */5 * * * ?");
request.setGroup("default");
XxlJobKey key = scheduleService.addJob(request);

// 暂停 / 恢复 / 触发
scheduleService.pauseJob(key);
scheduleService.triggerJob(key, Map.of("param", "value"));
```

### ORM 增强（MyBatis-Plus）

基于 MyBatis-Plus 插件体系，提供企业级 ORM 增强能力，全部通过配置开关控制。

**实体基类：** `BaseEntity`（自增主键 + 自动填充 + 逻辑删除）和 `TenantBaseEntity`（额外增加租户隔离）。

```java
@Data
@TableName("sys_user")
public class User extends BaseEntity {
    private String username;
    @TableField(typeHandler = AesEncryptTypeHandler.class)   // 入库加密，出库解密
    private String phone;
}
```

**核心能力：**

| 能力 | 说明 | 配置前缀 |
|------|------|---------|
| 自动填充 | INSERT/UPDATE 时自动填充 createBy、createTime、updateBy、updateTime、租户ID | — |
| 多租户隔离 | 自动在 SQL 中注入 `tenant_id = ?` 条件，支持忽略指定表 | `snowdrift.orm.mp.tenant` |
| 数据权限 | 基于 `@DataScope` 注解，支持 ALL / DEPT / DEPT_AND_SUB / SELF / CUSTOM 五种范围 | 注解驱动 |
| 字段加密 | 通过 `@TableField(typeHandler = AesEncryptTypeHandler.class)` 对敏感字段透明 AES-GCM 加解密 | `snowdrift.orm.mp.crypto` |
| 分页 | 多数据库方言、最大条数限制、溢出处理、LEFT JOIN count 优化 | `snowdrift.orm.mp.pagination` |
| 乐观锁 | 配合 `@Version` 注解，更新时自动版本号校验 | — |
| 防全表操作 | 阻止不带 WHERE 的 UPDATE/DELETE | — |

**数据权限示例：**

```java
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @DataScope(alias = "o", deptColumn = "dept_id", userColumn = "user_id")
    List<Order> selectOrderList(OrderQuery query);
}
// 自动追加 SQL: AND o.dept_id IN (1, 2, 3)  或  AND o.user_id = 100
```

```yaml
snowdrift:
  orm:
    mp:
      tenant:
        enabled: true
        ignore-tables:
          - sys_config
          - sys_dict
      crypto:
        enabled: true
        aes-key: your-16-byte-key!
      pagination:
        db-type: MYSQL
        max-limit: 1000
```

### 日志审计

`@ApiLog` 记录接口调用，`@LoginLog` 记录登录行为，支持参数脱敏。

```java
@ApiLog(bizType = BizTypeEnum.INSERT, module = "订单", summary = "创建订单", mask = {"phone", "idCard"})
public Result<Order> createOrder(@RequestBody OrderDTO dto) { ... }
```

### 通用工具

`snowdrift-common` 提供开箱即用的工具类：

| 类 | 功能 |
|----|------|
| `Result<T>` / `ResultCode` | 统一响应 + 可扩展状态码 |
| `BizException` | 业务异常（i18n key + 参数化） |
| `AssertUtil` | 断言式参数校验 |
| `DateTimeUtil` | 时间格式化/解析/转换 |
| `EncryptUtil` | MD5 / SHA / AES / RSA |
| `HttpUtil` | Java 11 HttpClient 封装 |
| `IpUtil` | IP 解析 + ip2region 归属地 |
| `SnowflakeUtil` | 雪花 ID 生成 |
| `ValidateUtil` | 身份证/手机号/邮箱等校验 |
| `CronUtil` | Cron 表达式生成与校验 |

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot / Spring Cloud / Spring Cloud Alibaba | 3.5.14 / 2025.0.2 / 2025.0.0.0 |
| ORM | MyBatis-Plus | 3.5.15 |
| 工具库 | Hutool | 5.8.44 |
| 对象映射 | MapStruct（Lombok 兼容） | 1.6.3 |
| API 文档 | Knife4j（OpenAPI 3） | 4.5.0 |
| 安全框架 | Sa-Token | 1.45.0 |
| 分布式锁 | Redisson | 4.3.0 |
| 多级缓存 | JetCache | 2.7.9 |
| 分布式调度 | XXL-JOB | 3.4.1 |
| JSON | FastJson2 | 2.0.61 |
| Excel | FastExcel | 1.3.0 |
| 数据库连接池 | Druid | 1.2.28 |
| 配置加密 | Jasypt | 4.0.4 |
| 操作日志 | BizLog | 3.0.6 |
| 上下文传递 | TransmittableThreadLocal | 2.14.5 |
| IP 归属地 | ip2region | 2.7.0 |
| RPC | Dubbo（BOM 中可用） | 3.3.6 |

## 开发进度

| 模块 | 状态 |
|------|------|
| snowdrift-common | ✅ 完成 |
| snowdrift-context | ✅ 完成 |
| snowdrift-log | ✅ 完成 |
| snowdrift-web | ✅ 完成 |
| snowdrift-cache | ✅ 完成 |
| snowdrift-oss | ✅ 完成 |
| snowdrift-security | ✅ 完成 |
| snowdrift-schedule | ✅ 完成 |
| snowdrift-mq | ✅ 完成 |
| snowdrift-orm | ✅ 完成 |
| snowdrift-plugin | 🚧 规划中 |

## 提交规范

使用 Conventional Changelog 风格，中文描述：

```
type(module): 描述

示例：
feat(cache): 新增 @DistributedLock 分布式锁注解
fix(schedule): 优化 XXL-JOB 调度服务异常处理
refactor(oss): 重构 OSS 配置属性结构
```

类型：`feat` / `fix` / `refactor` / `docs` / `style` / `test` / `chore`

## License

Apache License 2.0
