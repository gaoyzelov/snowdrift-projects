# snowdrift-orm

ORM 增强模块，基于 MyBatis-Plus 插件体系提供多租户隔离、数据权限、字段加密、自动填充等企业级 ORM 能力，全部通过配置开关控制。

## 模块结构

```
snowdrift-orm
├── snowdrift-orm-core        ← 通用层：BaseEntity、TenantBaseEntity、@DataScope
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

提供自增主键、自动填充、逻辑删除。

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
| `id` | `IdType.AUTO` | 自增主键 |
| `createBy` | `FieldFill.INSERT` | 创建人，自动从 SecurityContext 获取 |
| `createTime` | `FieldFill.INSERT` | 创建时间 |
| `updateBy` | `FieldFill.INSERT_UPDATE` | 更新人 |
| `updateTime` | `FieldFill.INSERT_UPDATE` | 更新时间 |
| `deleted` | `@TableLogic` | 逻辑删除（0=未删，1=已删） |

### TenantBaseEntity — 多租户实体

继承 `BaseEntity`，额外添加租户隔离字段。

```java
@Data
@TableName("sys_config")
public class Config extends TenantBaseEntity {
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
// DEPT_AND_SUB 和 CUSTOM 需要提供 SPI 实现
@Component
public class DeptDataScopeProvider implements IDataScopeProvider {
    @Override
    public List<Long> getCustomDeptIds(Long userId) {
        return deptService.getDeptIdsByUser(userId);
    }
    @Override
    public List<Long> getChildDeptIds(Long deptId) {
        return deptService.getChildDeptIds(deptId);
    }
}
```

### 字段加密 — @TableField(typeHandler = ...)

对敏感字段进行透明 AES-GCM 加密，入库自动加密，出库自动解密。

```yaml
snowdrift:
  orm:
    mp:
      crypto:
        enabled: true
        aes-key: 0123456789abcdef0123456789abcdef   # 32 位十六进制 = AES-256
```

```java
// 加密字段需要通过 TypeHandler 指定
@TableField(typeHandler = AesEncryptTypeHandler.class)
private String phone;
```

- 新加密使用 **AES-256-GCM**，每次加密生成随机 IV
- 密文带 `{ENC2}` 前缀，旧数据用 `{ENC}` 标记 ECB 模式，向后兼容
- 双重加密防护：已带 `{ENC2}` 或 `{ENC}` 前缀的数据拒绝再次加密

> 密码密钥支持 16/24/32 位十六进制字符，对应 AES-128/192/256。

### 自动填充

`FieldAutoFillHandler` 在 INSERT / UPDATE 时自动从 `SecurityContext` 填充审计字段和租户 ID，字段已有值时不会覆盖。

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
| 防全表操作 | ✅ 开启 | `BlockAttackInnerInterceptor` 阻止无 WHERE 的 UPDATE/DELETE |
| 乐观锁 | ❌ 关闭 | 配合 `@Version` 注解使用，`optimistic-lock: true` 开启 |

```yaml
snowdrift:
  orm:
    mp:
      optimistic-lock: true
```

## 扩展

实现 `IDataScopeProvider` 接口自定义数据权限规则，实现 `MetaObjectHandler` 接口自定义自动填充逻辑。

## 配置属性参考

### snowdrift.orm.mp

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `optimistic-lock` | Boolean | false | 乐观锁开关 |
| `crypto` | Boolean | false | 字段加密开关 |
| `crypto-key` | String | — | AES 密钥（16/24/32 位十六进制） |

### snowdrift.orm.mp.tenant

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enabled` | Boolean | null | 多租户开关（不配置时不启用） |
| `tenant-id-column` | String | tenant_id | 租户 ID 字段名 |
| `ignore-tables` | List\<String\> | [] | 忽略的表名 |

### snowdrift.orm.mp.pagination

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `db-type` | DbType | MYSQL | 数据库类型 |
| `max-limit` | Long | 1000 | 单页最大条数 |
| `overflow` | Boolean | true | 溢出处理 |
| `optimize-join` | Boolean | true | LEFT JOIN count 优化 |
