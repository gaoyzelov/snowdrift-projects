package com.snowdrift.framework.mq.kafka.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.kafka.config.AbstractKafkaListenerContainerFactory;
import org.springframework.kafka.listener.CompositeRecordInterceptor;
import org.springframework.kafka.listener.RecordInterceptor;

import java.lang.reflect.Field;

/**
 * 把 {@link MqKafkaContextRecordInterceptor} 挂到所有 {@link AbstractKafkaListenerContainerFactory} 上
 * <p>
 * 以 {@link BeanPostProcessor} 形式在容器工厂初始化后、监听容器创建前设置拦截器，
 * 使应用中所有原生 {@code @KafkaListener} 监听自动获得上下文恢复能力。
 * 设置时与工厂既有的（Boot/用户）RecordInterceptor 合并成组合拦截器，避免整体覆盖。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/9/4
 * @since 1.0.0
 */
@Slf4j
public class MqKafkaContainerContextBinder implements BeanPostProcessor {

    private static final String RECORD_INTERCEPTOR_FIELD = "recordInterceptor";

    private final MqKafkaContextRecordInterceptor recordInterceptor;

    public MqKafkaContainerContextBinder(MqKafkaContextRecordInterceptor recordInterceptor) {
        this.recordInterceptor = recordInterceptor;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof AbstractKafkaListenerContainerFactory factory) {
            RecordInterceptor existing = readExistingInterceptor(factory);
            if (existing == null) {
                factory.setRecordInterceptor(recordInterceptor);
            } else {
                factory.setRecordInterceptor(new CompositeRecordInterceptor(existing, recordInterceptor));
            }
            log.debug("已为 Kafka 监听容器工厂挂载 MQ 上下文拦截器: {}", beanName);
        }
        return bean;
    }

    /**
     * spring-kafka 的 {@link AbstractKafkaListenerContainerFactory} 未暴露拦截器 getter（仅 setter），
     * 只能反射读取其私有字段，以便与用户/Boot 已配置的 RecordInterceptor 组合而非覆盖。
     * 读取失败时降级为仅挂载本框架拦截器（等价于原行为）。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private RecordInterceptor readExistingInterceptor(AbstractKafkaListenerContainerFactory factory) {
        try {
            Field field = AbstractKafkaListenerContainerFactory.class.getDeclaredField(RECORD_INTERCEPTOR_FIELD);
            field.setAccessible(true);
            return (RecordInterceptor) field.get(factory);
        } catch (ReflectiveOperationException e) {
            log.warn("读取 Kafka 监听容器工厂既有 RecordInterceptor 失败，仅挂载 MQ 上下文拦截器: {}", e.toString());
            return null;
        }
    }
}
