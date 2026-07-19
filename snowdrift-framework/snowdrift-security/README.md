# snowdrift-security

安全认证模块，双安全框架实现共用 `ISecurityService` 抽象，`@AnonymousAccess` 标记公开接口。

## 模块结构

```
snowdrift-security
├── snowdrift-security-core        ← 通用层：ISecurityService、TokenInfo、SecurityException
├── snowdrift-security-satoken     ← Sa-Token 实现（轻量级）
└── snowdrift-security-spring      ← Spring Security 实现（企业级）
```

## 快速开始

按需引入一个安全实现即可，核心 API（`snowdrift-security-core`）会作为传递依赖自动引入。

```xml
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-security-satoken</artifactId>
</dependency>
<!-- 或 security-spring -->
```

## 配置

```yaml
snowdrift:
  security:
    header-name: Authorization         # Token 所在的请求头名称
    timeout: 86400                     # 绝对超时（秒），默认 24h
    active-timeout: 1800               # 闲置超时（秒），默认 30min
    prefix: Bearer                     # Token 前缀
```

### Sa-Token 实现

```yaml
snowdrift:
  security:
    sa-token:
      enabled: true
      concurrent: true                # 是否允许多端同时登录
      is-share: false                 # 多人登录同账号时是否共用 Token
      max-login-count: 12             # 最大登录数（is-share=false 时生效）
      token-style: uuid               # Token 风格：uuid / simple-uuid / random-32/64/128 / tik
      is-log: false                   # 是否输出 Sa-Token 框架日志
```

### Spring Security 实现

```yaml
snowdrift:
  security:
    spring:
      enabled: true
      csrf-enabled: false             # REST API 默认关闭 CSRF
      cors-enabled: true              # 默认开启 CORS
```

> Token 存储自动选择：classpath 中有 `RedisConnectionFactory` 时使用 `RedisTokenStore`，否则使用 `InMemoryTokenStore`。无需单独配置。

## 代码示例

### 登录

```java
@Autowired
private ISecurityService securityService;

// 构造 SecurityContext 后传入
SecurityContext ctx = SecurityContext.builder()
        .userId(userId)
        .username(username)
        .nickname(nickname)
        .tenantId(tenantId)
        .build();
TokenInfo token = securityService.login(ctx);
// TokenInfo { tokenValue, tokenName, prefix, expiresIn }
```

### 公开接口

```java
@AnonymousAccess
@PostMapping("/login")
public Result<TokenInfo> login(@RequestBody LoginDTO dto) { ... }
```

### 权限校验

```java
// 角色检查
securityService.hasRole("ADMIN");

// 权限检查（支持通配符）
securityService.hasPermission("order:create");
securityService.hasPermission("order:*");
```

### 获取当前用户

```java
Long userId = SecurityContextHolder.getUserId();
String username = SecurityContextHolder.getUsername();
Long tenantId = SecurityContextHolder.getTenantId();
```

### 自定义 Token 存储（Spring Security 实现）

```java
@Component
public class CustomTokenStore extends AbstractTokenStore {
    // 自定义 Token 存储（如数据库）
}
```

## 实现对比

| 特性 | Sa-Token | Spring Security |
|------|----------|-----------------|
| 框架体积 | 轻量 | 重量 |
| 注解鉴权 | `@SaCheckLogin` / `@SaCheckRole` / `@SaCheckPermission` | `@PreAuthorize` |
| Token 存储 | Sa-Token 内置 DAO | 可插拔 TokenStore（InMemory / Redis，自动选择） |
| 学习曲线 | 低 | 高 |
| 企业集成 | — | ✅ 标准安全体系 |

## 配置属性参考

### snowdrift.security（公共）

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `header-name` | String | Authorization | Token 请求头名称 |
| `timeout` | long | 86400 | 绝对超时（秒） |
| `active-timeout` | long | 1800 | 闲置超时（秒） |
| `prefix` | String | Bearer | Token 前缀 |
| `exclude-path-patterns` | List\<String\> | /favicon.ico, /swagger*/**, /v2/api-docs/**, /v3/api-docs/**, /doc.html, /swagger-ui.html, /error | 放行路径 |

### snowdrift.security.sa-token

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | Boolean | false | 启用开关 |
| `concurrent` | boolean | true | 多端同时登录 |
| `is-share` | boolean | false | 多人同账号共用 Token |
| `max-login-count` | int | 12 | 最大登录数 |
| `token-style` | String | uuid | Token 风格 |
| `is-log` | boolean | false | 是否输出 Sa-Token 框架日志 |

### snowdrift.security.spring

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | Boolean | false | 启用开关 |
| `csrf-enabled` | boolean | false | CSRF 开关 |
| `cors-enabled` | boolean | true | CORS 开关 |
