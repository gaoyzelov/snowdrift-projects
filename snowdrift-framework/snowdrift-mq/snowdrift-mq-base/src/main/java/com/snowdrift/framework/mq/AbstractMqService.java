package com.snowdrift.framework.mq;

import com.snowdrift.framework.context.security.SecurityContext;
import com.snowdrift.framework.context.security.SecurityContextHolder;
import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.MqMessageConverter;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import com.snowdrift.framework.mq.interceptor.MqSendInterceptor;
import com.snowdrift.framework.mq.model.MqMessage;
import com.snowdrift.framework.mq.model.MqSendResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 * 消息发送抽象基类 — broker 无关的发送流水线
 * <p>
 * 统一处理：参数校验、拦截器（before/after/error）、{@link MqMessageConverter} 序列化、
 * {@link MqContextPropagator} 上下文头组装、批量逐条发送与异步（executor 包装）默认实现。
 * 具体发送动作交给子类实现 {@link #doNativeSend}，由各实现模块基于原生客户端
 * （如 spring-kafka / spring-amqp / rocketmq-spring）完成。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractMqService implements IMqService {

    protected final Executor mqAsyncExecutor;
    protected final MqMessageConverter converter;
    protected final MqInterceptorRegistry interceptorRegistry;
    protected final MqContextPropagator contextPropagator;

    protected AbstractMqService(Executor mqAsyncExecutor,
                                MqMessageConverter converter,
                                MqInterceptorRegistry interceptorRegistry,
                                MqContextPropagator contextPropagator) {
        this.mqAsyncExecutor = mqAsyncExecutor;
        this.converter = converter;
        this.contextPropagator = contextPropagator;
        this.interceptorRegistry = interceptorRegistry;
    }

    // ========== 同步发送 ==========
    // 少参数便捷委托已由 IMqService 接口默认方法提供

    @Override
    public <T> MqSendResult send(String topic, String key, T payload, Map<String, String> headers) {
        validateSendArgs(topic, payload);
        return doSend(topic, key, payload, headers);
    }

    /**
     * 执行完整发送流水线：前置拦截器 → 序列化与上下文头组装 → {@link #doNativeSend} → 结果回调
     * <p>供同步与异步路径复用，保证拦截器与上下文传播语义一致。</p>
     */
    protected <T> MqSendResult doSend(String topic, String key, T payload, Map<String, String> headers) {
        // 触发发送前拦截器
        fireBeforeSend(topic, key, payload);

        try {
            byte[] body = converter.serialize(payload);
            Map<String, String> effectiveHeaders = buildHeaders(key, headers);
            MqSendResult result = doNativeSend(topic, key, body, effectiveHeaders);
            // 触发发送后拦截器
            fireAfterSend(topic, result);
            log.debug("消息发送成功: topic={}, key={}, type={}", topic, key, payload.getClass().getName());
            return result;
        } catch (Exception e) {
            // 触发发送异常拦截器
            fireOnSendError(topic, e);
            throw e;
        }
    }

    /**
     * 原生发送动作（由实现模块基于 broker 原生客户端实现）
     *
     * @param topic   目标 topic / destination
     * @param key     消息 Key（可空，broker 分区/路由用）
     * @param body    已序列化的消息体字节（JSON）
     * @param headers 组装完成的消息头（含上下文头与用户自定义头）
     * @return 发送结果（含 broker 元数据 messageId / partition 等）
     */
    protected abstract MqSendResult doNativeSend(String topic, String key, byte[] body, Map<String, String> headers);

    // ========== 异步发送 ==========
    // 少参数便捷委托已由 IMqService 接口默认方法提供

    @Override
    public <T> CompletableFuture<MqSendResult> sendAsync(String topic, String key, T payload, Map<String, String> headers) {
        validateSendArgs(topic, payload);
        // 捕获调用方线程上下文，在任务线程中恢复，保证发送时注入正确的 traceId / SecurityContext
        Map<String, String> callerMdc = MDC.getCopyOfContextMap();
        SecurityContext callerSecurity = captureSecurityContext();
        Executor executor = mqAsyncExecutor != null ? mqAsyncExecutor : ForkJoinPool.commonPool();
        return CompletableFuture.supplyAsync(() -> {
            replayContext(callerMdc, callerSecurity);
            try {
                return doSend(topic, key, payload, headers);
            } finally {
                SecurityContextHolder.clear();
                MDC.clear();
            }
        }, executor);
    }

    // ========== 延迟发送 ==========
    // sendDelay 默认不支持（抛 UnsupportedOperationException），已由 IMqService 接口 default 提供；
    // 具备原生延迟能力的实现（RocketMQ / RabbitMQ）覆写接口方法即可。以下模板供其复用：

    /**
     * 延迟发送统一入口（供具备延迟能力的实现模块复用）。
     * <p>
     * 负责延迟时长校验、headers 副本处理（避免污染调用方 Map）与完整发送流水线；
     * {@code delayHeaderApplier} 由各实现负责把 broker 特有的延迟头写入（副本上）。
     * </p>
     */
    protected <T> MqSendResult sendDelayInternal(String topic, String key, T payload, Duration delay,
                                                 Map<String, String> headers,
                                                 Consumer<Map<String, String>> delayHeaderApplier) {
        validateSendArgs(topic, payload);
        validateDelay(delay);
        Map<String, String> effectiveHeaders = headers == null ? new HashMap<>() : new HashMap<>(headers);
        delayHeaderApplier.accept(effectiveHeaders);
        return doSend(topic, key, payload, effectiveHeaders);
    }

    // ========== 批量发送 ==========

    @Override
    public <T> List<MqSendResult> sendBatch(String topic, List<MqMessage<T>> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        validateTopic(topic);
        List<MqSendResult> batchResults = new ArrayList<>(messages.size());
        List<Exception> errors = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            MqMessage<T> msg = messages.get(i);
            try {
                MqSendResult result = send(topic, msg.getKey(), msg.getPayload(), msg.getHeaders());
                batchResults.add(result);
            } catch (Exception e) {
                log.error("批量发送第 {} 条失败: topic={}", i, topic, e);
                errors.add(e);
            }
        }
        if (CollectionUtils.isNotEmpty(errors)) {
            throw new MqException(String.format("批量发送消息部分失败，总数：%d，成功：%d，失败：%d",
                    messages.size(), messages.size() - errors.size(), errors.size()));
        }
        return batchResults;
    }

    // ========== 消息头组装 ==========

    /**
     * 组装消息头：上下文头（traceId / SecurityContext / 可选签名）→ 消息 Key 头 → 用户自定义头
     * <p>用户自定义头可覆盖上下文头的同名键（与旧实现 MessageBuilder 语义一致）。</p>
     */
    protected Map<String, String> buildHeaders(String key, Map<String, String> userHeaders) {
        Map<String, String> headers = new LinkedHashMap<>();
        contextPropagator.inject(headers);
        if (StringUtils.isNotBlank(key)) {
            headers.put(MqContextPropagator.HEADER_MESSAGE_KEY, key);
        }
        if (userHeaders != null && !userHeaders.isEmpty()) {
            headers.putAll(userHeaders);
        }
        return headers;
    }

    // ========== 参数校验 ==========

    protected void validateTopic(String topic) {
        if (StringUtils.isBlank(topic)) {
            throw new MqException("MQ topic 不能为空");
        }
    }

    protected void validatePayload(Object payload) {
        if (payload == null) {
            throw new MqException("MQ 消息体 payload 不能为空");
        }
    }

    /**
     * 发送参数校验：topic + payload（发送前统一入口）
     */
    protected void validateSendArgs(String topic, Object payload) {
        validateTopic(topic);
        validatePayload(payload);
    }

    protected void validateDelay(Duration delay) {
        if (delay == null) {
            throw new MqException("延迟消息时长不能为空");
        }
        if (delay.isNegative() || delay.isZero()) {
            throw new MqException("延迟消息时长必须大于 0");
        }
    }

    // ========== 异步上下文捕获 / 恢复 ==========

    /**
     * 捕获当前线程安全上下文；无上下文时不抛异常（与 HTTP 线程之外调用异步发送的场景兼容）
     */
    private SecurityContext captureSecurityContext() {
        try {
            return SecurityContextHolder.getContext();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 在异步任务线程中恢复调用方上下文
     */
    private void replayContext(Map<String, String> callerMdc, SecurityContext callerSecurity) {
        if (callerMdc != null) {
            MDC.setContextMap(callerMdc);
        } else {
            MDC.clear();
        }
        if (callerSecurity != null) {
            SecurityContextHolder.setContext(callerSecurity);
        }
    }

    // ========== 拦截器辅助方法 ==========

    protected void fireBeforeSend(String topic, String key, Object payload) {
        for (MqSendInterceptor interceptor : interceptorRegistry.getInterceptors()) {
            try {
                interceptor.beforeSend(topic, key, payload);
            } catch (Exception e) {
                log.warn("拦截器 beforeSend 异常: {}", interceptor.getClass().getName(), e);
            }
        }
    }

    protected void fireAfterSend(String topic, MqSendResult result) {
        for (MqSendInterceptor interceptor : interceptorRegistry.getInterceptors()) {
            try {
                interceptor.afterSend(topic, result);
            } catch (Exception e) {
                log.warn("拦截器 afterSend 异常: {}", interceptor.getClass().getName(), e);
            }
        }
    }

    protected void fireOnSendError(String topic, Throwable ex) {
        for (MqSendInterceptor interceptor : interceptorRegistry.getInterceptors()) {
            try {
                interceptor.onSendError(topic, ex);
            } catch (Exception e) {
                log.warn("拦截器 onSendError 异常: {}", interceptor.getClass().getName(), e);
            }
        }
    }

}
