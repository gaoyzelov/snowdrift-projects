package com.snowdrift.framework.mq;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.FastJson2MqMessageConverter;
import com.snowdrift.framework.mq.convert.MqMessageConverter;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import com.snowdrift.framework.mq.interceptor.MqSendInterceptor;
import com.snowdrift.framework.mq.model.MqMessage;
import com.snowdrift.framework.mq.model.MqSendResult;
import com.snowdrift.framework.mq.properties.MqProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AbstractMqServiceTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description 发送流水线抽象基类单元测试：参数校验、拦截器时序、头组装、异步与批量语义
 * @since 1.0.0
 */
class AbstractMqServiceTest {

    /** 同步直执行器，使异步路径在测试中确定性地跑在当前线程 */
    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    private MqInterceptorRegistry registry;
    private MqContextPropagator propagator;
    private MqMessageConverter converter;
    private RecordingInterceptor recorder;

    @BeforeEach
    void setUp() {
        registry = new MqInterceptorRegistry();
        propagator = new MqContextPropagator(mqProperties());
        converter = new FastJson2MqMessageConverter();
        recorder = new RecordingInterceptor();
        registry.register(recorder);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
        MDC.clear();
    }

    private StubMqService newService() {
        return new StubMqService(DIRECT_EXECUTOR, converter, registry, propagator);
    }

    /** 记录回调事件的拦截器 */
    static class RecordingInterceptor implements MqSendInterceptor {
        final List<String> events = new ArrayList<>();

        @Override
        public void beforeSend(String topic, String key, Object payload) {
            events.add("before:" + topic);
        }

        @Override
        public void afterSend(String topic, MqSendResult result) {
            events.add("after:" + topic);
        }

        @Override
        public void onSendError(String topic, Throwable ex) {
            events.add("error:" + topic);
        }
    }

    /** 桩实现：记录原生发送调用，可注入失败 */
    static class StubMqService extends AbstractMqService {
        final List<String> topics = new ArrayList<>();
        RuntimeException failure;
        Map<String, String> lastHeaders;

        StubMqService(Executor executor, MqMessageConverter converter,
                      MqInterceptorRegistry registry, MqContextPropagator propagator) {
            super(executor, converter, registry, propagator);
        }

        @Override
        protected MqSendResult doNativeSend(String topic, String key, byte[] body, Map<String, String> headers) {
            topics.add(topic);
            lastHeaders = headers;
            if (failure != null) {
                throw failure;
            }
            return MqSendResult.builder()
                    .topic(topic).messageId("id-" + topics.size()).timestamp(1L).build();
        }
    }

    /** 支持延迟的桩实现，复用基类 sendDelayInternal 模板 */
    static class DelayStubMqService extends StubMqService {
        DelayStubMqService(Executor executor, MqMessageConverter converter,
                           MqInterceptorRegistry registry, MqContextPropagator propagator) {
            super(executor, converter, registry, propagator);
        }

        @Override
        public <T> MqSendResult sendDelay(String topic, String key, T payload, Duration delay, Map<String, String> headers) {
            return sendDelayInternal(topic, key, payload, delay, headers,
                    h -> h.put("x-delay", String.valueOf(delay.toMillis())));
        }
    }

    // ========== 参数校验 ==========

    @Test
    @DisplayName("topic 为空白时抛 MqException，不进入原生发送")
    void blankTopicRejected() {
        StubMqService service = newService();

        assertThatThrownBy(() -> service.send("  ", "payload"))
                .isInstanceOf(MqException.class)
                .hasMessageContaining("topic");
        assertThat(service.topics).isEmpty();
    }

    @Test
    @DisplayName("payload 为 null 时抛 MqException")
    void nullPayloadRejected() {
        StubMqService service = newService();

        assertThatThrownBy(() -> service.send("topic", null))
                .isInstanceOf(MqException.class)
                .hasMessageContaining("payload");
    }

    // ========== 同步发送 ==========

    @Test
    @DisplayName("同步发送成功：拦截器 before→after 有序触发并返回结果")
    void sendSuccessFiresInterceptorsInOrder() {
        StubMqService service = newService();

        MqSendResult result = service.send("order-paid", "k1", "hello");

        assertThat(result.getTopic()).isEqualTo("order-paid");
        assertThat(result.getMessageId()).isEqualTo("id-1");
        assertThat(recorder.events).containsExactly("before:order-paid", "after:order-paid");
    }

