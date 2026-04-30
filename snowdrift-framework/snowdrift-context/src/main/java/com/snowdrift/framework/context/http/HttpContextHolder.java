package com.snowdrift.framework.context.http;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * HttpContextHolder
 *
 * @author 83674
 * @date 2026/4/30-16:00
 * @description Http请求上下文Holder
 * @since 1.0.0
 */
public class HttpContextHolder {

    private static final ThreadLocal<HttpContext> HTTP_CONTEXT_HOLDER = new TransmittableThreadLocal<>();

    /**
     * 设置Http请求上下文
     *
     * @param context Http请求上下文
     */
    public static void setContext(HttpContext context) {
        HTTP_CONTEXT_HOLDER.set(context);
    }


    /**
     * 获取Http请求上下文
     *
     * @return Http请求上下文
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
