package com.snowdrift.framework.mq.kafka.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.kafka.config.AbstractKafkaListenerContainerFactory;
import org.springframework.kafka.listener.BatchInterceptor;
import org.springframework.kafka.listener.CompositeBatchInterceptor;
import org.springframework.kafka.listener.CompositeRecordInterceptor;
import org.springframework.kafka.listener.RecordInterceptor;

import java.lang.reflect.Field;

/**
 * 把 MQ 上下文拦截器挂到所有 {@link AbstractKafkaListenerContainerFactory} 上
 * <p>
 * 以 {@link BeanPostProcessor} 形式在容器工厂初始化后、监听容器创建前设置拦截器，
 * 使应用中所有原生 {@code @KafkaListener} 监听自动获得上下文处理能力。同时挂载两个：
 * </p>
 * <ul>
 *   <li>{@link RecordInterceptor}（{@link MqKafkaContextRecordInterceptor}）—— 逐记录消费时还原并清理上下文；</li>
 *   <li>{@link BatchInterceptor}（{@link MqKafkaContextBatchInterceptor}）—— 批消费
 *       （{@code batchListener = true}）时只做清理，不还原（原因见该类 javadoc）。</li>
 * </ul>
 * <p>
 * 两者设置时都与工厂既有的（Boot/用户）拦截器合并成组合拦截器，避免整体覆盖。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/9/4
 * @since 1.0.0
 */
@Slf4j
public class MqKafkaContainerContextBinder implements BeanPostProcessor {

    /**
     * spring-kafka 未暴露拦截器 getter（仅 setter），只能反射读取私有字段，以便与用户/Boot 已配置的拦截器组合而非覆盖
     */
    private static final String RECORD_INTERCEPTOR_FIELD = "recordInterceptor";
    private static final String BATCH_INTERCEPTOR_FIELD = "batchInterceptor";

    private final MqKafkaContextRecordInterceptor recordInterceptor;
    private final MqKafkaContextBatchInterceptor batchInterceptor;

    public MqKafkaContainerContextBinder(MqKafkaContextRecordInterceptor recordInterceptor,
                                         MqKafkaContextBatchInterceptor batchInterceptor) {
        this.recordInterceptor = recordInterceptor;
        this.batchInterceptor = batchInterceptor;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof AbstractKafkaListenerContainerFactory factory) {
            bindRecordInterceptor(factory);
            bindBatchInterceptor(factory);
            log.debug("已为 Kafka 监听容器工厂挂载 MQ 上下文拦截器（记录级 + 批级）: {}", beanName);
        }
        return bean;
    }

    /**
     * 挂载记录级拦截器：逐记录消费（{@code batchListener = false}）时还原并清理上下文
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void bindRecordInterceptor(AbstractKafkaListenerContainerFactory factory) {
        RecordInterceptor existing = readInterceptorField(factory, RECORD_INTERCEPTOR_FIELD);
        if (existing == null) {
            factory.setRecordInterceptor(recordInterceptor);
        } else {
            factory.setRecordInterceptor(new CompositeRecordInterceptor(existing, recordInterceptor));
        }
    }

    /**
     * 挂载批级拦截器：批消费（{@code batchListener = true}）时清理上下文
     * <p>若不挂载，批消费既不还原也不清理——容器线程上残留的上下文会被后续复用。</p>
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void bindBatchInterceptor(AbstractKafkaListenerContainerFactory factory) {
        BatchInterceptor existing = readInterceptorField(factory, BATCH_INTERCEPTOR_FIELD);
        if (existing == null) {
            factory.setBatchInterceptor(batchInterceptor);
        } else {
            factory.setBatchInterceptor(new CompositeBatchInterceptor(existing, batchInterceptor));
        }
    }

    /**
     * 反射读取容器工厂的拦截器私有字段
     * <p>读取失败时降级为"仅挂载本框架拦截器"，并输出告警——一旦 spring-kafka 改了字段名，
     * 这里会暴露出来，而不是静默失去组合能力。</p>
     *
     * @param factory   容器工厂
     * @param fieldName 字段名（{@code recordInterceptor} 或 {@code batchInterceptor}）
     * @return 既有拦截器；不存在或读取失败时返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    private <T> T readInterceptorField(AbstractKafkaListenerContainerFactory factory, String fieldName) {
        try {
            Field field = AbstractKafkaListenerContainerFactory.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(factory);
        } catch (ReflectiveOperationException e) {
            log.warn("读取 Kafka 监听容器工厂既有 {} 失败，仅挂载 MQ 上下文拦截器: {}", fieldName, e.toString());
            return null;
        }
    }
}
