package com.snowdrift.framework.mq.rocketmq.context;

import com.snowdrift.framework.mq.context.MqContextPropagator;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * 为每个 {@link DefaultRocketMQListenerContainer} Bean 织入 {@link MqRocketContextInterceptor}
 * <p>
 * rocketmq-spring 的消费监听器每条消息都会从 ApplicationContext 按名取回容器并调用
 * {@code handleMessage(MessageExt)}；这里在容器 Bean 初始化后用 CGLIB 代理包一层，
 * 使所有原生 {@code @RocketMQMessageListener} 自动获得「恢复上下文 → 调用 → 清理」。
 * 其余方法（生命周期等）均委托给原容器，语义不受影响。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/9/4
 * @since 1.0.0
 */
@Slf4j
public class MqRocketContainerContextBinder implements BeanPostProcessor {

    private final MqContextPropagator contextPropagator;

    public MqRocketContainerContextBinder(MqContextPropagator contextPropagator) {
        this.contextPropagator = contextPropagator;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DefaultRocketMQListenerContainer && !AopUtils.isAopProxy(bean)) {
            ProxyFactory factory = new ProxyFactory();
            factory.setTarget(bean);
            factory.setProxyTargetClass(true);
            factory.addAdvice(new MqRocketContextInterceptor(contextPropagator));
            log.debug("已为 RocketMQ 监听容器织入 MQ 上下文拦截器: {}", beanName);
            return factory.getProxy(bean.getClass().getClassLoader());
        }
        return bean;
    }
}
