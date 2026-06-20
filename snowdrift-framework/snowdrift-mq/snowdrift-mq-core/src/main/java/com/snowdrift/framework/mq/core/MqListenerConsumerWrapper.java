package com.snowdrift.framework.mq.core;

import com.alibaba.fastjson2.JSON;
import com.snowdrift.framework.mq.annotation.MqListener;
import com.snowdrift.framework.mq.exception.MqException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.messaging.Message;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * MQ 监听器消费包装器 — 实现 {@link Consumer}，由 {@link MqListenerBeanDefinitionRegistrar} 动态注册为 Bean
 * <p>
 * 生命周期：context restore → FastJson2 反序列化 → 反射调用用户方法 → 异常翻译为 MqException → finally context clear
 * </p>
 *
 * @author 83674
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class MqListenerConsumerWrapper implements Consumer<Message<byte[]>>, ApplicationContextAware {

    private final String targetBeanName;
    private final Method targetMethod;
    private final Class<?> targetParameterType;
    private final MqListener config;
    private ApplicationContext applicationContext;

    public MqListenerConsumerWrapper(String targetBeanName, Method targetMethod,
                                      Class<?> targetParameterType, MqListener config) {
        this.targetBeanName = targetBeanName;
        this.targetMethod = targetMethod;
        this.targetParameterType = targetParameterType;
        this.config = config;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void accept(Message<byte[]> message) {
        // 1. 恢复 TTL 上下文（traceId, SecurityContext）
        MqContextPropagator.restore(message);

        try {
            // 2. 反序列化 payload
            String typeName = MqContextPropagator.getOriginalType(message);
            Class<?> targetType;
            if (StringUtils.isNotBlank(typeName)) {
                try {
                    targetType = Class.forName(typeName, true, Thread.currentThread().getContextClassLoader());
                } catch (ClassNotFoundException e) {
                    log.warn("原始类型不存在，回退到方法参数类型: typeName={}", typeName);
                    targetType = targetParameterType;
                }
            } else {
                targetType = targetParameterType;
            }

            byte[] body = message.getPayload();
            Object payload;
            if (byte[].class.equals(targetType)) {
                // 如果用户方法参数是 byte[]，直接透传
                payload = body;
            } else if (String.class.equals(targetType)) {
                // 如果用户方法参数是 String，用 UTF-8 转
                payload = new String(body, java.nio.charset.StandardCharsets.UTF_8);
            } else {
                payload = JSON.parseObject(body, targetType);
            }

            // 3. 调用用户方法
            Object targetBean = applicationContext.getBean(targetBeanName);
            targetMethod.invoke(targetBean, payload);

        } catch (MqException e) {
            throw e;
        } catch (Exception e) {
            log.error("消息消费失败: topic={}, group={}", config.topic(), config.group(), e);
            throw new MqException("mq.consume.failed",
                    new Object[]{config.topic(), e.getMessage()}, e);
        } finally {
            // 4. 清除上下文
            MqContextPropagator.clear();
        }
    }
}
