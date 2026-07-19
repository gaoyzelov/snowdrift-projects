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
    exclude-path-patterns:             # 额外的放行路径
      - /swagger-ui/**
      - /v3/api-docs/**
```

### Sa-Token 实现

```yaml
snowdrift:
  security:
    sa-token:
      enabled: true
      token-style: uuid                # token / uuid / simple-uuid 等
      is-concurrent: true              # 是否允许并发登录
      is-share: true                   # 是否共享
      max-login-count: 1               # 最大登录数
```

### Spring Security 实现

```yaml
snowdrift:
  security:
    spring:
      enabled: true
      token-store: redis               # in-memory（默认） / redis
      csrf:
        enabled: false
      cors:
        enabled: true
```

## 代码示例

### 登录

```java
@Autowired
private ISecurityService securityService;

TokenInfo token = securityService.login(userId, username, nickname, tenantId);
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

### TokenStore 定制（Spring Security 实现）

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
| Token 存储 | Sa-Token 内置 DAO | 可插拔 TokenStore（InMemory / Redis） |
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
| `exclude-path-patterns` | List\<String\> | — | 额外放行路径 |

### snowdrift.security.sa-token

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | Boolean | false | 启用开关 |

### snowdrift.security.spring

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | Boolean | false | 启用开关 |
| `token-store` | String | in-memory | Token 存储类型（in-memory / redis） |
