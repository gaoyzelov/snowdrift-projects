package com.snowdrift.framework.rpc.dubbo.filter;

import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.rpc.dubbo.constant.RpcContextConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.MDC;


/**
 * DubboConsumerContextFilter
 *
 * @author gaoyzelov
 * @date 2026/7/9-16:31
 * @description Dubbo Consumer 端上下文传播 Filter
 * @since 1.0.0
 */
@Slf4j
@Activate(group = CommonConstants.CONSUMER, order = -200)
public class DubboConsumerContextFilter implements Filter {

    private static final String TRACE_ID_KEY = StrConst.TRACE_ID;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            injectContext();
        } catch (Exception e) {
            log.error("Dubbo RPC消费者上下文注入失败: interface={}, method={}",
                    invoker.getInterface().getName(), invocation.getMethodName(), e);
            RpcContext.getServiceContext().setAttachment(RpcContextConstants.CONTEXT_ERROR, StrConst.TRUE);
        }
        return invoker.invoke(invocation);
    }

    /**
     * 将 SecurityContext 和 traceId 注入到 Dubbo attachment
     */
    private void injectContext() {
        RpcContext rpcContext = RpcContext.getServiceContext();

        // 注入 TraceId（有则传，无则由 Provider 端自行生成）
        String traceId = MDC.get(TRACE_ID_KEY);
        if (StringUtils.isNotBlank(traceId)) {
            rpcContext.setObjectAttachment(RpcContextConstants.TRACE_ID, traceId);
        }

        // 注入安全上下文（可选：系统任务等无登录上下文的消费场景不注入，
        // 避免误报“上下文注入失败”；TraceId 仍正常透传，由 Provider 端自行兜底）
        SecurityContext ctx = SecurityContextHolder.peekContext();
        if (ctx != null) {
            rpcContext.setObjectAttachment(RpcContextConstants.SECURITY_CONTEXT, ctx);
        }
    }

}
