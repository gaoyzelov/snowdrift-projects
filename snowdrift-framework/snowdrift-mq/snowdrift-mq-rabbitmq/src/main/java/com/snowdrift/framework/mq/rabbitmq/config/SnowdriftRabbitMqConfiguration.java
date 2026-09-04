package com.snowdrift.framework.mq.rabbitmq.config;

import com.snowdrift.framework.mq.IMqService;
import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.MqMessageConverter;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import com.snowdrift.framework.mq.rabbitmq.context.MqRabbitContextAdvice;
import com.snowdrift.framework.mq.rabbitmq.context.MqRabbitListenerContextBinder;
import com.snowdrift.framework.mq.rabbitmq.properties.RabbitMqProperties;
import com.snowdrift.framework.mq.rabbitmq.service.RabbitMqServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executor;

/**
 * Snowdrift RabbitMQ MQ 自动配置 — 基于原生 Spring AMQP
 * <p>
 * 当 {@code snowdrift.mq.rabbitmq.enabled=true} 且 Spring AMQP 在 classpath 时激活，
 * 复用 Boot 自动配置的 {@link RabbitTemplate}，注册 {@link IMqService} 的 RabbitMQ 实现，
 * 并为 {@code @RabbitListener} 容器工厂挂上上下文恢复处理器。连接参数由 {@code spring.rabbitmq.*} 提供。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration")
@EnableConfigurationProperties(RabbitMqProperties.class)
@ConditionalOnProperty(prefix = "snowdrift.mq.rabbitmq", name = "enabled", havingValue = "true")
@ConditionalOnClass(RabbitTemplate.class)
public class SnowdriftRabbitMqConfiguration {

    @Bean
    @ConditionalOnMissingBean(IMqService.class)
    public RabbitMqServiceImpl rabbitMqService(RabbitTemplate rabbitTemplate,
                                               RabbitMqProperties rabbitProperties,
                                               Executor mqAsyncExecutor,
                                               MqMessageConverter converter,
                                               MqInterceptorRegistry interceptorRegistry,
                                               MqContextPropagator contextPropagator) {
        return new RabbitMqServiceImpl(rabbitTemplate, rabbitProperties, mqAsyncExecutor,
                converter, interceptorRegistry, contextPropagator);
    }

    // ========== 消费上下文自动恢复（原生 @RabbitListener 容器级） ==========

    @Bean
    public MqRabbitContextAdvice mqRabbitContextAdvice(MqContextPropagator contextPropagator) {
        return new MqRabbitContextAdvice(contextPropagator);
    }

    @Bean
    public MqRabbitListenerContextBinder mqRabbitListenerContextBinder(MqRabbitContextAdvice mqRabbitContextAdvice) {
        return new MqRabbitListenerContextBinder(mqRabbitContextAdvice);
    }
}
