package com.snowdrift.doc.knife4j.processor;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import springfox.documentation.spring.web.plugins.WebMvcRequestHandlerProvider;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ApiDocBeanPostProcessor
 *
 * @author gaoye
 * @date 2025/03/18 17:43:30
 * @description Knife4j 启动报错处理
 * @since 1.0.0
 */
public class ApiDocBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof WebMvcRequestHandlerProvider) {
            List<RequestMappingInfoHandlerMapping> handlerMappings = getHandlerMappings(bean);
            if (CollectionUtils.isNotEmpty(handlerMappings)) {
                handlerMappings.removeIf(mapping -> Objects.nonNull(mapping.getPatternParser()));
            }
        }
        return bean;
    }

    /**
     * 获取handlerMappings
     *
     * @param bean Bean
     * @return 处理器映射列表
     */
    @SuppressWarnings("unchecked")
    private List<RequestMappingInfoHandlerMapping> getHandlerMappings(Object bean) {
        try {
            Field field = ReflectionUtils.findField(bean.getClass(), "handlerMappings");
            if (Objects.nonNull(field)) {
                field.setAccessible(true);
                return (List<RequestMappingInfoHandlerMapping>) field.get(bean);
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return Collections.emptyList();
    }
}