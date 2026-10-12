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


    /**
     * 多租户实体上的租户属性名
     */
    private static final String TENANT_ID_FIELD = "tenantId";

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
        if (Boolean.TRUE.equals(tenantProperties.getEnabled()) && metaObject.hasSetter(TENANT_ID_FIELD)) {
            fillTenantId(metaObject);
        }
    }

    /**
     * 填充租户ID（按实体 Java 属性名匹配，与上方 createBy 等一致，故传属性名而非列名）
     *
     * <p><b>有租户上下文时，上下文租户是唯一权威。</b>实体自带的 {@code tenantId} 必须与上下文一致，
     * 不一致直接拒绝写入。原因：MyBatis-Plus 的 {@code TenantLineHandler#ignoreInsert} 默认实现会在
     * “插入列中已包含租户列”时跳过租户注入，因此实体上非空的 {@code tenantId} 会绕过拦截器，
     * 让请求体（如 {@code BeanUtils.copyProperties(requestBody, entity)}）决定数据落进哪个租户。</p>
     *
     * <p><b>无租户上下文时保留既有的两个逃生口</b>（系统任务 / MQ 消费 / 数据迁移等无登录场景）：
     * 调用方显式设置的实体 {@code tenantId}，或配置的 {@code default-tenant-id}；
     * 两者都没有则 fail-closed 抛错，避免数据落进任何租户都查不到的孤岛。</p>
     *
     * @param metaObject MyBatis-Plus 元数据对象
     * @throws BizException 实体租户与上下文租户不一致，或无上下文且未提供默认租户时抛出
     */
    private void fillTenantId(MetaObject metaObject) {
        Long contextTenantId = resolveTenantId();
        if (contextTenantId != null) {
            Object entityTenantId = metaObject.getValue(TENANT_ID_FIELD);
            if (entityTenantId != null && !contextTenantId.equals(entityTenantId)) {
                throw new BizException("实体 tenantId(" + entityTenantId + ") 与当前租户上下文("
                        + contextTenantId + ") 不一致，已拒绝写入以避免跨租户数据污染");
            }
            metaObject.setValue(TENANT_ID_FIELD, contextTenantId);
            return;
        }
        Long defaultTenantId = tenantProperties.getDefaultTenantId();
        if (defaultTenantId == null) {
            if (metaObject.getValue(TENANT_ID_FIELD) != null) {
                return;
            }
            throw new BizException("写入多租户实体时未获取到租户上下文：请在已登录上下文中写入，"
                    + "或显式设置实体的 tenantId，"
                    + "或配置 snowdrift.orm.mp.tenant.default-tenant-id 声明默认租户");
        }
        log.warn("无租户上下文，tenantId 写入显式配置的默认租户 {}", defaultTenantId);
        this.strictInsertFill(metaObject, TENANT_ID_FIELD, Long.class, defaultTenantId);
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
