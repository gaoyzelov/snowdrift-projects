package com.snowdrift.framework.mq.kafka.config;

import com.snowdrift.framework.mq.IMqService;
import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.MqMessageConverter;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import com.snowdrift.framework.mq.kafka.context.MqKafkaContainerContextBinder;
import com.snowdrift.framework.mq.kafka.context.MqKafkaContextRecordInterceptor;
import com.snowdrift.framework.mq.kafka.properties.KafkaMqProperties;
import com.snowdrift.framework.mq.kafka.service.KafkaMqServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.concurrent.Executor;

/**
 * Snowdrift Kafka MQ 自动配置 — 基于原生 {@code spring-kafka}
 * <p>
 * 当 {@code snowdrift.mq.kafka.enabled=true} 且 {@code spring-kafka} 在 classpath 时激活，
 * 复用 Boot {@code KafkaProperties}（{@code spring.kafka.*}）构建面向 byte[] 的 producer，
 * 注册 {@link IMqService} 的 Kafka 实现，并为 {@code @KafkaListener} 监听容器挂上上下文恢复拦截器。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration")
@EnableConfigurationProperties(KafkaMqProperties.class)
@ConditionalOnProperty(prefix = "snowdrift.mq.kafka", name = "enabled", havingValue = "true")
@ConditionalOnClass(KafkaTemplate.class)
public class SnowdriftKafkaMqConfiguration {

    /**
     * 面向 String-key / byte[]-value 的 producer 工厂（复用 Boot 的 {@code spring.kafka.*} 连接配置）
     */
    @Bean
    @ConditionalOnMissingBean(name = "mqKafkaProducerFactory")
    public ProducerFactory<String, byte[]> mqKafkaProducerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(
                kafkaProperties.buildProducerProperties(),
                new StringSerializer(),
                new ByteArraySerializer());
    }

    @Bean
    @ConditionalOnMissingBean(name = "mqKafkaTemplate")
    public KafkaTemplate<String, byte[]> mqKafkaTemplate(ProducerFactory<String, byte[]> mqKafkaProducerFactory) {
        return new KafkaTemplate<>(mqKafkaProducerFactory);
    }

    @Bean
    @ConditionalOnMissingBean(IMqService.class)
    public KafkaMqServiceImpl kafkaMqService(KafkaTemplate<String, byte[]> mqKafkaTemplate,
                                             Executor mqAsyncExecutor,
                                             MqMessageConverter converter,
                                             MqInterceptorRegistry interceptorRegistry,
                                             MqContextPropagator contextPropagator) {
        return new KafkaMqServiceImpl(mqKafkaTemplate, mqAsyncExecutor, converter, interceptorRegistry, contextPropagator);
    }

    // ========== 消费上下文自动恢复（原生 @KafkaListener 集成） ==========

    @Bean
    public MqKafkaContextRecordInterceptor mqKafkaContextRecordInterceptor(MqContextPropagator contextPropagator) {
        return new MqKafkaContextRecordInterceptor(contextPropagator);
    }

    @Bean
    public MqKafkaContainerContextBinder mqKafkaContainerContextBinder(
            MqKafkaContextRecordInterceptor mqKafkaContextRecordInterceptor) {
        return new MqKafkaContainerContextBinder(mqKafkaContextRecordInterceptor);
    }
}
