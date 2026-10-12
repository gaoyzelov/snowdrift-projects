package com.snowdrift.framework.rpc.dubbo.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.snowdrift.framework.base.exception.BizException;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.service.GenericService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DubboExceptionFilterTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 提供端异常处理黑盒测试：各类异常的直传判定、未知异常包装为 BizException、泛化调用跳过与 onError 日志
 * @since 1.0.0
 */
class DubboExceptionFilterTest {

    interface DemoService {
        void plain();

        void declared() throws IOException;
    }

    /** 非 JDK、非 Rpc/Biz 的未知运行时异常（位于 test-classes，与 DemoService 同 codebase） */
    static class UnknownException extends RuntimeException {
        UnknownException(String message) {
            super(message);
        }
    }

    @SuppressWarnings("unchecked")
    private final Invoker<DemoService> invoker = mock(Invoker.class);
    private final Invocation invocation = mock(Invocation.class);
    private final Result appResponse = mock(Result.class);
    private final DubboExceptionFilter filter = new DubboExceptionFilter();

    @AfterEach
    void clean() {
        RpcContext.removeServiceContext();
        RpcContext.removeServerAttachment();
    }

    private void stub(Class<?> iface, String method, Class<?>[] paramTypes, Throwable exception) {
        when(invoker.getInterface()).thenReturn((Class) iface);
        when(invocation.getMethodName()).thenReturn(method);
        when(invocation.getParameterTypes()).thenReturn(paramTypes);
        when(appResponse.hasException()).thenReturn(true);
        when(appResponse.getException()).thenReturn(exception);
    }

    @Test
    @DisplayName("无异常时不做任何处理")
    void noExceptionNoOp() {
        when(appResponse.hasException()).thenReturn(false);

        filter.onResponse(appResponse, invoker, invocation);

        verify(appResponse, never()).setException(any());
    }

    @Test
    @DisplayName("泛化调用（GenericService）跳过处理")
    @SuppressWarnings("unchecked")
    void genericServiceSkipped() {
        Invoker<GenericService> genericInvoker = mock(Invoker.class);
        when(genericInvoker.getInterface()).thenReturn((Class) GenericService.class);
        when(appResponse.hasException()).thenReturn(true);
        when(appResponse.getException()).thenReturn(new UnknownException("x"));

        filter.onResponse(appResponse, genericInvoker, invocation);

        verify(appResponse, never()).setException(any());
    }

    @Test
    @DisplayName("受检异常（非 RuntimeException）直接传播，不包装")
    void checkedExceptionPropagates() {
        stub(DemoService.class, "plain", new Class[0], new IOException("io"));

        filter.onResponse(appResponse, invoker, invocation);

        verify(appResponse, never()).setException(any());
    }

    @Test
    @DisplayName("方法签名声明的异常直接传播，不包装")
    void declaredExceptionPropagates() {
        stub(DemoService.class, "declared", new Class[0], new IOException("declared-io"));

        filter.onResponse(appResponse, invoker, invocation);

        verify(appResponse, never()).setException(any());
    }

    @Test
    @DisplayName("方法不存在（NoSuchMethod）时直接返回，不包装")
    void noSuchMethodReturns() {
        stub(DemoService.class, "methodThatDoesNotExist", new Class[0], new UnknownException("x"));

        filter.onResponse(appResponse, invoker, invocation);

        verify(appResponse, never()).setException(any());
    }

    @Test
    @DisplayName("JDK 异常（codebase 为 null）直接传播，不包装")
    void jdkExceptionPropagates() {
        stub(DemoService.class, "plain", new Class[0], new IllegalStateException("jdk"));

        filter.onResponse(appResponse, invoker, invocation);

        verify(appResponse, never()).setException(any());
    }

    @Test
    @DisplayName("RpcException 直接传播，不包装")
    void rpcExceptionPropagates() {
        stub(DemoService.class, "plain", new Class[0], new RpcException("rpc"));

        filter.onResponse(appResponse, invoker, invocation);

        verify(appResponse, never()).setException(any());
    }

    @Test
    @DisplayName("BizException 直接传播，不包装")
    void bizExceptionPropagates() {
        stub(DemoService.class, "plain", new Class[0], new BizException("biz"));

        filter.onResponse(appResponse, invoker, invocation);

        verify(appResponse, never()).setException(any());
    }

