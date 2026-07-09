package com.snowdrift.framework.context.security;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.snowdrift.framework.common.enums.DataScopeEnum;
import com.snowdrift.framework.common.exception.BizException;
import com.snowdrift.framework.common.util.AssertUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * SecurityContextHolder
 *
 * @author gaoyzelov
 * @date 2026/4/30-15:58
 * @description 安全上下文Holder
 * @since 1.0.0
 */
public class SecurityContextHolder {

    private static final ThreadLocal<SecurityContext> SECURITY_CONTEXT_HOLDER = new TransmittableThreadLocal<>();

    /**
     * 设置安全上下文
     *
     * @param context 安全上下文
     */
    public static void setContext(SecurityContext context) {
        AssertUtil.notNull(context, "security.context.null");
        SECURITY_CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取安全上下文
     *
     * @return SecurityContext
     */
    public static SecurityContext getContext() {
        SecurityContext ctx = SECURITY_CONTEXT_HOLDER.get();
        if (Objects.isNull(ctx)) {
            ctx = createEmptyContext();
            SECURITY_CONTEXT_HOLDER.set(ctx);
        }
        return ctx;
    }

    /**
     * 获取安全上下文
     *
     * @return SecurityContext
     */
    public static SecurityContext getRequiredContext() {
        SecurityContext ctx = SECURITY_CONTEXT_HOLDER.get();
        if (Objects.isNull(ctx)) {
            throw new BizException("security.context.null");
        }
        return ctx;
    }

    /**
     * 清除安全上下文
     */
    public static void clear() {
        SECURITY_CONTEXT_HOLDER.remove();
    }

    /**
     * 创建一个空的安全上下文
     *
     * @return SecurityContext
     */
    public static SecurityContext createEmptyContext() {
        return SecurityContext.builder().build();
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，未登录时抛出 BizException
     */
    public static Long getUserId() {
        return getRequiredContext().getUserId();
    }

    /**
     * 获取登录账号
     *
     * @return 登录账号，未登录时抛出 BizException
     */
    public static String getUsername() {
        SecurityContext ctx = getRequiredContext();
        return ctx.getUsername();
    }

    /**
     * 获取显示名称
     *
     * @return 显示名称，未登录时抛出 BizException
     */
    public static String getNickname() {
        SecurityContext ctx = getRequiredContext();
        return ctx.getNickname();
    }

    /**
     * 获取操作者显示名称（优先昵称，用于日志展示）
     *
     * @return 操作者名称，未登录时抛出 BizException
     */
    public static String getOperatorName() {
        SecurityContext ctx = getRequiredContext();
        String nickname = ctx.getNickname();
        if (StringUtils.isNotBlank(nickname)) {
            return nickname;
        }
        return ctx.getUsername();
    }

    /**
     * 获取当前租户ID
     *
     * @return 租户ID，未登录时抛出 BizException
     */
    public static Long getTenantId() {
        return getRequiredContext().getTenantId();
    }

    /**
     * 获取当前部门ID
     *
     * @return 部门ID，未登录时抛出 BizException
     */
    public static Long getDeptId() {
        return getRequiredContext().getDeptId();
    }

    /**
     * 获取数据范围
     *
     * @return 数据范围，未登录时抛出 BizException
     */
    public static DataScopeEnum getDataScope() {
        return DataScopeEnum.of(getRequiredContext().getDataScope());
    }
}
