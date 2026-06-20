package com.snowdrift.framework.mq.config;

import com.snowdrift.framework.mq.core.DefaultMqTemplate;
import com.snowdrift.framework.mq.core.IMqTemplate;
import com.snowdrift.framework.mq.core.MqListenerBeanDefinitionRegistrar;
import com.snowdrift.framework.mq.properties.MqProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;

/**
 * Snowdrift MQ 通用自动配置
 * <p>
 * 注册 {@link DefaultMqTemplate} 和 {@link MqListenerBeanDefinitionRegistrar}。
 * 当 {@code snowdrift.mq.enabled=true} 时激活（默认开启）。
 * 各 binder 实现模块的自动配置会在本配置之后运行，覆盖 template 实现。
 * </p>
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(MqProperties.class)
@ConditionalOnProperty(prefix = "snowdrift.mq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SnowdriftMqConfiguration {

    /**
     * 默认消息模板 — 包装 StreamBridge
     * <p>
     * 当 classpath 中存在具体的 binder template bean 时，本 bean 会被覆盖（{@link ConditionalOnMissingBean}）
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(IMqTemplate.class)
    public DefaultMqTemplate mqTemplate(StreamBridge streamBridge, MqProperties properties) {
        log.info("Snowdrift MQ 默认模板已注册（StreamBridge）");
        return new DefaultMqTemplate(streamBridge, properties);
    }

    /**
     * @MqListener 扫描注册器
     */
    @Bean
    public MqListenerBeanDefinitionRegistrar mqListenerBeanDefinitionRegistrar() {
        return new MqListenerBeanDefinitionRegistrar();
    }
}
