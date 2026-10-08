package com.snowdrift.framework.orm.mp.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.snowdrift.framework.base.exception.BizException;
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

    /**
     * 无登录上下文时的默认操作人（系统任务/匿名写库，避免审计列写入被阻断）
     */
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
        if (Boolean.TRUE.equals(tenantProperties.getEnabled())) {
            Long tenantId = resolveTenantId();
            if (tenantId == null) {
                // fail-closed：取不到租户上下文时不静默写 0，避免数据落进任何租户都查不到的孤岛。
                // 系统写入（定时任务、MQ 消费、数据迁移）需显式配置默认租户，或由调用方显式设置实体 tenantId。
                Long defaultTenantId = tenantProperties.getDefaultTenantId();
                if (defaultTenantId == null) {
                    throw new BizException("写入多租户实体时未获取到租户上下文：请在已登录上下文中写入，"
                            + "或显式设置实体的 tenantId，"
                            + "或配置 snowdrift.orm.mp.tenant.default-tenant-id 声明默认租户");
                }
                log.warn("无租户上下文，tenantId 写入显式配置的默认租户 {}", defaultTenantId);
                tenantId = defaultTenantId;
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

    /**
     * 解析当前租户ID
     * <p>使用 {@link SecurityContextHolder#peekContext()} 而非 {@code getTenantId()}：后者在无上下文时抛异常，
     * 若用宽泛的 {@code catch} 接住，会把“程序错误”与“确实没有租户上下文”混为一谈。</p>
     *
     * @return 租户ID；无安全上下文或上下文未携带租户时返回 {@code null}
     */
    private Long resolveTenantId() {
        SecurityContext context = SecurityContextHolder.peekContext();
        return context == null ? null : context.getTenantId();
    }
}
