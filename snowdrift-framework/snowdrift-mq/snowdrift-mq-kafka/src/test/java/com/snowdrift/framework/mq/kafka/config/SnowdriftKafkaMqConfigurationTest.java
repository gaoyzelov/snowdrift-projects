package com.snowdrift.framework.mq.kafka.config;

import com.snowdrift.framework.mq.IMqService;
import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.FastJson2MqMessageConverter;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import com.snowdrift.framework.mq.kafka.context.MqKafkaContainerContextBinder;
import com.snowdrift.framework.mq.kafka.context.MqKafkaContextBatchInterceptor;
import com.snowdrift.framework.mq.kafka.context.MqKafkaContextRecordInterceptor;
import com.snowdrift.framework.mq.kafka.service.KafkaMqServiceImpl;
import com.snowdrift.framework.mq.properties.MqProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnowdriftKafkaMqConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description Kafka 自动配置装配黑盒测试：producer 工厂/模板、IMqService 实现与消费端上下文 Bean 均可正确创建
 * @since 1.0.0
 */
class SnowdriftKafkaMqConfigurationTest {

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    private final SnowdriftKafkaMqConfiguration configuration = new SnowdriftKafkaMqConfiguration();
    private final MqContextPropagator propagator = new MqContextPropagator(mqProperties());

    @Test
    @DisplayName("构建面向 byte[] 的 producer 工厂与 KafkaTemplate")
    void buildsProducerFactoryAndTemplate() {
        ProducerFactory<String, byte[]> producerFactory = configuration.mqKafkaProducerFactory(new KafkaProperties());

        assertThat(producerFactory).isInstanceOf(DefaultKafkaProducerFactory.class);
        KafkaTemplate<String, byte[]> template = configuration.mqKafkaTemplate(producerFactory);
        assertThat(template).isNotNull();
    }

    @Test
    @DisplayName("注册 IMqService 的 Kafka 实现")
    void registersKafkaMqService() {
        KafkaTemplate<String, byte[]> template =
                configuration.mqKafkaTemplate(configuration.mqKafkaProducerFactory(new KafkaProperties()));

        KafkaMqServiceImpl service = configuration.kafkaMqService(template, DIRECT_EXECUTOR,
                new FastJson2MqMessageConverter(), new MqInterceptorRegistry(), propagator);

        assertThat(service).isInstanceOf(IMqService.class);
    }

    @Test
    @DisplayName("装配消费端上下文拦截器与容器绑定器")
    void wiresConsumerContextBeans() {
        MqKafkaContextRecordInterceptor recordInterceptor = configuration.mqKafkaContextRecordInterceptor(propagator);
        MqKafkaContextBatchInterceptor batchInterceptor = configuration.mqKafkaContextBatchInterceptor(propagator);

        MqKafkaContainerContextBinder binder =
                configuration.mqKafkaContainerContextBinder(recordInterceptor, batchInterceptor);

        assertThat(recordInterceptor).isNotNull();
        assertThat(batchInterceptor).isNotNull();
        assertThat(binder).isInstanceOf(MqKafkaContainerContextBinder.class);
    }

    /** 默认配置已开启消息签名，发送/消费流水线测试提供测试密钥 */
    private static MqProperties mqProperties() {
        MqProperties properties = new MqProperties();
        properties.setSignKey("unit-test-sign-key");
        return properties;
    }
}
