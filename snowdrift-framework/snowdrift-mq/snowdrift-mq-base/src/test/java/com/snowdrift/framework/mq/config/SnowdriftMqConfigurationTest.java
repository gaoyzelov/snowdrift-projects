package com.snowdrift.framework.mq.config;

import com.snowdrift.framework.base.exception.BizException;
import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.FastJson2MqMessageConverter;
import com.snowdrift.framework.mq.convert.MqMessageConverter;
import com.snowdrift.framework.mq.exception.MqException;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import com.snowdrift.framework.mq.interceptor.MqSendInterceptor;
import com.snowdrift.framework.mq.properties.MqProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SnowdriftMqConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description MQ 通用自动配置工厂方法黑盒测试：线程池装配与校验、拒绝策略、默认转换器、拦截器收集与传播器/守卫
 * @since 1.0.0
 */
class SnowdriftMqConfigurationTest {

    private final SnowdriftMqConfiguration configuration = new SnowdriftMqConfiguration();

    @Test
    @DisplayName("默认配置构建线程池：核心/最大/队列容量取自属性")
    void buildsExecutorFromProperties() {
        MqProperties properties = new MqProperties();

        Executor executor = configuration.mqAsyncExecutor(properties);

        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executor;
        try {
            assertThat(pool.getCorePoolSize()).isEqualTo(4);
            assertThat(pool.getMaxPoolSize()).isEqualTo(8);
        } finally {
            pool.shutdown();
        }
    }

    @Test
    @DisplayName("核心线程数大于最大线程数时启动失败")
    void rejectsCoreGreaterThanMax() {
        MqProperties properties = new MqProperties();
        properties.getExecutor().setCoreSize(10);
        properties.getExecutor().setMaxSize(5);

        assertThatThrownBy(() -> configuration.mqAsyncExecutor(properties))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("核心线程数不能大于最大线程数");
    }

    @Test
    @DisplayName("线程池饱和时拒绝策略抛 MqException（而非回退到调用线程执行）")
    void rejectionHandlerThrowsMqException() {
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) configuration.mqAsyncExecutor(new MqProperties());
        try {
            ThreadPoolExecutor underlying = pool.getThreadPoolExecutor();
            RejectedExecutionHandler handler = underlying.getRejectedExecutionHandler();

            assertThatThrownBy(() -> handler.rejectedExecution(() -> {
            }, underlying)).isInstanceOf(MqException.class);
        } finally {
            pool.shutdown();
        }
    }

    @Test
    @DisplayName("默认消息转换器为 FastJson2 实现")
    void defaultConverterIsFastJson2() {
        MqMessageConverter converter = configuration.mqMessageConverter();

        assertThat(converter).isInstanceOf(FastJson2MqMessageConverter.class);
    }

    @Test
    @DisplayName("拦截器注册表收集传入的全部拦截器")
    void registryCollectsInterceptors() {
        MqInterceptorRegistry registry = configuration.mqInterceptorRegistry(
                List.of(new MqSendInterceptor() {
                }, new MqSendInterceptor() {
                }));

        assertThat(registry.getInterceptors()).hasSize(2);
    }

    @Test
    @DisplayName("上下文传播器：默认（不签名）正常创建；开启签名却缺密钥则失败")
    void contextPropagatorSignKeyValidation() {
        assertThat(configuration.mqContextPropagator(new MqProperties()))
                .isInstanceOf(MqContextPropagator.class);

        MqProperties signNoKey = new MqProperties();
        signNoKey.setSign(true);
        assertThatThrownBy(() -> configuration.mqContextPropagator(signNoKey))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("互斥守卫：单个启用时正常创建")
    void binderGuardCreated() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("snowdrift.mq.kafka.enabled", "true");

        assertThat(configuration.mqBinderActivationGuard(env))
                .isInstanceOf(MqBinderActivationGuard.class);
    }
}
