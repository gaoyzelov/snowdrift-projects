package com.snowdrift.framework.orm.mp.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.orm.mp.properties.OrmMpTenantProperties;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Objects;
import java.util.Set;

/**
 * MultiTenantLineHandler
 *
 * @author gaoyzelov
 * @date 2026/7/1-15:13
 * @description 多租户 SQL 行级处理器
 * @since 1.0.0
 */
@Slf4j
public class MultiTenantLineHandler implements TenantLineHandler {

    /** 租户字段名 */
    private static final String TENANT_ID = "tenant_id";
    public static final Long DEFAULT_TENANT_ID = 0L;
    private final OrmMpTenantProperties tenantProperties;

    public MultiTenantLineHandler(OrmMpTenantProperties properties) {
        this.tenantProperties = properties;
    }

    /**
     * 获取当前请求上下文的租户ID，转换为 SQL 表达式
     *
     * @return 租户ID 的 {@link LongValue} 表达式；无租户上下文时返回默认值 {@link #DEFAULT_TENANT_ID} (0L)
     */
    @Override
    public Expression getTenantId() {
        Long tenantId = SecurityContextHolder.getContext().getTenantId();
        if (Objects.nonNull(tenantId)) {
            return new LongValue(tenantId);
        }
        return new LongValue(DEFAULT_TENANT_ID);
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
