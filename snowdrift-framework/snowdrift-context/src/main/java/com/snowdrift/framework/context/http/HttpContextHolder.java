package com.snowdrift.framework.context.http;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.base.util.AssertUtil;

/**
 * HttpContextHolder
 *
 * @author gaoyzelov
 * @date 2026/4/30-16:00
 * @description Http请求上下文Holder
 * @since 1.0.0
 */
public final class HttpContextHolder {

    private static final ThreadLocal<HttpContext> HTTP_CONTEXT_HOLDER = new TransmittableThreadLocal<>();

    private HttpContextHolder() {
    }

    /**
     * 设置Http请求上下文
     * <p>不接受 null（与 {@code ThreadLocal#set} 的“置空即清除”语义不同）：清空请调用 {@link #clear()}。</p>
     *
     * @param context Http请求上下文
     * @throws BizException context 为 null 时抛出
     */
    public static void setContext(HttpContext context) {
        AssertUtil.notNull(context,"HTTP上下文不能为空");
        HTTP_CONTEXT_HOLDER.set(context);
    }


    /**
     * 获取Http请求上下文
     *
     * @return Http请求上下文，<b>未设置时返回 null</b>（注意与 {@link com.snowdrift.framework.context.security.SecurityContextHolder#getContext()}
     * 的抛异常语义不同，调用方需自行判空）
     */
    public static HttpContext getContext() {
        return HTTP_CONTEXT_HOLDER.get();
    }

    /**
     * 清空Http请求上下文
     */
    public static void clear() {
        HTTP_CONTEXT_HOLDER.remove();
    }
}
