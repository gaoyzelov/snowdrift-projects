package com.snowdrift.framework.orm.mp.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.orm.mp.properties.OrmMpTenantProperties;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 字段自动填充处理器
 * <p>
 * 实现 MyBatis-Plus 的 {@link MetaObjectHandler}，在 INSERT 和 UPDATE 时自动填充通用字段：
 * </p>
 * <table>
 *   <caption>填充策略</caption>
 *   <tr><th>操作</th><th>字段</th><th>来源</th></tr>
 *   <tr><td>INSERT</td><td>createBy / updateBy</td><td>{@link SecurityContextHolder#getOperatorName()}</td></tr>
 *   <tr><td>INSERT</td><td>createTime / updateTime</td><td>{@link LocalDateTime#now()}</td></tr>
 *   <tr><td>INSERT</td><td>tenantId</td><td>{@link SecurityContextHolder#getContext() SecurityContext#getTenantId()}（仅多租户启用时）</td></tr>
 *   <tr><td>UPDATE</td><td>updateBy</td><td>{@link SecurityContextHolder#getOperatorName()}</td></tr>
 *   <tr><td>UPDATE</td><td>updateTime</td><td>{@link LocalDateTime#now()}</td></tr>
 * </table>
 * <p>
 * 使用 {@code strictInsertFill} / {@code strictUpdateFill} 仅填充未赋值的字段，不会覆盖用户显式设置的值。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/7/1-15:12
 * @since 1.0.0
 */
public class FieldAutoFillHandler implements MetaObjectHandler {

    private final OrmMpTenantProperties tenantProperties;

    public FieldAutoFillHandler(OrmMpTenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    /**
     * INSERT 时自动填充
     * <p>
     * 填充 createBy、createTime、updateBy、updateTime；
     * 若多租户启用则一并填充 tenantId（默认 0）。
     * </p>
     *
     * @param metaObject MyBatis-Plus 元数据对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        String operatorName = SecurityContextHolder.getOperatorName();
        this.strictInsertFill(metaObject, "createBy", String.class, operatorName);
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateBy", String.class, operatorName);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        if (Boolean.TRUE.equals(tenantProperties.getEnabled())){
            Long tenantId = Optional.ofNullable(SecurityContextHolder.getContext().getTenantId()).orElse(0L);
            this.strictInsertFill(metaObject, "tenantId", Long.class, tenantId);
        }
    }

    /**
     * UPDATE 时自动填充
     * <p>填充 updateBy、updateTime。</p>
     *
     * @param metaObject MyBatis-Plus 元数据对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        String operatorName = SecurityContextHolder.getOperatorName();
        this.strictUpdateFill(metaObject, "updateBy", String.class, operatorName);
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
