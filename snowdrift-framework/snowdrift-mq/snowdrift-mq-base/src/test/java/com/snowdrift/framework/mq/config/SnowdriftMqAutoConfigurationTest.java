package com.snowdrift.framework.mq.config;

import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.FastJson2MqMessageConverter;
import com.snowdrift.framework.mq.convert.MqMessageConverter;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnowdriftMqAutoConfigurationTest
 *
 * @author gaoyzelov
 * @date 2026/10/10
 * @description MQ 通用自动配置条件装配黑盒测试（ApplicationContextRunner）：总开关、Bean 覆盖、签名密钥校验与多实现互斥守卫
 * @since 1.0.0
 */
class SnowdriftMqAutoConfigurationTest {

    /** 消息签名默认开启，故除签名相关用例外，统一提供测试密钥 */
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SnowdriftMqConfiguration.class))
            .withPropertyValues("snowdrift.mq.sign-key=unit-test-sign-key");

    @Test
    @DisplayName("默认（缺省视为开启）装配全部基础 Bean")
    void defaultRegistersCoreBeans() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(MqContextPropagator.class);
            assertThat(ctx).hasSingleBean(MqInterceptorRegistry.class);
            assertThat(ctx).hasBean("mqAsyncExecutor");
            assertThat(ctx).hasSingleBean(MqActivationGuard.class);
            assertThat(ctx.getBean(MqMessageConverter.class)).isInstanceOf(FastJson2MqMessageConverter.class);
        });
    }

    @Test
    @DisplayName("总开关 snowdrift.mq.enabled=false 时整个模块不装配")
    void disabledSkipsAllBeans() {
        runner.withPropertyValues("snowdrift.mq.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(MqContextPropagator.class));
    }

    @Test
    @DisplayName("用户自定义 MqMessageConverter 覆盖默认 FastJson2 实现")
    void userConverterOverridesDefault() {
        MqMessageConverter custom = new MqMessageConverter() {
            @Override
            public byte[] serialize(Object payload) {
                return new byte[0];
            }

            @Override
            public <T> T deserialize(byte[] data, Class<T> targetType) {
                return null;
            }
        };

        runner.withBean(MqMessageConverter.class, () -> custom)
                .run(ctx -> {
                    assertThat(ctx.getBean(MqMessageConverter.class)).isSameAs(custom);
                    assertThat(ctx).doesNotHaveBean(FastJson2MqMessageConverter.class);
                });
    }

    @Test
    @DisplayName("默认开启签名：未配置 sign-key 时上下文启动失败，不允许无声地信任未签名身份头")
    void signWithoutKeyFailsContext() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(SnowdriftMqConfiguration.class))
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    @DisplayName("同时启用多个实现时上下文启动失败（互斥守卫生效）")
    void multipleVendorsFailContext() {
        runner.withPropertyValues(
                        "snowdrift.mq.kafka.enabled=true",
                        "snowdrift.mq.rabbitmq.enabled=true")
                .run(ctx -> assertThat(ctx).hasFailed());
    }
}
