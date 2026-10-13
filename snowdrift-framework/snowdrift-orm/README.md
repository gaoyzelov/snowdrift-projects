# snowdrift-orm

ORM 增强模块，基于 MyBatis-Plus 插件体系提供多租户隔离、数据权限、字段加密、自动填充等企业级 ORM 能力，全部通过配置开关控制。

## 模块结构

```
snowdrift-orm
├── snowdrift-orm-base        ← 通用层：BaseEntity、TenantEntity、@DataScope、分页模型
└── snowdrift-orm-mp          ← MyBatis-Plus 插件实现
```

## 快速开始

```xml
<dependency>
    <groupId>com.snowdrift</groupId>
    <artifactId>snowdrift-orm-mp</artifactId>
</dependency>
```

## 实体基类

### BaseEntity — 标准实体

提供自动填充、逻辑删除。

```java
@Data
@TableName("sys_user")
public class User extends BaseEntity {
    private String username;
    private String password;

    // 敏感字段通过 TypeHandler 实现透明加解密
    @TableField(typeHandler = AesEncryptTypeHandler.class)
    private String phone;
}
```

| 字段 | 填充策略 | 说明 |
|------|---------|------|
| `createBy` | `FieldFill.INSERT` | 创建人，自动从 SecurityContext 获取 |
| `createTime` | `FieldFill.INSERT` | 创建时间 |
| `updateBy` | `FieldFill.INSERT_UPDATE` | 更新人 |
| `updateTime` | `FieldFill.INSERT_UPDATE` | 更新时间 |
| `deleted` | `@TableLogic` | 逻辑删除标志（`0`=未删；删除时写入 `UNIX_TIMESTAMP()`，非 0 即已删） |

### TenantEntity — 多租户实体

继承 `BaseEntity`，额外添加租户隔离字段。

```java
@Data
@TableName("sys_config")
public class Config extends TenantEntity {
    private String configKey;
    private String configValue;
}
// 自动注入 WHERE tenant_id = ?，仅 INSERT 时填充
```

## 核心能力

### 多租户隔离

自动在所有查询中注入 `WHERE tenant_id = ?`，从 `SecurityContext` 获取当前租户 ID。

```yaml
snowdrift:
  orm:
    mp:
      tenant:
        enabled: true
        tenant-id-column: tenant_id
        ignore-tables:                  # 忽略多租户隔离的表
          - sys_config
          - sys_dict
```

```java
// 程序化跳过租户过滤（如系统初始化）
TenantUtil.ignore(() -> {
    List<SysConfig> all = configMapper.selectAll();
});
```

> **安全语义（fail-closed）：** 查询若取不到当前租户上下文会抛 `BizException`（`MultiTenantLineHandler`），避免跨租户读写；系统任务需放行时用 `TenantUtil.ignore(...)` 显式跳过。写入侧（`FieldAutoFillHandler`）在启用多租户且取不到租户上下文时**同样 fail-closed 抛 `BizException`**，除非显式配置 `snowdrift.orm.mp.tenant.default-tenant-id`（此时写入该默认租户并记 WARN）；**不会静默写 `tenant_id = 0`**。

> **写入侧身份以安全上下文为准：** 有租户上下文时，实体自带的 `tenantId` **必须与上下文一致**，否则直接抛 `BizException` 拒绝写入。
> 原因：MyBatis-Plus 的租户拦截器在“插入列中已包含租户列”时会跳过注入，因此实体上非空的 `tenantId`
> （典型来源：`BeanUtils.copyProperties(requestBody, entity)` 把请求体里的 `tenantId` 拷进实体）会让**请求方**
> 决定数据落进哪个租户。无租户上下文时仍保留既有的两个逃生口：显式设置实体 `tenantId`，或配置 `default-tenant-id`。

