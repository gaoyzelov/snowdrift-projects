package com.snowdrift.core.utils;

import com.snowdrift.core.exception.BaseException;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;
import java.util.Objects;

/**
 * SpringUtil
 *
 * @author gaoye
 * @date 2025/03/25 13:44:37
 * @description Spring 工具类
 * @since 1.0.0
 */
@SuppressWarnings("all")
public class SpringUtil implements BeanFactoryPostProcessor, ApplicationContextAware {

    private static ConfigurableListableBeanFactory beanFactory;
    private static ApplicationContext applicationContext;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        SpringUtil.beanFactory = beanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtil.applicationContext = applicationContext;
    }

    /**
     * 获取{@link ApplicationContext}
     *
     * @return {@link ApplicationContext}
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 获取{@link ListableBeanFactory}，可能为{@link ConfigurableListableBeanFactory} 或 {@link ApplicationContextAware}
     *
     * @return {@link ListableBeanFactory}
     */
    public static ListableBeanFactory getBeanFactory() {
        final ListableBeanFactory factory = null == beanFactory ? applicationContext : beanFactory;
        if (null == factory) {
            throw new BaseException("ConfigurableListableBeanFactory或ApplicationContext对象没有注入，请检查是否为Spring环境！");
        }
        return factory;
    }

    /**
     * 获取{@link ConfigurableListableBeanFactory}
     *
     * @return {@link ConfigurableListableBeanFactory}
     * @throws BaseException 当上下文非ConfigurableListableBeanFactory抛出异常
     */
    public static ConfigurableListableBeanFactory getConfigurableBeanFactory() throws BaseException {
        final ConfigurableListableBeanFactory factory;
        if (Objects.nonNull(beanFactory)) {
            factory = beanFactory;
        } else if (applicationContext instanceof ConfigurableApplicationContext) {
            factory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
        } else {
            throw new BaseException("从上下文中找不到ConfigurableListableBeanFactory对象！");
        }
        return factory;
    }

    /**
     * 通过name获取 Bean
     *
     * @param <T>  Bean类型
     * @param name Bean名称
     * @return Bean
     */
    public static <T> T getBean(String name) {
        return (T) getBeanFactory().getBean(name);
    }

    /**
     * 通过class获取Bean
     *
     * @param <T>   Bean类型
     * @param clazz Bean类
     * @return Bean对象
     */
    public static <T> T getBean(Class<T> clazz) {
        return getBeanFactory().getBean(clazz);
    }

    /**
     * 通过name,以及Clazz返回指定的Bean
     *
     * @param <T>   bean类型
     * @param name  Bean名称
     * @param clazz bean类型
     * @return Bean对象
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return getBeanFactory().getBean(name, clazz);
    }

    /**
     * 获取指定类型对应的所有Bean，包括子类
     *
     * @param <T>  Bean类型
     * @param type 类、接口，null表示获取所有bean
     * @return 类型对应的bean，key是bean注册的name，value是Bean
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> type) {
        return getBeanFactory().getBeansOfType(type);
    }

    /**
     * 获取指定类型对应的Bean名称，包括子类
     *
     * @param type 类、接口，null表示获取所有bean名称
     * @return bean名称
     */
    public static String[] getBeanNamesForType(Class<?> type) {
        return getBeanFactory().getBeanNamesForType(type);
    }

    /**
     * 获取配置文件配置项的值
     *
     * @param key 配置项key
     * @return 属性值
     */
    public static String getProperty(String key) {
        if (Objects.isNull(applicationContext)) {
            return null;
        }
        return applicationContext.getEnvironment().getProperty(key);
    }

    /**
     * 获取配置文件配置项的值
     *
     * @param key          配置项key
     * @param defaultValue 默认值
     * @return 属性值
     */
    public static String getProperty(String key, String defaultValue) {
        if (Objects.isNull(applicationContext)) {
            return null;
        }
        return applicationContext.getEnvironment().getProperty(key, defaultValue);
    }

    /**
     * 获取配置文件配置项的值
     *
     * @param <T>          属性值类型
     * @param key          配置项key
     * @param targetType   配置项类型
     * @param defaultValue 默认值
     * @return 属性值
     */
    public static <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        if (Objects.isNull(applicationContext)) {
            return null;
        }
        return applicationContext.getEnvironment().getProperty(key, targetType, defaultValue);
    }

    /**
     * 获取应用程序名称
     *
     * @return 应用程序名称
     */
    public static String getApplicationName() {
        return getProperty("spring.application.name");
    }

    /**
     * 获取当前的环境配置，无配置返回null
     *
     * @return 当前的环境配置
     */
    public static String[] getActiveProfiles() {
        if (null == applicationContext) {
            return null;
        }
        return applicationContext.getEnvironment().getActiveProfiles();
    }

    /**
     * 获取当前的环境配置，当有多个环境配置时，只获取第一个
     *
     * @return 当前的环境配置
     */
    public static String getActiveProfile() {
        final String[] activeProfiles = getActiveProfiles();
        return ArrayUtils.isNotEmpty(activeProfiles) ? activeProfiles[0] : null;
    }

    /**
     * 动态向Spring注册Bean
     * <p>
     * 由{@link org.springframework.beans.factory.BeanFactory} 实现，通过工具开放API
     * <p>
     *
     * @param <T>      Bean类型
     * @param beanName 名称
     * @param bean     bean
     */
    public static <T> void registerBean(String beanName, T bean) {
        final ConfigurableListableBeanFactory factory = getConfigurableBeanFactory();
        factory.autowireBean(bean);
        factory.registerSingleton(beanName, bean);
    }

    /**
     * 注销bean
     * <p>
     * 将Spring中的bean注销，请谨慎使用
     *
     * @param beanName bean名称
     */
    public static void unregisterBean(String beanName) {
        final ConfigurableListableBeanFactory factory = getConfigurableBeanFactory();
        if (factory instanceof DefaultSingletonBeanRegistry) {
            DefaultSingletonBeanRegistry registry = (DefaultSingletonBeanRegistry) factory;
            registry.destroySingleton(beanName);
        } else {
            throw new BaseException("Can not unregister bean, the factory is not a DefaultSingletonBeanRegistry!");
        }
    }

    /**
     * 发布事件
     *
     * @param event 待发布的事件
     */
    public static void publishEvent(ApplicationEvent event) {
        if (Objects.nonNull(applicationContext)) {
            applicationContext.publishEvent(event);
        }
    }

    /**
     * 发布事件
     * Spring 4.2+ 版本事件可以不再是{@link ApplicationEvent}的子类
     *
     * @param event 待发布的事件
     */
    public static void publishEvent(Object event) {
        if (Objects.nonNull(applicationContext)) {
            applicationContext.publishEvent(event);
        }
    }
}