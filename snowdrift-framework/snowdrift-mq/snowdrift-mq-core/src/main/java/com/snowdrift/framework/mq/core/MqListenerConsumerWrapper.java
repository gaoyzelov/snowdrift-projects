package com.snowdrift.framework.mq.core;

import com.snowdrift.framework.mq.annotation.MqListener;
import com.snowdrift.framework.mq.exception.MqException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.messaging.Message;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * MQ 监听器消费包装器 — 实现 {@link Consumer}，由 {@link MqListenerBeanDefinitionRegistrar} 动态注册为 Bean
 * <p>
 * 生命周期：context restore → 反序列化 → MethodHandle 调用用户方法 → 异常翻译为 MqException → finally context clear
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class MqListenerConsumerWrapper implements Consumer<Message<byte[]>>, ApplicationContextAware {

    private final String targetBeanName;
    private final Class<?> targetParameterType;
    private final MqListener config;
    private final MethodHandle methodHandle;
    private ApplicationContext applicationContext;
    private volatile MqMessageConverter converter;
    private volatile MqContextPropagator contextPropagator;
    private volatile Object cachedTargetBean;

    public MqListenerConsumerWrapper(String targetBeanName, Method targetMethod,
                                      Class<?> targetParameterType, MqListener config) {
        this.targetBeanName = targetBeanName;
        this.targetParameterType = targetParameterType;
        this.config = config;

        // 预编译 Method → MethodHandle，避免每次消费都走反射
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
                    targetMethod.getDeclaringClass(), MethodHandles.lookup());
            this.methodHandle = lookup.unreflect(targetMethod);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "无法创建 MethodHandle: " + targetMethod.getName(), e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void accept(Message<byte[]> message) {
        long start = System.currentTimeMillis();
        // 1. 恢复 TTL 上下文（traceId, SecurityContext）
        getContextPropagator().restore(message);

        try {
            // 2. 反序列化 payload（通过 MqMessageConverter SPI）
            byte[] body = message.getPayload();
            Object payload;
            if (byte[].class.equals(targetParameterType)) {
                payload = body;
            } else {
                try {
                    payload = getConverter().deserialize(body, targetParameterType);
                } catch (Exception e) {
                    log.error("消息反序列化失败: topic={}, group={}, type={}",
                            config.topic(), config.group(), targetParameterType.getName(), e);
                    throw new MqException("mq.deserialize.failed",
                            new Object[]{targetParameterType.getName()}, e);
                }
            }

            // 3. MethodHandle 调用用户方法
            Object targetBean = getTargetBean();
            methodHandle.invoke(targetBean, payload);

        } catch (MqException e) {
            throw e;
        } catch (Throwable e) {
            log.error("消息消费失败: topic={}, group={}", config.topic(), config.group(), e);
            throw new MqException("mq.consume.failed",
                    new Object[]{config.topic(), e.getMessage()}, e);
        } finally {
            // 4. 清除上下文
            getContextPropagator().clear();
        }
    }

    /**
     * 懒加载并发安全的 MqMessageConverter 获取
     */
    private MqMessageConverter getConverter() {
        if (this.converter == null) {
            synchronized (this) {
                if (this.converter == null) {
                    this.converter = applicationContext.getBean(MqMessageConverter.class);
                }
            }
        }
        return this.converter;
    }

    /**
     * 懒加载并发安全的 MqContextPropagator 获取
     */
    private MqContextPropagator getContextPropagator() {
        if (this.contextPropagator == null) {
            synchronized (this) {
                if (this.contextPropagator == null) {
                    this.contextPropagator = applicationContext.getBean(MqContextPropagator.class);
                }
            }
        }
        return this.contextPropagator;
    }

    /**
     * 懒加载目标 Bean 实例（缓存后不再每次查询 ApplicationContext）
     */
    private Object getTargetBean() {
        if (this.cachedTargetBean == null) {
            synchronized (this) {
                if (this.cachedTargetBean == null) {
                    this.cachedTargetBean = applicationContext.getBean(targetBeanName);
                }
            }
        }
        return this.cachedTargetBean;
    }
}
