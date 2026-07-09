package com.snowdrift.framework.rpc.dubbo.constant;

/**
 * RpcContextConstants
 *
 * @author gaoyzelov
 * @description 定义 Dubbo attachment 中的 key
 * @date 2026/7/9 16:28
 * @since 1.0.0
 */
public final class RpcContextConstants {

    private RpcContextConstants() {
    }

    /** 链路追踪 ID */
    public static final String TRACE_ID = "x-snowdrift-trace-id";

    /** 安全上下文 */
    public static final String SECURITY_CONTEXT = "x-snowdrift-security-context";

}