    @Test
    @DisplayName("原生发送抛异常：触发 onSendError 并统一包装")
    void sendFailureFiresOnError() {
        StubMqService service = newService();
        service.failure = new IllegalStateException("broker down");

        assertThatThrownBy(() -> service.send("order-paid", "k1", "hello"))
                .isInstanceOf(BizException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThat(recorder.events).containsExactly("before:order-paid", "error:order-paid");
    }

    @Test
    @DisplayName("头组装：写入消息 Key 头，用户自定义头可覆盖同名键")
    void buildHeadersIncludesKeyAndUserHeaders() {
        StubMqService service = newService();
        Map<String, String> userHeaders = new HashMap<>();
        userHeaders.put("biz", "pay");

        service.send("topic", "key-1", "hello", userHeaders);

        assertThat(service.lastHeaders)
                .containsEntry(MqContextPropagator.HEADER_MESSAGE_KEY, "key-1")
                .containsEntry("biz", "pay");
    }

    @Test
    @DisplayName("注入 traceId 后，发送头携带 trace-id")
    void injectsTraceIdHeader() {
        MDC.put(MqContextPropagator.TRACE_ID_KEY, "trace-99");
        StubMqService service = newService();

        service.send("topic", "hello");

        assertThat(service.lastHeaders).containsEntry(MqContextPropagator.HEADER_TRACE_ID, "trace-99");
    }

    @Test
    @DisplayName("安全：用户自定义头不能覆盖框架保留的 x-snowdrift-* 身份/签名头")
    void userHeadersCannotOverrideReservedContextHeaders() {
        SecurityContextHolder.setContext(com.snowdrift.framework.context.security.SecurityContext.builder()
                .userId(1L).username("real").build());
        StubMqService service = newService();
        Map<String, String> userHeaders = new HashMap<>();
        userHeaders.put(MqContextPropagator.HEADER_USER_ID, "999");
        userHeaders.put(MqContextPropagator.HEADER_USERNAME, "spoofed");
        userHeaders.put("x-snowdrift-signature", "forged");
        userHeaders.put("biz", "pay");

        service.send("topic", "key-1", "hello", userHeaders);

        // 保留头由框架注入的真实上下文决定，未被伪造值覆盖；签名头由框架按真实上下文重新计算
        assertThat(service.lastHeaders)
                .containsEntry(MqContextPropagator.HEADER_USER_ID, "1")
                .containsEntry(MqContextPropagator.HEADER_USERNAME, "real");
        assertThat(service.lastHeaders.get("x-snowdrift-signature"))
                .isNotBlank()
                .isNotEqualTo("forged");
        // 普通业务头正常写入
        assertThat(service.lastHeaders).containsEntry("biz", "pay");
    }

    @Test
    @DisplayName("安全：即使无上下文，用户也不能自行注入 x-snowdrift-* 头")
    void userHeadersCannotInjectReservedHeadersWithoutContext() {
        StubMqService service = newService();
        Map<String, String> userHeaders = new HashMap<>();
        userHeaders.put(MqContextPropagator.HEADER_TENANT_ID, "666");

        service.send("topic", "hello", userHeaders);

        assertThat(service.lastHeaders).doesNotContainKey(MqContextPropagator.HEADER_TENANT_ID);
    }

    // ========== 异步发送 ==========

    @Test
    @DisplayName("异步发送成功：future 完成并携带结果，拦截器有序触发")
    void sendAsyncSuccess() throws ExecutionException, InterruptedException {
        StubMqService service = newService();

        CompletableFuture<MqSendResult> future = service.sendAsync("topic", "k", "hello");

        assertThat(future.get().getTopic()).isEqualTo("topic");
        assertThat(recorder.events).containsExactly("before:topic", "after:topic");
    }

    @Test
    @DisplayName("异步发送参数非法：在提交任务前即同步抛出")
    void sendAsyncValidatesArgs() {
        StubMqService service = newService();

        assertThatThrownBy(() -> service.sendAsync("", "hello"))
                .isInstanceOf(MqException.class);
    }

    @Test
    @DisplayName("异步发送原生失败：future 以异常完成并触发 onSendError")
    void sendAsyncFailure() {
        StubMqService service = newService();
        service.failure = new IllegalStateException("boom");

        CompletableFuture<MqSendResult> future = service.sendAsync("topic", "k", "hello");

        assertThat(future).isCompletedExceptionally();
        assertThat(recorder.events).contains("error:topic");
    }

    @Test
    @DisplayName("异步发送：调用方 MDC/安全上下文跨线程传播到工作线程并注入发送头")
    void sendAsyncPropagatesCallerContextAcrossThreads() throws Exception {
        MDC.put(MqContextPropagator.TRACE_ID_KEY, "trace-caller");
        SecurityContextHolder.setContext(com.snowdrift.framework.context.security.SecurityContext.builder()
                .userId(77L).build());
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            StubMqService service = new StubMqService(pool, converter, registry, propagator);

            service.sendAsync("topic", "k", "hello").get();

            // 工作线程本无上下文，头中的值必来自捕获后回放的调用方上下文
            assertThat(service.lastHeaders)
                    .containsEntry(MqContextPropagator.HEADER_TRACE_ID, "trace-caller")
                    .containsEntry(MqContextPropagator.HEADER_USER_ID, "77");
        } finally {
            pool.shutdownNow();
        }
    }

