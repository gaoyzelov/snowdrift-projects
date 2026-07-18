package com.snowdrift.framework.orm.mp.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.snowdrift.framework.common.exception.BizException;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.orm.mp.properties.OrmMpTenantProperties;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * FieldAutoFillHandler
 *
 * @author gaoyzelov
 * @date 2026/7/1-15:12
 * @description 字段自动填充处理器
 * @since 1.0.0
 */
public class FieldAutoFillHandler implements MetaObjectHandler {

    private final OrmMpTenantProperties tenantProperties;

    public FieldAutoFillHandler(OrmMpTenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }

    /**
     * INSERT 时自动填充
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
            Long tenantId = SecurityContextHolder.getContext().getTenantId();
            if (tenantId == null) {
                throw new BizException("orm.tenant.context.missing");
            }
            this.strictInsertFill(metaObject, tenantProperties.getTenantIdColumn(), Long.class, tenantId);
        }
    }

    /**
     * UPDATE 时自动填充
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
