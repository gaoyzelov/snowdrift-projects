package com.snowdrift.framework.mq.rocketmq.config;

import com.snowdrift.framework.mq.IMqService;
import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.FastJson2MqMessageConverter;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import com.snowdrift.framework.mq.properties.MqProperties;
import com.snowdrift.framework.mq.rocketmq.context.MqRocketContainerContextBinder;
import com.snowdrift.framework.mq.rocketmq.properties.RocketMqProperties;
import com.snowdrift.framework.mq.rocketmq.service.RocketMqServiceImpl;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * SnowdriftRocketMqConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description RocketMQ 自动配置装配黑盒测试：IMqService 实现与消费端容器绑定器均可正确创建
 * @since 1.0.0
 */
class SnowdriftRocketMqConfigurationTest {

    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    private final SnowdriftRocketMqConfiguration configuration = new SnowdriftRocketMqConfiguration();
    private final MqContextPropagator propagator = new MqContextPropagator(new MqProperties());

    @Test
    @DisplayName("注册 IMqService 的 RocketMQ 实现")
    void registersRocketMqService() {
        RocketMqServiceImpl service = configuration.rocketMqService(mock(RocketMQTemplate.class),
                new RocketMqProperties(), DIRECT_EXECUTOR, new FastJson2MqMessageConverter(),
                new MqInterceptorRegistry(), propagator);

        assertThat(service).isInstanceOf(IMqService.class);
    }

    @Test
    @DisplayName("装配消费端容器上下文绑定器")
    void wiresConsumerContextBinder() {
        MqRocketContainerContextBinder binder = configuration.mqRocketContainerContextBinder(propagator);

        assertThat(binder).isInstanceOf(MqRocketContainerContextBinder.class);
    }
}
