package com.snowdrift.framework.orm.mp.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.orm.mp.properties.OrmMpTenantProperties;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Objects;
import java.util.Set;

/**
 * 多租户 SQL 行级处理器
 * <p>
 * 实现 MyBatis-Plus 的 {@link TenantLineHandler}，在 SQL 执行前自动注入租户过滤条件。
 * 工作原理：
 * <ol>
 *   <li>从 {@link SecurityContextHolder} 获取当前请求的租户ID</li>
 *   <li>在所有查询/更新/删除 SQL 中追加 <code>AND tenant_id = {当前租户ID}</code></li>
 *   <li>通过 {@link OrmMpTenantProperties#getIgnoreTables()} 可排除无需过滤的系统表</li>
 * </ol>
 * </p>
 *
 * <pre>
 * -- 原始 SQL
 * SELECT * FROM t_order WHERE status = 1
 * -- 处理后
 * SELECT * FROM t_order WHERE status = 1 AND tenant_id = 100
 * </pre>
 *
 * @author gaoyzelov
 * @date 2026/7/1-15:13
 * @since 1.0.0
 */
@Slf4j
public class MultiTenantLineHandler implements TenantLineHandler {

    /** 租户字段名 */
    private static final String TENANT_ID = "tenant_id";
    private final OrmMpTenantProperties tenantProperties;

    public MultiTenantLineHandler(OrmMpTenantProperties properties) {
        this.tenantProperties = properties;
    }

    /**
     * 获取当前请求上下文的租户ID，转换为 SQL 表达式
     *
     * @return 租户ID 的 {@link LongValue} 表达式；无租户上下文时返回 {@link NullValue}
     */
    @Override
    public Expression getTenantId() {
        Long tenantId = SecurityContextHolder.getContext().getTenantId();
        if (Objects.nonNull(tenantId)) {
            return new LongValue(tenantId);
        }
        return new NullValue();
    }

    /**
     * 租户字段的数据库列名
     *
     * @return 固定返回 {@code "tenant_id"}
     */
    @Override
    public String getTenantIdColumn() {
        return TENANT_ID;
    }

    /**
     * 判断当前表是否需要跳过租户过滤
     * <p>
     * 处理逻辑：
     * <ul>
     *   <li>安全上下文中无租户ID → 不应用租户过滤（{@code false}）</li>
     *   <li>配置了忽略表列表且当前表在列表中 → 跳过过滤（{@code true}）</li>
     *   <li>配置了忽略表列表但当前表不在列表中 → 应用过滤（{@code false}）</li>
     *   <li>未配置忽略表 → 对所有表应用过滤（{@code false}）</li>
     * </ul>
     * </p>
     *
     * @param tableName 当前 SQL 操作的表名
     * @return {@code true} 跳过租户过滤，{@code false} 应用租户过滤
     */
    @Override
    public boolean ignoreTable(String tableName) {
        Long tenantId = SecurityContextHolder.getContext().getTenantId();
        // 无租户上下文时不应用过滤
        if (Objects.nonNull(tenantId)) {
            // 检查当前表是否在忽略列表中
            Set<String> ignoreTables = tenantProperties.getIgnoreTables();
            if (CollectionUtils.isNotEmpty(ignoreTables)) {
                return ignoreTables.contains(tableName);
            }
        }
        return false;
    }
}
