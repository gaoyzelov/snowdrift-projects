package com.snowdrift.framework.mq.rabbitmq.core;

import com.snowdrift.framework.mq.core.DefaultMqTemplate;
import com.snowdrift.framework.mq.core.MqContextPropagator;
import com.snowdrift.framework.mq.core.MqMessageConverter;
import com.snowdrift.framework.mq.core.MqSendInterceptor;
import com.snowdrift.framework.mq.dto.MqMessage;
import com.snowdrift.framework.mq.dto.MqSendResult;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.properties.MqProperties;
import com.snowdrift.framework.mq.rabbitmq.config.RabbitMqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

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
 * </p>
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class RabbitMqTemplate extends DefaultMqTemplate implements ApplicationContextAware {

    private final RabbitMqProperties rabbitProperties;
    private ApplicationContext applicationContext;
    private volatile RabbitTemplate rabbitTemplate;

    public RabbitMqTemplate(StreamBridge streamBridge, MqProperties mqProperties,
                            RabbitMqProperties rabbitProperties,
                            Executor mqAsyncExecutor, MqMessageConverter converter,
                            List<MqSendInterceptor> interceptors) {
        super(streamBridge, mqProperties, mqAsyncExecutor, converter, interceptors);
        this.rabbitProperties = rabbitProperties;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public <T> MqSendResult sendDelay(String topic, String key, T payload, Duration delay, Map<String, String> headers) {
        fireBeforeSend(topic, key, payload);
        try {
            byte[] bytes = converter.serialize(payload);

            MessageBuilder<byte[]> builder = MessageBuilder.withPayload(bytes);

            if (Boolean.TRUE.equals(rabbitProperties.getDelayPluginEnabled())) {
                builder.setHeader("x-delay", delay.toMillis());
                log.debug("RabbitMQ 延迟消息（x-delay 插件）: topic={}, delay={}ms", topic, delay.toMillis());
            } else {
                builder.setHeader("x-message-ttl", delay.toMillis());
                log.debug("RabbitMQ 延迟消息（x-message-ttl + DLX）: topic={}, ttl={}ms", topic, delay.toMillis());
            }

            if (StringUtils.isNotBlank(key)) {
                builder.setHeader(MqContextPropagator.HEADER_MESSAGE_KEY, key);
            }

            MqContextPropagator.inject(builder);
            if (headers != null && !headers.isEmpty()) {
                headers.forEach(builder::setHeader);
            }

            Message<byte[]> message = builder.build();
            boolean success = streamBridge.send(topic, message);
            if (!success) {
                MqException ex = new MqException("mq.send.failed", new Object[]{topic});
                fireOnSendError(topic, ex);
                log.warn("RabbitMQ 延迟消息发送失败: topic={}, delay={}", topic, delay);
                throw ex;
            }

            MqSendResult result = MqSendResult.builder().topic(topic).timestamp(System.currentTimeMillis()).build();
            fireAfterSend(topic, result);
            log.debug("RabbitMQ 延迟消息发送成功: topic={}, delay={}", topic, delay);
            return result;
        } catch (Exception e) {
            if (!(e instanceof MqException)) {
                fireOnSendError(topic, e);
            }
            throw e;
        }
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
        for (MqMessage<T> mqMsg : messages) {
            byte[] body = converter.serialize(mqMsg.getPayload());
            MessageProperties props = new MessageProperties();
            if (StringUtils.isNotBlank(mqMsg.getKey())) {
                props.setHeader(MqContextPropagator.HEADER_MESSAGE_KEY, mqMsg.getKey());
            }
            org.springframework.amqp.core.Message amqpMsg =
                    new org.springframework.amqp.core.Message(body, props);
            try {
                template.send(topic, "", amqpMsg);
                results.add(MqSendResult.builder()
                        .topic(topic)
                        .timestamp(System.currentTimeMillis())
                        .build());
            } catch (Exception e) {
                log.error("RabbitMQ 批量消息发送失败: topic={}, key={}", topic, mqMsg.getKey(), e);
                throw new MqException("mq.send.failed", new Object[]{topic});
            }
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