> **使用限制（务必遵守）：** 租户条件由 MyBatis-Plus 默认的 `TenantLineInnerInterceptor` 注入，
> 它的覆盖范围有边界，以下写法**不会被注入租户条件**，请避免：
>
> | 限制 | 说明 |
> |------|------|
> | 语句类型 | 只处理 `SELECT`/`INSERT`/`UPDATE`/`DELETE`。`REPLACE`、`MERGE` 等会被**原样放行且不报错**，请改用 `INSERT`/`UPDATE`/`DELETE` |
> | 子查询位置 | 不会进入 `CASE` 表达式、`HAVING`、`ORDER BY` 以及函数参数内部的子查询。请把子查询放在 `FROM`/`JOIN`/`WHERE` 的 `IN`、`EXISTS`、比较运算等受支持的位置 |
> | 子查询不能替代外层 | `SELECT * FROM t_a WHERE id IN (SELECT id FROM t_b WHERE tenant_id = 42)` 里的 `t_a` 仍未被过滤 |
>
> 也就是说：**租户表出现在上述位置时会形成跨租户读取，且不会被框架拦下。** 手写 XML/注解 SQL 时请人工确认每张租户表都带了租户条件。

### 数据权限 — @DataScope

基于注解的声明式行级权限控制，支持五种范围。

```java
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @DataScope(alias = "o", deptColumn = "dept_id", userColumn = "user_id")
    List<Order> selectOrderList(OrderQuery query);
}
// 自动追加 SQL：AND o.dept_id IN (1, 2, 3)
```

| 范围 | 枚举值 | 自动追加的 SQL 条件 |
|------|--------|-------------------|
| 全部 | `ALL` | 无过滤 |
| 本部门 | `DEPT` | `dept_id = ?` |
| 仅本人 | `SELF` | `user_id = ?` |
| 本部门及子部门 | `DEPT_AND_SUB` | `dept_id IN (?, ?, ...)` — 需实现 `IDataScopeProvider` |
| 自定义 | `CUSTOM` | `dept_id IN (?, ?, ...)` — 需实现 `IDataScopeProvider` |

```java
// DEPT_AND_SUB 和 CUSTOM 需要提供 SPI 实现（getDataScope 返回该用户的数据权限范围）
@Component
public class DeptDataScopeProvider implements IDataScopeProvider {
    @Override
    public DataScopeEnum getDataScope(Long userId) {
        return roleService.getDataScope(userId);
    }
    @Override
    public List<Long> getCustomDeptIds(Long userId) {
        return deptService.getDeptIdsByUser(userId);
    }
    @Override
    public List<Long> getChildDeptIds(Long deptId, boolean inclusive) {
        return deptService.getChildDeptIds(deptId);
    }
}
```

> **安全语义（fail-closed）：** 无登录上下文、缺少 `deptId/userId`，或部门列表为空时，一律按 `1=0` 返回空集，不误放行任何数据。

### 字段加密 — @TableField(typeHandler = ...)

对敏感字段进行透明 AES-GCM 认证加密，入库自动加密，出库自动解密。**仅支持 GCM 一种格式**（存储为 `{ENC}<Base64(iv + ciphertext + tag)>`）。

```yaml
snowdrift:
  orm:
    mp:
      crypto:
        enabled: true
        crypto-key: 0123456789abcdef0123456789abcdef   # 32 位十六进制 = AES-256
```

```java
// 加密字段需要通过 TypeHandler 指定
@TableField(typeHandler = AesEncryptTypeHandler.class)
private String phone;
```

- 加密使用 **AES-GCM**（密钥长度由 `crypto-key` 决定，支持 AES-128/192/256），每次加密生成随机 IV，密文带 `{ENC}` 前缀
- 读取时：带 `{ENC}` 前缀走 GCM 解密，无前缀按明文原样返回
- 安全防护：已带 `{ENC}` 前缀的数据拒绝再次加密（大小写不敏感）；密钥不可用时加/解密均 fail-closed 抛 `BizException`
- **不支持旧 ECB 格式**：仅 GCM 单一格式，无 ECB 向后兼容与自动升级逻辑