    // ========== 延迟发送 ==========

    @Test
    @DisplayName("默认不支持延迟：未覆写时抛 MqException")
    void sendDelayUnsupportedByDefault() {
        StubMqService service = newService();

        assertThatThrownBy(() -> service.sendDelay("topic", "hello", Duration.ofSeconds(1)))
                .isInstanceOf(MqException.class);
    }

    @Test
    @DisplayName("延迟模板：写入延迟头且不污染调用方 headers")
    void sendDelayInternalAppliesDelayHeaderOnCopy() {
        DelayStubMqService service = new DelayStubMqService(DIRECT_EXECUTOR, converter, registry, propagator);
        Map<String, String> userHeaders = new HashMap<>();
        userHeaders.put("biz", "x");

        service.sendDelay("topic", "k", "hello", Duration.ofSeconds(5), userHeaders);

        assertThat(service.lastHeaders).containsEntry("x-delay", "5000");
        // 调用方原始 map 不被写入延迟头
        assertThat(userHeaders).doesNotContainKey("x-delay");
    }

    @Test
    @DisplayName("延迟时长非法（null/0/负）时抛 MqException")
    void sendDelayValidatesDuration() {
        DelayStubMqService service = new DelayStubMqService(DIRECT_EXECUTOR, converter, registry, propagator);

        assertThatThrownBy(() -> service.sendDelay("topic", "hello", null))
                .isInstanceOf(MqException.class);
        assertThatThrownBy(() -> service.sendDelay("topic", "hello", Duration.ZERO))
                .isInstanceOf(MqException.class);
        assertThatThrownBy(() -> service.sendDelay("topic", "hello", Duration.ofSeconds(-1)))
                .isInstanceOf(MqException.class);
    }

    // ========== 批量发送 ==========

    @Test
    @DisplayName("批量：null 或空列表返回空结果，不触发发送")
    void sendBatchEmpty() {
        StubMqService service = newService();

        assertThat(service.sendBatch("topic", null)).isEmpty();
        assertThat(service.sendBatch("topic", List.of())).isEmpty();
        assertThat(service.topics).isEmpty();
    }

    @Test
    @DisplayName("批量：全部成功按输入顺序返回结果")
    void sendBatchAllSuccess() {
        StubMqService service = newService();
        List<MqMessage<String>> messages = List.of(
                MqMessage.<String>builder().payload("a").key("1").build(),
                MqMessage.<String>builder().payload("b").key("2").build());

        List<MqSendResult> results = service.sendBatch("topic", messages);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(MqSendResult::getMessageId).containsExactly("id-1", "id-2");
    }

    @Test
    @DisplayName("批量：topic 空白时抛 MqException")
    void sendBatchValidatesTopic() {
        StubMqService service = newService();
        List<MqMessage<String>> messages = List.of(MqMessage.<String>builder().payload("a").build());

        assertThatThrownBy(() -> service.sendBatch("  ", messages))
                .isInstanceOf(MqException.class);
    }

    @Test
    @DisplayName("批量：部分失败时汇总抛 MqException，消息含成功/失败计数")
    void sendBatchPartialFailure() {
        // 第 2 条 payload 为 null，触发校验失败
        StubMqService service = newService();
        List<MqMessage<String>> messages = new ArrayList<>();
        messages.add(MqMessage.<String>builder().payload("a").build());
        messages.add(MqMessage.<String>builder().payload(null).build());
        messages.add(MqMessage.<String>builder().payload("c").build());

        assertThatThrownBy(() -> service.sendBatch("topic", messages))
                .isInstanceOf(MqException.class)
                .hasMessageContaining("总数：3")
                .hasMessageContaining("成功：2")
                .hasMessageContaining("失败：1");
    }

    @Test
    @DisplayName("批量：列表含 null 元素时计入失败并汇总抛 MqException")
    void sendBatchCountsNullElementAsError() {
        StubMqService service = newService();
        List<MqMessage<String>> messages = new ArrayList<>();
        messages.add(MqMessage.<String>builder().payload("a").build());
        messages.add(null);

        assertThatThrownBy(() -> service.sendBatch("topic", messages))
                .isInstanceOf(MqException.class)
                .hasMessageContaining("总数：2")
                .hasMessageContaining("成功：1")
                .hasMessageContaining("失败：1");
    }

    /** 默认配置已开启消息签名，发送/消费流水线测试提供测试密钥 */
    private static MqProperties mqProperties() {
        MqProperties properties = new MqProperties();
        properties.setSignKey("unit-test-sign-key");
        return properties;
    }
}
