package com.snowdrift.framework.context.security;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * SecurityContextHolder
 *
 * @author 83674
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
        SECURITY_CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取安全上下文
     *
     * @return SecurityContext
     */
    public static SecurityContext getContext() {
        return SECURITY_CONTEXT_HOLDER.get();
    }

    /**
     * 清除安全上下文
     */
    public static void clear() {
        SECURITY_CONTEXT_HOLDER.remove();
    }
}
