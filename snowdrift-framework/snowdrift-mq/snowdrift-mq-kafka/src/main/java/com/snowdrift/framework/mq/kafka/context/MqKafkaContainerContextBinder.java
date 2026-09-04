package com.snowdrift.framework.mq.kafka.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.kafka.config.AbstractKafkaListenerContainerFactory;

/**
 * 把 {@link MqKafkaContextRecordInterceptor} 挂到所有 {@link AbstractKafkaListenerContainerFactory} 上
 * <p>
 * 以 {@link BeanPostProcessor} 形式在容器工厂初始化后、监听容器创建前设置拦截器，
 * 使应用中所有原生 {@code @KafkaListener} 监听自动获得上下文恢复能力。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/9/4
 * @since 1.0.0
 */
@Slf4j
public class MqKafkaContainerContextBinder implements BeanPostProcessor {

    private final MqKafkaContextRecordInterceptor recordInterceptor;

    public MqKafkaContainerContextBinder(MqKafkaContextRecordInterceptor recordInterceptor) {
        this.recordInterceptor = recordInterceptor;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof AbstractKafkaListenerContainerFactory) {
            @SuppressWarnings({"rawtypes", "unchecked"})
            AbstractKafkaListenerContainerFactory factory = (AbstractKafkaListenerContainerFactory) bean;
            factory.setRecordInterceptor(recordInterceptor);
            log.debug("已为 Kafka 监听容器工厂挂载 MQ 上下文拦截器: {}", beanName);
        }
        return bean;
    }
}
