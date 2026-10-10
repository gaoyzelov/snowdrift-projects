package com.snowdrift.framework.rpc.dubbo.filter;

import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.rpc.dubbo.constant.RpcContextConstants;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DubboConsumerContextFilterTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 消费端上下文注入黑盒测试：写入 client attachment（Dubbo 3 正确通道）、无上下文降级、委托调用
 * @since 1.0.0
 */
class DubboConsumerContextFilterTest {

    interface DemoService {
        void call();
    }

    @SuppressWarnings("unchecked")
    private final Invoker<DemoService> invoker = mock(Invoker.class);
    private final Invocation invocation = mock(Invocation.class);
    private final Result result = mock(Result.class);
    private final DubboConsumerContextFilter filter = new DubboConsumerContextFilter();

    @AfterEach
    void clean() {
        RpcContext.removeClientAttachment();
        RpcContext.removeServiceContext();
        MDC.clear();
        SecurityContextHolder.clear();
    }

    @Test
    @DisplayName("有 traceId 与安全上下文时注入到 client attachment，并委托 invoker.invoke")
    void injectsTraceIdAndSecurityContextIntoClientAttachment() throws RpcException {
        when(invoker.invoke(invocation)).thenReturn(result);
        MDC.put("traceId", "t-1");
        SecurityContext ctx = SecurityContext.builder().userId(1L).tenantId(2L).build();
        SecurityContextHolder.setContext(ctx);

        Result returned = filter.invoke(invoker, invocation);

        assertThat(returned).isSameAs(result);
        verify(invoker).invoke(invocation);
        assertThat(RpcContext.getClientAttachment().getObjectAttachment(RpcContextConstants.TRACE_ID)).isEqualTo("t-1");
        assertThat(RpcContext.getClientAttachment().getObjectAttachment(RpcContextConstants.SECURITY_CONTEXT)).isSameAs(ctx);
    }

    @Test
    @DisplayName("回归防护：traceId 最终落入 client attachment（Dubbo 实际发送通道），确保能跨进程传播")
    void traceIdLandsInClientAttachmentSendChannel() throws RpcException {
        when(invoker.invoke(invocation)).thenReturn(result);
        MDC.put("traceId", "t-2");

        filter.invoke(invoker, invocation);

        // getServiceContext().setObjectAttachment 经 Dubbo 基类委托写入 CLIENT_ATTACHMENT，即 AbstractInvoker 发送的通道
        assertThat(RpcContext.getClientAttachment().getObjectAttachment(RpcContextConstants.TRACE_ID)).isEqualTo("t-2");
    }

    @Test
    @DisplayName("仅 traceId、无安全上下文：只注入 traceId，不设置降级标记")
    void injectsOnlyTraceIdWhenNoSecurityContext() throws RpcException {
        when(invoker.invoke(invocation)).thenReturn(result);
        MDC.put("traceId", "t-3");

        filter.invoke(invoker, invocation);

        assertThat(RpcContext.getClientAttachment().getObjectAttachment(RpcContextConstants.TRACE_ID)).isEqualTo("t-3");
        assertThat(RpcContext.getClientAttachment().getObjectAttachment(RpcContextConstants.SECURITY_CONTEXT)).isNull();
        assertThat(RpcContext.getClientAttachment().getAttachment(RpcContextConstants.CONTEXT_ERROR)).isNull();
    }

    @Test
    @DisplayName("无 traceId 且无上下文：不注入任何附件（由 Provider 端兜底生成 traceId）")
    void injectsNothingWhenNoContext() throws RpcException {
        when(invoker.invoke(invocation)).thenReturn(result);

        filter.invoke(invoker, invocation);

        assertThat(RpcContext.getClientAttachment().getObjectAttachment(RpcContextConstants.TRACE_ID)).isNull();
        assertThat(RpcContext.getClientAttachment().getObjectAttachment(RpcContextConstants.SECURITY_CONTEXT)).isNull();
    }

    @Test
    @DisplayName("空白 traceId 视为无，不注入")
    void blankTraceIdNotInjected() throws RpcException {
        when(invoker.invoke(invocation)).thenReturn(result);
        MDC.put("traceId", "   ");

        filter.invoke(invoker, invocation);

        assertThat(RpcContext.getClientAttachment().getObjectAttachment(RpcContextConstants.TRACE_ID)).isNull();
    }
}
