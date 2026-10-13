package com.snowdrift.framework.rpc.dubbo.filter;

import com.snowdrift.framework.base.exception.BizException;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.snowdrift.framework.base.exception.BizException;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DubboProviderLogFilterTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 提供端调用日志黑盒测试：成功 DEBUG（含 caller）、BizException INFO、其它异常 ERROR、RpcException ERROR 且原样抛出
 * @since 1.0.0
 */
class DubboProviderLogFilterTest {

    interface DemoService {
        void call();
    }

    @SuppressWarnings("unchecked")
    private final Invoker<DemoService> invoker = mock(Invoker.class);
    private final Invocation invocation = mock(Invocation.class);
    private final Result result = mock(Result.class);
    private final DubboProviderLogFilter filter = new DubboProviderLogFilter();

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUpLog() {
        logger = (Logger) LoggerFactory.getLogger(DubboProviderLogFilter.class);
        logger.setLevel(Level.ALL);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        when(invoker.getInterface()).thenReturn((Class) DemoService.class);
        when(invocation.getMethodName()).thenReturn("call");
    }

    @AfterEach
    void tearDownLog() {
        logger.detachAppender(appender);
        RpcContext.removeServiceContext();
    }

    private boolean hasLog(Level level, String fragment) {
        return appender.list.stream()
                .anyMatch(e -> e.getLevel() == level && e.getFormattedMessage().contains(fragment));
    }

    @Test
    @DisplayName("成功调用：返回结果并记 DEBUG（含 caller 字段）")
    void successLogsDebug() throws RpcException {
        when(invoker.invoke(invocation)).thenReturn(result);
        when(result.hasException()).thenReturn(false);

        Result returned = filter.invoke(invoker, invocation);

        assertThat(returned).isSameAs(result);
        assertThat(hasLog(Level.DEBUG, "caller=")).isTrue();
    }

    @Test
    @DisplayName("结果为 BizException：记 INFO")
    void bizExceptionLogsInfo() throws RpcException {
        when(invoker.invoke(invocation)).thenReturn(result);
        when(result.hasException()).thenReturn(true);
        when(result.getException()).thenReturn(new BizException("库存不足"));

        filter.invoke(invoker, invocation);

        assertThat(hasLog(Level.INFO, "库存不足")).isTrue();
    }

    @Test
    @DisplayName("结果为非 BizException：记 ERROR")
    void otherExceptionLogsError() throws RpcException {
        when(invoker.invoke(invocation)).thenReturn(result);
        when(result.hasException()).thenReturn(true);
        when(result.getException()).thenReturn(new IllegalStateException("boom"));

        filter.invoke(invoker, invocation);

        assertThat(hasLog(Level.ERROR, "call")).isTrue();
    }

    @Test
    @DisplayName("invoker 抛 RpcException：记 ERROR 并包装为 BizException")
    void rpcExceptionLoggedAndRethrown() throws RpcException {
        when(invoker.invoke(invocation)).thenThrow(new RpcException("timeout"));

        assertThatThrownBy(() -> filter.invoke(invoker, invocation))
                .isInstanceOf(BizException.class)
                .hasCauseInstanceOf(RpcException.class)
                .hasRootCauseMessage("timeout");
        assertThat(hasLog(Level.ERROR, "call")).isTrue();
    }
}
