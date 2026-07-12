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
 * DubboConsumerContextFilter
 *
 * @author gaoyzelov
 * @description Dubbo Consumer 端上下文传播 Filter
 * @date 2026/7/9 16:31
 * @since 1.0.0
 */
@Slf4j
@Activate(group = CommonConstants.CONSUMER, order = -200)
public class DubboConsumerContextFilter implements Filter {

    private static final String TRACE_ID_KEY = "traceId";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            injectContext();
        } catch (Exception e) {
            log.error("RPC 消费者上下文注入失败: interface={}, method={}",
                    invoker.getInterface().getName(), invocation.getMethodName(), e);
            RpcContext.getServiceContext().setAttachment(RpcContextConstants.CONTEXT_ERROR, "true");
        }
        return invoker.invoke(invocation);
    }

    /**
     * 将 SecurityContext 和 traceId 注入到 Dubbo attachment
     */
    private void injectContext() {
        RpcContext rpcContext = RpcContext.getServiceContext();

        // 注入 TraceId
        String traceId = MDC.get(TRACE_ID_KEY);
        if (StringUtils.isNotBlank(traceId)) {
            rpcContext.setObjectAttachment(RpcContextConstants.TRACE_ID, traceId);
        } else {
            traceId = UUID.randomUUID().toString();
            MDC.put(TRACE_ID_KEY, traceId);
            rpcContext.setObjectAttachment(RpcContextConstants.TRACE_ID, traceId);
        }

        // 注入安全上下文
        SecurityContext ctx = SecurityContextHolder.getContext();
        rpcContext.setObjectAttachment(RpcContextConstants.SECURITY_CONTEXT, ctx);
    }

}