> 密钥为十六进制字符串，长度需为 **32/48/64 位**（对应 AES-128/192/256），且为偶数位；**无效长度或含非十六进制字符均在启动期即报错**（fail-fast，与运行期 `EncryptUtil` 的十六进制解析一致）。

### 自动填充

`FieldAutoFillHandler` 在 INSERT / UPDATE 时自动填充审计字段和租户 ID（实体字段需标注 `@TableField(fill = ...)`），**字段已有值时不会覆盖**。

- `createBy` / `updateBy`：取自 `SecurityContext`（昵称优先，否则账号）；
- **无登录上下文（匿名/系统任务）写库不因操作人缺失而报错**：操作人降级为 `system`（日志按 debug 记录，避免批量写入刷屏）；但若启用多租户且取不到租户上下文，INSERT 会 fail-closed 抛 `BizException`，除非配置了 `default-tenant-id`（见「多租户隔离」）。

### 分页

```yaml
snowdrift:
  orm:
    mp:
      pagination:
        db-type: MYSQL
        max-limit: 1000                # 单页最大条数
        overflow: true                 # 溢出处理，默认 true
        optimize-join: true            # LEFT JOIN count 优化
```

### 安全防护

| 能力 | 默认 | 说明 |
|------|------|------|
| 防全表操作 | ✅ 开启 | `BlockAttackInnerInterceptor` 阻止无 WHERE 的 UPDATE/DELETE。它排在多租户拦截器**之前**，否则租户条件会先把无 WHERE 语句改写掉，守卫不再触发 |
| 乐观锁 | ❌ 关闭 | 配合 `@Version` 注解使用，`optimistic-lock: true` 开启 |

```yaml
snowdrift:
  orm:
    mp:
      optimistic-lock: true
```

## 扩展

实现 `IDataScopeProvider` 接口自定义数据权限规则，实现 `MetaObjectHandler` 接口自定义自动填充逻辑。需要扩展 MyBatis-Plus 拦截器时使用 `MybatisPlusInterceptorCustomizer`，保留框架默认的租户、数据权限、防全表操作和分页保护：

```java
@Bean
MybatisPlusInterceptorCustomizer customizer() {
    return interceptor -> interceptor.addInnerInterceptor(new MyCustomInnerInterceptor());
}
```

直接声明 `MybatisPlusInterceptor` 会整体替换框架默认实例，默认启动失败。只有确实要自行承担全部拦截器保护时，才显式设置 `snowdrift.orm.mp.allow-interceptor-override=true`；启动时仍会记录告警。

## 配置属性参考

### snowdrift.orm.mp

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `optimistic-lock` | Boolean | false | 乐观锁开关 |
| `allow-interceptor-override` | Boolean | false | 是否显式允许自定义 `MybatisPlusInterceptor` 整体替换框架默认实例；开启后会告警，通常应改用 `MybatisPlusInterceptorCustomizer` |
| `crypto` | Boolean | false | 字段加密开关 |
| `crypto-key` | String | — | AES 密钥（十六进制，32/48/64 位对应 AES-128/192/256） |

### snowdrift.orm.mp.tenant

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | Boolean | false | 多租户开关（默认关闭） |
| `tenant-id-column` | String | tenant_id | 租户 ID 字段名 |
| `ignore-tables` | Set\<String\> | — | 忽略租户过滤的表名（匹配忽略大小写） |
| `default-tenant-id` | Long | — | 无租户上下文写入时使用的默认租户；不配置则 fail-closed 抛错 |

### snowdrift.orm.mp.pagination

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `db-type` | DbType | MYSQL | 数据库类型 |
| `max-limit` | Long | 1000 | 单页最大条数 |
| `overflow` | Boolean | true | 溢出处理 |
| `optimize-join` | Boolean | true | LEFT JOIN count 优化 |
