package com.snowdrift.framework.rpc.dubbo.filter;

import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.rpc.dubbo.constant.RpcContextConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * DubboProviderContextFilter
 *
 * @author gaoyzelov
 * @description Dubbo Provider 端上下文恢复 Filter
 * @date 2026/7/9 16:34
 * @since 1.0.0
 */
@Slf4j
@Activate(group = CommonConstants.PROVIDER, order = -200)
public class DubboProviderContextFilter implements Filter {

    private static final String TRACE_ID_KEY = "traceId";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        restoreContext();
        try {
            return invoker.invoke(invocation);
        } finally {
            clearContext();
        }
    }

    /**
     * 从 Dubbo attachment 恢复上下文
     */
    private void restoreContext() {
        RpcContext rpcContext = RpcContext.getServiceContext();

        // 恢复 TraceId
        String traceId = (String) rpcContext.getObjectAttachment(RpcContextConstants.TRACE_ID);
        if (StringUtils.isNotBlank(traceId)) {
            MDC.put(TRACE_ID_KEY, traceId);
        } else {
            MDC.put(TRACE_ID_KEY, UUID.randomUUID().toString());
        }

        // 恢复安全上下文
        SecurityContext ctx = (SecurityContext) rpcContext.getObjectAttachment(RpcContextConstants.SECURITY_CONTEXT);
        if (ctx != null) {
            SecurityContextHolder.setContext(ctx);
        }
    }

    /**
     * 清除上下文，避免线程池复用污染
     */
    private void clearContext() {
        SecurityContextHolder.clear();
        MDC.remove(TRACE_ID_KEY);
    }

}
