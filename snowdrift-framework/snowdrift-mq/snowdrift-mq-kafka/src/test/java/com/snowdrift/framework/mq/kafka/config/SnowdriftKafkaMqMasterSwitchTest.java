package com.snowdrift.framework.mq.kafka.config;

import com.snowdrift.framework.mq.IMqService;
import com.snowdrift.framework.mq.config.SnowdriftMqConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnowdriftKafkaMqMasterSwitchTest
 *
 * @author gaoyzelov
 * @date 2026/10/12
 * @description 总闸与厂商开关的交互：snowdrift.mq.enabled=false 必须让厂商配置一并退避，
 * 而不是留下一个依赖已消失公共 Bean 的厂商配置导致启动失败
 * @since 1.0.0
 */
class SnowdriftKafkaMqMasterSwitchTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    SnowdriftMqConfiguration.class, SnowdriftKafkaMqConfiguration.class))
            // 真实应用中由 Boot 的 KafkaAutoConfiguration 提供
            .withBean(KafkaProperties.class, KafkaProperties::new);

    @Test
    @DisplayName("总闸关闭 + 厂商开启：厂商配置随之退避，上下文正常启动而非依赖缺失失败")
    void masterSwitchAlsoDisablesVendorConfiguration() {
        runner.withPropertyValues(
                        "snowdrift.mq.enabled=false",
                        "snowdrift.mq.kafka.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(IMqService.class);
                });
    }

    @Test
    @DisplayName("总闸缺省（视为开启）+ 厂商开启：正常装配 IMqService")
    void vendorConfigurationWorksWhenMasterEnabled() {
        runner.withPropertyValues(
                        "snowdrift.mq.sign-key=unit-test-sign-key",
                        "snowdrift.mq.kafka.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(IMqService.class);
                });
    }
}
