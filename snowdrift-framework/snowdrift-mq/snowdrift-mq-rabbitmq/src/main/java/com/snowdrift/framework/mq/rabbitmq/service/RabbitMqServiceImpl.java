package com.snowdrift.framework.mq.rabbitmq.service;

import com.snowdrift.framework.base.constant.StrConst;
import com.snowdrift.framework.mq.AbstractMqService;
import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.MqMessageConverter;
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
 * 同步发送走原生 {@link RabbitTemplate#send(String, String, Message)}；异步复用基类 executor 包装（Rabbit 无原生异步发送）。
 * 延迟消息依赖 rabbitmq-delayed-message-exchange 插件（{@code x-delay} 头，目标 exchange 须为 delayed-exchange）；
 * 未启用插件时 {@link #sendDelay} 直接抛 {@link UnsupportedOperationException}，不做静默 TTL 降级。
 * 消费请使用原生 {@code @RabbitListener}，框架以 afterReceive 处理器自动恢复上下文。
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
            headers.forEach(properties::setHeader);
        }
        // RabbitMQ 同步 send 不返回 broker 消息 ID，自生成 messageId 便于链路追踪
        String messageId = UUID.randomUUID().toString();
        properties.setMessageId(messageId);

        String routingKey = StringUtils.defaultIfBlank(key, StrConst.EMPTY);
        rabbitTemplate.send(topic, routingKey, new Message(body, properties));
        return MqSendResult.builder()
                .topic(topic)
                .messageId(messageId)
                .timestamp(System.currentTimeMillis())
                .build();
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
