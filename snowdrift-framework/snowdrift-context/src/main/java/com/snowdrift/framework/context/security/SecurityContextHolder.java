package com.snowdrift.framework.context.security;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.util.AssertUtil;
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
public final class SecurityContextHolder {

    private static final ThreadLocal<SecurityContext> SECURITY_CONTEXT_HOLDER = new TransmittableThreadLocal<>();

    private SecurityContextHolder() {
    }

    /**
     * 设置安全上下文
     *
     * @param context 安全上下文
     */
    public static void setContext(SecurityContext context) {
        AssertUtil.notNull(context, "安全上下文不能为空");
        SECURITY_CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取安全上下文，不存在时抛出 {@link BizException}
     *
     * @return SecurityContext
     */
    public static SecurityContext getContext() {
        SecurityContext ctx = SECURITY_CONTEXT_HOLDER.get();
        if (Objects.isNull(ctx)) {
            throw new BizException("未获取到安全上下文，请确认请求已登录且安全上下文已注入");
        }
        return ctx;
    }

    /**
     * 窥探安全上下文（可为空，不抛异常）
     *
     * @return SecurityContext，未初始化时为 null
     */
    public static SecurityContext peekContext() {
        return SECURITY_CONTEXT_HOLDER.get();
    }

    /**
     * 清除安全上下文
     */
    public static void clear() {
        SECURITY_CONTEXT_HOLDER.remove();
    }

    /**
     * 获取当前用户ID
     *
     * @return 用户ID，无上下文时抛出 BizException，上下文未携带该字段时为 null
     */
    public static Long getUserId() {
        return getContext().getUserId();
    }

    /**
     * 获取登录账号
     *
     * @return 登录账号，无上下文时抛出 BizException，上下文未携带该字段时为 null
     */
    public static String getUsername() {
        return getContext().getUsername();
    }

    /**
     * 获取显示名称
     *
     * @return 显示名称，无上下文时抛出 BizException，上下文未携带该字段时为 null
     */
    public static String getNickname() {
        return getContext().getNickname();
    }

    /**
     * 获取操作者显示名称（优先昵称，用于日志展示）
     *
     * @return 操作者名称，无上下文时抛出 BizException，上下文未携带该字段时为 null
     */
    public static String getOperator() {
        SecurityContext ctx = getContext();
        String nickname = ctx.getNickname();
        if (StringUtils.isNotBlank(nickname)) {
            return nickname;
        }
        return ctx.getUsername();
    }

    /**
     * 获取当前租户ID
     *
     * @return 租户ID，无上下文时抛出 BizException，上下文未携带该字段时为 null
     */
    public static Long getTenantId() {
        return getContext().getTenantId();
    }

    /**
     * 获取当前部门ID
     *
     * @return 部门ID，无上下文时抛出 BizException，上下文未携带该字段时为 null
     */
    public static Long getDeptId() {
        return getContext().getDeptId();
    }
}
