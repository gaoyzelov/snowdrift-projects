package com.snowdrift.framework.mq.rabbitmq.service;

import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.mq.AbstractMqService;
import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.MqMessageConverter;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import com.snowdrift.framework.mq.model.MqSendResult;
import com.snowdrift.framework.mq.rabbitmq.properties.RabbitMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * RabbitMQ 消息服务实现 — 基于原生 {@link RabbitTemplate}
 * <p>
 * 统一发送语义映射（交换机模式）：
 * — {@code topic} = AMQP exchange 名；
 * — {@code key} = routing key（缺省为空串，适配 fanout / 已绑定空路由的队列）。
 * 同步发送走原生 {@link RabbitTemplate#send(String, String, Message)}：<b>发出即返回，不含投递确认</b>，
 * 返回的 {@link MqSendResult} 表示"已交给客户端"而非"broker 已接收"；异步复用基类 executor 包装（Rabbit 无原生异步发送）。
 * 延迟消息依赖 rabbitmq-delayed-message-exchange 插件（{@code x-delay} 头，目标 exchange 须为 delayed-exchange）；
 * 未启用插件时 {@link #sendDelay} 直接抛 {@link UnsupportedOperationException}，不做静默 TTL 降级。
 * 消费请使用原生 {@code @RabbitListener}，框架以 adviceChain 拦截器（{@code MqRabbitContextAdvice}）自动恢复/清理上下文。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class RabbitMqServiceImpl extends AbstractMqService {

    private static final String X_DELAY_HEADER = "x-delay";

    private final RabbitMqProperties rabbitProperties;
    private final RabbitTemplate rabbitTemplate;

    public RabbitMqServiceImpl(RabbitTemplate rabbitTemplate,
                               RabbitMqProperties rabbitProperties,
                               Executor mqAsyncExecutor,
                               MqMessageConverter converter,
                               MqInterceptorRegistry interceptorRegistry,
                               MqContextPropagator contextPropagator) {
        super(mqAsyncExecutor, converter, interceptorRegistry, contextPropagator);
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitProperties = rabbitProperties;
    }

    @Override
    protected MqSendResult doNativeSend(String topic, String key, byte[] body, Map<String, String> headers) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        if (headers != null && !headers.isEmpty()) {
            headers.forEach((name, value) -> {
                // x-delay 须为整数毫秒（delayed-message-exchange 插件期望数值类型头），
                // 不能以 String longstr 写入，否则延迟消息可能不延迟/被插件拒绝
                if (X_DELAY_HEADER.equals(name)) {
                    try {
                        properties.setDelayLong(Long.parseLong(value));
                    } catch (NumberFormatException ex) {
                        log.warn("无效的 x-delay 延迟毫秒值，按普通头写入: {}", value);
                        properties.setHeader(name, value);
                    }
                } else {
                    properties.setHeader(name, value);
                }
            });
        }
        // 生成 AMQP message_id 属性：该属性按规范由发布方设置、broker 原样透传，
        // 并非 broker 回传值（RabbitMQ 的同步 send 不返回任何回执）。仅用于链路追踪。
        String messageId = UUID.randomUUID().toString();
        properties.setMessageId(messageId);

        String routingKey = StringUtils.defaultIfBlank(key, StrConst.EMPTY);
        try {
            // 发出即返回：无 publisher-confirm / mandatory 校验，不可路由的消息同样会在此视为发送成功。
            // 需要投递确认请由使用方开启 spring.rabbitmq.publisher-confirm-type=correlated 与
            // spring.rabbitmq.publisher-returns=true；框架不代为设置，以免改变应用内所有发送方的行为。
            rabbitTemplate.send(topic, routingKey, new Message(body, properties));
        } catch (Exception e) {
            throw new MqException("RabbitMQ 消息发送失败: exchange=" + topic + "，原因=" + describe(e), e);
        }
        return MqSendResult.builder()
                .topic(topic)
                .messageId(messageId)
                // 真实投递队列由 exchange 绑定决定、客户端不可知，这里回填客户端实际使用的 routing key
                .partitionOrQueue(routingKey)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 摘要发送异常原因（message 为空时回退到异常类型名）
     */
    private static String describe(Throwable t) {
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }

    @Override
    public <T> MqSendResult sendDelay(String topic, String key, T payload, Duration delay, Map<String, String> headers) {
        if (!Boolean.TRUE.equals(rabbitProperties.getDelayPluginEnabled())) {
            throw new UnsupportedOperationException(
                    "RabbitMQ 延迟消息需要启用 rabbitmq-delayed-message-exchange 插件"
                            + "（snowdrift.mq.rabbitmq.delay-plugin-enabled=true）并将发送目标（topic）指向 delayed-exchange，"
                            + "当前未启用，不支持延迟发送");
        }
        return sendDelayInternal(topic, key, payload, delay, headers,
                effectiveHeaders -> effectiveHeaders.put(X_DELAY_HEADER, String.valueOf(delay.toMillis())));
    }
}
