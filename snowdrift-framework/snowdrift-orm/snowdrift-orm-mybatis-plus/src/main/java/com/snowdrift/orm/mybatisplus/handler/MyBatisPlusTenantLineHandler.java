package com.snowdrift.orm.mybatisplus.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.snowdrift.core.context.SecurityContextHolder;
import com.snowdrift.orm.mybatisplus.properties.MybatisPlusTenantProperties;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.apache.commons.collections4.CollectionUtils;

/**
 * MyBatisPlusTenantLineHandler
 *
 * @author gaoye
 * @date 2025/03/20 19:05:04
 * @description 多租户处理器
 * @since 1.0.0
 */
public class MyBatisPlusTenantLineHandler implements TenantLineHandler {

    private final MybatisPlusTenantProperties tenantProperties;

    public MyBatisPlusTenantLineHandler(MybatisPlusTenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    /**
     * 获取租户ID
     * @return 租户ID
     */
    @Override
    public Expression getTenantId() {
        Object tenantId = SecurityContextHolder.getTenantId(tenantProperties.getDefaultTenantId());
        return new StringValue(tenantId.toString());
    }

    /**
     * 获取租户ID字段
     * @return 租户ID字段
     */
    @Override
    public String getTenantIdColumn() {
        return tenantProperties.getTenantIdColumn();
    }

    /**
     * 是否忽略表
     * @param tableName 表名
     * @return 是否忽略表
     */
    @Override
    public boolean ignoreTable(String tableName) {
        if (CollectionUtils.isEmpty(tenantProperties.getIgnoreTables())){
            return true;
        }
        return tenantProperties.getIgnoreTables().contains(tableName);
    }

}