package com.snowdrift.framework.orm.mp.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.orm.mp.properties.OrmMpTenantProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
@Slf4j
public class FieldAutoFillHandler implements MetaObjectHandler {

    /** 无登录上下文时的默认操作人（系统任务/匿名写库，避免审计列写入被阻断） */
    private static final String SYSTEM_OPERATOR = "system";

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
        String operatorName = resolveOperator();
        this.strictInsertFill(metaObject, "createBy", String.class, operatorName);
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateBy", String.class, operatorName);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        if (Boolean.TRUE.equals(tenantProperties.getEnabled())){
            Long tenantId;
            try {
                tenantId = SecurityContextHolder.getTenantId();
            } catch (Exception e) {
                tenantId = null;
            }
            if (tenantId == null) {
                log.debug("无安全上下文，tenantId 降级为 0（系统租户）");
                tenantId = 0L;
            }
            // strictInsertFill 按实体 Java 属性名匹配（与上方 createBy 等一致），此处传属性名而非列名
            this.strictInsertFill(metaObject, "tenantId", Long.class, tenantId);
        }
    }

    /**
     * UPDATE 时自动填充
     *
     * @param metaObject MyBatis-Plus 元数据对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        String operatorName = resolveOperator();
        this.strictUpdateFill(metaObject, "updateBy", String.class, operatorName);
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 解析当前操作人：优先昵称，无则账号；无登录上下文或两者皆空时降级为 {@link #SYSTEM_OPERATOR}。
     * <p>系统任务/匿名（如注册）写库不允许抛异常阻断 SQL，审计列统一记 system。</p>
     */
    private String resolveOperator() {
        SecurityContext context = SecurityContextHolder.peekContext();
        if (context == null) {
            log.debug("无安全上下文，操作人降级为 system");
            return SYSTEM_OPERATOR;
        }
        String name = StringUtils.isNotBlank(context.getNickname())
                ? context.getNickname()
                : context.getUsername();
        return StringUtils.isBlank(name) ? SYSTEM_OPERATOR : name;
    }
}
