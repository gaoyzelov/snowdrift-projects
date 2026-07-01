package com.snowdrift.framework.log.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * LogTraceUtil
 * @author gaoyzelov
 * @date 2026/4/29-17:00
 * @description 日志链路追踪工具类
 * @since 1.0.0
 */
public final class LogTraceUtil {

    private static final String TRACE_ID_KEY = "traceId";

    private LogTraceUtil(){}

    public static void setTraceId() {
        MDC.put(TRACE_ID_KEY, UUID.randomUUID().toString());
    }

    /**
     * 设置自定义 TraceId
     */
    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * 获取当前 TraceId
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    /**
     * 清除 TraceId
     */
    public static void clearTraceId() {
        MDC.remove(TRACE_ID_KEY);
    }
}
