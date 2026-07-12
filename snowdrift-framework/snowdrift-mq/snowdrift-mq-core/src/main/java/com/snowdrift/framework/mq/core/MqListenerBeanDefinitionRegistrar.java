package com.snowdrift.framework.mq.core;

import com.snowdrift.framework.mq.annotation.MqListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MQ 监听器 Bean 注册器 — 扫描 {@link MqListener} 注解，动态注册 Consumer Bean
 * <p>
 * 实现 {@link BeanDefinitionRegistryPostProcessor}，在 Spring 容器刷新前扫描所有 BeanDefinition，
 * 为每个 {@code @MqListener} 方法自动注册对应的 SCS Consumer Bean 和 binding 配置。
 * </p>
 *
 * @author gaoyzelov
 * @date 2026/6/20
 * @since 1.0.0
 */
@Slf4j
public class MqListenerBeanDefinitionRegistrar implements BeanDefinitionRegistryPostProcessor,
        EnvironmentAware {

    private ConfigurableEnvironment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment) environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Map<String, Object> dynamicProperties = new HashMap<>();
        List<String> functionNames = new ArrayList<>();

        String[] beanNames = registry.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            BeanDefinition bd = registry.getBeanDefinition(beanName);
            if (bd.getBeanClassName() == null) {
                continue;
            }
            try {
                Class<?> beanClass = Class.forName(bd.getBeanClassName(), false,
                        Thread.currentThread().getContextClassLoader());
                // 扫描当前类及父类的方法
                for (Method method : beanClass.getDeclaredMethods()) {
                    MqListener annotation = method.getAnnotation(MqListener.class);
                    if (annotation != null) {
                        registerConsumerBean(registry, beanName, method, annotation,
                                dynamicProperties, functionNames);
                    }
                }
            } catch (ClassNotFoundException e) {
                log.debug("Bean 类加载失败，跳过扫描: {}", beanName);
            }
        }

        // 注册 function.definition，确保 SCS 激活所有 Consumer Bean
        if (!functionNames.isEmpty()) {
            String existing = environment.getProperty("spring.cloud.function.definition");
            String definition = String.join(";", functionNames);
            if (StringUtils.isNotBlank(existing)) {
                definition = existing + ";" + definition;
            }
            dynamicProperties.put("spring.cloud.function.definition", definition);
        }

        // 一次性注入所有动态 binding 配置
        if (!dynamicProperties.isEmpty()) {
            environment.getPropertySources()
                    .addFirst(new MapPropertySource("snowdrift-mq-dynamic-bindings", dynamicProperties));
            log.info("已注册 {} 个 @MqListener Consumer: {}",
                    functionNames.size(), functionNames);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // no-op：Bean 注册已在 postProcessBeanDefinitionRegistry 完成
    }

    /**
     * 为单个 @MqListener 方法注册 Consumer Bean + binding 配置
     */
    private void registerConsumerBean(BeanDefinitionRegistry registry, String beanName,
                                       Method method, MqListener annotation,
                                       Map<String, Object> dynamicProperties,
                                       List<String> functionNames) {
        // 校验方法签名：必须有且只有一个参数
        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length != 1) {
            log.warn("@MqListener 方法签名无效（需要恰好 1 个参数），跳过: {}.{}",
                    beanName, method.getName());
            return;
        }
        Class<?> paramType = paramTypes[0];

        String functionName = "mqListener_" + beanName + "_" + method.getName();
        functionNames.add(functionName);
        String bindingName = functionName + "-in-0";

        // 1. 注册 Consumer Bean
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
                .genericBeanDefinition(MqListenerConsumerWrapper.class);
        builder.addConstructorArgValue(beanName);
        builder.addConstructorArgValue(method);
        builder.addConstructorArgValue(paramType);
        builder.addConstructorArgValue(annotation);
        builder.setLazyInit(false);
        builder.addDependsOn(beanName); // 确保目标 Bean 先于 Consumer 初始化

        registry.registerBeanDefinition(functionName, builder.getBeanDefinition());

        // 2. 收集 binding 配置
        dynamicProperties.put("spring.cloud.stream.bindings." + bindingName + ".destination",
                annotation.topic());
        if (StringUtils.isNotBlank(annotation.group())) {
            dynamicProperties.put("spring.cloud.stream.bindings." + bindingName + ".group",
                    annotation.group());
        }
        dynamicProperties.put("spring.cloud.stream.bindings." + bindingName + ".consumer.max-attempts",
                String.valueOf(annotation.maxRetry()));
        dynamicProperties.put("spring.cloud.stream.bindings." + bindingName + ".consumer.concurrency",
                String.valueOf(annotation.concurrency()));

        log.info("@MqListener 注册成功: {}.{} → topic={}, group={}, concurrency={}",
                beanName, method.getName(), annotation.topic(), annotation.group(), annotation.concurrency());
    }
}
