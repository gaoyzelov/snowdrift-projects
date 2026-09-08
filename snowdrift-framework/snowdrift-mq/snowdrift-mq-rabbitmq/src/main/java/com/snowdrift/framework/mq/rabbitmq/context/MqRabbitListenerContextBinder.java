package com.snowdrift.framework.mq.rabbitmq.context;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.aop.Advice;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Arrays;

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
            // 合并既有 advice chain（Boot/用户重试、恢复器等），不能整体覆盖
            Advice[] existingChain = factory.getAdviceChain();
            Advice[] mergedChain;
            if (ArrayUtils.isEmpty(existingChain)) {
                mergedChain = new Advice[]{contextAdvice};
            } else {
                mergedChain = Arrays.copyOf(existingChain, existingChain.length + 1);
                mergedChain[existingChain.length] = contextAdvice;
            }
            factory.setAdviceChain(mergedChain);
            log.debug("已为 RabbitMQ 监听容器工厂挂载 MQ 上下文 Advice: {}", beanName);
        }
        return bean;
    }
}
