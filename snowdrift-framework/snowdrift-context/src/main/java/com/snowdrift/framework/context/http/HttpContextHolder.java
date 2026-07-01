package com.snowdrift.framework.context.http;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.snowdrift.framework.common.util.AssertUtil;

import java.util.Objects;

/**
 * HttpContextHolder
 *
 * @author gaoyzelov
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
        AssertUtil.notNull(context,"http.context.null");
        HTTP_CONTEXT_HOLDER.set(context);
    }


    /**
     * 获取Http请求上下文
     *
     * @return Http请求上下文
     */
    public static HttpContext getContext() {
        HttpContext ctx = HTTP_CONTEXT_HOLDER.get();
        if (Objects.isNull(ctx)){
            ctx = createEmptyContext();
            HTTP_CONTEXT_HOLDER.set(ctx);
        }
        return ctx;
    }

    /**
     * 清空Http请求上下文
     */
    public static void clear() {
        HTTP_CONTEXT_HOLDER.remove();
    }

    /**
     * 创建一个空的Http请求上下文
     *
     * @return Http请求上下文
     */
    public static HttpContext createEmptyContext() {
        return HttpContext.builder().build();
    }
}
