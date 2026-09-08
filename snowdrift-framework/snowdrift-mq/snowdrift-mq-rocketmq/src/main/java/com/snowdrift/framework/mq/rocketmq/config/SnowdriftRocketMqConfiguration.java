package com.snowdrift.framework.mq.rocketmq.config;

import com.snowdrift.framework.mq.IMqService;
import com.snowdrift.framework.mq.context.MqContextPropagator;
import com.snowdrift.framework.mq.convert.MqMessageConverter;
import com.snowdrift.framework.mq.interceptor.MqInterceptorRegistry;
import com.snowdrift.framework.mq.rocketmq.context.MqRocketContainerContextBinder;
import com.snowdrift.framework.mq.rocketmq.properties.RocketMqProperties;
import com.snowdrift.framework.mq.rocketmq.service.RocketMqServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executor;

/**
 * Snowdrift RocketMQ MQ 自动配置 — 基于原生 rocketmq-spring-boot-starter
 * <p>
 * 当 {@code snowdrift.mq.rocketmq.enabled=true} 且 {@link RocketMQTemplate} 在 classpath 时激活，
 * 复用 starter 自动配置的 {@link RocketMQTemplate}（nameServer 等由 {@code rocketmq.name-server} 提供），
 * 注册 {@link IMqService} 的 RocketMQ 实现。
 * </p>
 * <p><b>兼容性说明</b>：rocketmq-spring-boot-starter 2.3.5 面向 Boot2.7/Spring5.3 构建，
 * 在 Boot3.5 下可编译与自动发现（有 AutoConfiguration.imports、无 jakarta/javax 硬引用），
 * 但运行时兼容需真实 broker 联调验证。</p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration(afterName = "org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration")
@EnableConfigurationProperties(RocketMqProperties.class)
@ConditionalOnProperty(prefix = "snowdrift.mq.rocketmq", name = "enabled", havingValue = "true")
@ConditionalOnClass(RocketMQTemplate.class)
public class SnowdriftRocketMqConfiguration {

    @Bean
    @ConditionalOnMissingBean(IMqService.class)
    public RocketMqServiceImpl rocketMqService(RocketMQTemplate rocketMQTemplate,
                                               RocketMqProperties rocketMqProperties,
                                               Executor mqAsyncExecutor,
                                               MqMessageConverter converter,
                                               MqInterceptorRegistry interceptorRegistry,
                                               MqContextPropagator contextPropagator) {
        return new RocketMqServiceImpl(rocketMQTemplate, rocketMqProperties, mqAsyncExecutor,
                converter, interceptorRegistry, contextPropagator);
    }

    // ========== 消费上下文自动恢复（原生 @RocketMQMessageListener 容器级） ==========

    @Bean
    public MqRocketContainerContextBinder mqRocketContainerContextBinder(MqContextPropagator contextPropagator) {
        return new MqRocketContainerContextBinder(contextPropagator);
    }
}
