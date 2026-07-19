package com.snowdrift.framework.mq.rabbitmq.service;

import com.snowdrift.framework.mq.core.DefaultMqServiceImpl;
import com.snowdrift.framework.mq.core.MqContextPropagator;
import com.snowdrift.framework.mq.core.MqInterceptorRegistry;
import com.snowdrift.framework.mq.core.MqMessageConverter;
import com.snowdrift.framework.mq.dto.MqMessage;
import com.snowdrift.framework.mq.dto.MqSendResult;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.properties.MqProperties;
import com.snowdrift.framework.mq.rabbitmq.config.RabbitMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.messaging.Message;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * RabbitMQ 消息发送模板
 * <p>
 * 基于 Spring Cloud Stream Rabbit Binder。
 * 批量发送优先使用 {@link RabbitTemplate}（如可用）。
 * 延迟消息需安装 rabbitmq-delayed-message-exchange 插件，未启用时降级为即时发送。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class RabbitMqServiceImpl extends DefaultMqServiceImpl implements ApplicationContextAware {

    private final RabbitMqProperties rabbitProperties;
    private ApplicationContext applicationContext;
    private volatile RabbitTemplate rabbitTemplate;

    public RabbitMqServiceImpl(StreamBridge streamBridge, MqProperties mqProperties,
                               RabbitMqProperties rabbitProperties,
                               Executor mqAsyncExecutor, MqMessageConverter converter,
                               MqInterceptorRegistry interceptorRegistry,
                               MqContextPropagator contextPropagator) {
        super(streamBridge, mqProperties, mqAsyncExecutor, converter, interceptorRegistry, contextPropagator);
        this.rabbitProperties = rabbitProperties;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public <T> MqSendResult sendDelay(String topic, String key, T payload, Duration delay, Map<String, String> headers) {
        boolean useDelayPlugin = Boolean.TRUE.equals(rabbitProperties.getDelayPluginEnabled());
        return doSendDelay(topic, key, payload, delay, headers, builder -> {
            if (useDelayPlugin) {
                builder.setHeader("x-delay", delay.toMillis());
            } else {
                // 降级方案：设置消息过期时间 + 死信队列，需用提前配置
                log.warn("RabbitMQ 延迟消息使用 x-message-ttl 降级方案，请确保对应队列已配置 DLX（死信队列），"
                        + "否则到期消息将被直接丢弃。建议开启 delay-plugin-enabled 使用 x-delay 插件。");
                builder.setHeader("x-message-ttl", delay.toMillis());
            }
        });
    }

    // ========== 批量发送（RabbitTemplate） ==========

    @Override
    public <T> List<MqSendResult> sendBatch(String topic, List<MqMessage<T>> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        RabbitTemplate template = getRabbitTemplate();
        if (template != null) {
            return sendBatchWithRabbitTemplate(topic, messages, template);
        }

        log.debug("RabbitTemplate 不可用，使用 StreamBridge 循环批量发送");
        return super.sendBatch(topic, messages);
    }

    private <T> List<MqSendResult> sendBatchWithRabbitTemplate(String topic,
                                                                List<MqMessage<T>> messages,
                                                                RabbitTemplate template) {
        List<MqSendResult> results = new ArrayList<>(messages.size());
        List<Exception> errors = new ArrayList<>();

        for (int i = 0; i < messages.size(); i++) {
            MqMessage<T> mqMsg = messages.get(i);
            fireBeforeSend(topic, mqMsg.getKey(), mqMsg.getPayload());

            byte[] body = converter.serialize(mqMsg.getPayload());
            MessageProperties props = new MessageProperties();

            // 使用 buildMessageFromBytes 复用已序列化的 body，避免 buildMessage 内部二次序列化
            Message<byte[]> springMsg = buildMessageFromBytes(mqMsg.getKey(),
                    body, mqMsg.getHeaders());
            springMsg.getHeaders().forEach(props::setHeader);

            org.springframework.amqp.core.Message amqpMsg =
                    new org.springframework.amqp.core.Message(body, props);
            try {
                template.send(topic, "", amqpMsg);
                MqSendResult result = MqSendResult.builder()
                        .topic(topic)
                        .timestamp(System.currentTimeMillis())
                        .build();
                results.add(result);
                fireAfterSend(topic, result);
            } catch (Exception e) {
                log.error("RabbitMQ 批量发送第 {} 条失败: topic={}, key={}", i, topic, mqMsg.getKey(), e);
                fireOnSendError(topic, e);
                errors.add(e);
                results.add(null); // 占位，保持索引对齐
            }
        }

        if (!errors.isEmpty()) {
            long successCount = results.stream().filter(java.util.Objects::nonNull).count();
            throw new MqException("mq.send.batch.partial-failed",
                    new Object[]{topic, successCount, messages.size()});
        }
        log.debug("RabbitMQ 批量发送完成: topic={}, count={}", topic, messages.size());
        return results;
    }

    private RabbitTemplate getRabbitTemplate() {
        if (this.rabbitTemplate == null) {
            synchronized (this) {
                if (this.rabbitTemplate == null) {
                    try {
                        this.rabbitTemplate = applicationContext.getBean(RabbitTemplate.class);
                    } catch (Exception e) {
                        log.debug("RabbitTemplate Bean 不存在，批量发送将使用 StreamBridge 循环");
                    }
                }
            }
        }
        return this.rabbitTemplate;
    }
}