    @Test
    @DisplayName("异常类与接口同 codebase（如同 jar）时直接传播，不包装")
    void sameCodebasePropagates() {
        // DemoService 与 UnknownException 同在 test-classes，getCodeBase 相等 → 直接传播
        stub(DemoService.class, "plain", new Class[0], new UnknownException("same-codebase"));

        filter.onResponse(appResponse, invoker, invocation);

        verify(appResponse, never()).setException(any());
    }

    @Test
    @DisplayName("未知运行时异常且跨 codebase：包装为 BizException，防止序列化失败")
    void unknownCrossCodebaseExceptionIsWrapped() {
        stub(DemoService.class, "plain", new Class[0], new UnknownException("unknown"));

        try (MockedStatic<ReflectUtils> mocked = mockStatic(ReflectUtils.class)) {
            mocked.when(() -> ReflectUtils.getCodeBase(DemoService.class)).thenReturn("codebase-A");
            mocked.when(() -> ReflectUtils.getCodeBase(UnknownException.class)).thenReturn("codebase-B");

            filter.onResponse(appResponse, invoker, invocation);
        }

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(appResponse).setException(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(BizException.class).hasMessage("RPC 调用异常");
    }

    @Test
    @DisplayName("被内置 ExceptionFilter 改写的异常（java.lang.RuntimeException + 含堆栈的消息）仍被替换为无细节 BizException")
    void rewritesExceptionAlreadyWrappedByBuiltInFilter() {
        // Dubbo 内置 ExceptionFilter 会把「未在签名声明且跨 codebase」的异常改写为
        // new RuntimeException(StringUtils.toString(原异常))，消息里带着原异常类名与完整堆栈。
        // 本过滤器必须把它重新剥离成无细节的 BizException，否则内部包名/文件行号会随响应泄露给调用方。
        String leaked = "org.springframework.jdbc.BadSqlGrammarException: ### Error querying database\n"
                + "\tat com.acme.internal.SecretRepository.query(SecretRepository.java:42)\n"
                + "\tat com.acme.order.OrderServiceImpl.list(OrderServiceImpl.java:88)";
        stub(DemoService.class, "plain", new Class[0], new RuntimeException(leaked));

        filter.onResponse(appResponse, invoker, invocation);

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(appResponse).setException(captor.capture());
        assertThat(captor.getValue())
                .isInstanceOf(BizException.class)
                .hasMessage("RPC 调用异常")
                .hasMessageNotContaining("com.acme.internal");
    }

    @Test
    @DisplayName("业务自己抛出的普通 java.lang.RuntimeException（消息不含堆栈）原样传播，不误伤")
    void plainRuntimeExceptionWithoutStackTraceMarkerPropagates() {
        stub(DemoService.class, "plain", new Class[0], new RuntimeException("订单已存在"));

        filter.onResponse(appResponse, invoker, invocation);

        verify(appResponse, never()).setException(any());
    }

    @Test
    @DisplayName("顺序不变量：本过滤器必须比内置 ExceptionFilter 更靠外层（order 更小），否则看不到它改写后的异常")
    void activatedOutsideBuiltInExceptionFilter() {
        int builtIn = org.apache.dubbo.rpc.filter.ExceptionFilter.class
                .getAnnotation(org.apache.dubbo.common.extension.Activate.class).order();
        int ours = DubboExceptionFilter.class
                .getAnnotation(org.apache.dubbo.common.extension.Activate.class).order();

        // Dubbo 中 order 越小越靠外层；响应监听器按「内层先执行」回调，
        // 因此 order 更小的本过滤器会在内置 ExceptionFilter 之后收到 onResponse，
        // 才有机会把它改写出来的 java.lang.RuntimeException 再剥离成无细节的 BizException。
        // 若日后把本过滤器调到内置之后（order 更大），将永远看不到被改写的异常，本条与上一条用例都会失败。
        assertThat(ours).isLessThan(builtIn);
    }

    @Test
    @DisplayName("onError 记录 ERROR 日志（含接口与方法名）")
    void onErrorLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(DubboExceptionFilter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        when(invoker.getInterface()).thenReturn((Class) DemoService.class);
        when(invocation.getMethodName()).thenReturn("plain");
        try {
            filter.onError(new RpcException("boom"), invoker, invocation);

            assertThat(appender.list).anyMatch(e -> e.getLevel() == Level.ERROR
                    && e.getFormattedMessage().contains("plain"));
        } finally {
            logger.detachAppender(appender);
        }
    }
}
