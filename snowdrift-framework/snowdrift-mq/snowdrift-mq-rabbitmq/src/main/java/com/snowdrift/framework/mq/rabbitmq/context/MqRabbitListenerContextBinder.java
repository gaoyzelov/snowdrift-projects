package com.snowdrift.framework.mq.rabbitmq.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * 把 {@link MqRabbitContextAdvice} 挂到所有 {@link SimpleRabbitListenerContainerFactory} 的 adviceChain 上
 * <p>
 * 以 {@link BeanPostProcessor} 形式在容器工厂初始化后、监听容器创建前设置 advice，
 * 使应用中所有原生 {@code @RabbitListener} 自动获得「恢复上下文 → 调用 → 清理」的容器级语义。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/9/4
 * @since 1.0.0
 */
@Slf4j
public class MqRabbitListenerContextBinder implements BeanPostProcessor {

    private final MqRabbitContextAdvice contextAdvice;

    public MqRabbitListenerContextBinder(MqRabbitContextAdvice contextAdvice) {
        this.contextAdvice = contextAdvice;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SimpleRabbitListenerContainerFactory factory) {
            factory.setAdviceChain(contextAdvice);
            log.debug("已为 RabbitMQ 监听容器工厂挂载 MQ 上下文 Advice: {}", beanName);
        }
        return bean;
    }
}
