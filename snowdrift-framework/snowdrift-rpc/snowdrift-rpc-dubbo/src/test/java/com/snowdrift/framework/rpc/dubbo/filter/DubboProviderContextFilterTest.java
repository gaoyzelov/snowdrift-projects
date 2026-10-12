package com.snowdrift.framework.rpc.dubbo.filter;

import com.snowdrift.framework.base.constant.StrConst;
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
 * DubboProviderContextFilterTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 提供端上下文恢复黑盒测试：从 server attachment 恢复、traceId 兜底、降级标记跳过、onResponse/onError 清理
 * @since 1.0.0
 */
class DubboProviderContextFilterTest {

    interface DemoService {
        void call();
    }

    @SuppressWarnings("unchecked")
    private final Invoker<DemoService> invoker = mock(Invoker.class);
    private final Invocation invocation = mock(Invocation.class);
    private final Result result = mock(Result.class);
    private final DubboProviderContextFilter filter = new DubboProviderContextFilter();

    @AfterEach
    void clean() {
        RpcContext.removeServerAttachment();
        RpcContext.removeServiceContext();
        MDC.clear();
        SecurityContextHolder.clear();
        System.clearProperty(DubboProviderContextFilter.TRUST_INBOUND_SECURITY_CONTEXT_PROPERTY);
    }

    @Test
    @DisplayName("默认不信任调用方传来的安全上下文：只恢复 traceId，身份留空")
    void doesNotTrustInboundSecurityContextByDefault() throws RpcException {
        when(invoker.invoke(invocation)).thenReturn(result);
        SecurityContext forged = SecurityContext.builder().userId(999L).tenantId(999L).build();
        RpcContext.getServerAttachment().setObjectAttachment(RpcContextConstants.TRACE_ID, "t-9");
        RpcContext.getServerAttachment().setObjectAttachment(RpcContextConstants.SECURITY_CONTEXT, forged);

        Result returned = filter.invoke(invoker, invocation);

        assertThat(returned).isSameAs(result);
        verify(invoker).invoke(invocation);
        assertThat(MDC.get("traceId")).isEqualTo("t-9");
        assertThat(SecurityContextHolder.peekContext()).isNull();
    }

    @Test
    @DisplayName("显式开启信任开关后才恢复调用方身份（需网络层可信隔离）")
    void restoresInboundSecurityContextOnlyWhenExplicitlyTrusted() throws RpcException {
        System.setProperty(DubboProviderContextFilter.TRUST_INBOUND_SECURITY_CONTEXT_PROPERTY, "true");
        when(invoker.invoke(invocation)).thenReturn(result);
        SecurityContext ctx = SecurityContext.builder().userId(7L).tenantId(8L).build();
        RpcContext.getServerAttachment().setObjectAttachment(RpcContextConstants.TRACE_ID, "t-9");
        RpcContext.getServerAttachment().setObjectAttachment(RpcContextConstants.SECURITY_CONTEXT, ctx);

        filter.invoke(invoker, invocation);

        assertThat(MDC.get("traceId")).isEqualTo("t-9");
        assertThat(SecurityContextHolder.getContext()).isSameAs(ctx);
    }

    @Test
    @DisplayName("上游未传 traceId 时兜底生成非空 traceId；无安全上下文则不设置")
    void generatesTraceIdWhenAbsent() throws RpcException {
        when(invoker.invoke(invocation)).thenReturn(result);

        filter.invoke(invoker, invocation);

        assertThat(MDC.get("traceId")).isNotBlank();
        assertThat(SecurityContextHolder.peekContext()).isNull();
    }

    @Test
    @DisplayName("检测到 CONTEXT_ERROR 降级标记时跳过恢复（traceId 附件被忽略）")
    void skipsRestoreOnContextErrorFlag() throws RpcException {
        when(invoker.invoke(invocation)).thenReturn(result);
        RpcContext.getServerAttachment().setAttachment(RpcContextConstants.CONTEXT_ERROR, StrConst.TRUE);
        RpcContext.getServerAttachment().setObjectAttachment(RpcContextConstants.TRACE_ID, "should-be-ignored");

        filter.invoke(invoker, invocation);

        assertThat(MDC.get("traceId")).isNull();
        assertThat(SecurityContextHolder.peekContext()).isNull();
    }

    @Test
    @DisplayName("onResponse 清理 MDC 与安全上下文，避免线程池复用污染")
    void onResponseClearsContext() {
        MDC.put("traceId", "t");
        SecurityContextHolder.setContext(SecurityContext.builder().userId(1L).build());

        filter.onResponse(result, invoker, invocation);

        assertThat(MDC.get("traceId")).isNull();
        assertThat(SecurityContextHolder.peekContext()).isNull();
    }

    @Test
    @DisplayName("onError 同样清理上下文")
    void onErrorClearsContext() {
        MDC.put("traceId", "t");
        SecurityContextHolder.setContext(SecurityContext.builder().userId(1L).build());

        filter.onError(new RuntimeException("x"), invoker, invocation);

        assertThat(MDC.get("traceId")).isNull();
        assertThat(SecurityContextHolder.peekContext()).isNull();
    }
}
